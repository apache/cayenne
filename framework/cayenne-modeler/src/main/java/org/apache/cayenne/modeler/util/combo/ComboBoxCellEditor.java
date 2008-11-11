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

package org.apache.cayenne.modeler.util.combo;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

/**
 * ComboBoxCellEditor class is a workaround of collision between DefaultCellEditor and 
 * AutoCompletion behavior. Using DefaultCellEditor will cause combo popup to close
 * out of time.
 *
 */
public class ComboBoxCellEditor extends AbstractCellEditor 
    implements ActionListener, TableCellEditor, FocusListener, Serializable {
    
    static final String IS_TABLE_CELL_EDITOR_PROPERTY = "JComboBox.isTableCellEditor";
    
    private final JComboBox comboBox;
    
    public ComboBoxCellEditor(JComboBox comboBox) {
        this.comboBox = comboBox;
        this.comboBox.putClientProperty(IS_TABLE_CELL_EDITOR_PROPERTY, Boolean.TRUE);

        // hitting enter in the combo box should stop cellediting (see below)
        this.comboBox.addActionListener(this);
        
        //  Editing should be stopped when textfield loses its focus
        //  otherwise the value may get lost (e.g. see CAY-1104)
        this.comboBox.getEditor().getEditorComponent().addFocusListener(this);

        // remove the editor's border - the cell itself already has one
        ((JComponent) comboBox.getEditor().getEditorComponent()).setBorder(null);
    }
    
    // Implementing ActionListener
    public void actionPerformed(ActionEvent e) {
        // Selecting an item results in an actioncommand "comboBoxChanged".
        // We should ignore these ones.
        
        // Hitting enter results in an actioncommand "comboBoxEdited"
        if (e.getActionCommand().equals("comboBoxEdited")) {
            stopCellEditing();
        }
    }
    
    public Object getCellEditorValue() {
        return comboBox.getSelectedItem();
    }
    
    @Override
    public boolean stopCellEditing() {
        if (comboBox.isEditable()) {
            // Notify the combo box that editing has stopped (e.g. User pressed F2)
            comboBox.actionPerformed(new ActionEvent(this, 0, ""));
        }

        fireEditingStopped();

        return true;
    }
    
    public Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
        comboBox.setSelectedItem(value);
        
        return comboBox;
    }
    
    @Override
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            //allow multiple selection without 
            
            MouseEvent me = (MouseEvent) e;
            if (me.isControlDown() || me.isShiftDown()) {
                return false;
            }
        }
        
        return true;
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        if (e.getOppositeComponent() != null) {
            stopCellEditing();
        }
    }
}
