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

package org.apache.cayenne.dba;

import org.apache.cayenne.MockSerializable;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.RuntimeCaseDataSourceFactory;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.EMPTY_PROJECT)
public class TypesMappingIT extends RuntimeCase {

    @Inject
    private RuntimeCaseDataSourceFactory dataSourceFactory;

    @Test
    public void testGetSqlTypeByJava() throws Exception {
        assertEquals(Types.VARCHAR, TypesMapping.getSqlTypeByJava(String.class));

        // make sure we can handle arrays...
        assertEquals(Types.BINARY, TypesMapping.getSqlTypeByJava(byte[].class));

        assertEquals(Types.TIMESTAMP, TypesMapping.getSqlTypeByJava(Calendar.class));
        assertEquals(
                Types.TIMESTAMP,
                TypesMapping.getSqlTypeByJava(GregorianCalendar.class));
        assertEquals(Types.BIGINT, TypesMapping.getSqlTypeByJava(BigInteger.class));

        assertEquals(
                Types.VARBINARY,
                TypesMapping.getSqlTypeByJava(MockSerializable.class));
        assertEquals(Types.VARCHAR, TypesMapping.getSqlTypeByJava(char[].class));
        assertEquals(Types.VARCHAR, TypesMapping.getSqlTypeByJava(Character[].class));
        assertEquals(Types.VARBINARY, TypesMapping.getSqlTypeByJava(Byte[].class));
    }

    @Test
    public void testGetSqlTypeByJavaString() throws Exception {
        assertEquals(Types.VARCHAR, TypesMapping.getSqlTypeByJava(String.class.getName()));

        // make sure we can handle arrays...
        assertEquals(Types.BINARY, TypesMapping.getSqlTypeByJava("byte[]"));

        assertEquals(
                Types.TIMESTAMP,
                TypesMapping.getSqlTypeByJava(Calendar.class.getName()));
        assertEquals(
                Types.TIMESTAMP,
                TypesMapping.getSqlTypeByJava(GregorianCalendar.class.getName()));
        assertEquals(
                Types.BIGINT,
                TypesMapping.getSqlTypeByJava(BigInteger.class.getName()));

        assertEquals(
                Types.VARBINARY,
                TypesMapping.getSqlTypeByJava(MockSerializable.class.getName()));

        assertEquals(Types.VARCHAR, TypesMapping.getSqlTypeByJava("char[]"));
        assertEquals(
                Types.VARCHAR,
                TypesMapping.getSqlTypeByJava("java.lang.Character[]"));
        assertEquals(Types.VARBINARY, TypesMapping.getSqlTypeByJava("java.lang.Byte[]"));
    }

    @Test
    public void testGetSqlTypeByJavaPrimitive() throws Exception {
        assertEquals(Types.INTEGER, TypesMapping.getSqlTypeByJava(Integer.TYPE));
        assertEquals(Types.BIGINT, TypesMapping.getSqlTypeByJava(Long.TYPE));
    }

    @Test
    public void testDecimalType() {
        DbAttribute attribute = new DbAttribute("test");
        attribute.setType(Types.DECIMAL);

        attribute.setScale(0);
        attribute.setMaxLength(38);
        assertEquals(BigInteger.class.getName(), TypesMapping.getJavaBySqlType(attribute));

        attribute.setScale(0);
        attribute.setMaxLength(5);
        assertEquals(Integer.class.getName(), TypesMapping.getJavaBySqlType(attribute));

        attribute.setScale(0);
        attribute.setMaxLength(10);
        assertEquals(Long.class.getName(), TypesMapping.getJavaBySqlType(attribute));

        attribute.setScale(1);
        attribute.setMaxLength(5);
        assertEquals(BigDecimal.class.getName(), TypesMapping.getJavaBySqlType(attribute));

        attribute.setScale(5);
        attribute.setMaxLength(38);
        assertEquals(BigDecimal.class.getName(), TypesMapping.getJavaBySqlType(attribute));
    }

    @Test
    public void testTypeInfoCompleteness() throws Exception {
        // check counts
        // since more then 1 database type can map to a single JDBC type
        int len = 0;
        try (Connection conn = dataSourceFactory.getSharedDataSource().getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getTypeInfo()) {
                while (rs.next()) {
                    len++;
                }
            }
        }

        int actualLen = 0;
        TypesMapping map = createTypesMapping();

        for (List<TypesMapping.TypeInfo> entry : map.databaseTypes.values()) {
            actualLen += entry.size();
        }

        // this is bad assertion, since due to some hacks
        // the same database types may map more then once,
        // so we have to use <=
        assertTrue(len <= actualLen);
    }

    private TypesMapping createTypesMapping() throws Exception {
        try (Connection conn = dataSourceFactory.getSharedDataSource().getConnection()) {
            DatabaseMetaData md = conn.getMetaData();
            return new TypesMapping(md);
        }
    }
}
