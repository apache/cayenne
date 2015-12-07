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
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.border.MatteBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.util.Hashtable;

public class SortButtonRenderer extends JButton implements TableCellRenderer {

    public static final int NONE = 0;
    public static final int DOWN = 1;
    public static final int UP = 2;

    private int pushedColumn;
    private Hashtable state;
    private JButton downButton, upButton;

    public SortButtonRenderer() {
        MatteBorder matteBorder = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.gray);
        setBorder(matteBorder);

        pushedColumn = -1;
        state = new Hashtable();

        setMargin(new Insets(0, 0, 0, 0));
        setHorizontalTextPosition(CENTER);
        setIcon(new BlankIcon());

        downButton = new JButton();

        downButton.setBorder(matteBorder);
        downButton.setMargin(new Insets(0, 0, 0, 0));
        downButton.setHorizontalTextPosition(LEFT);
        downButton.setIcon(new BevelArrowIcon(BevelArrowIcon.DOWN, false, false));
        downButton.setPressedIcon(new BevelArrowIcon(BevelArrowIcon.DOWN, false, true));

        upButton = new JButton();
        upButton.setBorder(matteBorder);
        upButton.setMargin(new Insets(0, 0, 0, 0));
        upButton.setHorizontalTextPosition(LEFT);
        upButton.setIcon(new BevelArrowIcon(BevelArrowIcon.UP, false, false));
    }

    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
        JButton button = this;
        Object obj = state.get(new Integer(column));

        if (obj != null) {
            if (((Integer) obj).intValue() == DOWN) {
                button = downButton;
            } else {
                button = upButton;
            }
        }
        button.setText((value == null) ? "" : value.toString());
        return button;
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
        } else {
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
        } else {
            if (((Integer) obj).intValue() == DOWN) {
                retValue = DOWN;
            } else {
                retValue = UP;
            }
        }
        return retValue;
    }
}