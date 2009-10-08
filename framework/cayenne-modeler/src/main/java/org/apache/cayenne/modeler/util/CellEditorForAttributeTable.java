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
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Hashtable;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;


public class CellEditorForAttributeTable implements TableCellEditor {

        protected Hashtable editors;
        protected TableCellEditor editor, defaultEditor;
        JTable table;

        public CellEditorForAttributeTable(JTable table, JComboBox combo) {
            this.table = table;
            editors = new Hashtable();
            if (combo != null) {
                defaultEditor = new DefaultCellEditor(combo);
            }
            else {
                defaultEditor = new DefaultCellEditor(new JComboBox());
            }
        }

        public void setEditorAt(int row, TableCellEditor editor) {
            editors.put(new Integer(row), editor);
        }

        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {

            return editor.getTableCellEditorComponent(
                    table,
                    value,
                    isSelected,
                    row,
                    column);
        }

        public Object getCellEditorValue() {
            return editor.getCellEditorValue();
        }

        public boolean stopCellEditing() {
            return editor.stopCellEditing();
        }

        public void cancelCellEditing() {
            editor.cancelCellEditing();
        }

        public boolean isCellEditable(EventObject anEvent) {
            selectEditor((MouseEvent) anEvent);
            return editor.isCellEditable(anEvent);
        }

        public void addCellEditorListener(CellEditorListener l) {
            editor.addCellEditorListener(l);
        }

        public void removeCellEditorListener(CellEditorListener l) {
            editor.removeCellEditorListener(l);
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            selectEditor((MouseEvent) anEvent);
            return editor.shouldSelectCell(anEvent);
        }

        protected void selectEditor(MouseEvent e) {
            int row;
            if (e == null) {
                row = table.getSelectionModel().getAnchorSelectionIndex();
            }
            else {
                row = table.rowAtPoint(e.getPoint());
            }
            editor = (TableCellEditor) editors.get(new Integer(row));
            if (editor == null) {
                editor = defaultEditor;
            }
        }
    

}
