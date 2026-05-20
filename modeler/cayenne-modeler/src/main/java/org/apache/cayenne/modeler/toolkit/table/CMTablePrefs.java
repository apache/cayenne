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

import org.apache.cayenne.modeler.pref.PreferenceAdapter;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.util.Map;
import java.util.prefs.Preferences;

public final class CMTablePrefs extends PreferenceAdapter {

    private static final String SORT_COLUMN_KEY = "sortColumn";
    private static final String SORT_ORDER_KEY = "sortOrder";
    private static final String WIDTH_KEY = "colWidth";
    private static final String ORDER_KEY = "colOrder";
    private static final String LISTENER_PROPERTY = "TablePrefs.listener";

    public CMTablePrefs(Preferences prefs) {
        super(prefs);
    }

    /**
     * Binds widths and column order to the table's stored preferences. No sort wiring.
     */
    public void bind(CMTable table, Map<Integer, Integer> minSizes) {
        bind(table, minSizes, -1);
    }

    /**
     * Binds widths, column order, and an initial ascending sort on the given column.
     */
    public void bind(CMTable table, Map<Integer, Integer> minSizes, int defaultSortColumn) {
        doBind(table, minSizes, defaultSortColumn);
        if (defaultSortColumn >= 0) {
            table.setSortChangedListener((sortCol, asc) -> {
                prefs.putInt(SORT_COLUMN_KEY, sortCol);
                prefs.putBoolean(SORT_ORDER_KEY, asc);
            });
        }
    }

    private void doBind(CMTable table, Map<Integer, Integer> minSizes, int defaultSortColumn) {

        int columnCount = table.getColumnCount();
        TableColumnModel columnModel = table.getColumnModel();

        Object prev = table.getClientProperty(LISTENER_PROPERTY);
        if (prev instanceof TableColumnModelListener) {
            columnModel.removeColumnModelListener((TableColumnModelListener) prev);
        }

        applyWidths(columnModel, columnCount, minSizes);
        applyOrder(table, columnModel, columnCount);
        applySort(table, defaultSortColumn);

        // Cache last value written per modelIndex to avoid hitting the native
        // Preferences store on every margin / move event. Swing fires margin
        // events in floods during layout and column drags; without this guard,
        // a single resize triggers thousands of putInt() calls.

        int[] lastWidth = new int[columnCount];
        int[] lastOrder = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            TableColumn c = columnModel.getColumn(i);
            int modelIndex = c.getModelIndex();
            lastWidth[modelIndex] = c.getPreferredWidth();
            lastOrder[modelIndex] = i;
        }

        TableColumnModelListener listener = new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {
            }

            @Override
            public void columnRemoved(TableColumnModelEvent e) {
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
            }

            @Override
            public void columnMarginChanged(ChangeEvent e) {
                for (int i = 0; i < columnCount; i++) {
                    TableColumn c = columnModel.getColumn(i);
                    int modelIndex = c.getModelIndex();
                    int w = c.getPreferredWidth();
                    if (lastWidth[modelIndex] != w) {
                        prefs.putInt(WIDTH_KEY + modelIndex, w);
                        lastWidth[modelIndex] = w;
                    }
                }
            }

            @Override
            public void columnMoved(TableColumnModelEvent e) {
                for (int i = 0; i < columnCount; i++) {
                    TableColumn c = columnModel.getColumn(i);
                    int modelIndex = c.getModelIndex();
                    if (lastOrder[modelIndex] != i) {
                        prefs.putInt(ORDER_KEY + modelIndex, i);
                        lastOrder[modelIndex] = i;
                    }
                }
                applySort(table, defaultSortColumn);
            }
        };
        table.putClientProperty(LISTENER_PROPERTY, listener);
        columnModel.addColumnModelListener(listener);
    }

    private void applyWidths(TableColumnModel columnModel, int columnCount,
                             Map<Integer, Integer> minSizes) {
        for (int i = 0; i < columnCount; i++) {
            TableColumn column = columnModel.getColumn(i);
            int modelIndex = column.getModelIndex();

            if (minSizes != null && minSizes.containsKey(modelIndex)) {
                column.setMinWidth(minSizes.get(modelIndex));
            }

            int width = prefs.getInt(WIDTH_KEY + modelIndex, column.getPreferredWidth());
            if (column.getPreferredWidth() != width) {
                column.setPreferredWidth(width);
            }
        }
    }

    private void applyOrder(CMTable table, TableColumnModel columnModel, int columnCount) {
        TableModel model = table.getModel();
        for (int i = 0; i < columnCount; i++) {
            String columnName = model.getColumnName(i);
            TableColumn column = table.getColumn(columnName);
            int modelIndex = column.getModelIndex();
            int orderIndex = prefs.getInt(ORDER_KEY + modelIndex, modelIndex);
            if (i != orderIndex) {
                table.moveColumn(columnModel.getColumnIndex(columnName), orderIndex);
            }
        }
    }

    private void applySort(CMTable table, int defaultSortColumn) {
        if (defaultSortColumn >= 0) {
            int sortCol = prefs.getInt(SORT_COLUMN_KEY, defaultSortColumn);
            boolean asc = prefs.getBoolean(SORT_ORDER_KEY, true);
            table.sort(sortCol, asc);
        }
    }
}
