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

import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.pref.CayennePreference;

public class TableColumnPreferences extends CayennePreference {

    private static final String SORT_COLUMN_KEY = "sort_column";
    private static final String SORT_ORDER_KEY = "sort_order";
    private static final String WIDTH_KEY = "width_";
    public static final String TABLE_COLUMN_PREF_KEY = "table_column";
    private static final String ORDER_KEY = "order_";
    private JTable table;
    private int columnCount;
    private int defaultSortColumn;
    private boolean defaultSortOrder;
    private int[] currentWidth;

    private TableColumnModelListener listener = new TableColumnModelListener() {

        public void columnAdded(TableColumnModelEvent e) {
        }

        public void columnMarginChanged(ChangeEvent e) {
            TableColumn column;
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
            updateSort(defaultSortColumn, defaultSortOrder);
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
     * allowing to specify an initial offset compared to the stored position. Allow to specify
     * initial sorting.
     * 
     * @param table
     * @param minSizes
     * @param maxSizes
     * @param defaultSizes
     * @param defaultSortColumn
     * @param defaultSortOrder
     */
    public void bind(
            final JTable table,
            Map<Integer, Integer> minSizes,
            Map<Integer, Integer> maxSizes,
            Map<Integer, Integer> defaultSizes,
            int defaultSortColumn,
            boolean defaultSortOrder) {
        bind(table, minSizes, maxSizes, defaultSizes);
        ((CayenneTable) table).setSortPreferenceSaver(this);
        this.defaultSortColumn = defaultSortColumn;
        this.defaultSortOrder = defaultSortOrder;
        updateSort(defaultSortColumn, defaultSortOrder);
    }

    /**
     * Binds this preference object to synchronize its state with a given table component,
     * allowing to specify an initial offset compared to the stored position. 
     * 
     * @param table
     * @param minSizes
     * @param maxSizes
     * @param defaultSizes
     */
    public void bind(
            final JTable table,
            Map<Integer, Integer> minSizes,
            Map<Integer, Integer> maxSizes,
            Map<Integer, Integer> defaultSizes) {

        this.table = table;
        this.columnCount = table.getColumnCount();
        this.currentWidth = new int[columnCount];

        table.getColumnModel().removeColumnModelListener(listener);
        updateTable(minSizes, maxSizes, defaultSizes);
        table.getColumnModel().addColumnModelListener(listener);

    }

    private void updateTable(
            Map<Integer, Integer> minSizes,
            Map<Integer, Integer> maxSizes, Map<Integer, Integer> defaultSizes) {
        updateWidths(minSizes, maxSizes, defaultSizes);
        updateOrder();
    }

    private void updateWidths(
            Map<Integer, Integer> minSizes,
            Map<Integer, Integer> maxSizes, Map<Integer, Integer> defaultSizes) {
        TableColumn column = null;
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnCount; i++) {
            column = columnModel.getColumn(i);
            int modelIndex = column.getModelIndex();

            int defaultWidth;
            if (minSizes != null && minSizes.containsKey(modelIndex)) {
                column.setMinWidth(minSizes.get(modelIndex));
                
            }

            if (maxSizes != null && maxSizes.containsKey(modelIndex)) {
                column.setMaxWidth(maxSizes.get(modelIndex));
            }

            if(defaultSizes!=null && defaultSizes.containsKey(modelIndex)){
                defaultWidth=defaultSizes.get(modelIndex);
            } else {
                defaultWidth = column.getPreferredWidth();
            }
            
            int width = getWidth(modelIndex, defaultWidth);
            if (column.getPreferredWidth() != width) {
                column.setPreferredWidth(width);
            }
        }
    }

    private void updateOrder() {
        TableColumn column;
        TableColumnModel columnModel = table.getColumnModel();
        TableModel model = table.getModel();
        String columnName = "";
        for (int i = 0; i < columnCount; i++) {
            columnName = model.getColumnName(i);
            column = table.getColumn(columnName);
            int modelIndex = column.getModelIndex();
            int orderIndex = getOrderIndex(modelIndex, modelIndex);
            if (i != orderIndex) {
                table.moveColumn(columnModel.getColumnIndex(columnName), orderIndex);
            }
        }
    }

    private void updateSort(int defaultSortColumn, boolean defaultSortOrder) {
        ((CayenneTable) table).sort(
                getSortColumn(SORT_COLUMN_KEY, defaultSortColumn),
                getSortOrder(SORT_ORDER_KEY, defaultSortOrder));
    }

    private int getWidth(int index, int defaultWidth) {
        if(currentWidth[index] == 0) {
            currentWidth[index] = getPreference().getInt(WIDTH_KEY + Integer.toString(index), defaultWidth);
        }
        return currentWidth[index];
    }

    private void setWidth(int index, int width) {
        if(currentWidth[index] != width) {
            getPreference().putInt(WIDTH_KEY + Integer.toString(index), width);
            currentWidth[index] = width;
        }
    }

    private int getOrderIndex(int columnIndex, int defaultOrderIndex) {
        return getPreference().getInt(
                ORDER_KEY + Integer.toString(columnIndex),
                defaultOrderIndex);
    }

    private void setOrderIndex(int columnIndex, int defaultOrderIndex) {
        getPreference().putInt(
                ORDER_KEY + Integer.toString(columnIndex),
                defaultOrderIndex);
    }

    private boolean getSortOrder(String sortOrderKey, boolean defaultSortOrder) {
        return getPreference().getBoolean(SORT_ORDER_KEY, defaultSortOrder);
    }

    public void setSortOrder(boolean isAscent) {
        getPreference().putBoolean(SORT_ORDER_KEY, isAscent);
    }

    private int getSortColumn(String sortColumnKey, int defaultSortColumn) {
        return getPreference().getInt(SORT_COLUMN_KEY, defaultSortColumn);
    }

    public void setSortColumn(int sortCol) {
        getPreference().putInt(SORT_COLUMN_KEY, sortCol);
    }

}
