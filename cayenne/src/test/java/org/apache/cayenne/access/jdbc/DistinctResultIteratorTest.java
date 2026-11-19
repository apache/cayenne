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

package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.unit.CompiledTables;
import org.apache.cayenne.util.ResultIteratorIterator;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DistinctResultIteratorTest {

    private static CompiledTable binaryPkEntity() {
        DbEntity entity = new DbEntity("E");
        DbAttribute pk = new DbAttribute("ID", Types.VARBINARY, entity);
        pk.setPrimaryKey(true);
        entity.addAttribute(pk);
        entity.addAttribute(new DbAttribute("NAME", Types.VARCHAR, entity));
        return compile(entity);
    }

    private static CompiledTable compile(DbEntity entity) {
        DataMap map = new DataMap("M");
        map.addDbEntity(entity);
        return CompiledTables.compile(map).table(entity.getName());
    }

    private static final DataRowLayout ID_NAME = DataRowLayout.of("ID", "NAME");
    private static final DataRowLayout ID = DataRowLayout.of("ID");

    private static DataRow row(byte[] id, String name) {
        return DataRow.of("E", ID_NAME, new Object[]{id, name}, 1);
    }

    @Test
    public void distinctByIdBinaryPK() {
        List<DataRow> rows = List.of(
                row(new byte[]{1, 2, 3}, "a"),
                row(new byte[]{1, 2, 3}, "a"),
                row(new byte[]{4, 5, 6}, "b"),
                row(new byte[]{1, 2, 3}, "a"));

        try (DistinctResultIterator<DataRow> it =
                     new DistinctResultIterator<>(new ListIterator<>(rows), binaryPkEntity(), false)) {
            List<DataRow> distinct = it.allRows();
            assertEquals(2, distinct.size());
            assertEquals("a", distinct.get(0).get("NAME"));
            assertEquals("b", distinct.get(1).get("NAME"));
        }
    }

    @Test
    public void distinctFullRowsBinaryValues() {
        List<DataRow> rows = List.of(
                row(new byte[]{1, 2, 3}, "a"),
                row(new byte[]{1, 2, 3}, "a"),
                row(new byte[]{1, 2, 3}, "b"));

        try (DistinctResultIterator<DataRow> it =
                     new DistinctResultIterator<>(new ListIterator<>(rows), binaryPkEntity(), true)) {
            assertEquals(2, it.allRows().size());
        }
    }

    @Test
    public void distinctFullArrayRowsBinaryValues() {
        List<Object[]> rows = List.of(
                new Object[]{new byte[]{1, 2, 3}, "a"},
                new Object[]{new byte[]{1, 2, 3}, "a"},
                new Object[]{new byte[]{1, 2, 3}, "b"},
                new Object[]{new byte[]{1, 2, 3}, "a"});

        try (DistinctResultIterator<Object[]> it =
                     new DistinctResultIterator<>(new ListIterator<>(rows), binaryPkEntity(), true)) {
            List<Object[]> distinct = it.allRows();
            assertEquals(2, distinct.size());
            assertEquals("a", distinct.get(0)[1]);
            assertEquals("b", distinct.get(1)[1]);
        }
    }

    @Test
    public void distinctFullScalarRowsBinaryValues() {
        List<byte[]> rows = List.of(new byte[]{1, 2, 3}, new byte[]{1, 2, 3}, new byte[]{1, 2, 4});

        try (DistinctResultIterator<byte[]> it =
                     new DistinctResultIterator<>(new ListIterator<>(rows), binaryPkEntity(), true)) {
            List<byte[]> distinct = it.allRows();
            assertEquals(2, distinct.size());
            assertSame(rows.get(0), distinct.get(0));
            assertSame(rows.get(2), distinct.get(1));
        }
    }

    @Test
    public void distinctByIdNumericPK() {
        DbEntity entity = new DbEntity("E");
        DbAttribute pk = new DbAttribute("ID", Types.INTEGER, entity);
        pk.setPrimaryKey(true);
        entity.addAttribute(pk);

        List<DataRow> rows = new ArrayList<>();
        for (int id : new int[]{1, 2, 1, 3, 2}) {
            rows.add(DataRow.of("E", ID, new Object[]{id}, 1));
        }

        try (DistinctResultIterator<DataRow> it =
                     new DistinctResultIterator<>(new ListIterator<>(rows), compile(entity), false)) {
            assertEquals(3, it.allRows().size());
        }
    }

    private record ListIterator<T>(Iterator<T> rows) implements ResultIterator<T> {

            ListIterator(List<T> rows) {
                this(rows.iterator());
            }

            @Override
            public Iterator<T> iterator() {
                return new ResultIteratorIterator<>(this);
            }

            @Override
            public List<T> allRows() {
                List<T> list = new ArrayList<>();
                rows.forEachRemaining(list::add);
                return list;
            }

            @Override
            public boolean hasNextRow() {
                return rows.hasNext();
            }

            @Override
            public T nextRow() {
                return rows.next();
            }

            @Override
            public void skipRow() {
                rows.next();
            }

            @Override
            public void close() {
            }
        }
}
