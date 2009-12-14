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

package org.apache.cayenne.modeler.util;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;

import org.apache.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.apache.cayenne.modeler.undo.JTextFieldUndoListener;
import org.apache.cayenne.validation.ValidationException;

/**
 * A validating adapter for JTextComponent. Implement {@link #updateModel(String)}to
 * initialize model on text change.
 * 
 */
public abstract class TextAdapter {

    protected Color defaultBGColor;
    protected Color errorColor;

    protected String defaultToolTip;
    protected boolean modelUpdateDisabled;
    protected UndoableEditListener undoableListener;

    protected JTextComponent textComponent;

    public TextAdapter(JTextField textField) {
        this(textField, true, false, true);
    }

    public TextAdapter(JTextField textField, boolean checkOnFocusLost,
            boolean checkOnTyping, boolean checkOnEnter) {

        this(textField, true, false);

        if (checkOnEnter) {

            textField.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateModel();
                }
            });
        }
    }

    public TextAdapter(JTextComponent textField) {
        this(textField, false, true);
    }

    public TextAdapter(JTextComponent textComponent, boolean checkOnFocusLost,
            boolean checkOnTyping) {

        this.errorColor = ValidatorDialog.WARNING_COLOR;
        this.defaultBGColor = textComponent.getBackground();
        this.defaultToolTip = textComponent.getToolTipText();
        this.textComponent = textComponent;

        this.undoableListener = new JTextFieldUndoListener(this);
        this.textComponent.getDocument().addUndoableEditListener(this.undoableListener);

        if (checkOnFocusLost) {
            textComponent.setInputVerifier(new InputVerifier() {

                public boolean verify(JComponent c) {
                    updateModel();
                    // release focus after coloring the field...
                    return true;
                }
            });
        }

        if (checkOnTyping) {
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
    }

    /**
     * Updates bound model with document text.
     */
    protected abstract void updateModel(String text) throws ValidationException;

    /**
     * Returns internal text component.
     */
    public JTextComponent getComponent() {
        return textComponent;
    }

    /**
     * Sets the text of the underlying text field.
     */
    public void setText(String text) {
        modelUpdateDisabled = true;

        this.textComponent
                .getDocument()
                .removeUndoableEditListener(this.undoableListener);

        try {
            clear();
            textComponent.setText(text);
        }
        finally {
            modelUpdateDisabled = false;
            this.textComponent.getDocument().addUndoableEditListener(
                    this.undoableListener);
        }

    }

    public void updateModel() {
        try {
            updateModel(textComponent.getText());
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
