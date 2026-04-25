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

package org.apache.cayenne.modeler.util;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;

/**
 * Utility for sizing JTable columns based on sample values.
 */
public final class TableSizer {

    private TableSizer() {
    }

    /**
     * Sets the preferred width of each column to fit either its header label or the
     * rendered width of the corresponding sample value, whichever is wider.
     * Pass {@code null} for any column whose width should only be sized to its header.
     *
     * @param table        the table whose columns to resize
     * @param sampleValues one sample value per column; may be shorter than the column count
     */
    public static void sizeColumns(JTable table, Object... sampleValues) {
        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            Component header = headerRenderer.getTableCellRendererComponent(
                    null, column.getHeaderValue(), false, false, 0, 0);
            int width = header.getPreferredSize().width;
            if (i < sampleValues.length && sampleValues[i] != null) {
                Component cell = table.getDefaultRenderer(table.getModel().getColumnClass(i))
                        .getTableCellRendererComponent(table, sampleValues[i], false, false, 0, i);
                width = Math.max(width, cell.getPreferredSize().width);
            }
            column.setPreferredWidth(width);
        }
    }
}
