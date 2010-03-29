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

package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.objentity.EntitySyncController;
import org.apache.cayenne.modeler.undo.DbEntitySyncUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.EntityMergeSupport;

/**
 * Action that synchronizes all ObjEntities with the current state of the selected
 * DbEntity.
 * 
 */
public class DbEntitySyncAction extends CayenneAction {

    public static String getActionName() {
        return "Sync Dependent ObjEntities with DbEntity";
    }

    public DbEntitySyncAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-sync.gif";
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        synchDbEntity();
    }

    protected void synchDbEntity() {
        ProjectController mediator = getProjectController();

        DbEntity dbEntity = mediator.getCurrentDbEntity();

        if (dbEntity != null) {

            Iterator it = dbEntity.getDataMap().getMappedEntities(dbEntity).iterator();
            if (!it.hasNext()) {
                return;
            }

            EntityMergeSupport merger = new EntitySyncController(Application
                    .getInstance()
                    .getFrameController(), dbEntity).createMerger();

            if (merger == null) {
                return;
            }

            DbEntitySyncUndoableEdit undoableEdit = new DbEntitySyncUndoableEdit(mediator
                    .getCurrentDataDomain(), mediator.getCurrentDataMap());

            while (it.hasNext()) {
                ObjEntity entity = (ObjEntity) it.next();

                DbEntitySyncUndoableEdit.EntitySyncUndoableListener listener = undoableEdit.new EntitySyncUndoableListener(
                        entity);

                merger.addEntityMergeListener(listener);

                if (merger.isRemoveMeaningfulFKs()) {
                    undoableEdit.addEdit(undoableEdit.new MeaningfulFKsUndoableEdit(
                            entity,
                            merger.getMeaningfulFKs(entity)));
                }

                if (merger.synchronizeWithDbEntity(entity)) {
                    mediator.fireObjEntityEvent(new EntityEvent(
                            this,
                            entity,
                            MapEvent.CHANGE));
                }

                merger.removeEntityMergeListener(listener);
            }

            application.getUndoManager().addEdit(undoableEdit);
        }
    }

    /**
     * Returns <code>true</code> if path contains a ObjEntity object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DbEntity.class) != null;
    }
}
