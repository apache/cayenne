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

/**
 * @since 5.0
 */
class PathParser {

    private static final int INITIAL_SEGMENTS_COUNT = 4;
    private static final int MAX_SEGMENTS_COUNT = 10000;

    /**
     * NOTE: this method is optimized for internal use only and expects non-empty string
     *
     * @param path string to parse, must be not null
     * @return array of path components or {@code null} if path is a simple segment
     *
     * @see CayennePath#of(String, int)
     */
    static CayennePathSegment[] parseAllSegments(String path) {
        int off = 0;
        int i = 0;
        int next;
        CayennePathSegment[] result = null;
        while ((next = path.indexOf('.', off)) != -1) {
            if(off == next) {
                throw new IllegalArgumentException("Illegal path expression");
            }
            if(result == null) {
                result = new CayennePathSegment[INITIAL_SEGMENTS_COUNT];
            }
            result[i++] = parseSegment(path, off, next);
            if(i == result.length) {
                if(result.length > MAX_SEGMENTS_COUNT) {
                    // some sanity check
                    throw new IllegalArgumentException("Illegal path expression");
                }
                CayennePathSegment[] newList = new CayennePathSegment[result.length * 2];
                System.arraycopy(result, 0, newList, 0, result.length);
                result = newList;
            }
            off = next + 1;
        }
        if(off == 0) {
            return null;
        }
        if(off == path.length()) {
            throw new IllegalArgumentException("Illegal path expression");
        }
        // Add remaining segment
        result[i] = parseSegment(path, off, path.length());
        return result;
    }

    static CayennePathSegment parseSegment(String segment, int start, int end) {
        if(segment.charAt(end - 1) == '+') {
            return new CayennePathSegment(segment.substring(start, end - 1), true);
        } else {
            return new CayennePathSegment(segment.substring(start, end), false);
        }
    }

    static CayennePathSegment parseSegment(String segment) {
        boolean outer = false;
        if(segment.length() > 1 && segment.charAt(segment.length() - 1) == '+') {
            segment = segment.substring(0, segment.length() - 1);
            outer = true;
        }
        return new CayennePathSegment(segment, outer);
    }
}
