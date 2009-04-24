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


package org.apache.cayenne.modeler.dialog;

import java.awt.Component;
import javax.swing.WindowConstants;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;

/**
 */
public class ValidationResultBrowser extends CayenneController {

    protected ValidationResultBrowserView view;

    public ValidationResultBrowser(CayenneController parent) {
        super(parent);

        this.view = new ValidationResultBrowserView();

        initController();
    }

    protected void initController() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);
        builder.bindToAction(view.getCloseButton(), "closeDialogAction()");
    }

    public Component getView() {
        return view;
    }

    public void closeDialogAction() {
        view.dispose();
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

    /**
     * Creates validation text for the validation result.
     */
    protected String buildValidationText(ValidationResult validationResult) {
        StringBuffer buffer = new StringBuffer();
        String separator = System.getProperty("line.separator");

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
