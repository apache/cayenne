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


package org.apache.cayenne.modeler.dialog;

import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.mvc.RootController;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

import javax.swing.WindowConstants;
import java.awt.Component;
import java.util.Collection;


public class ValidationResultBrowserController extends ChildController<RootController> {

    private final ValidationResultBrowserView view;

    public ValidationResultBrowserController(RootController parent) {
        super(parent);
        this.view = new ValidationResultBrowserView();
        view.getCloseButton().addActionListener(e -> closeDialogAction());
    }

    @Override
    public Component getView() {
        return view;
    }

    public void closeDialogAction() {
        view.dispose();
    }

    public void startupAction(
            String title,
            String message,
            Collection<ValidationResult> failures) {

        this.view.setTitle(title);
        this.view.getMessageLabel().setText(message);

        for (ValidationResult failure : failures) {
            if (failure != null) {
                this.view.getErrorsDisplay().append(buildValidationText(failure) + " ");
            }
        }

        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }

    public void startupAction(
            String title,
            String message,
            ValidationResult validationResult) {

        this.view.setTitle(title);
        this.view.getMessageLabel().setText(message);
        this.view.getErrorsDisplay().setText(buildValidationText(validationResult));

        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);
    }

    private String buildValidationText(ValidationResult validationResult) {
        StringBuilder buffer = new StringBuilder();
        String separator = System.lineSeparator();

        for (ValidationFailure failure : validationResult.getFailures()) {

            if (buffer.length() > 0) {
                buffer.append(separator);
            }

            if (failure.getSource() != null) {
                buffer.append("[SQL: ").append(failure.getSource()).append("] - ");
            }

            if (failure.getDescription() != null) {
                buffer.append(failure.getDescription());
            }
        }

        return buffer.toString();
    }
}
