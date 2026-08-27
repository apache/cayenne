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
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.action.CreateAttributeAction;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeAction;

public class CreateObjAttributeUndoableEdit extends CayenneUndoableEdit {

    private final DataChannelDescriptor domain;
    private final DataMap dataMap;
    private final ObjEntity objEntity;
    private final ObjAttribute attribute;

    public CreateObjAttributeUndoableEdit(ProjectSession session, DataChannelDescriptor domain, DataMap map,
            ObjEntity objEntity, ObjAttribute attribute) {
        super(session);
        this.domain = domain;
        this.dataMap = map;
        this.objEntity = objEntity;
        this.attribute = attribute;
    }

    @Override
    public String getPresentationName() {
        return "Create Attribute";
    }

    @Override
    public void redo() throws CannotRedoException {
        CreateAttributeAction action = globalActions.getAction(CreateAttributeAction.class);
        action.createObjAttribute(dataMap, objEntity, attribute);
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveAttributeAction action = globalActions.getAction(RemoveAttributeAction.class);
        action.removeObjAttributes(objEntity, new ObjAttribute[] {attribute});
        session.displayObjEntity(new ObjEntityDisplayEvent(this, domain, dataMap, objEntity));
    }
}
