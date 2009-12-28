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
package org.apache.cayenne.modeler.pref;

import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.apache.cayenne.pref.CayennePreference;

public class TableColumnPreferences extends CayennePreference {

    private static final String WIDTH_KEY = "width_";
    public static final String TABLE_COLUMN_PREF_KEY = "table_column";
    private static final String ORDER_KEY = "order_";
    private JTable table;
    private int columnCount;
    private TableColumnModelListener listener = new TableColumnModelListener() {

        public void columnAdded(TableColumnModelEvent e) {
        }

        public void columnMarginChanged(ChangeEvent e) {
            TableColumn column = null;
            for (int i = 0; i < columnCount; i++) {
                column = table.getColumnModel().getColumn(i);
                setWidth(column.getModelIndex(), column.getPreferredWidth());
            }
        }

        public void columnMoved(TableColumnModelEvent e) {
            TableColumn column = null;
            for (int i = 0; i < columnCount; i++) {
                column = table.getColumnModel().getColumn(i);
                setOrderIndex(column.getModelIndex(), i);
            }
        }

        public void columnRemoved(TableColumnModelEvent e) {
        }

        public void columnSelectionChanged(ListSelectionEvent e) {
        }
    };
    

    public TableColumnPreferences(Class className, String path) {
        setCurrentNodeForPreference(className, path);
    }

    public Preferences getPreference() {
        if (getCurrentPreference() == null) {
            setCurrentNodeForPreference(this.getClass(), TABLE_COLUMN_PREF_KEY);
        }
        return getCurrentPreference();
    }

    /**
     * Binds this preference object to synchronize its state with a given table component,
     * allowing to specify an initial offset compared to the stored position.
     * 
     * @param minSizes
     * @param maxSizes 
     */
    public void bind(final JTable table, Map<Integer, Integer> minSizes, Map<Integer, Integer> maxSizes) {

        this.table = table;
        this.columnCount=table.getColumnCount();
        table.getColumnModel().removeColumnModelListener(listener);
        updateTable(minSizes, maxSizes);
        table.getColumnModel().addColumnModelListener(listener);
       
    }

    private void updateTable(Map<Integer, Integer> minSizes, Map<Integer, Integer> maxSizes) {
        updateWidths(minSizes, maxSizes);
        updateOrder();
    }

    
    private void updateWidths(
            Map<Integer, Integer> minSizes,
            Map<Integer, Integer> maxSizes) {
        TableColumn column = null;
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnCount; i++) {
            column = columnModel.getColumn(i);
            int modelIndex = column.getModelIndex();

            int defaultWidth;
            if (minSizes != null && minSizes.containsKey(modelIndex)) {
                Integer minWidth = minSizes.get(modelIndex);
                column.setMinWidth(minWidth);
                defaultWidth = minWidth;
            } else {
                defaultWidth = column.getPreferredWidth();
            }

            if (maxSizes != null && maxSizes.containsKey(modelIndex)) {
                column.setMaxWidth(maxSizes.get(modelIndex));
            }

            int width = getWidth(modelIndex, defaultWidth);
            if (column.getPreferredWidth() != width) {
                column.setPreferredWidth(width);
            }
        }
    }
    
    private void updateOrder() {
        TableColumn column=null;
        TableColumnModel columnModel = table.getColumnModel();
        TableModel model = table.getModel();
        String columnName = "";
        for (int i = 0; i < columnCount; i++) {
            columnName = model.getColumnName(i);
            column=table.getColumn(columnName);
            int modelIndex = column.getModelIndex();
            int orderIndex = getOrderIndex(modelIndex, modelIndex);
            if (i != orderIndex) {
                table.moveColumn(columnModel.getColumnIndex(columnName), orderIndex);
            }
        }
    }

    private int getWidth(int index, int defaultWidth) {
        return getPreference().getInt(WIDTH_KEY+Integer.toString(index), defaultWidth);
    }

    private void setWidth(int index, int width) {
        getPreference().putInt(WIDTH_KEY+Integer.toString(index), width);
    }

    private int getOrderIndex(int columnIndex, int defaultOrderIndex) {
        return getPreference().getInt(ORDER_KEY+Integer.toString(columnIndex), defaultOrderIndex);
    }

    private void setOrderIndex(int columnIndex, int defaultOrderIndex) {
        getPreference().putInt(ORDER_KEY+Integer.toString(columnIndex), defaultOrderIndex);
    }

    
}
