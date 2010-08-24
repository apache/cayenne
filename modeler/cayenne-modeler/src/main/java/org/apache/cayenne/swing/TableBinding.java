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


package org.apache.cayenne.swing;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.cayenne.util.Util;
import org.apache.commons.collections.map.SingletonMap;

/**
 * A binding for a JTable.
 * 
 */
public class TableBinding extends BindingBase {

    /**
     * A variable exposed in the context of set/get cell value.
     */
    public static final String ITEM_VAR = "item";

    protected JTable table;
    protected String[] headers;
    protected BindingExpression[] columns;
    protected boolean[] editableState;
    protected Class[] columnClass;
    protected List list;

    public TableBinding(JTable table, String listBinding, String[] headers,
            BindingExpression[] columns, Class[] columnClass, boolean[] editableState,
            Object[] sampleLongValues) {

        super(listBinding);
        this.table = table;
        this.headers = headers;
        this.columns = columns;
        this.editableState = editableState;
        this.columnClass = columnClass;

        table.setModel(new BoundTableModel());
        resizeColumns(sampleLongValues);
    }

    protected void resizeColumns(Object[] sampleLongValues) {

        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        TableColumnModel columnModel = table.getColumnModel();
        TableModel tableModel = table.getModel();

        for (int i = 0; i < columnModel.getColumnCount(); i++) {

            TableColumn column = columnModel.getColumn(i);

            Component header = headerRenderer.getTableCellRendererComponent(null, column
                    .getHeaderValue(), false, false, 0, 0);
            int headerWidth = header.getPreferredSize().width;

            if (sampleLongValues[i] != null) {
                Component bigCell = table
                        .getDefaultRenderer(tableModel.getColumnClass(i))
                        .getTableCellRendererComponent(
                                table,
                                sampleLongValues[i],
                                false,
                                false,
                                0,
                                i);
                int cellWidth = bigCell.getPreferredSize().width;
                column.setPreferredWidth(Math.max(headerWidth, cellWidth));
            }
            else {
                column.setPreferredWidth(headerWidth);
            }
        }
    }

    public void setContext(Object object) {
        super.setContext(object);

        this.list = updateList();
    }

    public Component getView() {
        return table;
    }

    public void updateView() {
        this.list = updateList();
        ((BoundTableModel) table.getModel()).fireTableDataChanged();
    }

    int getListSize() {
        return (list != null) ? list.size() : 0;
    }

    List updateList() {
        if (getContext() == null) {
            return null;
        }

        Object list = getValue();
        if (list == null) {
            return null;
        }

        if (list instanceof List) {
            return (List) list;
        }

        if (list instanceof Object[]) {
            Object[] objects = (Object[]) list;
            return Arrays.asList(objects);
        }

        if (list instanceof Collection) {
            return new ArrayList((Collection) list);
        }

        throw new BindingException("List expected, got - " + list);
    }

    final class BoundTableModel extends AbstractTableModel {

        // this map is used as "flyweight", providing on the spot context for Ognl
        // expression evaluation
        Map listContext = new SingletonMap(ITEM_VAR, null);

        public int getColumnCount() {
            return headers.length;
        }

        public int getRowCount() {
            return getListSize();
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return editableState[columnIndex];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Object item = list.get(rowIndex);
            listContext.put(ITEM_VAR, item);
            return columns[columnIndex].getValue(getContext(), listContext);
        }

        public String getColumnName(int column) {
            // per CAY-513 - if an empty string is passed for header, table header will
            // have zero height on Windows... So we have to check for this condition
            return Util.isEmptyString(headers[column]) ? " " : headers[column];
        }

        public Class getColumnClass(int columnIndex) {
            return columnClass[columnIndex];
        }

        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            Object item = list.get(rowIndex);
            listContext.put(ITEM_VAR, item);
            columns[columnIndex].setValue(getContext(), listContext, value);
        }
    }
}
