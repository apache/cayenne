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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public class SortButtonRenderer  extends DefaultTableCellRenderer {

    public static final int NONE = 0;
    public static final int DOWN = 1;
    public static final int UP = 2;

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

        if (column == currentColumn) {
            if (currentState == DOWN) {
                setIcon(new BevelArrowIcon(BevelArrowIcon.DOWN, false, false));
            } else {
                setIcon(new BevelArrowIcon(BevelArrowIcon.UP, false, false));
            }
        }else {
            setIcon(new ImageIcon());
        }

        setText( ((value == null) ? "" : value.toString()));
        setFont(new Font("Verdana", Font.BOLD, 12));
        setHorizontalTextPosition(JLabel.LEFT);
        CompoundBorder compoundBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY),
                BorderFactory.createEmptyBorder(0, 5, 0, 0));
        setBorder(compoundBorder);
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
}