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

import org.junit.Test;

import static org.junit.Assert.*;

public class PathParserTest {

    @Test
    public void testParseSingle() {
        assertNull(PathParser.parseAllSegments(""));
        assertNull(PathParser.parseAllSegments("bd:test+"));
        assertNull(PathParser.parseAllSegments("test"));
    }

    @Test
    public void testTwoSegmentsSimple() {
        CayennePathSegment[] segments = PathParser.parseAllSegments("abc.bed");
        assertNotNull(segments);
        assertEquals(4, segments.length);
        assertEquals("abc", segments[0].value());
        assertFalse(segments[0].isOuterJoin());
        assertEquals("bed", segments[1].value());
        assertFalse(segments[1].isOuterJoin());
        assertNull(segments[2]);
        assertNull(segments[3]);
    }

    @Test
    public void testTwoSegmentsOuter() {
        CayennePathSegment[] segments = PathParser.parseAllSegments("a+.b+");
        assertNotNull(segments);
        assertEquals(4, segments.length);
        assertEquals("a", segments[0].value());
        assertTrue(segments[0].isOuterJoin());
        assertEquals("b", segments[1].value());
        assertTrue(segments[1].isOuterJoin());
        assertNull(segments[2]);
        assertNull(segments[3]);
    }

    @Test
    public void testThreeSegments() {
        CayennePathSegment[] segments = PathParser.parseAllSegments("a.b.c");
        assertNotNull(segments);
        assertEquals(4, segments.length);
        assertEquals("a", segments[0].value());
        assertFalse(segments[0].isOuterJoin());
        assertEquals("b", segments[1].value());
        assertFalse(segments[1].isOuterJoin());
        assertEquals("c", segments[2].value());
        assertFalse(segments[2].isOuterJoin());
        assertNull(segments[3]);
    }

    @Test
    public void testFourSegments() {
        CayennePathSegment[] segments = PathParser.parseAllSegments("a.b+.c+.d");
        assertNotNull(segments);
        assertEquals(4, segments.length);
        assertEquals("a", segments[0].value());
        assertFalse(segments[0].isOuterJoin());
        assertEquals("b", segments[1].value());
        assertTrue(segments[1].isOuterJoin());
        assertEquals("c", segments[2].value());
        assertTrue(segments[2].isOuterJoin());
        assertEquals("d", segments[3].value());
        assertFalse(segments[3].isOuterJoin());
    }

    @Test
    public void testFiveSegments() {
        CayennePathSegment[] segments = PathParser.parseAllSegments("a.b+.c+.d.e");
        assertNotNull(segments);
        assertEquals(8, segments.length);
        assertEquals("a", segments[0].value());
        assertFalse(segments[0].isOuterJoin());
        assertEquals("b", segments[1].value());
        assertTrue(segments[1].isOuterJoin());
        assertEquals("c", segments[2].value());
        assertTrue(segments[2].isOuterJoin());
        assertEquals("d", segments[3].value());
        assertFalse(segments[3].isOuterJoin());
        assertEquals("e", segments[4].value());
        assertFalse(segments[4].isOuterJoin());
        assertNull(segments[5]);
        assertNull(segments[6]);
        assertNull(segments[7]);
    }

}