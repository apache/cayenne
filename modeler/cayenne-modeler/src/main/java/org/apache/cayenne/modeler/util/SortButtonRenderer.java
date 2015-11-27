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
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Hashtable;

public class SortButtonRenderer extends JLabel implements TableCellRenderer {

    public static final int NONE = 0;
    public static final int DOWN = 1;
    public static final int UP = 2;

    private int pushedColumn;
    private Hashtable state;
    private JLabel downLabel , upLabel;

    public SortButtonRenderer() {
        pushedColumn = -1;
        state = new Hashtable();

        downLabel = new JLabel();
        downLabel.setIcon(new BevelArrowIcon(BevelArrowIcon.DOWN, false, false));

        upLabel = new JLabel();
        upLabel.setIcon(new BevelArrowIcon(BevelArrowIcon.UP, false, false));
    }

    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
        JLabel  label = this;
        Object obj = state.get(new Integer(column));

        if (obj != null) {
            if (((Integer) obj).intValue() == DOWN) {
                label = downLabel;
            }
            else {
                label = upLabel;
            }
        }
        Border border = BorderFactory.createLineBorder(Color.GRAY, 1);
        label.setText(" " + ((value == null) ? "" : value.toString()));
        label.setFont(new Font("Verdana", Font.BOLD , 13));
        label.setHorizontalTextPosition(JLabel.LEFT);
        label.setBorder(border);
        label.setOpaque(true);
        label.setBackground(new Color(204, 238, 255));
        return label;
    }

    public void setPressedColumn(int col) {
        pushedColumn = col;
    }

    public void setSelectedColumn(int col, boolean isAscOrder) {
        if (col < 0)
            return;
        Integer value = null;
        //shows the direction of ordering
        if (isAscOrder) {
            value = new Integer(DOWN);
        }
        else {
            value = new Integer(UP);
        }

        state.clear();
        state.put(new Integer(col), value);
    }

    public int getState(int col) {
        int retValue;
        Object obj = state.get(new Integer(col));
        if (obj == null) {
            retValue = NONE;
        }
        else {
            if (((Integer) obj).intValue() == DOWN) {
                retValue = DOWN;
            }
            else {
                retValue = UP;
            }
        }
        return retValue;
    }
}