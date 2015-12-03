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

import org.apache.cayenne.modeler.pref.TableColumnPreferences;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TableHeaderListener extends MouseAdapter {

    private JTableHeader header;
    private SortButtonRenderer renderer;
    private JTable table;
    private TableColumnPreferences tableColumnPreferences;

    private static final int EPSILON = 5;
    private static final Cursor EAST = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
    private static final Cursor WEST = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);

    public TableHeaderListener(JTableHeader header, SortButtonRenderer renderer) {
        this.header = header;
        this.renderer = renderer;
        table = header.getTable();

    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() > 1 && isResizeCursor()) {
            resize(getLeftColumn(e.getPoint()));
        } else if (!isResizeCursor()) {
            int col = header.columnAtPoint(e.getPoint());
            int sortCol = table.convertColumnIndexToModel(col);
            if (((CayenneTableModel) table.getModel()).isColumnSortable(sortCol)) {
                boolean isAscent;
                if (SortButtonRenderer.DOWN == renderer.getState(col)) {
                    isAscent = false;
                } else {
                    isAscent = true;
                }
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
        if (model.isColumnSortable(sortCol)) {
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

        for(int i=0, ub = table.getRowCount(); i!=ub; ++i) {
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
