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

package org.apache.cayenne.access.translator.select;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class PathComponentsTest {

    private PathComponents components1;
    private PathComponents components2;
    private PathComponents components3;

    @Before
    public void setUp() {
        components1 = new PathComponents("a");
        components2 = new PathComponents("a+.bcd");
        components3 = new PathComponents("a.bc.defg");
    }

    @Test
    public void size() {
        assertEquals(1, components1.size());
        assertEquals(2, components2.size());
        assertEquals(3, components3.size());
    }

    @Test
    public void getLast() {
        assertEquals("a", components1.getLast());
        assertEquals("bcd", components2.getLast());
        assertEquals("defg", components3.getLast());
    }

    @Test
    public void getParent() {
        assertEquals("", components1.getParent());
        assertEquals("a+", components2.getParent());
        assertEquals("a.bc", components3.getParent());
    }

    @Test
    public void getAll() {
        assertArrayEquals(new String[]{"a"}, components1.getAll());
        assertArrayEquals(new String[]{"a+", "bcd"}, components2.getAll());
        assertArrayEquals(new String[]{"a", "bc", "defg"}, components3.getAll());
    }

    @Test
    public void getPath() {
        assertEquals("a", components1.getPath());
        assertEquals("a+.bcd", components2.getPath());
        assertEquals("a.bc.defg", components3.getPath());
    }
}