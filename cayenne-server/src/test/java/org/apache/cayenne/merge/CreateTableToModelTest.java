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
package org.apache.cayenne.merge;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class CreateTableToModelTest extends MergeCase {

    public void testAddTable() throws Exception {
        dropTableIfPresent("NEW_TABLE");
        assertTokensAndExecute(0, 0);

        DbEntity dbEntity = new DbEntity("NEW_TABLE");

        DbAttribute column1 = new DbAttribute("ID", Types.INTEGER, dbEntity);
        column1.setMandatory(true);
        column1.setPrimaryKey(true);
        dbEntity.addAttribute(column1);

        DbAttribute column2 = new DbAttribute("NAME", Types.VARCHAR, dbEntity);
        column2.setMaxLength(10);
        column2.setMandatory(false);
        dbEntity.addAttribute(column2);

        // for the new entity to the db
        execute(mergerFactory().createCreateTableToDb(dbEntity));

        List<MergerToken> tokens = createMergeTokens();
        assertEquals(1, tokens.size());
        MergerToken token = tokens.get(0);
        if (token.getDirection().isToDb()) {
            token = token.createReverse(mergerFactory());
        }
        assertTrue(token.getClass().getName(),
                token instanceof CreateTableToModel);

        execute(token);

        ObjEntity objEntity = null;
        for (ObjEntity candidate : map.getObjEntities()) {
            if (dbEntity.getName().equalsIgnoreCase(candidate.getDbEntityName())) {
                objEntity = candidate;
                break;
            }
        }
        assertNotNull(objEntity);

        assertEquals(objEntity.getClassName(), map.getDefaultPackage() + "."
                + objEntity.getName());
        assertEquals(objEntity.getSuperClassName(), map.getDefaultSuperclass());
        assertEquals(objEntity.getClientClassName(),
                map.getDefaultClientPackage() + "." + objEntity.getName());
        assertEquals(objEntity.getClientSuperClassName(),
                map.getDefaultClientSuperclass());

        assertEquals(1, objEntity.getAttributes().size());
        assertEquals("java.lang.String", objEntity.getAttributes().iterator()
                .next().getType());

        // clear up
        // fix psql case issue
        map.removeDbEntity(objEntity.getDbEntity().getName(), true);
        map.removeObjEntity(objEntity.getName(), true);
        map.removeDbEntity(dbEntity.getName(), true);
        resolver.refreshMappingCache();
        assertNull(map.getObjEntity(objEntity.getName()));
        assertNull(map.getDbEntity(dbEntity.getName()));
        assertFalse(map.getDbEntities().contains(dbEntity));

        assertTokensAndExecute(1, 0);
        assertTokensAndExecute(0, 0);
    }
}
