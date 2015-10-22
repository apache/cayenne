/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.di.spi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The implementation here is basically an adjacency list, but a {@link Map} is
 * used to map each vertex to its list of adjacent vertices.
 *
 * @param <V>
 *            A type of a vertex.
 * @since 4.0
 */
class DIGraph<V> {

	/**
	 * Note: {@link LinkedHashMap} is used for supporting insertion order.
	 */
	private Map<V, List<V>> neighbors = new LinkedHashMap<>();

	public DIGraph() {
	}

	/**
	 * Add a vertex to the graph. Nothing happens if vertex is already in graph.
	 */
	public void add(V vertex) {
		if (neighbors.containsKey(vertex)) {
			return;
		}

		neighbors.put(vertex, new ArrayList<V>());
	}

	/**
	 * Add an edge to the graph; if either vertex does not exist, it's added.
	 * This implementation allows the creation of multi-edges and self-loops.
	 */
	public void add(V from, V to) {
		this.add(from);
		this.add(to);
		neighbors.get(from).add(to);
	}

	/**
	 * True iff graph contains vertex.
	 */
	public boolean contains(V vertex) {
		return neighbors.containsKey(vertex);
	}

	/**
	 * Remove an edge from the graph. Nothing happens if no such edge.
	 *
	 * @throws IllegalArgumentException
	 *             if either vertex doesn't exist.
	 */
	public void remove(V from, V to) {
		if (!(this.contains(from) && this.contains(to)))
			throw new IllegalArgumentException("Nonexistent vertex");

		neighbors.get(from).remove(to);
	}

	/**
	 * Return (as a Map) the out-degree of each vertex.
	 */
	public Map<V, Integer> outDegree() {
		Map<V, Integer> result = new LinkedHashMap<>();

		for (Map.Entry<V, List<V>> entry : neighbors.entrySet()) {
			result.put(entry.getKey(), entry.getValue().size());
		}

		return result;
	}

	/**
	 * Return (as a Map) the in-degree of each vertex.
	 */
	public Map<V, Integer> inDegree() {
		Map<V, Integer> result = new LinkedHashMap<>();

		for (V v : neighbors.keySet()) {
			result.put(v, 0);
		}

		for (V from : neighbors.keySet()) {
			for (V to : neighbors.get(from)) {
				result.put(to, result.get(to) + 1);
			}
		}

		return result;
	}

	/**
	 * Return (as a List) the topological sort of the vertices; null for no such
	 * sort.
	 */
	public List<V> topSort() {
		Map<V, Integer> degree = inDegree();
		Deque<V> zeroDegree = new ArrayDeque<>();
		LinkedList<V> result = new LinkedList<>();

		for (Map.Entry<V, Integer> entry : degree.entrySet()) {
			if (entry.getValue() == 0) {
				zeroDegree.push(entry.getKey());
			}
		}

		while (!zeroDegree.isEmpty()) {
			V v = zeroDegree.pop();
			result.push(v);

			for (V neighbor : neighbors.get(v)) {
				degree.put(neighbor, degree.get(neighbor) - 1);
				if (degree.get(neighbor) == 0) {
					zeroDegree.push(neighbor);
				}
			}
		}

		// Check that we have used the entire graph (if not, there was a cycle)
		if (result.size() != neighbors.size()) {
			return null;
		}

		return result;
	}

	/**
	 * String representation of graph.
	 */
	public String toString() {
		StringBuffer s = new StringBuffer();

		for (Map.Entry<V, List<V>> entry : neighbors.entrySet()) {
			s.append("\n    " + entry.getKey() + " -> " + entry.getValue());
		}

		return s.toString();
	}

}