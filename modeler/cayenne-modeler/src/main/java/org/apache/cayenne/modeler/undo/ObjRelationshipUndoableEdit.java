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

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.event.model.ObjRelationshipEvent;
import org.apache.cayenne.modeler.project.ProjectSession;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class ObjRelationshipUndoableEdit extends CayenneUndoableEdit {

    private final ObjRelationship relationship;
    private final ObjRelationship prevRelationship;

    public ObjRelationshipUndoableEdit(ProjectSession session, ObjRelationship relationship) {
        super(session);
        this.relationship = relationship;
        this.prevRelationship = copyRelationship(relationship);
    }

    @Override
    public void redo() throws CannotRedoException {
        fireRelationshipEvent(relationship, prevRelationship);
    }

    @Override
    public void undo() throws CannotUndoException {
        fireRelationshipEvent(prevRelationship, relationship);
    }

    private void fireRelationshipEvent(ObjRelationship relToFire, ObjRelationship currRel) {
        ObjEntity objEntity = currRel.getSourceEntity();
        objEntity.removeRelationship(currRel.getName());
        objEntity.addRelationship(relToFire);
        session.fireObjRelationshipEvent(ObjRelationshipEvent.ofAdd(this, relToFire, relToFire.getSourceEntity()));
    }

    @Override
    public String getRedoPresentationName() {
        return "Redo Edit relationship";
    }

    @Override
    public String getUndoPresentationName() {
        return "Undo Edit relationship";
    }

    private ObjRelationship copyRelationship(ObjRelationship objRelationship) {
        ObjRelationship rel = new ObjRelationship();
        rel.setName(objRelationship.getName());
        rel.setTargetEntityName(objRelationship.getTargetEntityName());
        rel.setSourceEntity(objRelationship.getSourceEntity());
        rel.setDeleteRule(objRelationship.getDeleteRule());
        rel.setUsedForLocking(objRelationship.isUsedForLocking());
        rel.setDbRelationshipPath(objRelationship.getDbRelationshipPath());
        rel.setCollectionType(objRelationship.getCollectionType());
        rel.setMapKey(objRelationship.getMapKey());
        return rel;
    }
}
