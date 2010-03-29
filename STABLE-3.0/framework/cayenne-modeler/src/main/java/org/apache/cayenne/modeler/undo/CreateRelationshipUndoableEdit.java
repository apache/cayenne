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
package org.apache.cayenne.modeler.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;

public class CreateRelationshipUndoableEdit extends CayenneUndoableEdit {

    private ObjEntity objEnt;
    private ObjRelationship[] objectRel;

    private DbEntity dbEnt;
    private DbRelationship[] dbRel;

    public CreateRelationshipUndoableEdit(ObjEntity objEnt, ObjRelationship[] objectRel) {
        this.objEnt = objEnt;
        this.objectRel = objectRel;
    }

    public CreateRelationshipUndoableEdit(DbEntity dbEnt, DbRelationship[] dbRel) {
        this.dbEnt = dbEnt;
        this.dbRel = dbRel;
    }
    
    @Override
    public String getPresentationName() {
        return "Create Relationship";
    }

    @Override
    public void redo() throws CannotRedoException {
        CreateRelationshipAction action = (CreateRelationshipAction) actionManager
                .getAction(CreateRelationshipAction.getActionName());

        if (objEnt != null) {
            for (ObjRelationship rel : objectRel) {
                action.createObjRelationship(objEnt, rel);
            }
        }

        if (dbEnt != null) {
            for (DbRelationship rel : dbRel) {
                action.createDbRelationship(dbEnt, rel);
            }
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveRelationshipAction action = (RemoveRelationshipAction) actionManager
                .getAction(RemoveRelationshipAction.getActionName());

        if (objEnt != null) {
            action.removeObjRelationships(objEnt, objectRel);
            controller.fireObjEntityDisplayEvent(new EntityDisplayEvent(
                    this,
                    objEnt,
                    objEnt.getDataMap(),
                    controller.getCurrentDataDomain()));
        }

        if (dbEnt != null) {
            action.removeDbRelationships(dbEnt, dbRel);
            controller.fireDbEntityDisplayEvent(new EntityDisplayEvent(this, dbEnt, dbEnt
                    .getDataMap(), controller.getCurrentDataDomain()));
        }
    }
}
