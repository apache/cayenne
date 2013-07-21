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

import java.sql.Types;

import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class JdbcAdapterTest extends ServerCase {

    @Inject
    private DbAdapter dbAdapter;
    
    @Inject
    private AdhocObjectFactory objectFactory;

    public void testExternalTypesForJdbcType() throws Exception {
        // check a few types
        checkType(Types.BLOB);
        checkType(Types.ARRAY);
        checkType(Types.DATE);
        checkType(Types.VARCHAR);
    }

    private void checkType(int type) throws java.lang.Exception {
        JdbcAdapter adapter = objectFactory.newInstance(
                JdbcAdapter.class, 
                JdbcAdapter.class.getName());

        String[] types = adapter.externalTypesForJdbcType(type);
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals(TypesMapping.getSqlNameByType(type), types[0]);
    }

    public void testCreateTableQuoteSqlIdentifiers() {

        if (dbAdapter instanceof MySQLAdapter) {

            DbEntity entity = new DbEntity();
            DbAttribute attr = new DbAttribute();
            attr.setName("name column");
            attr.setType(1);
            entity.addAttribute(attr);

            DbKeyGenerator id = new DbKeyGenerator();
            entity.setPrimaryKeyGenerator(id);

            DataMap dm = new DataMap();
            dm.setQuotingSQLIdentifiers(true);
            entity.setDataMap(dm);
            entity.setName("name table");

            MySQLAdapter adaptMySQL = (MySQLAdapter) dbAdapter;
            String str = "CREATE TABLE `name table` (`name column` CHAR NULL) ENGINE=InnoDB";
            assertEquals(str, adaptMySQL.createTable(entity));
        }
    }
}
