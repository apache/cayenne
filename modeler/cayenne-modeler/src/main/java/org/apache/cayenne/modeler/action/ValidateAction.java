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

package org.apache.cayenne.modeler.action;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.validation.ProjectValidator;
import org.apache.cayenne.validation.ValidationResult;

/**
 * UI action that performs full project validation.
 * 
 */
public class ValidateAction extends CayenneAction {

    public static String getActionName() {
        return "Validate Project";
    }

    public ValidateAction(Application application) {
        super(getActionName(), application);
    }

    public KeyStroke getAcceleratorKey() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit
                .getDefaultToolkit()
                .getMenuShortcutKeyMask()
                | ActionEvent.SHIFT_MASK);
    }

    /**
     * Validates project for possible conflicts and incomplete mappings.
     */
    public void performAction(ActionEvent e) {

        ProjectValidator projectValidator = getApplication().getInjector().getInstance(
                ProjectValidator.class);
        ValidationResult validationResult = projectValidator.validate(getCurrentProject()
                .getRootNode());

        // If there were errors or warnings at validation, display them
        if (validationResult.getFailures().size() > 0) {
            ValidatorDialog.showDialog(Application.getFrame(), validationResult
                    .getFailures());
        }
        else {
            ValidatorDialog.showValidationSuccess(Application.getFrame());
        }
    }
}