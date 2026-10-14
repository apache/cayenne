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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataRowLayoutTest {

    private static final DataRowLayout ABC = DataRowLayout.of("a", "b", "c");
    private static final DataRowLayout CBA = DataRowLayout.of("c", "b", "a");

    @Test
    public void indexOf() {
        DataRowLayout layout = DataRowLayout.of("a", "b", "c");
        assertEquals(3, layout.size());
        assertEquals(0, layout.indexOf("a"));
        assertEquals(2, layout.indexOf("c"));
        assertEquals(-1, layout.indexOf("d"));
        assertEquals("b", layout.nameAt(1));
    }

    @Test
    public void duplicates_LastWins() {
        String[] names = {"a", "b", "a", "a"};
        DataRowLayout layout = DataRowLayout.of(names);

        assertEquals(4, layout.size());
        assertEquals(3, layout.indexOf("a"));
        assertEquals(2, layout.indexOf("a_"));
        assertEquals(0, layout.indexOf("a__"));
        assertEquals(1, layout.indexOf("b"));

        assertArrayEquals(new String[]{"a", "b", "a", "a"}, names, "the argument must not be changed");
    }

    @Test
    public void duplicates_ExistingSuffixedName() {
        DataRowLayout layout = DataRowLayout.of("a", "a_", "a");
        assertEquals(2, layout.indexOf("a"));
        assertEquals(1, layout.indexOf("a_"));
        assertEquals(0, layout.indexOf("a__"));
    }

    @Test
    public void reader_OwnLayout() {
        DataRowLayout.Reader reader = ABC.reader();
        DataRow row = DataRow.of("E", ABC, new Object[]{1, null, "z"}, 1);

        assertEquals(1, reader.get(row, 0));
        assertNull(reader.get(row, 1));
        assertEquals("z", reader.get(row, 2));
        assertTrue(reader.has(row, 1), "a null value is not a missing column");
    }

    @Test
    public void reader_OtherLayout() {
        DataRowLayout.Reader reader = DataRowLayout.of("c", "a", "x").reader();
        DataRow row = DataRow.of("E", ABC, new Object[]{1, null, "z"}, 1);

        assertEquals("z", reader.get(row, 0));
        assertEquals(1, reader.get(row, 1));
        assertNull(reader.get(row, 2), "a column the row does not have");
        assertTrue(reader.has(row, 0));
        assertFalse(reader.has(row, 2));
    }

    @Test
    public void reader_NullValue() {
        DataRowLayout.Reader reader = DataRowLayout.of("b").reader();
        DataRow row = DataRow.of("E", ABC, new Object[]{1, null, "z"}, 1);

        assertNull(reader.get(row, 0));
        assertTrue(reader.has(row, 0), "a null value is not a missing column");
    }

    @Test
    public void reader_NullAndMissingColumns() {
        DataRowLayout.Reader reader = DataRowLayout.of("c", "a", "x", "n").reader();
        DataRow row = DataRow.of("E", DataRowLayout.of("a", "c", "n"), new Object[]{1, "z", null}, 1);

        assertEquals("z", reader.get(row, 0));
        assertEquals(1, reader.get(row, 1));
        assertNull(reader.get(row, 2));
        assertFalse(reader.has(row, 2));
        assertNull(reader.get(row, 3));
        assertTrue(reader.has(row, 3));
    }

    @Test
    public void reader_RowsOfSeveralLayouts() {
        DataRowLayout.Reader reader = DataRowLayout.of("a", "c").reader();
        DataRow abc = DataRow.of("E", ABC, new Object[]{1, 2, 3}, 1);
        DataRow cba = DataRow.of("E", CBA, new Object[]{1, 2, 3}, 1);
        DataRow ac = DataRow.of("E", DataRowLayout.of("a", "c"), new Object[]{10, 30}, 1);

        // the positions are resolved per layout, whatever the order the rows come in
        for (int i = 0; i < 3; i++) {
            assertEquals(1, reader.get(abc, 0));
            assertEquals(3, reader.get(abc, 1));
            assertEquals(3, reader.get(cba, 0));
            assertEquals(1, reader.get(cba, 1));
            assertEquals(10, reader.get(ac, 0));
            assertEquals(30, reader.get(ac, 1));
        }
    }

    @Test
    public void reader_MergedRow() {
        DataRowLayout.Reader reader = DataRowLayout.of("b", "d").reader();
        DataRow row = DataRow.of("E", ABC, new Object[]{1, 2, 3}, 1);

        DataRow sameLayout = row.merged(Map.of("b", 20), 2);
        assertEquals(20, reader.get(sameLayout, 0));
        assertFalse(reader.has(sameLayout, 1));

        DataRow widened = row.merged(Map.of("d", 4), 3);
        assertEquals(2, reader.get(widened, 0));
        assertEquals(4, reader.get(widened, 1));
        assertTrue(reader.has(widened, 1));
    }
}
