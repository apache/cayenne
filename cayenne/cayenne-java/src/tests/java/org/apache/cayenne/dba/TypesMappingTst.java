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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.unit.CayenneTestCase;

public class TypesMappingTst extends CayenneTestCase {

    public void testGetSqlTypeByJava() throws Exception {
        assertEquals(Types.VARCHAR, TypesMapping.getSqlTypeByJava(String.class));

        // make sure we can handle arrays...
        assertEquals(Types.BINARY, TypesMapping.getSqlTypeByJava(byte[].class));
    }
    
    public void testGetSqlTypeByJavaPrimitive() throws Exception {
        assertEquals(Types.INTEGER, TypesMapping.getSqlTypeByJava(Integer.TYPE));
        assertEquals(Types.BIGINT, TypesMapping.getSqlTypeByJava(Long.TYPE));
    }

    public void testTypeInfoCompleteness() throws Exception {
        // check counts
        // since more then 1 database type can map to a single JDBC type
        Connection conn = getConnection();
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
        Iterator it = map.databaseTypes.keySet().iterator();
        while (it.hasNext()) {
            List vals = (List) map.databaseTypes.get(it.next());
            actualLen += vals.size();
        }

        // this is bad assertion, since due to some hacks
        // the same database types may map more then once,
        // so we have to use <=
        assertTrue(len <= actualLen);
    }

    TypesMapping createTypesMapping() throws Exception {
        Connection conn = getConnection();

        try {
            DatabaseMetaData md = conn.getMetaData();
            return new TypesMapping(md);
        }
        finally {
            conn.close();
        }
    }
}
