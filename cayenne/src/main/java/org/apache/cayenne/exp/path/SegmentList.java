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

package org.apache.cayenne.exp.path;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Specialized List implementation that is just a view over a shared array of path segments.
 *
 * @since 5.0
 */
class SegmentList extends AbstractList<CayennePathSegment> implements RandomAccess, Serializable {

    /**
     * This array is shared with several lists, so it's a caller responsibility to protect its content
     */
    final CayennePathSegment[] segments;

    /**
     * The first index of the segments array we are presenting as the content og this list
     */
    final int fromIdx;

    /**
     * The last index of the segments array we are presenting as the content og this list
     */
    final int toIdx;

    SegmentList(CayennePathSegment[] data, int fromIdx, int toIdx) {
        this.segments = Objects.requireNonNull(data);
        this.fromIdx = fromIdx;
        this.toIdx = toIdx;
        if (toIdx > data.length || fromIdx > toIdx) {
            throw new IndexOutOfBoundsException(toIdx);
        }
    }

    @Override
    public CayennePathSegment get(int index) {
        if (index >= size()) {
            throw new IndexOutOfBoundsException(index);
        }
        return segments[index + fromIdx];
    }

    @Override
    public int size() {
        return (toIdx - fromIdx);
    }

    // this method is optimized for head() and tail() operations of the CayennePath
    @Override
    public List<CayennePathSegment> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (toIndex > size()) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        return new SegmentList(segments, fromIdx + fromIndex, fromIdx + toIndex);
    }

    @Override
    public Object[] toArray() {
        int size = size();
        Object[] copy = new Object[size];
        System.arraycopy(segments, fromIdx, copy, 0, size);
        return copy;
    }
}
