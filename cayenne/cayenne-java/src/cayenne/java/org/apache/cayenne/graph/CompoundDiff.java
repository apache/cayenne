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

package org.apache.cayenne.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A GraphDiff that is a list of other GraphDiffs.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class CompoundDiff implements GraphDiff {

    protected List diffs;

    /**
     * Creates an empty CompoundDiff instance.
     */
    public CompoundDiff() {
    }

    /**
     * Creates CompoundDiff instance. Note that a List is not cloned in this constructor,
     * so subsequent calls to add and addAll would modify the original list.
     */
    public CompoundDiff(List diffs) {
        this.diffs = diffs;
    }

    /**
     * Returns true if this diff has no other diffs or if all of its diffs are noops.
     */
    public boolean isNoop() {
        if (diffs == null || diffs.isEmpty()) {
            return true;
        }

        Iterator it = diffs.iterator();
        while (it.hasNext()) {
            if (!((GraphDiff) it.next()).isNoop()) {
                return false;
            }
        }

        return true;
    }

    public List getDiffs() {
        return (diffs != null)
                ? Collections.unmodifiableList(diffs)
                : Collections.EMPTY_LIST;
    }

    public void add(GraphDiff diff) {
        nonNullDiffs().add(diff);
    }

    public void addAll(Collection diffs) {
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
        Iterator it = diffs.iterator();
        while (it.hasNext()) {
            GraphDiff change = (GraphDiff) it.next();
            change.apply(tracker);
        }
    }

    /**
     * Iterates over diffs list in reverse order, calling "apply" on each individual diff.
     */
    public void undo(GraphChangeHandler tracker) {
        if (diffs == null) {
            return;
        }

        ListIterator it = diffs.listIterator(diffs.size());
        while (it.hasPrevious()) {
            GraphDiff change = (GraphDiff) it.previous();
            change.undo(tracker);
        }
    }

    List nonNullDiffs() {
        if (diffs == null) {
            synchronized (this) {
                if (diffs == null) {
                    diffs = new ArrayList();
                }
            }
        }

        return diffs;
    }
}
