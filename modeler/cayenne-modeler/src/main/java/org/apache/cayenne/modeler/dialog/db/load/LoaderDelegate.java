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

package org.apache.cayenne.modeler.dialog.db.load;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dbsync.reverse.dbload.DefaultDbLoaderDelegate;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;

final class LoaderDelegate extends DefaultDbLoaderDelegate {

    private DbLoaderContext context;

    LoaderDelegate(DbLoaderContext dbLoaderContext) {
        this.context = dbLoaderContext;
    }

    @Override
    public void dbEntityAdded(DbEntity entity) {
        checkCanceled();
        context.setStatusNote("Importing table '" + entity.getName() + "'...");
    }

    @Override
    public void dbEntityRemoved(DbEntity entity) {
        checkCanceled();
        if (context.isExistingDataMap()) {
            context.getProjectController().fireDbEntityEvent(new EntityEvent(Application.getFrame(), entity, MapEvent.REMOVE));
        }
    }

    @Override
    public boolean dbRelationship(DbEntity entity) {
        checkCanceled();
        context.setStatusNote("Load relationships for '" + entity.getName() + "'...");
        return true;
    }

    @Override
    public boolean dbRelationshipLoaded(DbEntity entity, DbRelationship relationship) {
        checkCanceled();
        context.setStatusNote("Load relationship: '" + entity.getName() + "'; '" + relationship.getName() + "'...");
        return true;
    }

    private void checkCanceled() {
        if (context.isStopping()) {
            throw new CayenneRuntimeException("Reengineering was canceled.");
        }
    }
}
