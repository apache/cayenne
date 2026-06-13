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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.ui.action.RemoveRelationshipAction;

public class CreateDbRelationshipUndoableEdit extends CayenneUndoableEdit {

    private final DbEntity dbEntity;
    private final DbRelationship[] relationships;

    public CreateDbRelationshipUndoableEdit(ProjectSession session, DbEntity dbEntity, DbRelationship[] relationships) {
        super(session);
        this.dbEntity = dbEntity;
        this.relationships = relationships;
    }

    @Override
    public String getPresentationName() {
        return "Create Relationship";
    }

    @Override
    public void redo() throws CannotRedoException {
        CreateRelationshipAction action = globalActions.getAction(CreateRelationshipAction.class);
        for (DbRelationship rel : relationships) {
            action.createDbRelationship(dbEntity, rel);
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveRelationshipAction action = globalActions.getAction(RemoveRelationshipAction.class);
        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        action.removeDbRelationships(dbEntity, relationships);
        session.displayDbEntity(new DbEntityDisplayEvent(this, domain, dbEntity.getDataMap(), dbEntity));
    }
}
