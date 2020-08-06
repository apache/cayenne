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

package org.apache.cayenne.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

/**
 * A GraphDiff that is a list of other GraphDiffs.
 * 
 * @since 1.2
 */
public class CompoundDiff implements GraphDiff {

	private static final long serialVersionUID = 5930690302335603082L;

	protected List<GraphDiff> diffs;

	/**
	 * Creates an empty CompoundDiff instance.
	 */
	public CompoundDiff() {
	}

	/**
	 * Creates CompoundDiff instance. Note that a List is not cloned in this
	 * constructor, so subsequent calls to add and addAll would modify the
	 * original list.
	 */
	public CompoundDiff(List<GraphDiff> diffs) {
		this.diffs = diffs;
	}

	/**
	 * Returns true if this diff has no other diffs or if all of its diffs are
	 * noops.
	 */
	public boolean isNoop() {
		if (diffs == null || diffs.isEmpty()) {
			return true;
		}

		for (GraphDiff diff : diffs) {
			if (!diff.isNoop()) {
				return false;
			}
		}
		return true;
	}

	public List<GraphDiff> getDiffs() {
		return (diffs != null) ? Collections.unmodifiableList(diffs) : Collections.<GraphDiff> emptyList();
	}

	public void add(GraphDiff diff) {
		nonNullDiffs().add(diff);
	}

	public void addAll(Collection<? extends GraphDiff> diffs) {
		nonNullDiffs().addAll(diffs);
	}

	/**
	 * Iterates over diffs list, calling "apply" on each individual diff.
	 */
	public void apply(GraphChangeHandler tracker) {
		if (diffs == null) {
			return;
		}

		// implements a naive linear commit - simply replay stored operations
		for (GraphDiff change : diffs) {
			change.apply(tracker);
		}
	}

	/**
	 * Iterates over diffs list in reverse order, calling "apply" on each
	 * individual diff.
	 */
	public void undo(GraphChangeHandler tracker) {
		if (diffs == null) {
			return;
		}

		ListIterator<GraphDiff> it = diffs.listIterator(diffs.size());
		while (it.hasPrevious()) {
			GraphDiff change = it.previous();
			change.undo(tracker);
		}
	}

	List<GraphDiff> nonNullDiffs() {
		if (diffs == null) {
			synchronized (this) {
				if (diffs == null) {
					diffs = new ArrayList<>();
				}
			}
		}

		return diffs;
	}
}
