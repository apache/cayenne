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

package org.objectstyle.cayenne.modeler.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Utility class to create standard Swing widgets following default look-and-feel of
 * CayenneModeler.
 * 
 * @author Andrei Adamchik
 */

// TODO: (Andrus) investigate performance impact of substituting
// constructors for all new widgets with cloning the prototype
public class CayenneWidgetFactory {

    /**
     * Not intended for instantiation.
     */
    protected CayenneWidgetFactory() {
        super();
    }

    /**
     * Creates a new JComboBox with a collection of model objects.
     */
    public static JComboBox createComboBox(Collection model, boolean sort) {
        return createComboBox(model.toArray(), sort);
    }

    /**
     * Creates a new JComboBox with an array of model objects.
     */
    public static JComboBox createComboBox(Object[] model, boolean sort) {
        JComboBox comboBox = CayenneWidgetFactory.createComboBox();

        if (sort) {
            Arrays.sort(model);
        }

        comboBox.setModel(new DefaultComboBoxModel(model));
        return comboBox;
    }

    /**
     * Creates a new JComboBox.
     */
    public static JComboBox createComboBox() {
        JComboBox comboBox = new JComboBox();
        initFormWidget(comboBox);
        comboBox.setBackground(Color.WHITE);
        return comboBox;
    }

    /**
     * Creates a new JTextField with a default columns count of 20.
     */
    public static JTextField createTextField() {
        return createTextField(20);
    }

    /**
     * Creates a new JTextField with a specified columns count.
     */
    public static JTextField createTextField(int columns) {
        final JTextField textField = new JTextField(columns);
        initFormWidget(textField);
        initTextField(textField);
        return textField;
    }

    public static JLabel createLabel(String text) {
        return new JLabel(text);
    }

    protected static void initTextField(final JTextField textField) {
        // config focus
        textField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                // transfer focus
                textField.transferFocus();
            }
        });
    }

    /**
     * Initializes a "form" element with a standard font and height.
     */
    protected static void initFormWidget(JComponent component) {
        component.setFont(component.getFont().deriveFont(Font.PLAIN, 12));

        /*
         * Dimension size = component.getPreferredSize(); if (size == null) { size = new
         * Dimension(); } size.setSize(size.getWidth(), 20);
         * component.setPreferredSize(size);
         */
    }

    /**
     * Creates a borderless button that can be used as a clickable label.
     */
    public static JButton createLabelButton(String text) {
        JButton but = createButton(text);
        but.setBorderPainted(false);
        but.setHorizontalAlignment(SwingConstants.LEFT);
        but.setFocusPainted(false);
        but.setMargin(new Insets(0, 0, 0, 0));
        but.setBorder(null);
        return but;
    }

    /**
     * Creates a normal button.
     */
    public static JButton createButton(String text) {
        return new JButton(text);
    }
}