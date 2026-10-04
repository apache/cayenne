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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.project.Project;
import org.apache.cayenne.modeler.event.model.ProjectEvent;
import org.apache.cayenne.project.xml.ProjectMetaData;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.undo.CayenneUndoManager;
import org.apache.cayenne.modeler.undo.UpdateValidationConfigUndoableEdit;
import org.apache.cayenne.projecttools.validation.ValidationConfig;

import java.awt.event.ActionEvent;

/**
 * Requires config provided with {@link UpdateValidationConfigAction#putConfig(ValidationConfig)}.
 */
public class UpdateValidationConfigAction extends AppAction {

    private static final String CONFIG_PARAM = "config";

    private boolean undoable;

    public UpdateValidationConfigAction(Application application) {
        super(application, "Update ValidationConfig");
        this.undoable = true;
    }

    protected UpdateValidationConfigAction(String name, Application application) {
        super(application, name);
    }

    public void performAction(Object source) {
        performAction(new ActionEvent(source, ActionEvent.ACTION_PERFORMED, null));
    }

    @Override
    public void performAction(ActionEvent e) {
        ProjectMetaData metaData = app.getMetaData();
        Project project = app.getFrame().getProjectSession().project();
        ValidationConfig config = (ValidationConfig) getValue(CONFIG_PARAM);
        ValidationConfig oldConfig = ValidationConfig.fromMetadata(metaData, project);
        metaData.add(project, config);

        if (undoable) {
            CayenneUndoManager undoManager = app.getUndoManager();
            undoManager.addEdit(new UpdateValidationConfigUndoableEdit(getProjectSession(), oldConfig, config));
        }
        getProjectSession().fireProjectEvent(ProjectEvent.ofChange(e.getSource(), project));
    }

    public UpdateValidationConfigAction putConfig(ValidationConfig config) {
        putValue(CONFIG_PARAM, config);
        return this;
    }

    public UpdateValidationConfigAction setUndoable(boolean undoable) {
        this.undoable = undoable;
        return this;
    }
}
