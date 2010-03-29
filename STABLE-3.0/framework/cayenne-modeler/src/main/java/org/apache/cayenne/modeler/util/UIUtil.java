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

import javax.swing.*;
import java.awt.*;

/**
 * @since 1.1
 */
public class UIUtil {

    /**
     * Scrolls table within JViewport to the selected row if there is one.
     */
    public static void scrollToSelectedRow(JTable table) {
        int row = table.getSelectedRow();
        if (row >= 0) {
            scroll(table, row, 0);
        }
    }

    /**
    * Scrolls view if it is located in a JViewport, so that the specified cell
    * is displayed in the center.
    */
    public static void scroll(JTable table, int rowIndex, int vColIndex) {
        if (!(table.getParent() instanceof JViewport)) {
            return;
        }

        JViewport viewport = (JViewport) table.getParent();
        Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);
        Rectangle viewRect = viewport.getViewRect();

        if (viewRect.intersects(rect)) {
            return;
        }

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0).
        rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

        // Calculate location of rect if it were at the center of view
        int centerX = (viewRect.width - rect.width) / 2;
        int centerY = (viewRect.height - rect.height) / 2;

        // Fake the location of the cell so that scrollRectToVisible
        // will move the cell to the center
        if (rect.x < centerX) {
            centerX = -centerX;
        }
        if (rect.y < centerY) {
            centerY = -centerY;
        }
        rect.translate(centerX, centerY);

        // Scroll the area into view.
        viewport.scrollRectToVisible(rect);
    }
    
    /**
     * "Injects" windows's content to another window
     */
    public static void dock(Window window, Window dockTo) {
        window.setVisible(false);
        
        ((RootPaneContainer) dockTo).getContentPane()
            .add(((RootPaneContainer) window).getContentPane(), BorderLayout.SOUTH);
        
        dockTo.setVisible(true);
    }
    
    /**
     * "Injects" windows's content to parent window
     */
    public static void dock(Window window) {
        dock(window, window.getOwner());
    }
}
