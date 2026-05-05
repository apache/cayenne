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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.validator.ProjectValidatorDialog;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * UI action that performs full project validation.
 */
public class ValidateAction extends ModelerAbstractAction {

    private ProjectValidatorDialog dialog;

    public ValidateAction(Application application) {
        super("Validate Project", application);
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | InputEvent.SHIFT_DOWN_MASK);
    }

    /**
     * Validates project for possible conflicts and incomplete mappings.
     */
    @Override
    public void performAction(ActionEvent e) {

        ProjectValidator projectValidator = app.getProjectValidator();
        ValidationResult validationResult = projectValidator.validate(getCurrentProject()
                .getRootNode());

        if (!validationResult.getFailures().isEmpty()) {
            showFailures(validationResult.getFailures());
        }
        else {
            disposeDialog();
            ProjectValidatorDialog.showOnSuccess(app);
        }
    }

    /**
     * Displays the given failures, reusing the existing validator dialog if one is open.
     * Used both by this action and by other callers that need to surface failures (project
     * open, save-as) so that at most one dialog is visible at a time.
     */
    public void showFailures(List<ValidationFailure> failures) {
        if (dialog == null || !dialog.isDisplayable()) {
            dialog = new ProjectValidatorDialog(getProjectSession(), app.getFrame());
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    dialog = null;
                }
            });
        }
        dialog.showOnFailures(failures);
    }

    private void disposeDialog() {
        if (dialog != null && dialog.isDisplayable()) {
            dialog.dispose();
        }
    }
}