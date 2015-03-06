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

import org.apache.cayenne.access.loader.DefaultDbLoaderDelegate;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @since 4.0
 */
class DbImportDbLoaderDelegate extends DefaultDbLoaderDelegate {

    private final List<DbEntity> addedDbEntities;
    private final List<DbEntity> removedDbEntities;
    private final List<ObjEntity> addedObjEntities;
    private final List<ObjEntity> removedObjEntities;

    DbImportDbLoaderDelegate() {
        addedDbEntities = new ArrayList<DbEntity>();
        removedDbEntities = new ArrayList<DbEntity>();
        addedObjEntities = new ArrayList<ObjEntity>();
        removedObjEntities = new ArrayList<ObjEntity>();
    }

    public void dbEntityAdded(DbEntity ent) {
        ent.getDataMap().addDbEntity(ent);
        addedDbEntities.add(ent);
    }

    public void dbEntityRemoved(DbEntity ent) {
        ent.getDataMap().removeDbEntity(ent.getName());
        removedDbEntities.add(ent);
    }

    public void objEntityAdded(ObjEntity ent) {
        ent.getDataMap().addObjEntity(ent);
        addedObjEntities.add(ent);
    }

    public void objEntityRemoved(ObjEntity ent) {
        ent.getDataMap().removeObjEntity(ent.getName());
        removedObjEntities.add(ent);
    }

    public List<DbEntity> getAddedDbEntities() {
        return Collections.unmodifiableList(addedDbEntities);
    }

    public List<DbEntity> getRemovedDbEntities() {
        return Collections.unmodifiableList(removedDbEntities);
    }

    public List<ObjEntity> getAddedObjEntities() {
        return Collections.unmodifiableList(addedObjEntities);
    }

    public List<ObjEntity> getRemovedObjEntities() {
        return Collections.unmodifiableList(removedObjEntities);
    }
}
