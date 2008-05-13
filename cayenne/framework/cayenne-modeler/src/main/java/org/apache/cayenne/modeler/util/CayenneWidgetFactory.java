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
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Utility class to create standard Swing widgets following default look-and-feel of
 * CayenneModeler.
 * 
 * @author Andrus Adamchik
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
