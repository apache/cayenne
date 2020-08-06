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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class ColumnDescriptorTest {

    @Test
    public void testName() {
        ColumnDescriptor column = new ColumnDescriptor();
        column.setName("abc");
        assertEquals("abc", column.getName());
    }

    @Test
    public void testLabel() {
        ColumnDescriptor column = new ColumnDescriptor();
        column.setDataRowKey("abc");
        assertEquals("abc", column.getDataRowKey());
    }

    @Test
    public void testDbAttributeConstructor() {
        DbEntity entity = new DbEntity("entity");
        DbAttribute a = new DbAttribute();
        a.setName("name");
        a.setType(Types.VARCHAR);
        a.setEntity(entity);

        entity.addAttribute(a);

        ColumnDescriptor column = new ColumnDescriptor(a, null);
        assertEquals("name", column.getName());
        assertEquals("name", column.getQualifiedColumnName());
        assertEquals("entity", column.getTableName());
        assertEquals(String.class.getName(), column.getJavaClass());
        assertEquals("name", column.getDataRowKey());
        assertEquals(Types.VARCHAR, column.getJdbcType());
    }

    @Test
    public void testEquals() {
        ColumnDescriptor column1 = new ColumnDescriptor();
        column1.setName("n1");
        column1.namePrefix = "np1";
        column1.setTableName("t1");
        // type should be ignored in the comparison
        column1.setJdbcType(Types.VARCHAR);

        ColumnDescriptor column2 = new ColumnDescriptor();
        column2.setName("n1");
        column2.namePrefix = "np1";
        column2.setTableName("t1");
        column2.setJdbcType(Types.BOOLEAN);

        ColumnDescriptor column3 = new ColumnDescriptor();
        column3.setName("n1");
        column3.namePrefix = "np3";
        column3.setTableName("t1");

        assertEquals(column1, column2);
        assertFalse(column1.equals(column3));
        assertFalse(column3.equals(column2));
    }

    @Test
    public void testHashCode() {
        ColumnDescriptor column1 = new ColumnDescriptor();
        column1.setName("n1");
        column1.namePrefix = "np1";
        column1.setTableName("t1");
        // type should be ignored in the comparison
        column1.setJdbcType(Types.VARCHAR);

        ColumnDescriptor column2 = new ColumnDescriptor();
        column2.setName("n1");
        column2.namePrefix = "np1";
        column2.setTableName("t1");
        column2.setJdbcType(Types.BOOLEAN);

        ColumnDescriptor column3 = new ColumnDescriptor();
        column3.setName("n1");
        column3.namePrefix = "np3";
        column3.setTableName("t1");

        assertEquals(column1.hashCode(), column2.hashCode());

        // this is not really required by the hashcode contract... but just to see that
        // different columns generally end up in different buckets..
        assertTrue(column1.hashCode() != column3.hashCode());
    }
}
