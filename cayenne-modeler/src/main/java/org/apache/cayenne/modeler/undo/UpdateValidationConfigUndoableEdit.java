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

import org.apache.cayenne.modeler.event.model.ProjectEvent;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.projecttools.validation.ValidationConfig;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.List;

public class UpdateValidationConfigUndoableEdit extends CayenneUndoableEdit {

    private final List<String> oldDisabledInspections;
    private final ValidationConfig newConfig;

    /**
     * @param oldDisabledInspections restored as is on undo, including the names unknown to {@link ValidationConfig}
     */
    public UpdateValidationConfigUndoableEdit(ProjectSession session, List<String> oldDisabledInspections,
                                              ValidationConfig newConfig) {
        super(session);
        this.oldDisabledInspections = oldDisabledInspections;
        this.newConfig = newConfig;
    }

    @Override
    public String getPresentationName() {
        return "Update ValidationConfig";
    }

    @Override
    public void redo() throws CannotRedoException {
        Project project = session.project();
        newConfig.applyTo(project);
        session.fireProjectEvent(ProjectEvent.ofChange(this, project));
    }

    @Override
    public void undo() throws CannotUndoException {
        Project project = session.project();
        project.setDisabledInspections(oldDisabledInspections);
        session.fireProjectEvent(ProjectEvent.ofChange(this, project));
    }
}
