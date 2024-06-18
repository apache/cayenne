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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Generic version of the path with more than one segment
 *
 * @since 5.0
 */
class MultiSegmentCayennePath implements CayennePath, Serializable {

    private final List<CayennePathSegment> segments;

    private final int marker;

    private transient String fullPath;

    MultiSegmentCayennePath(List<CayennePathSegment> segments, int marker) {
        this.segments = segments;
        this.marker = marker;
    }

    @Override
    public List<CayennePathSegment> segments() {
        return segments;
    }

    @Override
    public int marker() {
        return marker;
    }

    @Override
    public String toString() {
        // cache value
        if(fullPath != null) {
            return fullPath;
        }
        return fullPath = String.join(".", segments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CayennePath)) {
            return false;
        }

        CayennePath that = (CayennePath) o;
        return that.marker() == marker && segments.equals(that.segments());
    }

    @Override
    public int hashCode() {
        return 31 * marker + segments.hashCode();
    }

    @Override
    public Iterator<CayennePathSegment> iterator() {
        return new PathIterator();
    }

    class PathIterator implements Iterator<CayennePathSegment> {
        int position;

        @Override
        public boolean hasNext() {
            return position < segments.size();
        }

        @Override
        public CayennePathSegment next() {
            int next = position;
            if(next >= segments.size()) {
                throw new NoSuchElementException();
            }
            CayennePathSegment segment = segments.get(next);
            position = next + 1;
            return segment;
        }
    }

}
