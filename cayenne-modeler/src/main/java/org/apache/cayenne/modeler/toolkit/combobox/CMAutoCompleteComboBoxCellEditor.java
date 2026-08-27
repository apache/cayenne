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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Combo box cell editor for autocomplete combos. Wires up the
 * {@code "comboBoxEdited"} action-command path so that pressing Enter commits
 * the edit, and sets {@code JComboBox.isTableCellEditor} so the Swing L&F
 * keeps popup behaviour table-friendly.
 */
public class CMAutoCompleteComboBoxCellEditor extends CMComboBoxCellEditor implements ActionListener {

    // Read by Swing's combo UI to keep the popup behaviour table-friendly.
    private static final String IS_TABLE_CELL_EDITOR_PROPERTY = "JComboBox.isTableCellEditor";

    public CMAutoCompleteComboBoxCellEditor(JComboBox<?> comboBox) {
        super(comboBox);
        comboBox.putClientProperty(IS_TABLE_CELL_EDITOR_PROPERTY, Boolean.TRUE);
        comboBox.addActionListener(this);
    }

    @Override
    public boolean stopCellEditing() {
        if (comboBox.isEditable()) {
            // Notify the combo that editing has stopped (e.g. focus lost, F2).
            comboBox.actionPerformed(new ActionEvent(this, 0, ""));
        }

        return super.stopCellEditing();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Selecting an item produces "comboBoxChanged" — ignore.
        // Hitting enter produces "comboBoxEdited" — stop editing.
        if ("comboBoxEdited".equals(e.getActionCommand())) {
            stopCellEditing();
        }
    }
}
