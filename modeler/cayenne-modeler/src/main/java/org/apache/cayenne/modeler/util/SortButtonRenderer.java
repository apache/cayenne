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

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public class SortButtonRenderer extends DefaultTableCellRenderer {

    public static final int NONE = 0;
    public static final int DOWN = 1;
    public static final int UP = 2;

    private static final Icon ICON_DOWN = ModelerUtil.buildIcon("icon-sort-desc.png");
    private static final Icon ICON_UP = ModelerUtil.buildIcon("icon-sort-asc.png");
    private static final Font FONT;
    private static final CompoundBorder BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY),
            BorderFactory.createEmptyBorder(0, 5, 0, 0));

    static {
        // Get default font for current system
        FONT = new JLabel().getFont().deriveFont(Font.BOLD);
    }

    private boolean sortingEnabled = true;
    private int currentState;
    private int currentColumn;

    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (sortingEnabled && column == currentColumn) {
            if (currentState == DOWN) {
                setIcon(ICON_DOWN);
            } else {
                setIcon(ICON_UP);
            }
        } else {
            setIcon(null);
        }

        setText(value == null ? "" : value.toString());
        setFont(FONT);
        setHorizontalTextPosition(JLabel.LEFT);
        setBorder(BORDER);
        return this;
    }

    public void setSelectedColumn(int col, boolean isAscOrder) {
        if (col < 0) {
            return;
        }
        //shows the direction of ordering
        if (isAscOrder) {
            currentState = DOWN;
        } else {
            currentState = UP;
        }
        currentColumn = col;
    }

    public int getState(int col) {
        if (col == currentColumn){
            return currentState;
        }
        return NONE;
    }

    public boolean isSortingEnabled() {
        return sortingEnabled;
    }

    public void setSortingEnabled(boolean sortingEnabled) {
        this.sortingEnabled = sortingEnabled;
    }
}