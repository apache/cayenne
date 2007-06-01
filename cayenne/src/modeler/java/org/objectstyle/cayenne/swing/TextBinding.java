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
 */package org.objectstyle.cayenne.swing;

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

import org.objectstyle.cayenne.modeler.dialog.validator.ValidatorDialog;
import org.objectstyle.cayenne.validation.ValidationException;

/**
 * A generic text adapter that is bound to a bean property.
 * 
 * @author Andrei Adamchik
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
                updateModel();
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
    public Component getComponent() {
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