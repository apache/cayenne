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

package org.apache.cayenne.modeler.toolkit.combobox;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;

/**
 * Widens a combo box drop-down to fit its content, up to {@link ComboBoxPopup#MAX_WIDTH}.
 * Handles both heavyweight (own Window) and lightweight (embedded) popups correctly.
 */
public class CMComboBoxPopupResizer implements PopupMenuListener {

    private final JComboBox<?> comboBox;

    public CMComboBoxPopupResizer(JComboBox<?> comboBox) {
        this.comboBox = comboBox;
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        // BasicComboPopup.show() calls getPopupLocation() after this listener fires,
        // constraining the popup to the column width. Adjust on the next EDT cycle —
        // invokeLater lands ahead of the first WM_PAINT, so no flash occurs.
        SwingUtilities.invokeLater(this::adjustPopupWidth);
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    private void adjustPopupWidth() {
        Object child = comboBox.getUI().getAccessibleChild(comboBox, 0);
        if (!(child instanceof JPopupMenu popup)) {
            return;
        }
        JScrollPane scrollPane = findScrollPane(popup);
        if (scrollPane == null) {
            return;
        }

        // Heavyweight popup → its own top-level Window (distinct from the combo's window).
        // Lightweight popup → no dedicated Window; getWindowAncestor returns the *main app frame*,
        // which we must NOT resize. Whether a popup is heavy or light is decided per-show by Swing
        // based on whether it fits inside the parent window (Windows tends to use lightweight when
        // it fits; macOS uses heavyweight more often).
        Window comboWindow = SwingUtilities.getWindowAncestor(comboBox);
        Window popupWindow = SwingUtilities.getWindowAncestor(popup);
        boolean heavyweight = popupWindow != null && popupWindow != comboWindow;

        // Clear sizes set by BasicComboPopup.getPopupLocation() so the scroll pane
        // reports its natural content-based width.
        scrollPane.setPreferredSize(null);
        scrollPane.setMaximumSize(null);
        popup.setPreferredSize(null);

        int naturalWidth = scrollPane.getPreferredSize().width;
        int targetWidth = Math.min(Math.max(naturalWidth, comboBox.getWidth()), ComboBoxPopup.MAX_WIDTH);

        // For the height, BasicComboPopup has already sized the popup's container correctly based
        // on row count. Reuse that height when available; otherwise fall back to popup pref height.
        int targetHeight = heavyweight ? popupWindow.getHeight() : popup.getPreferredSize().height;
        Dimension scrollSize = new Dimension(targetWidth, targetHeight);
        scrollPane.setPreferredSize(scrollSize);
        scrollPane.setMaximumSize(scrollSize);
        popup.setPreferredSize(new Dimension(targetWidth, targetHeight));

        if (heavyweight) {
            // Widen the dedicated popup window; preserve its location.
            Point loc = popupWindow.getLocation();
            popupWindow.setSize(targetWidth, targetHeight);
            popupWindow.setLocation(loc);
        }

        popup.revalidate();
        popup.repaint();
    }

    private static JScrollPane findScrollPane(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JScrollPane) {
                return (JScrollPane) c;
            }
            if (c instanceof Container) {
                JScrollPane found = findScrollPane((Container) c);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
