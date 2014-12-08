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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.apache.cayenne.merge.builders.ObjectMother.dataMap;
import static org.apache.cayenne.merge.builders.ObjectMother.dbAttr;
import static org.apache.cayenne.merge.builders.ObjectMother.dbEntity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.0.
 */
public class TokensToModelExecution {

    @Test
    public void testCreateAndDropTable() throws Exception {
        DbEntity entity = dbEntity().build();

        DataMap dataMap = dataMap().build();
        assertTrue(dataMap.getDbEntityMap().isEmpty());
        assertTrue(dataMap.getObjEntityMap().isEmpty());

        MergerContext context = new ExecutingMergerContext(dataMap, new DataNode());
        factory().createCreateTableToModel(entity).execute(context);

        assertEquals(1, dataMap.getDbEntityMap().size());
        assertEquals(1, dataMap.getObjEntities().size());
        assertEquals(entity, dataMap.getDbEntity(entity.getName()));

        factory().createDropTableToModel(entity).execute(context);
        assertTrue(dataMap.getDbEntityMap().isEmpty());
        assertTrue(dataMap.getObjEntityMap().isEmpty());
    }

    @Test
    public void testCreateAndDropColumn() throws Exception {
        DbAttribute attr = dbAttr("attr").build();
        DbEntity entity = dbEntity().build();

        DataMap dataMap = dataMap().with(entity).build();
        assertEquals(1, dataMap.getDbEntityMap().size());
        assertTrue(dataMap.getObjEntityMap().isEmpty());

        MergerContext context = new ExecutingMergerContext(dataMap, new DataNode());
        factory().createAddColumnToModel(entity, attr).execute(context);

        assertEquals(1, dataMap.getDbEntityMap().size());
        assertEquals(1, entity.getAttributes().size());
        assertEquals(attr, entity.getAttribute(attr.getName()));

        factory().createDropColumnToModel(entity, attr).execute(context);
        assertEquals(1, dataMap.getDbEntityMap().size());
        assertTrue(entity.getAttributes().isEmpty());
        assertTrue(dataMap.getObjEntityMap().isEmpty());
    }

    private MergerFactory factory() {
        return new MergerFactory();
    }
}
