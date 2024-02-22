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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Special case of an empty path
 *
 * @since 5.0
 */
class EmptyCayennePath implements CayennePath, Serializable {

    final int marker;

    EmptyCayennePath(int marker) {
        this.marker = marker;
    }

    @Override
    public int marker() {
        return marker;
    }

    @Override
    public CayennePath withMarker(int marker) {
        if(marker == this.marker) {
            return this;
        }
        if(marker == NO_MARKER) {
            return EMPTY_PATH;
        }
        return new EmptyCayennePath(marker);
    }

    @Override
    public CayennePath dot(CayennePathSegment next) {
        return new SingleSegmentCayennePath(next, marker());
    }

    @Override
    public CayennePath dot(CayennePath next) {
        return CayennePath.of(next, marker());
    }

    @Override
    public List<CayennePathSegment> segments() {
        return List.of();
    }

    @Override
    public CayennePath parent() {
        return null;
    }

    @Override
    public CayennePathSegment last() {
        return null;
    }

    @Override
    public CayennePathSegment first() {
        return null;
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    public CayennePath tail(int start) {
        throw new IndexOutOfBoundsException(start);
    }

    public CayennePath head(int end) {
        throw new IndexOutOfBoundsException(end);
    }

    @Override
    public Iterator<CayennePathSegment> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public String toString() {
        return "";
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
        return that.marker() == marker && that.isEmpty();
    }

    @Override
    public int hashCode() {
        return 31 * marker;
    }
}
