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

package org.apache.cayenne;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class RowLayoutTest {

    @Test
    public void indexOf() {
        RowLayout layout = new RowLayout(new String[]{"a", "b", "c"});
        assertEquals(3, layout.size());
        assertEquals(0, layout.indexOf("a"));
        assertEquals(2, layout.indexOf("c"));
        assertEquals(-1, layout.indexOf("d"));
        assertEquals("b", layout.nameAt(1));
    }

    @Test
    public void duplicates_LastWins() {
        String[] names = {"a", "b", "a", "a"};
        RowLayout layout = new RowLayout(names);

        assertEquals(4, layout.size());
        assertEquals(3, layout.indexOf("a"));
        assertEquals(2, layout.indexOf("a_"));
        assertEquals(0, layout.indexOf("a__"));
        assertEquals(1, layout.indexOf("b"));

        assertArrayEquals(new String[]{"a", "b", "a", "a"}, names, "the argument must not be changed");
    }

    @Test
    public void duplicates_ExistingSuffixedName() {
        RowLayout layout = new RowLayout(new String[]{"a", "a_", "a"});
        assertEquals(2, layout.indexOf("a"));
        assertEquals(1, layout.indexOf("a_"));
        assertEquals(0, layout.indexOf("a__"));
    }

    @Test
    public void noDuplicates_NoCopy() {
        RowLayout layout = new RowLayout(new String[]{"a", "b"});
        assertSame("a", layout.nameAt(0));
    }
}
