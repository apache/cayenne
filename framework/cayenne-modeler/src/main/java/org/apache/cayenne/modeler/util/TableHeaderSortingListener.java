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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.apache.cayenne.modeler.pref.TableColumnPreferences;

public class TableHeaderSortingListener extends MouseAdapter {

    private JTableHeader header;
    private SortButtonRenderer renderer;
    private JTable table;
    private TableColumnPreferences tableColumnPreferences;

    public TableHeaderSortingListener(JTableHeader header, SortButtonRenderer renderer) {
        this.header = header;
        this.renderer = renderer;
        table = header.getTable();

    }

    public void mousePressed(MouseEvent e) {

        int col = header.columnAtPoint(e.getPoint());
        int sortCol = table.convertColumnIndexToModel(col);
        if (((CayenneTableModel) table.getModel()).isColumnSortable(sortCol)) {
            boolean isAscent;
            if (SortButtonRenderer.DOWN == renderer.getState(col)) {
                isAscent = false;
            }
            else {
                isAscent = true;
            }
            sortByDefinedColumn(col, sortCol, isAscent);
            tableColumnPreferences.setSortOrder(isAscent);
            tableColumnPreferences.setSortColumn(sortCol);
        }
    }

    public void mouseReleased(MouseEvent e) {
        renderer.setPressedColumn(-1); // clear
        header.repaint();
    }

    public void sortByDefinedColumn(int col, int sortCol, boolean order) {
        CayenneTableModel model = (CayenneTableModel) table.getModel();
        if (model.isColumnSortable(sortCol)) {
            renderer.setPressedColumn(col);
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

}
