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
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.action.CreateAttributeAction;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeAction;

public class CreateDbAttributeUndoableEdit extends CayenneUndoableEdit {

    private final DataChannelDescriptor domain;
    private final DataMap dataMap;
    private final DbEntity dbEntity;
    private final DbAttribute attribute;

    public CreateDbAttributeUndoableEdit(ProjectSession session, DataChannelDescriptor domain, DataMap map,
            DbEntity dbEntity, DbAttribute attribute) {
        super(session);
        this.domain = domain;
        this.dataMap = map;
        this.dbEntity = dbEntity;
        this.attribute = attribute;
    }

    @Override
    public String getPresentationName() {
        return "Create Attribute";
    }

    @Override
    public void redo() throws CannotRedoException {
        CreateAttributeAction action = globalActions.getAction(CreateAttributeAction.class);
        action.createDbAttribute(dataMap, dbEntity, attribute);
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveAttributeAction action = globalActions.getAction(RemoveAttributeAction.class);
        action.removeDbAttributes(dataMap, dbEntity, new DbAttribute[] {attribute});
        session.displayDbEntity(new DbEntityDisplayEvent(this, domain, dataMap, dbEntity));
    }
}
