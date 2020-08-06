/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
/* ====================================================================
 *
 * Copyright(c) 2003, Andriy Shapochka
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * 3. Neither the name of the ASHWOOD nor the
 *    names of its contributors may be used to endorse or
 *    promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by
 * individuals on behalf of the ASHWOOD Project and was originally
 * created by Andriy Shapochka.
 *
 */
package org.apache.cayenne.ashwood.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @since 3.1
 */
public class StrongConnection<E, V> implements Iterator<Collection<E>> {

	private DigraphIteration<E, V> digraph;
	private DigraphIteration<E, V> reverseDigraph;
	private DigraphIteration<E, V> filteredDigraph;
	private DepthFirstStampSearch<E> directDfs;
	private DepthFirstSearch<E> reverseDfs;
	private Set<E> seen = new HashSet<E>();
	private Iterator<E> vertexIterator;
	private ArrayDeque<E> dfsStack;
	private DFSSeenVerticesPredicate reverseDFSFilter;

	public StrongConnection(DigraphIteration<E, V> digraph) {

		this.dfsStack = new ArrayDeque<>();
		this.reverseDFSFilter = new DFSSeenVerticesPredicate();
		this.digraph = digraph;
		this.filteredDigraph = new FilterIteration<>(digraph, new NotSeenPredicate(), arc -> true);
		this.reverseDigraph = new FilterIteration<>(new ReversedIteration<>(digraph), reverseDFSFilter, arc -> true);
		this.vertexIterator = filteredDigraph.vertexIterator();

		runDirectDFS();
	}

	@Override
	public boolean hasNext() {
		return !dfsStack.isEmpty();
	}

	@Override
	public Collection<E> next() {
		Collection<E> component = buildStronglyConnectedComponent();
		if (dfsStack.isEmpty()) {
			runDirectDFS();
		}
		return component;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Method remove() not supported.");
	}

	public Digraph<Collection<E>, Collection<V>> contract(Digraph<Collection<E>, Collection<V>> contractedDigraph) {

		Collection<Collection<E>> components = new ArrayList<>();
		while (this.hasNext()) {
			components.add(this.next());
		}

		Map<E, Collection<E>> memberToComponent = new HashMap<>();

		for (Collection<E> c : components) {
			for (E e : c) {
				memberToComponent.put(e, c);
			}
		}

		for (Collection<E> origin : components) {

			contractedDigraph.addVertex(origin);

			for (E member : origin) {

				for (ArcIterator<E, V> k = digraph.outgoingIterator(member); k.hasNext();) {
					V arc = k.next();
					E dst = k.getDestination();
					if (origin.contains(dst)) {
						continue;
					}
					Collection<E> destination = memberToComponent.get(dst);

					Collection<V> contractedArc = contractedDigraph.getArc(origin, destination);
					if (contractedArc == null) {
						contractedArc = Collections.singletonList(arc);
						contractedDigraph.putArc(origin, destination, contractedArc);
					} else {
						if (contractedArc.size() == 1) {
							Collection<V> tmp = contractedArc;
							contractedArc = new ArrayList<>();
							contractedArc.addAll(tmp);
							contractedDigraph.putArc(origin, destination, contractedArc);
						}
						contractedArc.add(arc);
					}

				}
			}
		}
		return contractedDigraph;
	}

	private E nextDFSRoot() {
		return vertexIterator.hasNext() ? vertexIterator.next() : null;
	}

	private boolean runDirectDFS() {
		dfsStack.clear();
		reverseDFSFilter.seenVertices.clear();
		E root = nextDFSRoot();
		if (root == null) {
			return false;
		}
		if (directDfs == null) {
			directDfs = new DepthFirstStampSearch<>(filteredDigraph, root);
		} else {
			directDfs.reset(root);
		}
		int stamp;
		E vertex;
		while (directDfs.hasNext()) {
			vertex = directDfs.next();
			stamp = directDfs.getStamp();
			if (stamp == DepthFirstStampSearch.SHRINK_STAMP || stamp == DepthFirstStampSearch.LEAF_STAMP) {
				// if (seen.add(vertex)) {
				dfsStack.push(vertex);
				reverseDFSFilter.seenVertices.add(vertex);
				// }
			}
		}
		seen.addAll(dfsStack);
		return true;
	}

	private Collection<E> buildStronglyConnectedComponent() {
		E root = (E) dfsStack.pop();
		Collection<E> component = Collections.singletonList(root);
		boolean singleton = true;
		if (reverseDfs == null) {
			reverseDfs = new DepthFirstSearch<>(reverseDigraph, root);
		} else {
			reverseDfs.reset(root);
		}
		while (reverseDfs.hasNext()) {
			E vertex = reverseDfs.next();
			if (vertex != root) {
				if (singleton) {
					Collection<E> tmp = component;
					component = new ArrayList<>();
					component.addAll(tmp);
					singleton = false;
				}
				component.add(vertex);
				dfsStack.remove(vertex);
			}
		}
		reverseDFSFilter.seenVertices.removeAll(component);
		return component;
	}

	private class DFSSeenVerticesPredicate implements Predicate<E> {

		private Set<E> seenVertices = new HashSet<>();

		@Override
		public boolean test(E vertex) {
			return seenVertices.contains(vertex);
		}
	}

	private class NotSeenPredicate implements Predicate<E> {

		@Override
		public boolean test(E vertex) {
			return !seen.contains(vertex);
		}
	}
}
