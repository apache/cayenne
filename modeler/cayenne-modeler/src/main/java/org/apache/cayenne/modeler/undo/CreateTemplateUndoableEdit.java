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

import org.apache.cayenne.map.template.ClassTemplate;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.action.CreateTemplateAction;
import org.apache.cayenne.modeler.action.RemoveAction;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * @since 4.0
 */
public class CreateTemplateUndoableEdit extends CayenneUndoableEdit {
    @Override
    public boolean canRedo() {
        return true;
    }

    @Override
    public String getPresentationName() {
        return "Create Template";
    }

    private DataMap map;
    private ClassTemplate template;

    public CreateTemplateUndoableEdit(DataMap map, ClassTemplate template) {
        this.map = map;
        this.template = template;
    }

    @Override
    public void redo() throws CannotRedoException {
        CreateTemplateAction action = actionManager.getAction(CreateTemplateAction.class);
        action.createTemplate(map, template);
    }

    @Override
    public void undo() throws CannotUndoException {
        RemoveAction action = actionManager.getAction(RemoveAction.class);
        action.removeTemplate(map, template);
    }
}
