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
package org.apache.cayenne.modeler.event;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

/**
 * A class to handle mouse right-click on table and show popup after selecting specified
 * table row
 */
public class TablePopupHandler extends MouseAdapter {

    private final JTable table;

    private final JPopupMenu popup;

    /**
     * Creates new mouse handler for table, which shows specified popupmenu on right-click
     */
    public TablePopupHandler(JTable table, JPopupMenu popup) {
        this.table = table;
        this.popup = popup;
    }

    /**
     * Creates and installs mouse listener for a table
     */
    public static void install(JTable table, JPopupMenu popup) {
        table.addMouseListener(new TablePopupHandler(table, popup));
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        mouseReleased(e);
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        table.editingCanceled(new ChangeEvent(this));

        int row = table.rowAtPoint(e.getPoint());
        if (row != -1 && !table.getSelectionModel().isSelectedIndex(row)) {
            table.setRowSelectionInterval(row, row);
        }

        popup.show(table, e.getX(), e.getY());
    }
}
