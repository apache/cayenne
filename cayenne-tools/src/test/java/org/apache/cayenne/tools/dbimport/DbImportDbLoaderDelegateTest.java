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

package org.apache.cayenne.tools.dbimport;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.tools.dbimport.DbImportDbLoaderDelegate;

public class DbImportDbLoaderDelegateTest extends TestCase {

    private DbImportDbLoaderDelegate delegate;
    private DataMap dataMap;
    private DbEntity dbEntity;
    private ObjEntity objEntity;

    @Override
    public void setUp() {
        delegate = new DbImportDbLoaderDelegate();
        dataMap = new DataMap();

        dbEntity = new DbEntity("TestDbEntity");
        dbEntity.setDataMap(dataMap);

        objEntity = new ObjEntity("TestObjEntity");
        objEntity.setDataMap(dataMap);
    }

    public void testOverwriteDbEntity() throws CayenneException {
        assertFalse(delegate.overwriteDbEntity(dbEntity));
    }

    public void testDbEntityAdded() {
        delegate.dbEntityAdded(dbEntity);

        final List<DbEntity> entities = Arrays.asList(dbEntity);

        assertEquals(1, dataMap.getDbEntities().size());
        assertTrue(dataMap.getDbEntities().containsAll(entities));

        assertEquals(entities, delegate.getAddedDbEntities());
    }

    public void testDbEntityRemoved() {
        // Make sure the entity is in the datamap to start.
        dataMap.addDbEntity(dbEntity);

        delegate.dbEntityRemoved(dbEntity);

        // The entity should no longer be in the map.
        assertEquals(0, dataMap.getDbEntities().size());

        assertEquals(Arrays.asList(dbEntity), delegate.getRemovedDbEntities());
    }

    public void testObjEntityAdded() {
        delegate.objEntityAdded(objEntity);

        final List<ObjEntity> entities = Arrays.asList(objEntity);

        assertEquals(1, dataMap.getObjEntities().size());
        assertTrue(dataMap.getObjEntities().containsAll(entities));

        assertEquals(entities, delegate.getAddedObjEntities());
    }

    public void testObjEntityRemoved() {
        // Make sure the entity is in the datamap to start.
        dataMap.addObjEntity(objEntity);

        delegate.objEntityRemoved(objEntity);

        // The entity should no longer be in the map.
        assertEquals(0, dataMap.getObjEntities().size());

        assertEquals(Arrays.asList(objEntity), delegate.getRemovedObjEntities());
    }
}
