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
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This interface represents some path in the Cayenne model.
 * Value is just a string of comma-separated identifiers.
 * <p>
 * Usage:
 * <pre>{@code
 *      CayennePath path = CayennePath.of("a.b+");
 *      CayennePath nextPath = path.dot("c+");
 *      CayennePath root = path.head(1);
 *      CayennePathSegment last = path.last();
 *      if(last.isOuterJoin()) {
 *          // ...
 *      }
 * }</pre>
 * <p>
 * It could be used in expressions or in the internal processing logic.
 * Implementation tries to optimize performance of the common path-related logic
 * or at least shift costs from operation side to the initialization.
 *
 * @since 5.0
 */
public interface CayennePath extends Iterable<CayennePathSegment>, Serializable {

    /**
     * No special markers
     */
    int NO_MARKER = 0;

    /**
     * Prefetch path marker
     */
    int PREFETCH_MARKER = 1;

    /**
     * Marker denotes paths inside tree resolution logic
     */
    int TABLE_TREE_MARKER = 2;

    /**
     * Constant value for an empty path
     */
    CayennePath EMPTY_PATH = new EmptyCayennePath(NO_MARKER);

    /**
     * Create path from a given String
     * <p>
     * This method will return {@link #EMPTY_PATH}
     * if {@code null} or an empty string is provided as an argument.
     * <p>
     * This method will throw {@link IllegalArgumentException}
     * if path value is malformed (e.g. containing dot at the start of two consecutive dots).
     *
     * @param path dot-separated path value
     * @return path for the given value
     */
    static CayennePath of(String path) {
        return of(path, NO_MARKER);
    }

    /**
     * Create path from a given String with a marker.
     * <p>
     * This method will return {@link #EMPTY_PATH}
     * if {@code null} or an empty string is provided as an argument.
     * <p>
     * This method will throw {@link IllegalArgumentException}
     * if path value is malformed (e.g. containing dot at the start of two consecutive dots).
     *
     * @param path dot-separated path value
     * @param marker to use for a path
     * @return path for the given value
     *
     * @see #NO_MARKER
     * @see #PREFETCH_MARKER
     */
    static CayennePath of(String path, int marker) {
        // quick check for the empty path
        if(path == null || path.isEmpty()) {
            if(marker == NO_MARKER) {
                return EMPTY_PATH;
            } else {
                return new EmptyCayennePath(marker);
            }
        }

        CayennePathSegment[] segments = PathParser.parseAllSegments(path);
        // fast case for a single segment path
        if (segments == null) {
            return new SingleSegmentCayennePath(PathParser.parseSegment(path), marker);
        }

        // general case for a path with a multiple segments
        int counter = 0;
        while(counter < segments.length && segments[counter] != null) {
            counter++;
        }
        return new MultiSegmentCayennePath(new SegmentList(segments, 0, counter), marker);
    }

    /**
     * Clone given path with a different marker
     *
     * @param path to clone
     * @param marker to set
     * @return clone of a given path with new marker
     *
     * @see #NO_MARKER
     * @see #PREFETCH_MARKER
     */
    static CayennePath of(CayennePath path, int marker) {
        if(path.marker() == marker) {
            return path;
        }
        return of(path.segments(), marker);
    }

    /**
     * Create path from a given list of {@link CayennePathSegment}
     *
     * @param segments list of path segments
     * @return path containing given segments
     */
    static CayennePath of(List<CayennePathSegment> segments) {
        return of(segments, NO_MARKER);
    }

    /**
     * Create path from a given list of {@link CayennePathSegment} and a marker
     *
     * @param segments list of path segments
     * @param marker to set
     * @return path containing given segments
     *
     * @see #NO_MARKER
     * @see #PREFETCH_MARKER
     */
    static CayennePath of(List<CayennePathSegment> segments, int marker) {
        switch (segments.size()) {
            case 0:
                return marker == NO_MARKER ? EMPTY_PATH : new EmptyCayennePath(marker);
            case 1:
                return new SingleSegmentCayennePath(segments.get(0), marker);
            default: {
                if(segments instanceof SegmentList) {
                    return new MultiSegmentCayennePath(segments, marker);
                } else {
                    return new MultiSegmentCayennePath(List.copyOf(segments), marker);
                }
            }
        }
    }

