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
package org.apache.cayenne.modeler.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.ui.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.ui.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.ui.project.ProjectController;

public class RemoveRelationshipUndoableEdit extends BaseRemovePropertyUndoableEdit {

    private ObjRelationship[] rels;
    private DbRelationship[] dbRels;

    public RemoveRelationshipUndoableEdit(ProjectController controller, ObjEntity objEntity, ObjRelationship[] rels) {
        super(controller);
        this.objEntity = objEntity;
        this.rels = rels;
    }

    public RemoveRelationshipUndoableEdit(ProjectController controller, DbEntity dbEntity, DbRelationship[] dbRels) {
        super(controller);
        this.dbEntity = dbEntity;
        this.dbRels = dbRels;
    }

    @Override
    public String getPresentationName() {
        if (objEntity != null) {
            return "Remove Obj Relationship";
        } else {
            return "Remove Db Relationship";
        }
    }

    @Override
    public void redo() throws CannotRedoException {
        RemoveRelationshipAction action = globalActions.getAction(RemoveRelationshipAction.class);
        if (objEntity != null) {
            action.removeObjRelationships(objEntity, rels);
            focusObjEntity();
        } else {
            action.removeDbRelationships(dbEntity, dbRels);
            focusDBEntity();
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        CreateRelationshipAction action = globalActions.getAction(CreateRelationshipAction.class);
        if (objEntity != null) {
            for (ObjRelationship r : rels) {
                action.createObjRelationship(objEntity, r);
            }
            focusObjEntity();
        } else {
            for (DbRelationship dr : dbRels) {
                action.createDbRelationship(dbEntity, dr);
            }
            focusDBEntity();
        }
    }
}
