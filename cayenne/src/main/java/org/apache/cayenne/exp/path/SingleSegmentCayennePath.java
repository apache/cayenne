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
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Special case of the path with a single segment
 *
 * @since 5.0
 */
class SingleSegmentCayennePath implements CayennePath, Serializable {

    private final CayennePathSegment segment;

    private final int marker;

    SingleSegmentCayennePath(CayennePathSegment segment, int marker) {
        this.segment = Objects.requireNonNull(segment, "Segment can't be null");
        this.marker = marker;
    }

    @Override
    public CayennePath dot(CayennePathSegment next) {
        return new MultiSegmentCayennePath(List.of(segment, next), marker);
    }

    @Override
    public List<CayennePathSegment> segments() {
        return List.of(segment);
    }

    @Override
    public int marker() {
        return marker;
    }

    @Override
    public CayennePath parent() {
        return EMPTY_PATH;
    }

    @Override
    public CayennePathSegment last() {
        return segment;
    }

    @Override
    public CayennePathSegment first() {
        return segment;
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
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
        if(that.length() != 1) {
            return false;
        }
        return that.marker() == marker && segment.equals(that.segments().get(0));
    }

    @Override
    public int hashCode() {
        return 31 * marker + segment.hashCode();
    }

    @Override
    public String toString() {
        return segment.toString();
    }

    @Override
    public Iterator<CayennePathSegment> iterator() {
        return new PathIterator();
    }

    class PathIterator implements Iterator<CayennePathSegment> {
        boolean advanced;

        @Override
        public boolean hasNext() {
            return !advanced;
        }

        @Override
        public CayennePathSegment next() {
            if(advanced) {
                throw new NoSuchElementException();
            }
            advanced = true;
            return segment;
        }

        @Override
        public void forEachRemaining(Consumer<? super CayennePathSegment> action) {
            Objects.requireNonNull(action);
            if(!advanced) {
                action.accept(segment);
                advanced = true;
            }
        }
    }
}
