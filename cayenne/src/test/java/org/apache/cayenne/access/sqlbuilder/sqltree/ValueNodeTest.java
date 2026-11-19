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

package org.apache.cayenne.access.sqlbuilder.sqltree;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.sqlbuilder.DefaultSQLAppendable;
import org.apache.cayenne.access.sqlbuilder.SQLAppendable;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationContext;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.unit.CompiledTables;
import org.apache.cayenne.unit.TestAdapters;
import org.apache.cayenne.unit.TestObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ValueNodeTest {

    private static final byte[] KEY1 = {1, 2, 3};
    private static final byte[] KEY2 = {4, 5, 6};

    private SQLGenerationContext context;
    private List<PSParameter<?>> bindings;

    @BeforeEach
    public void createContext() {
        bindings = new ArrayList<>();
        context = new TestContext(TestAdapters.jdbc(), bindings);
    }

    @Test
    public void binaryValuesInList() {
        for (int type : new int[]{Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB}) {
            bindings.clear();
            String sql = append(new ValueNode(new Object[]{KEY1, KEY2}, true, column(type), true));
            assertBindings(sql, KEY1, KEY2);
        }
    }

    @Test
    public void binaryValuesInListWithoutAttribute() {
        String sql = append(new ValueNode(new Object[]{KEY1, KEY2}, true, null, true));
        assertBindings(sql, KEY1, KEY2);
    }

    @Test
    public void byteArrayWithoutAttributeIsSingleValue() {
        String sql = append(new ValueNode(KEY1, true, null, true));
        assertBindings(sql, KEY1);
    }

    @Test
    public void byteArrayForNonBinaryAttributeIsExpanded() {
        String sql = append(new ValueNode(new byte[]{1, 2, 3}, true, column(Types.TINYINT), true));

        assertEquals(3, placeholders(sql), sql);
        assertEquals(3, bindings.size());
        assertEquals((byte) 1, bindings.get(0).value());
    }

    @Test
    public void objectIdsInListAreNotExpanded() {
        Object[] ids = {ObjectId.of("E", "ID", KEY1), TestObjects.committedObject(ObjectId.of("E", "ID", KEY2))};

        for (CompiledColumn column : Arrays.asList(null, column(Types.TINYINT))) {
            bindings.clear();
            String sql = append(new ValueNode(ids, true, column, true));
            assertBindings(sql, KEY1, KEY2);
        }
    }

    @Test
    public void primitiveArraysInListAreExpanded() {
        String sql = append(new ValueNode(new Object[]{new int[]{1, 2, 3}}, true, null, true));

        assertEquals(3, placeholders(sql), sql);
        assertEquals(List.of(1, 2, 3), bindings.stream().map(PSParameter::value).toList());
    }

    private String append(ValueNode node) {
        SQLAppendable buffer = new DefaultSQLAppendable(null);
        node.append(buffer, context);
        return buffer.toString();
    }

    private void assertBindings(String sql, Object... values) {
        assertEquals(values.length, placeholders(sql), sql);
        assertEquals(values.length, bindings.size());
        for (int i = 0; i < values.length; i++) {
            assertSame(values[i], bindings.get(i).value());
        }
    }

    private static long placeholders(String sql) {
        return sql.chars().filter(c -> c == '?').count();
    }

    private static CompiledColumn column(int type) {
        DbEntity table = new DbEntity("T");
        table.addAttribute(new DbAttribute("KEY", type, table));
        DataMap map = new DataMap("M");
        map.addDbEntity(table);
        return CompiledTables.compile(map).table("T").column("KEY");
    }

    private record TestContext(DbAdapter getAdapter, List<PSParameter<?>> getBindings) implements SQLGenerationContext {

        @Override
        public CompiledTable getRootDbEntity() {
            return null;
        }

        @Override
        public boolean isSingleTableSQL() {
            return true;
        }
    }
}
