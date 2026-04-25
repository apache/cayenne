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

package org.apache.cayenne.modeler.swing.table;

import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.swing.WidgetFactory;
import org.apache.cayenne.modeler.util.CayenneTableModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * Common superclass of tables used in Cayenne. Contains some common configuration settings and utility methods.
 */
public class CayenneTable extends JTable {

    private final SortButtonRenderer renderer;
    private final CayenneTableHeaderListener tableHeaderListener;

    private boolean isColumnWidthChanged;

    public CayenneTable() {
        setRowHeight(25);
        setRowMargin(3);

        this.renderer = new SortButtonRenderer();
        this.tableHeaderListener = new CayenneTableHeaderListener(getTableHeader(), renderer);
        
        getTableHeader().addMouseListener(tableHeaderListener);
        setSelectionModel(new CayenneListSelectionModel());
    }

    @Override
    public void setModel(TableModel dataModel) {
        super.setModel(dataModel);

        if (!(dataModel instanceof DefaultTableModel)) {
            TableColumnModel model = getColumnModel();

            for (int i = 0; i < getColumnCount(); i++) {
                model.getColumn(i).setHeaderRenderer(renderer);
            }
        }
    }

    @Override
    protected void createDefaultEditors() {
        super.createDefaultEditors();

        JTextField textField = new JTextField(20);
        DefaultCellEditor textEditor = WidgetFactory.createCellEditor(textField);
        textEditor.setClickCountToStart(1);

        setDefaultEditor(Object.class, textEditor);
        setDefaultEditor(String.class, textEditor);
    }

    /**
     * @return CayenneTableModel, or null if model can't be casted to CayenneTableModel.
     */
    public CayenneTableModel getCayenneModel() {
        TableModel model = getModel();
        if (model instanceof CayenneTableModel) {
            return (CayenneTableModel) model;
        }
        return null;
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
     * Selects multiple rows at once. Fires not more than only one ListSelectionEvent
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
     * ListSelectionModel for Cayenne table. Has a method to set multiple rows selection at once.
     */
    class CayenneListSelectionModel extends DefaultListSelectionModel {

        boolean fireForbidden = false;

        /**
         * Selects selection on multiple rows at once. Fires no more than one
         * ListSelectionEvent
         */
        public void setSelection(int[] rows) {
            // First check if we must do anything at all
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

                        if (!inNewSelection) {
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

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {

        boolean result = false;
        if (isCellEditable(row, column)) {
            result = super.editCellAt(row, column, null);
            editorComp.requestFocus();
        }
        return result;
    }

    @Override
    public void changeSelection(final int row, final int column, boolean toggle, boolean extend) {
        super.changeSelection(row, column, toggle, extend);
    }

    public void sort(int column, boolean isAscend) {
        tableHeaderListener.sortByDefinedColumn(
                convertColumnIndexToView(column),
                column,
                isAscend);
    }

    public void setSortPreferenceSaver(TableColumnPreferences tableColumnPreferences) {
        tableHeaderListener.setPreferences(tableColumnPreferences);
    }

    public boolean getColumnWidthChanged() {
        return isColumnWidthChanged;
    }

    public void setColumnWidthChanged(boolean widthChanged) {
        isColumnWidthChanged = widthChanged;
    }

    public void setSortable(boolean sortable) {
        renderer.setSortingEnabled(sortable);
    }

    static class CayenneTableHeaderListener extends MouseAdapter {

        private static final int EPSILON = 5;
        private static final Cursor EAST = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        private static final Cursor WEST = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);

        private JTableHeader header;
        private SortButtonRenderer renderer;
        private JTable table;

        private TableColumnPreferences tableColumnPreferences;

        public CayenneTableHeaderListener(JTableHeader header, SortButtonRenderer renderer) {
            this.header = header;
            this.renderer = renderer;
            table = header.getTable();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1 && isResizeCursor()) {
                resize(getLeftColumn(e.getPoint()));
            } else if (!isResizeCursor()) {
                int col = header.columnAtPoint(e.getPoint());
                int sortCol = table.convertColumnIndexToModel(col);
                if (renderer.isSortingEnabled() && ((CayenneTableModel) table.getModel()).isColumnSortable(sortCol)) {
                    boolean isAscent = SortButtonRenderer.DOWN != renderer.getState(col);
                    sortByDefinedColumn(col, sortCol, isAscent);
                    tableColumnPreferences.setSortOrder(isAscent);
                    tableColumnPreferences.setSortColumn(sortCol);
                }
            }
        }

        public void mouseReleased(MouseEvent e) {
            header.repaint();
        }

        public void sortByDefinedColumn(int col, int sortCol, boolean order) {
            CayenneTableModel model = (CayenneTableModel) table.getModel();
            if (renderer.isSortingEnabled() && model.isColumnSortable(sortCol)) {
                renderer.setSelectedColumn(col, order);
                header.repaint();

                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }

                model.sortByColumn(sortCol, order);
            }
        }

        public void setPreferences(TableColumnPreferences tableColumnPreferences) {
            if (this.tableColumnPreferences == null) {
                this.tableColumnPreferences = tableColumnPreferences;
            }
        }

        private boolean isResizeCursor() {
            Cursor cursor = table.getTableHeader().getCursor();
            return cursor.equals(EAST) || cursor.equals(WEST);
        }

        private int getLeftColumn(Point pt) {
            pt.x -= EPSILON;
            return table.getTableHeader().columnAtPoint(pt);
        }

        private void resize(int col) {
            TableColumnModel tcm = table.getColumnModel();
            TableColumn tc = tcm.getColumn(col);
            TableCellRenderer tcr = tc.getHeaderRenderer();
            if (tcr == null)
                tcr = table.getTableHeader().getDefaultRenderer();
            Object obj = tc.getHeaderValue();
            Component comp = tcr.getTableCellRendererComponent(table, obj, false, false, 0, 0);
            int maxWidth = comp.getPreferredSize().width;

            for (int i = 0, ub = table.getRowCount(); i != ub; ++i) {
                tcr = table.getCellRenderer(i, col);
                obj = table.getValueAt(i, col);
                comp = tcr.getTableCellRendererComponent(table, obj, false, false, i, col);
                int w = comp.getPreferredSize().width;
                if (w > maxWidth)
                    maxWidth = w;
            }
            maxWidth += 10;
            tc.setPreferredWidth(maxWidth);
            tc.setWidth(maxWidth);
        }

    }
}
