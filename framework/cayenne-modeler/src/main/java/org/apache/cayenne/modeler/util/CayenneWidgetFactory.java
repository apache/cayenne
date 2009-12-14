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

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;

import org.apache.cayenne.modeler.ModelerPreferences;
import org.apache.cayenne.modeler.undo.JComboBoxUndoListener;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.modeler.util.combo.ComboBoxCellEditor;
import org.apache.cayenne.swing.components.textpane.JCayenneTextPane;
import org.apache.cayenne.swing.components.textpane.syntax.EJBQLSyntaxConstant;
import org.syntax.jedit.DefaultInputHandler;
import org.syntax.jedit.JEditTextArea;

/**
 * Utility class to create standard Swing widgets following default look-and-feel of
 * CayenneModeler.
 * 
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
    public static JComboBox createComboBox(Collection<String> model, boolean sort) {
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
        comboBox.setMaximumRowCount(ModelerPreferences.COMBOBOX_MAX_VISIBLE_SIZE);
        return comboBox;
    }

    /**
     * Creates undoable JComboBox.
     * 
     */
    public static JComboBox createUndoableComboBox() {
        JComboBox comboBox = new JComboBox();
        initFormWidget(comboBox);
        comboBox.addItemListener(new JComboBoxUndoListener());
        comboBox.setBackground(Color.WHITE);
        comboBox.setMaximumRowCount(ModelerPreferences.COMBOBOX_MAX_VISIBLE_SIZE);
        return comboBox;
    }

    /**
     * Creates undoable JTextField.
     * 
     */
    public static JTextField createUndoableTextField() {
        return new JTextFieldUndoable();
    }

    /**
     * Creates undoable JTextField.
     * 
     */
    public static JTextField createUndoableTextField(int size) {
        return new JTextFieldUndoable(size);
    }

    /**
     * Creates cell editor for text field
     */
    public static DefaultCellEditor createCellEditor(JTextField textField) {
        return new CayenneCellEditor(textField);
    }

    /**
     * Creates cell editor for a table with combo as editor component. Type of this editor
     * depends on auto-completion behavior of JComboBox
     * 
     * @param combo JComboBox to be used as editor component
     */
    public static TableCellEditor createCellEditor(JComboBox combo) {
        if (Boolean.TRUE.equals(combo
                .getClientProperty(AutoCompletion.AUTOCOMPLETION_PROPERTY))) {
            return new ComboBoxCellEditor(combo);
        }

        DefaultCellEditor editor = new DefaultCellEditor(combo);
        editor.setClickCountToStart(1);

        return editor;
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

    /**
     * Creates and returns a JEdit text component with syntax highlighing
     */
    public static JEditTextArea createJEditTextArea() {
        JEditTextArea area = new JEditTextAreaUndoable();
        if (OperatingSystem.getOS() == OperatingSystem.MAC_OS_X) {
            area.setInputHandler(new MacInputHandler());
        }

        return area;
    }

    // public static JSQLTextPane createJSQLTextPane() {
    // JSQLTextPane area = new JSQLTextPane();
    // return area;
    // }

    public static JCayenneTextPane createJEJBQLTextPane() {
        JCayenneTextPane area = new JCayenneTextPaneUndoable(new EJBQLSyntaxConstant());
        return area;
    }

    /**
     * Class for enabling Mac OS X keys
     */
    private static class MacInputHandler extends DefaultInputHandler {

        MacInputHandler() {
            addDefaultKeyBindings();
        }

        /**
         * Sets up the default key bindings.
         */
        public void addDefaultKeyBindings() {
            addKeyBinding("BACK_SPACE", BACKSPACE);
            addKeyBinding("M+BACK_SPACE", BACKSPACE_WORD);
            addKeyBinding("DELETE", DELETE);
            addKeyBinding("M+DELETE", DELETE_WORD);

            addKeyBinding("ENTER", INSERT_BREAK);
            addKeyBinding("TAB", INSERT_TAB);

            addKeyBinding("INSERT", OVERWRITE);
            addKeyBinding("M+\\", TOGGLE_RECT);

            addKeyBinding("HOME", HOME);
            addKeyBinding("END", END);
            addKeyBinding("M+A", SELECT_ALL);
            addKeyBinding("S+HOME", SELECT_HOME);
            addKeyBinding("S+END", SELECT_END);
            addKeyBinding("M+HOME", DOCUMENT_HOME);
            addKeyBinding("M+END", DOCUMENT_END);
            addKeyBinding("MS+HOME", SELECT_DOC_HOME);
            addKeyBinding("MS+END", SELECT_DOC_END);

            addKeyBinding("PAGE_UP", PREV_PAGE);
            addKeyBinding("PAGE_DOWN", NEXT_PAGE);
            addKeyBinding("S+PAGE_UP", SELECT_PREV_PAGE);
            addKeyBinding("S+PAGE_DOWN", SELECT_NEXT_PAGE);

            addKeyBinding("LEFT", PREV_CHAR);
            addKeyBinding("S+LEFT", SELECT_PREV_CHAR);
            addKeyBinding("A+LEFT", PREV_WORD); // option + left
            addKeyBinding("AS+LEFT", SELECT_PREV_WORD); // option + shift + left
            addKeyBinding("RIGHT", NEXT_CHAR);
            addKeyBinding("S+RIGHT", SELECT_NEXT_CHAR);
            addKeyBinding("A+RIGHT", NEXT_WORD); // option + right
            addKeyBinding("AS+RIGHT", SELECT_NEXT_WORD); // option + shift + right
            addKeyBinding("UP", PREV_LINE);
            addKeyBinding("S+UP", SELECT_PREV_LINE);
            addKeyBinding("DOWN", NEXT_LINE);
            addKeyBinding("S+DOWN", SELECT_NEXT_LINE);

            addKeyBinding("M+ENTER", REPEAT);

            // Clipboard
            addKeyBinding("M+C", CLIP_COPY); // command + c
            addKeyBinding("M+V", CLIP_PASTE); // command + v
            addKeyBinding("M+X", CLIP_CUT); // command + x
        }
    }
}
