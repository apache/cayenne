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
package org.apache.cayenne.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.apache.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.apache.cayenne.validation.ValidationException;

/**
 * A generic text adapter that is bound to a bean property.
 * 
 */
public class TextBinding extends BindingBase {

    protected JTextComponent textComponent;

    protected Color defaultBGColor;
    protected Color errorColor;
    protected String defaultToolTip;

    public TextBinding(JTextField textField, String expression) {
        super(expression);
        this.textComponent = textField;
        initComponentDefaults();

        textField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!modelUpdateDisabled) {
                    updateModel();
                }
            }
        });

        textComponent.setInputVerifier(new InputVerifier() {

            public boolean verify(JComponent c) {
                updateModel();
                // release focus after coloring the field...
                return true;
            }
        });

    }

    public TextBinding(JTextArea textArea, String property) {
        super(property);
        this.textComponent = textArea;
        initComponentDefaults();

        textComponent.getDocument().addDocumentListener(new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                verifyTextChange(e);
            }

            public void changedUpdate(DocumentEvent e) {
                verifyTextChange(e);
            }

            public void removeUpdate(DocumentEvent e) {
                verifyTextChange(e);
            }

            void verifyTextChange(DocumentEvent e) {
                if (!modelUpdateDisabled) {
                    updateModel();
                }
            }
        });
    }

    protected void initComponentDefaults() {
        this.errorColor = ValidatorDialog.WARNING_COLOR;
        this.defaultBGColor = textComponent.getBackground();
        this.defaultToolTip = textComponent.getToolTipText();
    }

    public void updateView() {
        Object value = getValue();
        String text = (value != null) ? value.toString() : null;

        modelUpdateDisabled = true;
        try {
            clear();
            textComponent.setText(text);
        }
        finally {
            modelUpdateDisabled = false;
        }
    }

    /**
     * Returns internal text component.
     */
    public Component getView() {
        return textComponent;
    }

    protected void updateModel() {
        try {
            setValue(textComponent.getText());
            clear();
        }
        catch (ValidationException vex) {
            textComponent.setBackground(errorColor);
            textComponent.setToolTipText(vex.getUnlabeledMessage());
        }
    }

    protected void clear() {
        textComponent.setBackground(defaultBGColor);
        textComponent.setToolTipText(defaultToolTip);
    }
}
