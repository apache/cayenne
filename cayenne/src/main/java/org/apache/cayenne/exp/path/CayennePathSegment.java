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
import java.util.Objects;

/**
 * Value object representing segment of a {@link CayennePath}
 * <p>
 * This class is a thin wrapper for a String value.
 *
 * @see CayennePath#segmentOf(String, boolean)
 * @see CayennePath#segmentOf(String)
 *
 * @since 5.0
 */
public class CayennePathSegment implements CharSequence, Serializable {

    private final String segment;

    private final boolean outer;

    CayennePathSegment(String segment, boolean outer) {
        this.segment = Objects.requireNonNull(segment, "Path segment can't be null");
        this.outer = outer;
    }

    public String value() {
        return segment;
    }

    public boolean isOuterJoin() {
        return outer;
    }

    public CayennePathSegment outer() {
        if(this.outer) {
            return this;
        }
        return CayennePath.segmentOf(segment, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CayennePathSegment that = (CayennePathSegment) o;

        if (outer != that.outer) {
            return false;
        }
        return segment.equals(that.segment);
    }

    @Override
    public int hashCode() {
        int result = segment.hashCode();
        result = 31 * result + Boolean.hashCode(outer);
        return result;
    }

    @Override
    public String toString() {
        if (!outer) {
            return segment;
        }
        return segment + "+";
    }

    @Override
    public int length() {
        return segment.length();
    }

    @Override
    public char charAt(int index) {
        return segment.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new CayennePathSegment(segment.substring(start, end), outer);
    }

}