    static CayennePathSegment segmentOf(String segment, boolean outer) {
        return new CayennePathSegment(segment, outer);
    }

    static CayennePathSegment segmentOf(String segment) {
        return segmentOf(segment, false);
    }

    /**
     * Get all segments of this path
     *
     * @return list of segments
     */
    List<CayennePathSegment> segments();

    /**
     * Get marker of this path or {@link #NO_MARKER} if non set
     *
     * @return marker
     *
     * @see #NO_MARKER
     * @see #PREFETCH_MARKER
     */
    default int marker() {
        return NO_MARKER;
    }

    /**
     * Clone this path with new marker
     *
     * @param marker to use
     * @return new path with a marker
     *
     * @see #NO_MARKER
     * @see #PREFETCH_MARKER
     */
    default CayennePath withMarker(int marker) {
        return of(this, marker);
    }

    /**
     * Check if this path has required marker
     *
     * @param marker to check
     * @return true if this path has the marker
     *
     * @see #PREFETCH_MARKER
     */
    default boolean hasMarker(int marker) {
        return (marker() & marker) > 0;
    }

    /**
     * Get length of this path is segments
     *
     * @return segments count
     */
    default int length() {
        return segments().size();
    }

    /**
     * Check if this path is empty
     * @return true if this path has no segments
     */
    default boolean isEmpty() {
        return segments().isEmpty();
    }

    @Override
    String toString();

    /**
     * Get string representation of this path,
     * it'll be dot-separated segments of this path.
     *
     * @return string value for this path
     */
    default String value() {
        return toString();
    }

    /**
     * Get the first segment of this path if it's not empty
     * @return segment or null if path is empty
     */
    default CayennePathSegment first() {
        if (isEmpty()) {
            return null;
        }
        return segments().get(0);
    }

    /**
     * Get the last segment of this path if it's not empty
     * @return segment or null if path is empty
     */
    default CayennePathSegment last() {
        int length = length();
        if (length == 0) {
            return null;
        }
        return segments().get(length - 1);
    }

    /**
     * Create sub path from this path starting from the given index and to the end.
     * @param start index from
     * @return a new path starting from the start index
     */
    default CayennePath tail(int start) {
        if(start == 0) {
            return this;
        }
        return of(segments().subList(start, length()), marker());
    }

    /**
     * Create sub path from this path from the first element and ending at the given index.
     * @param end index to
     * @return a new path ending at the end index
     */
    default CayennePath head(int end) {
        if(end == length()) {
            return this;
        }
        return of(segments().subList(0, end), marker());
    }

    /**
     * Get the parent path, that is a path up to the last segment of this path.
     * @return parent path
     */
    default CayennePath parent() {
        if(isEmpty()) {
            return EMPTY_PATH.withMarker(marker());
        }
        return head(length() - 1);
    }

    /**
     * Create new path appending next segment to this.
     *
     * @param next segment to append
     * @return new path equal to this path + next segment
     */
    default CayennePath dot(String next) {
        return dot(PathParser.parseSegment(next));
    }

    /**
     * Create new path appending next segment to this.
     *
     * @param next segment to append
     * @return new path equal to this path + next segment
     */
    default CayennePath dot(CayennePathSegment next) {
        List<CayennePathSegment> segments = new ArrayList<>(length() + 1);
        segments.addAll(segments());
        segments.add(next);
        return new MultiSegmentCayennePath(segments, marker());
    }

    /**
     * Create new path appending all segments of the next path to this.
     * @param next path to append
     * @return new path equal to this path + next path
     */
    default CayennePath dot(CayennePath next) {
        List<CayennePathSegment> segments = new ArrayList<>(length() + next.length());
        segments.addAll(segments());
        segments.addAll(next.segments());
        return new MultiSegmentCayennePath(segments, marker());
    }
}
