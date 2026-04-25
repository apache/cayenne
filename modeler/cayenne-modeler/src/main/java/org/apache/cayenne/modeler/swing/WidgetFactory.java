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

package org.apache.cayenne.modeler.swing;

import org.apache.cayenne.modeler.swing.combo.AutoCompletion;
import org.apache.cayenne.modeler.swing.combo.ComboBoxCellEditor;
import org.apache.cayenne.modeler.swing.table.CayenneCellEditor;
import org.apache.cayenne.modeler.undo.JComboBoxUndoListener;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;

/**
 * Utility class to create modeler-flavored Swing widgets.
 */
public final class WidgetFactory {

    /**
     * Number of items in combobox visible without scrolling
     */
    private static final int COMBOBOX_MAX_VISIBLE_SIZE = 12;

    private WidgetFactory() {
    }

    /**
     * Creates a new JComboBox with a collection of model objects.
     */
    public static JComboBox<String> createComboBox(Collection<String> model, boolean sort) {
        return createComboBox(model.toArray(new String[0]), sort);
    }

    /**
     * Creates a new JComboBox with an array of model objects.
     */
    public static <T> JComboBox<T> createComboBox(T[] model, boolean sort) {
        JComboBox<T> comboBox = createComboBox();

        if (sort) {
            Arrays.sort(model);
        }

        comboBox.setModel(new DefaultComboBoxModel<>(model));
        return comboBox;
    }

    /**
     * Creates a new JComboBox.
     */
    public static <T> JComboBox<T> createComboBox() {
        JComboBox<T> comboBox = new JComboBox<>();
        comboBox.setFont(UIManager.getFont("Label.font"));
        comboBox.setBackground(Color.WHITE);
        comboBox.setMaximumRowCount(COMBOBOX_MAX_VISIBLE_SIZE);
        return comboBox;
    }

    /**
     * Creates undoable JComboBox.
     */
    public static <T> JComboBox<T> createUndoableComboBox() {
        JComboBox<T> comboBox = new JComboBox<>();
        comboBox.addItemListener(new JComboBoxUndoListener());
        comboBox.setBackground(Color.WHITE);
        comboBox.setMaximumRowCount(COMBOBOX_MAX_VISIBLE_SIZE);
        return comboBox;
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
    public static TableCellEditor createCellEditor(JComboBox<?> combo) {
        if (Boolean.TRUE.equals(combo.getClientProperty(AutoCompletion.AUTOCOMPLETION_PROPERTY))) {
            return new ComboBoxCellEditor(combo);
        }

        DefaultCellEditor editor = new DefaultCellEditor(combo);
        editor.setClickCountToStart(1);

        return editor;
    }

    /**
     * Creates and returns a panel with right-centered buttons.
     */
    public static JPanel createButtonPanel(JButton[] buttons) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 7));
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        for (JButton button : buttons) {
            panel.add(button);
        }

        return panel;
    }

    /**
     * Creates panel with table within scroll panel and buttons in the bottom.
     * Also sets the resizing and selection policies of the table to
     * AUTO_RESIZE_OFF and SINGLE_SELECTION respectively.
     */
    public static JPanel createTablePanel(final JTable table, JButton[] buttons) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));
        // Create table with two columns and no rows.
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
            }
        });

        // Panel to add space between table and EAST/WEST borders
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add Add and Remove buttons
        if (buttons != null) {
            panel.add(createButtonPanel(buttons), BorderLayout.SOUTH);
        }
        return panel;
    }
}
