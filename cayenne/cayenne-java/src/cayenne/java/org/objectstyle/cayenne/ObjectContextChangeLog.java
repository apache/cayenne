/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.graph.ArcCreateOperation;
import org.objectstyle.cayenne.graph.ArcDeleteOperation;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.NodeDiff;

/**
 * Stores graph operations in the order they were performed, optionally allowing to set
 * named markers.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectContextChangeLog {

    List diffs;
    Map markers;

    ObjectContextChangeLog() {
        reset();
    }

    void unregisterNode(Object nodeId) {
        Iterator it = diffs.iterator();
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
        markers.put(markerTag, new Integer(diffs.size()));
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
        Integer pos = (Integer) markers.get(markerTag);
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
        this.diffs = new ArrayList();
        this.markers = new HashMap();
    }

    int size() {
        return diffs.size();
    }

    int sizeAfterMarker(String markerTag) {
        Integer pos = (Integer) markers.get(markerTag);
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
    private List immutableList(int fromIndex, int toIndex) {
        if (toIndex - fromIndex == 0) {
            return Collections.EMPTY_LIST;
        }

        // Assuming that internal diffs list can only grow and can never be trimmed,
        // return sublist will never change - something that callers are expecting
        return Collections.unmodifiableList(new SubList(diffs, fromIndex, toIndex));
    }

    // moded Sublist from JDK that doesn't check for co-modification, as the underlying
    // list is guaranteed to only grow and never shrink or be replaced.
    static class SubList extends AbstractList implements Serializable {

        private List list;
        private int offset;
        private int size;

        SubList(List list, int fromIndex, int toIndex) {
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

        public Object get(int index) {
            rangeCheck(index);
            return list.get(index + offset);
        }

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
            return new ArrayList(list.subList(offset, offset + size));
        }
    }

}
