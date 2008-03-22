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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmDeleteDialog;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.ProjectPath;

/**
 * Removes currently selected relationship from either the DbEntity or ObjEntity.
 * 
 * @author Garry Watkins
 */
public class RemoveRelationshipAction extends RemoveAction {

    private final static String ACTION_NAME = "Remove Relationship";

    public static String getActionName() {
        return ACTION_NAME;
    }

    public RemoveRelationshipAction(Application application) {
        super(ACTION_NAME, application);
    }

    /**
     * Returns <code>true</code> if last object in the path contains a removable
     * relationship.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.getObject() instanceof Relationship;
    }

    public void performAction(ActionEvent e) {
        ConfirmDeleteDialog dialog = getConfirmDeleteDialog();

        if (getProjectController().getCurrentObjRelationship() != null) {
            if (dialog.shouldDelete("ObjRelationship", getProjectController()
                    .getCurrentObjRelationship().getName())) {
                removeObjRelationship();
            }
        }
        else if (getProjectController().getCurrentDbRelationship() != null) {
            if (dialog.shouldDelete("DbRelationship", getProjectController()
                    .getCurrentDbRelationship().getName())) {
                removeDbRelationship();
            }
        }
    }

    protected void removeObjRelationship() {
        ProjectController mediator = getProjectController();
        ObjEntity entity = mediator.getCurrentObjEntity();
        ObjRelationship rel = mediator.getCurrentObjRelationship();
        entity.removeRelationship(rel.getName());
        RelationshipEvent e = new RelationshipEvent(
                Application.getFrame(),
                rel,
                entity,
                MapEvent.REMOVE);
        mediator.fireObjRelationshipEvent(e);
    }

    protected void removeDbRelationship() {
        ProjectController mediator = getProjectController();
        DbEntity entity = mediator.getCurrentDbEntity();
        DbRelationship rel = mediator.getCurrentDbRelationship();
        entity.removeRelationship(rel.getName());
        ProjectUtil.cleanObjMappings(mediator.getCurrentDataMap());

        RelationshipEvent e = new RelationshipEvent(
                Application.getFrame(),
                rel,
                entity,
                MapEvent.REMOVE);
        mediator.fireDbRelationshipEvent(e);
    }
}
