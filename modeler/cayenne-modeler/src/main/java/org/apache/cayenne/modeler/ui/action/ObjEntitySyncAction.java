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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dbsync.merge.context.EntityMergeSupport;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.event.model.ModelEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.entitysync.EntitySyncController;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Action that synchronizes a given ObjEntity with the current state of the underlying
 * DbEntity.
 *
 */
public class ObjEntitySyncAction extends ModelerAbstractAction {

    public static String getActionName() {
        return "Sync ObjEntity with DbEntity";
    }

    public ObjEntitySyncAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-sync.png";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    /**
     * @see ModelerAbstractAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        syncObjEntity();
    }

    protected void syncObjEntity() {
        ProjectController mediator = getProjectController();
        ObjEntity entity = mediator.getSelectedObjEntity();

        if (entity != null && entity.getDbEntity() != null) {
            EntityMergeSupport merger = new EntitySyncController(application.getFrameController(), entity).createMerger();

            if (merger == null) {
                return;
            }

            merger.setNameGenerator(new DbEntitySyncAction.PreserveRelationshipNameGenerator());

            if (merger.synchronizeWithDbEntity(entity)) {
                mediator
                        .fireObjEntityEvent(ObjEntityEvent.ofChange(this, entity));
                mediator.displayObjEntity(new EntityDisplayEvent(
                        this,
                        entity,
                        entity.getDataMap(),
                        (DataChannelDescriptor) mediator.getProject().getRootNode()));
            }
        }
    }
}
