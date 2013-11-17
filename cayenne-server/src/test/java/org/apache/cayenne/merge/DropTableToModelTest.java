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
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DropTableToModelTest extends MergeCase {

    public void testDropTable() throws Exception {
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

        map.addDbEntity(dbEntity);

        assertTokensAndExecute(1, 0);
        assertTokensAndExecute(0, 0);

        ObjEntity objEntity = new ObjEntity("NewTable");
        objEntity.setDbEntity(dbEntity);
        ObjAttribute oatr1 = new ObjAttribute("name");
        oatr1.setDbAttributePath(column2.getName());
        oatr1.setType("java.lang.String");
        objEntity.addAttribute(oatr1);
        map.addObjEntity(objEntity);

        // force drop table in db
        MergerToken token = mergerFactory().createDropTableToDb(dbEntity);
        execute(token);

        List<MergerToken> tokens = createMergeTokens();
        assertEquals(1, tokens.size());
        token = tokens.get(0);
        if (token.getDirection().isToDb()) {
            token = token.createReverse(mergerFactory());
        }
        assertTrue(token instanceof DropTableToModel);
        execute(token);
        resolver.refreshMappingCache();
        assertNull(map.getDbEntity(dbEntity.getName()));
        assertNull(map.getObjEntity(objEntity.getName()));

        // clear up
        map.removeObjEntity(objEntity.getName(), true);
        map.removeDbEntity(dbEntity.getName(), true);
        resolver.refreshMappingCache();
        assertNull(map.getObjEntity(objEntity.getName()));
        assertNull(map.getDbEntity(dbEntity.getName()));
        assertFalse(map.getDbEntities().contains(dbEntity));

        assertTokensAndExecute(0, 0);
    }

}
