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

package org.apache.cayenne.modeler.ui.project.validator;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.DisableValidationInspectionAction;
import org.apache.cayenne.modeler.action.ShowValidationOptionAction;
import org.apache.cayenne.modeler.action.ValidateAction;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ProjectValidationFailure;
import org.apache.cayenne.validation.ValidationFailure;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

public class ProjectValidatorDialogController extends ChildController<ProjectController> {

    private final ProjectValidatorDialogView view;

    public ProjectValidatorDialogController(ProjectController parent) {
        super(parent);
        this.view = new ProjectValidatorDialogView(this);
        view.centerWindow();
    }

    @Override
    public Component getView() {
        return view;
    }

    public ProjectController getProjectController() {
        return parent;
    }

    public void showOnFailures(List<ValidationFailure> failures) {
        view.showProblems(failures);
    }

    public static void showOnSuccess() {
        JOptionPane.showMessageDialog(Application.getInstance().getFrameController().getView(), "Cayenne project is valid.");
    }

    void onClose() {
        view.setVisible(false);
        view.dispose();
    }

    void onRefresh(ActionEvent e) {
        getApplication().getActionManager().getAction(ValidateAction.class).actionPerformed(e);
    }

    void onFailedObjectSelected(ValidationFailure failure) {
        ValidationDisplayHandler.getErrorMsg(failure).displayField(parent, application.getFrameController().getView());
    }

    void onSelectionChanged(ValidationFailure failure) {
        Inspection inspection = failure instanceof ProjectValidationFailure
                ? ((ProjectValidationFailure) failure).getInspection()
                : null;
        ActionManager actionManager = getApplication().getActionManager();
        actionManager.getAction(DisableValidationInspectionAction.class)
                .putInspection(inspection)
                .setEnabled(inspection != null);
        actionManager.getAction(ShowValidationOptionAction.class)
                .putInspection(inspection);
    }
}
