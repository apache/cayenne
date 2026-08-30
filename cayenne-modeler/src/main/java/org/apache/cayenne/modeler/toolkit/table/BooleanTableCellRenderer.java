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

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

/**
 * Renders Boolean columns with the look and feel's own checkbox renderer, greying out the cells that
 * the table model reports as non-editable. The stock renderer paints those as if they were editable.
 */
public class BooleanTableCellRenderer implements TableCellRenderer {

    private final TableCellRenderer lafRenderer;

    public BooleanTableCellRenderer(TableCellRenderer lafRenderer) {
        this.lafRenderer = lafRenderer;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int col) {

        Component c = lafRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        c.setEnabled(table.isCellEditable(row, col));
        return c;
    }
}
