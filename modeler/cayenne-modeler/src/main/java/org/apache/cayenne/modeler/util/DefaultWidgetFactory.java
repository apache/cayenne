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
import java.util.Arrays;
import java.util.Collection;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import org.apache.cayenne.modeler.ModelerPreferences;
import org.apache.cayenne.modeler.undo.JComboBoxUndoListener;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.modeler.util.combo.ComboBoxCellEditor;
import org.syntax.jedit.JEditTextArea;

public class DefaultWidgetFactory implements WidgetFactory {

    /**
     * Creates a new JComboBox with a collection of model objects.
     */
    public JComboBox createComboBox(Collection<String> model, boolean sort) {
        return createComboBox(model.toArray(), sort);
    }

    /**
     * Creates a new JComboBox with an array of model objects.
     */
    public JComboBox createComboBox(Object[] model, boolean sort) {
        JComboBox comboBox = createComboBox();

        if (sort) {
            Arrays.sort(model);
        }

        comboBox.setModel(new DefaultComboBoxModel(model));
        return comboBox;
    }

    /**
     * Creates a new JComboBox.
     */
    public JComboBox createComboBox() {
        JComboBox comboBox = new JComboBox();
        comboBox.setFont(new Font("Verdana", Font.PLAIN , 12));
        comboBox.setBackground(Color.WHITE);
        comboBox.setMaximumRowCount(ModelerPreferences.COMBOBOX_MAX_VISIBLE_SIZE);
        return comboBox;
    }

    /**
     * Creates undoable JComboBox.
     * 
     */
    public JComboBox createUndoableComboBox() {
        JComboBox comboBox = new JComboBox();
        comboBox.addItemListener(new JComboBoxUndoListener());
        comboBox.setBackground(Color.WHITE);
        comboBox.setMaximumRowCount(ModelerPreferences.COMBOBOX_MAX_VISIBLE_SIZE);
        return comboBox;
    }

    /**
     * Creates cell editor for text field
     */
    public DefaultCellEditor createCellEditor(JTextField textField) {
        return new CayenneCellEditor(textField);
    }

    /**
     * Creates cell editor for a table with combo as editor component. Type of this editor
     * depends on auto-completion behavior of JComboBox
     * 
     * @param combo JComboBox to be used as editor component
     */
    public TableCellEditor createCellEditor(JComboBox combo) {
        if (Boolean.TRUE.equals(combo
                .getClientProperty(AutoCompletion.AUTOCOMPLETION_PROPERTY))) {
            return new ComboBoxCellEditor(combo);
        }

        DefaultCellEditor editor = new DefaultCellEditor(combo);
        editor.setClickCountToStart(1);

        return editor;
    }

    /**
     * Creates and returns a JEdit text component with syntax highlighing
     */
    public JEditTextArea createJEditTextArea() {
        return new JEditTextAreaUndoable();
    }
}
