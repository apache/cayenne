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

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.apache.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.apache.cayenne.validation.ValidationException;

/**
 */
public class ComboSelectionBinding extends BindingBase {

    protected JComboBox comboBox;

    protected Color defaultBGColor;
    protected Color errorColor;
    protected String defaultToolTip;
    protected String noSelectionValue;

    /**
     * Binds to update model for a combo box selection events. For editable combo boxes
     * model is updated whenever a new value is entered.
     */
    public ComboSelectionBinding(JComboBox comboBox, String expression,
            String noSelectionValue) {
        super(expression);
        this.comboBox = comboBox;
        this.noSelectionValue = noSelectionValue;

        // insert no selection value as first item in the combobox if it is not there
        if (noSelectionValue != null
                && (comboBox.getItemCount() == 0 || comboBox.getItemAt(0) != noSelectionValue)) {

            comboBox.insertItemAt(noSelectionValue, 0);
        }

        comboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!modelUpdateDisabled) {
                    updateModel();
                }
            }
        });

        // init error colors
        initComponentDefaults();
    }

    protected void initComponentDefaults() {
        this.errorColor = ValidatorDialog.WARNING_COLOR;

        if (comboBox.getEditor() != null) {

            Component editor = comboBox.getEditor().getEditorComponent();
            if (editor instanceof JComponent) {
                JComponent jEditor = (JComponent) editor;
                this.defaultBGColor = jEditor.getBackground();
                this.defaultToolTip = jEditor.getToolTipText();
            }
        }
    }

    public void updateView() {
        Object value = getValue();
        modelUpdateDisabled = true;
        try {
            clear();
            if (value != null) {
                this.comboBox.setSelectedItem(value.toString());
            }
            else if (noSelectionValue != null) {
                this.comboBox.setSelectedItem(noSelectionValue);
            }
            else {
                this.comboBox.setSelectedIndex(-1);
            }
        }
        finally {
            modelUpdateDisabled = false;
        }
    }

    protected void updateModel() {
        try {
            Object value = comboBox.getSelectedItem();
            if (noSelectionValue != null && noSelectionValue.equals(value)) {
                value = null;
            }
            setValue(value);
            clear();
        }
        catch (ValidationException vex) {
            initWarning(vex.getLocalizedMessage());
        }
    }

    public Component getView() {
        return comboBox;
    }

    protected void clear() {
        if (comboBox.getEditor() != null) {

            Component editor = comboBox.getEditor().getEditorComponent();
            if (editor instanceof JComponent) {
                JComponent jEditor = (JComponent) editor;
                jEditor.setBackground(defaultBGColor);
                jEditor.setToolTipText(defaultToolTip);
            }
        }
    }

    protected void initWarning(String message) {
        if (comboBox.getEditor() != null) {

            Component editor = comboBox.getEditor().getEditorComponent();
            if (editor instanceof JComponent) {
                JComponent jEditor = (JComponent) editor;
                jEditor.setBackground(errorColor);
                jEditor.setToolTipText(message);
            }
        }
    }
}
