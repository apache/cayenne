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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.objentity.EntitySyncController;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.EntityMergeSupport;

/**
 * Action that synchronizes a given ObjEntity with the current state of the underlying
 * DbEntity.
 * 
 */
public class ObjEntitySyncAction extends CayenneAction {

    public static String getActionName() {
        return "Sync ObjEntity with DbEntity";
    }

    public ObjEntitySyncAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-sync.gif";
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_U, Toolkit
                .getDefaultToolkit()
                .getMenuShortcutKeyMask());
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        synchObjEntity();
    }

    protected void synchObjEntity() {
        ProjectController mediator = getProjectController();
        ObjEntity entity = mediator.getCurrentObjEntity();

        if (entity != null && entity.getDbEntity() != null) {
            EntityMergeSupport merger = new EntitySyncController(Application
                    .getInstance()
                    .getFrameController(), entity).createMerger();

            if (merger == null) {
                return;
            }

            if (merger.synchronizeWithDbEntity(entity)) {
                mediator
                        .fireObjEntityEvent(new EntityEvent(this, entity, MapEvent.CHANGE));
                mediator.fireObjEntityDisplayEvent(new EntityDisplayEvent(
                        this,
                        entity,
                        entity.getDataMap(),
                        mediator.getCurrentDataDomain()));
            }
        }
    }

    /**
     * Returns <code>true</code> if path contains a ObjEntity object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(ObjEntity.class) != null;
    }
}
