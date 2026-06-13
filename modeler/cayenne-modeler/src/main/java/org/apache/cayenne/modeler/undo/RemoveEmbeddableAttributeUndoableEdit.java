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

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.action.CreateAttributeAction;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeAction;

public class RemoveEmbeddableAttributeUndoableEdit extends BaseRemovePropertyUndoableEdit {

    private final EmbeddableAttribute[] attributes;

    public RemoveEmbeddableAttributeUndoableEdit(ProjectSession session, Embeddable embeddable,
            EmbeddableAttribute[] attributes) {
        super(session);
        this.embeddable = embeddable;
        this.attributes = attributes;
    }

    @Override
    public void redo() throws CannotRedoException {
        RemoveAttributeAction action = globalActions.getAction(RemoveAttributeAction.class);
        action.removeEmbeddableAttributes(embeddable, attributes);
        focusEmbeddable();
    }

    @Override
    public void undo() throws CannotUndoException {
        CreateAttributeAction action = globalActions.getAction(CreateAttributeAction.class);
        for (EmbeddableAttribute attr : attributes) {
            action.createEmbAttribute(embeddable, attr);
        }
        focusEmbeddable();
    }

    @Override
    public String getPresentationName() {
        return (attributes.length > 1) ? "Remove Embeddable Attributes" : "Remove Embeddable Attribute";
    }
}
