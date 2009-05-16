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

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;

/**
 * Common superclass of tables used in Cayenne. Contains some common configuration
 * settings and utility methods.
 * 
 */
public class CayenneTable extends JTable {

    public CayenneTable() {
        super();
        this.setRowHeight(25);
        this.setRowMargin(3);
        
        setSelectionModel(new CayenneListSelectionModel());
    }

    @Override
    protected void createDefaultEditors() {
        super.createDefaultEditors();

        JTextField textField = CayenneWidgetFactory.createTextField(0);
        final DefaultCellEditor textEditor = CayenneWidgetFactory.createCellEditor(textField);
        textEditor.setClickCountToStart(1);

        setDefaultEditor(Object.class, textEditor);
        setDefaultEditor(String.class, textEditor);
    }

    public CayenneTableModel getCayenneModel() {
        return (CayenneTableModel) getModel();
    }

    /**
     * Cancels editing of any cells that maybe currently edited. This method should be
     * called before updating any selections.
     */
    public void cancelEditing() {
        editingCanceled(new ChangeEvent(this));
    }

    public void select(Object row) {
        if (row == null) {
            return;
        }

        CayenneTableModel model = getCayenneModel();
        int ind = model.getObjectList().indexOf(row);

        if (ind >= 0) {
            getSelectionModel().setSelectionInterval(ind, ind);
        }
    }

    public void select(int index) {

        CayenneTableModel model = getCayenneModel();
        if (index >= model.getObjectList().size()) {
            index = model.getObjectList().size() - 1;
        }

        if (index >= 0) {
            getSelectionModel().setSelectionInterval(index, index);
        }
    }
    
    /**
     * Selects multiple rows at once. Fires not more than only one 
     * ListSelectionEvent
     */
    public void select(int[] rows) {
        ((CayenneListSelectionModel) getSelectionModel()).setSelection(rows);
    }

    public JTextComponent getSelectedTextComponent() {
        int row = getSelectedRow();
        int column = getSelectedColumn();
        if (row < 0 || column < 0) {
            return null;
        }

        TableCellEditor editor = this.getCellEditor(row, column);
        if (editor instanceof DefaultCellEditor) {
            Component comp = ((DefaultCellEditor) editor).getComponent();
            if (comp instanceof JTextComponent) {
                return (JTextComponent) comp;
            }
        }
        return null;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        cancelEditing();
        super.tableChanged(e);
    }
    
    /**
     * ListSelectionModel for Cayenne table. Has a method to set multiple
     * rows selection at once.
     */
    class CayenneListSelectionModel extends DefaultListSelectionModel {
        boolean fireForbidden = false;
        
        /**
         * Selects selection on multiple rows at once. Fires no more than one
         * ListSelectionEvent
         */
        public void setSelection(int[] rows) {
            /**
             * First check if we must do anything at all
             */
            boolean selectionChanged = false;
            for (int row : rows) {
                if (!isRowSelected(row)) {
                    selectionChanged = true;
                    break;
                }
            }
            
            if (!selectionChanged) {
                for (int i = getMinSelectionIndex(); i < getMaxSelectionIndex(); i++) {
                    if (isSelectedIndex(i)) {
                        boolean inNewSelection = false;
                        for (int row : rows) {
                            if (row == i) {
                                inNewSelection = true;
                                break;
                            }
                        }
                        
                        if(!inNewSelection) {
                            selectionChanged = true;
                            break;
                        }
                    }
                }
            }
            
            if (!selectionChanged) {
                return;
            }
            
            fireForbidden = true;
            
            clearSelection();
            for (int row : rows) {
                if (row >= 0 && row < getRowCount()) {
                    addRowSelectionInterval(row, row);
                }
            }
            
            fireForbidden = false;
            
            fireValueChanged(getValueIsAdjusting());
        }
        
        @Override
        protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
            if (!fireForbidden) {
                super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
            }
        }
    }
}
