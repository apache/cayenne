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
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;

public class CreateTableToModelTest extends MergeCase {

    public void testAddTable() throws Exception {
        dropTableIfPresent(node, "NEW_TABLE");
        assertTokensAndExecute(node, map, 0, 0);

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
        assertTrue(token.getClass().getName(), token instanceof CreateTableToModel);
        logTokens(Collections.singletonList(token));
        execute(token);

        ObjEntity objEntity = null;
        for (ObjEntity candiate : map.getObjEntities()) {
            if (candiate.getDbEntity().getName().equalsIgnoreCase(dbEntity.getName())) {
                objEntity = candiate;
                break;
            }
        }
        assertNotNull(objEntity);

        assertEquals(1, objEntity.getAttributes().size());
        assertEquals("java.lang.String", objEntity
                .getAttributes()
                .iterator()
                .next()
                .getType());

        DataContext ctxt = createDataContext();

        // clear up
        map.removeObjEntity(objEntity.getName(), true);
        map.removeDbEntity(dbEntity.getName(), true);
        ctxt.getEntityResolver().clearCache();
        assertNull(map.getObjEntity(objEntity.getName()));
        assertNull(map.getDbEntity(dbEntity.getName()));
        assertFalse(map.getDbEntities().contains(dbEntity));

        assertTokensAndExecute(node, map, 1, 0);
        assertTokensAndExecute(node, map, 0, 0);
    }

}
