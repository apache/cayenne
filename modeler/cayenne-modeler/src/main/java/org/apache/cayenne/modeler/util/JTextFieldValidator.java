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
package org.apache.cayenne.modeler.util;

import org.apache.cayenne.modeler.dialog.validator.ValidatorDialog;

import javax.swing.JTextField;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @since 4.2
 */
public class JTextFieldValidator implements FocusListener {

    private final JTextField jTextField;
    private final Predicate<String> validator;
    private final Function<String, String> message;

    static public void addValidation(JTextField field, Predicate<String> validator, Function<String, String> message) {
        JTextFieldValidator validationListener
                = new JTextFieldValidator(field, validator, message);
        field.addFocusListener(validationListener);
    }

    static public void addValidation(JTextField field, Predicate<String> validator, String message) {
        addValidation(field, validator, text -> message);
    }

    static public void addValidation(JTextField field, Predicate<String> validator) {
        addValidation(field, validator, text -> "There are illegal chars in this field");
    }

    JTextFieldValidator(JTextField jTextField, Predicate<String> validator, Function<String, String> message) {
        this.jTextField = jTextField;
        this.validator = validator;
        this.message = message;
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        String text = jTextField.getText();
        if(validator.test(text)) {
            jTextField.setBackground(ValidatorDialog.WARNING_COLOR);
            jTextField.setToolTipText(message.apply(text));
        }
    }
}
