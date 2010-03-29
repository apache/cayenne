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

package org.apache.cayenne;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.graph.ArcCreateOperation;
import org.apache.cayenne.graph.ArcDeleteOperation;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeDiff;

/**
 * Stores graph operations in the order they were performed, optionally allowing to set
 * named markers.
 * 
 * @since 1.2
 */
class ObjectContextChangeLog {

    List<GraphDiff> diffs;
    Map<String, Integer> markers;

    ObjectContextChangeLog() {
        reset();
    }

    void unregisterNode(Object nodeId) {
        Iterator<?> it = diffs.iterator();
        while (it.hasNext()) {
            Object next = it.next();

            if (next instanceof NodeDiff) {

                if (nodeId.equals(((NodeDiff) next).getNodeId())) {
                    it.remove();
                }
                else if (next instanceof ArcCreateOperation) {
                    if (nodeId.equals(((ArcCreateOperation) next).getTargetNodeId())) {
                        it.remove();
                    }
                }
                else if (next instanceof ArcDeleteOperation) {
                    if (nodeId.equals(((ArcDeleteOperation) next).getTargetNodeId())) {
                        it.remove();
                    }
                }
            }
        }
    }

    void setMarker(String markerTag) {
        markers.put(markerTag, diffs.size());
    }

    void removeMarker(String markerTag) {
        markers.remove(markerTag);
    }

    /**
     * Returns a combined GraphDiff for all recorded operations.
     */
    GraphDiff getDiffs() {
        return new CompoundDiff(immutableList(0, diffs.size()));
    }

    GraphDiff getDiffsAfterMarker(String markerTag) {
        Integer pos = markers.get(markerTag);
        int marker = (pos == null) ? -1 : pos.intValue();
        if (marker < 0) {
            throw new IllegalStateException("No marked position for tag '"
                    + markerTag
                    + "'");
        }

        return new CompoundDiff(immutableList(marker, diffs.size()));
    }

    boolean hasMarker(String markerTag) {
        return markers.containsKey(markerTag);
    }

    /**
     * "Forgets" all stored operations.
     */
    void reset() {
        // must create a new list instead of clearing an existing one, as the original
        // list may have been exposed via events or "getDiffs", and trimming it is
        // undesirable.
        this.diffs = new ArrayList<GraphDiff>();
        this.markers = new HashMap<String, Integer>();
    }

    int size() {
        return diffs.size();
    }

    int sizeAfterMarker(String markerTag) {
        Integer pos = markers.get(markerTag);
        int marker = (pos == null) ? -1 : pos.intValue();
        if (marker < 0) {
            throw new IllegalStateException("No marked position for tag '"
                    + markerTag
                    + "'");
        }

        return diffs.size() - marker;
    }

    /**
     * Adds an operation to the list.
     */
    void addOperation(GraphDiff diff) {
        diffs.add(diff);
    }

    /**
     * Returns a sublist of the diffs list that shouldn't change when OperationRecorder is
     * cleared or new operations are added.
     */
    private List<GraphDiff> immutableList(int fromIndex, int toIndex) {
        if (toIndex - fromIndex == 0) {
            return Collections.EMPTY_LIST;
        }

        // Assuming that internal diffs list can only grow and can never be trimmed,
        // return sublist will never change - something that callers are expecting
        return Collections.unmodifiableList(new SubList(diffs, fromIndex, toIndex));
    }

    // moded Sublist from JDK that doesn't check for co-modification, as the underlying
    // list is guaranteed to only grow and never shrink or be replaced.
    static class SubList extends AbstractList<GraphDiff> implements Serializable {

        private List<GraphDiff> list;
        private int offset;
        private int size;

        SubList(List<GraphDiff> list, int fromIndex, int toIndex) {
            if (fromIndex < 0)
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            if (toIndex > list.size()) {
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            }
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException("fromIndex("
                        + fromIndex
                        + ") > toIndex("
                        + toIndex
                        + ")");
            }
            this.list = list;
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        @Override
        public GraphDiff get(int index) {
            rangeCheck(index);
            return list.get(index + offset);
        }

        @Override
        public int size() {
            return size;
        }

        private void rangeCheck(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ",Size: " + size);
            }
        }

        // serialization method...
        private Object writeReplace() throws ObjectStreamException {
            return new ArrayList<GraphDiff>(list.subList(offset, offset + size));
        }
    }
}
