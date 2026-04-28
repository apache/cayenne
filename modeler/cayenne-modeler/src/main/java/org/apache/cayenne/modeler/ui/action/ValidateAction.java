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
import org.apache.cayenne.modeler.ui.project.validator.ProjectValidatorDialogController;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * UI action that performs full project validation.
 */
public class ValidateAction extends ModelerAbstractAction {

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

        ProjectValidator projectValidator = application.getProjectValidator();
        ValidationResult validationResult = projectValidator.validate(getCurrentProject()
                .getRootNode());

        if (!validationResult.getFailures().isEmpty()) {
            new ProjectValidatorDialogController(getProjectController()).showOnFailures(validationResult.getFailures());
        }
        else {
            ProjectValidatorDialogController.showOnSuccess(application);
        }
    }
}