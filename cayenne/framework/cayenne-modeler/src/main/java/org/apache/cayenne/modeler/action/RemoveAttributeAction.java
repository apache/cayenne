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

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.ProjectPath;

/**
 * Removes currently selected attribute from either the DbEntity or ObjEntity.
 * 
 * @author Garry Watkins
 */
public class RemoveAttributeAction extends RemoveAction {

    private final static String ACTION_NAME = "Remove Attribute";

    public static String getActionName() {
        return ACTION_NAME;
    }

    public RemoveAttributeAction(Application application) {
        super(ACTION_NAME, application);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable
     * attribute.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.getObject() instanceof Attribute;
    }

    public void performAction(ActionEvent e) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog();

        if (getProjectController().getCurrentObjAttribute() != null) {
            if (dialog.shouldDelete("ObjAttribute", getProjectController()
                    .getCurrentObjAttribute().getName())) {
                removeObjAttribute();
            }
        }
        else if (getProjectController().getCurrentDbAttribute() != null) {
            if (dialog.shouldDelete("DbAttribute", getProjectController()
                    .getCurrentDbAttribute().getName())) {
                removeDbAttribute();
            }
        }
    }

    protected void removeDbAttribute() {
        ProjectController mediator = getProjectController();
        DbEntity entity = mediator.getCurrentDbEntity();
        DbAttribute attrib = mediator.getCurrentDbAttribute();
        entity.removeAttribute(attrib.getName());
        ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());

        AttributeEvent e = new AttributeEvent(
                Application.getFrame(),
                attrib,
                entity,
                MapEvent.REMOVE);
        mediator.fireDbAttributeEvent(e);
    }

    protected void removeObjAttribute() {
        ProjectController mediator = getProjectController();
        ObjEntity entity = mediator.getCurrentObjEntity();
        ObjAttribute attrib = mediator.getCurrentObjAttribute();

        entity.removeAttribute(attrib.getName());
        AttributeEvent e = new AttributeEvent(
                Application.getFrame(),
                attrib,
                entity,
                MapEvent.REMOVE);
        mediator.fireObjAttributeEvent(e);
    }
}
