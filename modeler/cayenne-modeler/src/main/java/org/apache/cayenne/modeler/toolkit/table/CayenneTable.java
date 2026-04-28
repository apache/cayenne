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

package org.apache.cayenne.modeler.toolkit.table;

import org.apache.cayenne.modeler.pref.TableColumnPreferences;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * Common superclass of tables used in Cayenne. Contains some common configuration settings and utility methods.
 */
public class CayenneTable extends JTable {

    private final static int EPSILON = 5;
    private final static Cursor east = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
    private final static Cursor west = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);

    private final SortButtonRenderer renderer;

    private TableColumnPreferences columnPreferences;
    private boolean isColumnWidthChanged;

    public CayenneTable() {
        setRowHeight(25);
        setRowMargin(3);

        this.renderer = new SortButtonRenderer();

        getTableHeader().addMouseListener(new HeaderListener());
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
        CayenneTextFieldCellEditor textEditor = new CayenneTextFieldCellEditor(textField);
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

    /**
     * Scrolls this table within its enclosing JViewport to the selected row, if any.
     */
    public void scrollToSelectedRow() {
        int row = getSelectedRow();
        if (row < 0 || !(getParent() instanceof JViewport)) {
            return;
        }

        JViewport viewport = (JViewport) getParent();
        Rectangle rect = getCellRect(row, 0, true);
        Rectangle viewRect = viewport.getViewRect();

        if (viewRect.intersects(rect)) {
            return;
        }

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        // Calculate location of rect if it were at the center of view
        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;

        // Fake the location of the cell so that scrollRectToVisible
        // will move the cell to the center
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);

        viewport.scrollRectToVisible(rect);
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
        sortByDefinedColumn(
                convertColumnIndexToView(column),
                column,
                isAscend);
    }

    public void setColumnPreferences(TableColumnPreferences tableColumnPreferences) {
        if (this.columnPreferences == null) {
            this.columnPreferences = tableColumnPreferences;
        }
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

    private void sortByDefinedColumn(int col, int sortCol, boolean order) {
        CayenneTableModel model = (CayenneTableModel) getModel();
        if (renderer.isSortingEnabled() && model.isColumnSortable(sortCol)) {
            renderer.setSelectedColumn(col, order);
            getTableHeader().repaint();

            if (isEditing()) {
                getCellEditor().stopCellEditing();
            }

            model.sortByColumn(sortCol, order);
        }
    }

    class HeaderListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            JTableHeader header = getTableHeader();
            if (e.getClickCount() > 1 && isResizeCursor()) {
                resize(getLeftColumn(e.getPoint()));
            } else if (!isResizeCursor()) {
                int col = header.columnAtPoint(e.getPoint());
                int sortCol = convertColumnIndexToModel(col);
                if (renderer.isSortingEnabled() && ((CayenneTableModel) getModel()).isColumnSortable(sortCol)) {
                    boolean isAscent = SortButtonRenderer.DOWN != renderer.getState(col);
                    sortByDefinedColumn(col, sortCol, isAscent);
                    columnPreferences.setSortOrder(isAscent);
                    columnPreferences.setSortColumn(sortCol);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            getTableHeader().repaint();
        }

        private boolean isResizeCursor() {
            Cursor cursor = getTableHeader().getCursor();
            return cursor.equals(east) || cursor.equals(west);
        }

        private int getLeftColumn(Point pt) {
            pt.x -= EPSILON;
            return getTableHeader().columnAtPoint(pt);
        }

        private void resize(int col) {
            TableColumn tc = getColumnModel().getColumn(col);
            TableCellRenderer tcr = tc.getHeaderRenderer();
            if (tcr == null)
                tcr = getTableHeader().getDefaultRenderer();
            Object obj = tc.getHeaderValue();
            Component comp = tcr.getTableCellRendererComponent(CayenneTable.this, obj, false, false, 0, 0);
            int maxWidth = comp.getPreferredSize().width;

            for (int i = 0, ub = getRowCount(); i != ub; ++i) {
                tcr = getCellRenderer(i, col);
                obj = getValueAt(i, col);
                comp = tcr.getTableCellRendererComponent(CayenneTable.this, obj, false, false, i, col);
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
