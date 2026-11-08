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

import org.apache.cayenne.access.jdbc.PSBatchParameter;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;
import org.apache.cayenne.query.resolved.DeleteBatch;
import org.apache.cayenne.query.resolved.InsertBatch;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Types;
import java.util.List;
import java.util.Set;

import static org.apache.cayenne.dba.AdhocTables.compile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JdbcAdapterIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void nativeColumnTypes() throws Exception {
        // check a few types
        checkType(Types.BLOB);
        checkType(Types.ARRAY);
        checkType(Types.DATE);
        checkType(Types.VARCHAR);
    }

    private void checkType(int type) throws java.lang.Exception {
        JdbcAdapter adapter = env.adhocObjectFactory().newInstance(
                JdbcAdapter.class,
                JdbcAdapter.class.getName());

        NativeColumnType[] types = adapter.nativeColumnTypes(type);
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals(type, types[0].jdbcType());
        assertEquals(TypesMapping.getSqlNameByType(type), types[0].nativeType());
        assertFalse(types[0].autoIncrement());
    }

    @Test
    public void createTableUnconstrainedVarcharUsesDefaultLength() {
        JdbcAdapter adapter = env.adhocObjectFactory().newInstance(
                JdbcAdapter.class,
                JdbcAdapter.class.getName());
        DbEntity e = new DbEntity("Test");
        DbAttribute c = new DbAttribute("c");
        c.setType(Types.VARCHAR);   // no max length -> default char length
        e.addAttribute(c);

        assertEquals("CREATE TABLE Test (c VARCHAR(255) NULL)", adapter.createTable(compile(e)));
    }

    @Test
    public void createTableQuoteSqlIdentifiers() {

        if (env.dataNode().getAdapter() instanceof MySQLAdapter mySQLAdapter) {

            DbEntity entity = new DbEntity();
            DbAttribute attr = new DbAttribute();
            attr.setName("name column");
            attr.setType(Types.CHAR);
            attr.setMaxLength(10);
            entity.addAttribute(attr);

            DbKeyGenerator id = new DbKeyGenerator();
            entity.setPrimaryKeyGenerator(id);

            entity.setName("name table");
            DataMap dm = new DataMap();
            dm.setQuotingSQLIdentifiers(true);
            dm.addDbEntity(entity);

            String str = "CREATE TABLE `name table` (`name column` CHAR(10) NULL) ENGINE=InnoDB";
            assertEquals(str, mySQLAdapter.createTable(compile(entity)));
        }
    }

    @Test
    public void runsAsBatch() {
        JdbcAdapter adapter = env.adhocObjectFactory().newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        adapter.setSupportsBatchUpdates(true);

        DbEntity entity = new DbEntity("T");
        TranslatedBatch insert = new TranslatedBatch(new InsertBatch(compile(entity), List.of()), "INSERT",
                new PSBatchParameter[0]);
        assertTrue(adapter.runsAsBatch(insert));

        // optimistic locking needs the per-row update counts, so it never runs as a JDBC batch
        TranslatedBatch lockingDelete = new TranslatedBatch(
                new DeleteBatch(compile(entity), List.of(), Set.of(), true, List.of()), "DELETE",
                new PSBatchParameter[0]);
        assertFalse(adapter.runsAsBatch(lockingDelete));

        adapter.setSupportsBatchUpdates(false);
        assertFalse(adapter.runsAsBatch(insert));
    }
}
