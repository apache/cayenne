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

package org.apache.cayenne.exp;

import org.apache.cayenne.exp.path.CayennePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class CayennePathTest {

    private CayennePath[] paths;

    @BeforeEach
    public void setUp() {
        paths = new CayennePath[]{
                CayennePath.of("a"),
                CayennePath.of("a+.bcd"),
                CayennePath.of("a.bc.defg"),
                CayennePath.of("a.bc.defg.edad")
        };
    }

    @Test
    public void parse() {
        CayennePath path = CayennePath.of("a.bc+.defg");
        assertEquals(3, path.length());
        assertEquals("a.bc+", path.parent().toString());
    }

    @Test
    public void size() {
        assertEquals(1, paths[0].length());
        assertEquals(2, paths[1].length());
        assertEquals(3, paths[2].length());
        assertEquals(4, paths[3].length());
    }

    @Test
    public void getLast() {
        assertEquals("a", paths[0].last().toString());
        assertEquals("bcd", paths[1].last().toString());
        assertEquals("defg", paths[2].last().toString());
        assertEquals("edad", paths[3].last().toString());
    }

    @Test
    public void getParent() {
        assertEquals("", paths[0].parent().toString());
        assertEquals("a+", paths[1].parent().toString());
        assertEquals("a.bc", paths[2].parent().toString());
        assertEquals("a.bc.defg", paths[3].parent().toString());
    }

    @Test
    public void getPath() {
        assertEquals("a", paths[0].toString());
        assertEquals("a+.bcd", paths[1].toString());
        assertEquals("a.bc.defg", paths[2].toString());
        assertEquals("a.bc.defg.edad", paths[3].toString());
    }


    @Test
    public void expandAliases() {
        Map<String, String> aliases = Map.of("a", "x+.y", "b", "z");

        assertEquals(CayennePath.of("x+.y.c"), CayennePath.of("a.c").expandAliases(aliases));
        assertEquals(CayennePath.of("c.x+.y"), CayennePath.of("c.a").expandAliases(aliases));
        assertEquals(CayennePath.of("x+.y.z"), CayennePath.of("a.b").expandAliases(aliases));
        assertEquals(CayennePath.of("x+.y"), CayennePath.of("a").expandAliases(aliases));
        assertEquals(CayennePath.of("c+.d"), CayennePath.of("c+.d").expandAliases(aliases));
        assertSame(paths[1], paths[1].expandAliases(Map.of("q", "z")));
        assertSame(paths[2], paths[2].expandAliases(Map.of()));

        CayennePath prefetch = CayennePath.of("a.c", CayennePath.PREFETCH_MARKER);
        assertEquals(CayennePath.PREFETCH_MARKER, prefetch.expandAliases(aliases).marker());
        assertEquals("x+.y.c", prefetch.expandAliases(aliases).value());
    }
}
