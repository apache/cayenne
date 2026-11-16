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

import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.sqlbuilder.DefaultSQLAppendable;
import org.apache.cayenne.access.sqlbuilder.SQLAppendable;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationContext;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueNodeTest {

    private SQLGenerationContext context;
    private List<PSParameter<?>> bindings;

    @BeforeEach
    public void createContext() {
        DbAdapter adapter = mock(DbAdapter.class);
        when(adapter.getExtendedTypes()).thenReturn(new ExtendedTypeMap());

        bindings = new ArrayList<>();
        context = mock(SQLGenerationContext.class);
        when(context.getAdapter()).thenReturn(adapter);
        when(context.getBindings()).thenReturn(bindings);
    }

    @Test
    public void binaryValuesInList() {
        for (int type : new int[]{Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB}) {
            bindings.clear();
            byte[] key1 = {1, 2, 3};
            byte[] key2 = {4, 5, 6};

            ValueNode node = new ValueNode(new Object[]{key1, key2}, true, attribute(type), true);
            String sql = append(node);

            assertEquals(2, sql.chars().filter(c -> c == '?').count(), sql);
            assertEquals(2, bindings.size());
            assertSame(key1, bindings.get(0).value());
            assertSame(key2, bindings.get(1).value());
        }
    }

    @Test
    public void byteArrayForNonBinaryAttributeIsExpanded() {
        ValueNode node = new ValueNode(new byte[]{1, 2, 3}, true, attribute(Types.TINYINT), true);
        String sql = append(node);

        assertEquals(3, sql.chars().filter(c -> c == '?').count(), sql);
        assertEquals(3, bindings.size());
        assertEquals((byte) 1, bindings.get(0).value());
    }

    @Test
    public void byteArrayWithoutAttributeIsExpanded() {
        ValueNode node = new ValueNode(new byte[]{1, 2, 3}, true, null, true);
        append(node);

        assertEquals(3, bindings.size());
    }

    private String append(ValueNode node) {
        SQLAppendable buffer = new DefaultSQLAppendable(null);
        node.append(buffer, context);
        return buffer.toString();
    }

    private static DbAttribute attribute(int type) {
        DbAttribute attribute = new DbAttribute("KEY");
        attribute.setType(type);
        return attribute;
    }
}
