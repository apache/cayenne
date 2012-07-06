/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dba;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.cayenne.MockSerializable;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;

public class TypesMappingTest extends ServerCase {

    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;

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

    public void testGetSqlTypeByJavaPrimitive() throws Exception {
        assertEquals(Types.INTEGER, TypesMapping.getSqlTypeByJava(Integer.TYPE));
        assertEquals(Types.BIGINT, TypesMapping.getSqlTypeByJava(Long.TYPE));
    }

    public void testTypeInfoCompleteness() throws Exception {
        // check counts
        // since more then 1 database type can map to a single JDBC type
        Connection conn = dataSourceFactory.getSharedDataSource().getConnection();
        int len = 0;
        try {
            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getTypeInfo();
            try {
                while (rs.next()) {
                    len++;
                }
            }
            finally {
                rs.close();
            }
        }
        finally {
            conn.close();
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

    TypesMapping createTypesMapping() throws Exception {
        Connection conn = dataSourceFactory.getSharedDataSource().getConnection();

        try {
            DatabaseMetaData md = conn.getMetaData();
            return new TypesMapping(md);
        }
        finally {
            conn.close();
        }
    }
}
