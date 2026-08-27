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

package org.apache.cayenne.modeler.toolkit.combobox;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.EventObject;

/**
 * Plain combo box cell editor for the modeler. Editing is suppressed for
 * ctrl/shift-clicks so the user can extend a multi-row selection without
 * opening the editor.
 */
public class CMComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor, Serializable {

    protected final JComboBox<?> comboBox;

    public CMComboBoxCellEditor(JComboBox<?> comboBox) {
        this.comboBox = comboBox;
        comboBox.addPopupMenuListener(new CMComboBoxPopupResizer(comboBox));
    }

    public static boolean isTableCellEditable(EventObject e) {
        if (e instanceof MouseEvent me) {
            return !me.isControlDown() && !me.isShiftDown();
        }
        return true;
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        return isTableCellEditable(e);
    }

    @Override
    public Object getCellEditorValue() {
        return comboBox.getSelectedItem();
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table, Object value, boolean isSelected, int row, int column) {
        comboBox.setSelectedItem(value);
        return comboBox;
    }

    @Override
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }
}
