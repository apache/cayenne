/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.objectstyle.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.objectstyle.cayenne.validation.ValidationException;

/**
 * @author Andrei Adamchik
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

    public Component getComponent() {
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