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

package org.apache.cayenne.modeler.dialog.welcome;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputListener;

/**
 * Renderer for the list of last files. Ignores the selection, instead paints with
 * ROLLOVER_BACKGROUND (currently red) the row mouse is hovering over
 */
class RecentFileListRenderer extends DefaultListCellRenderer implements MouseInputListener {

    interface OnFileClickListener {
        void onFileSelect(File file);
    }

    /**
     * Color for background of row mouse is over
     */
    private final Color ROLLOVER_BACKGROUND = new Color(223, 223, 223);

    /**
     * List which is rendered
     */
    private final JList<String> list;

    /**
     * Row mouse is over
     */
    private int rolloverRow;

    private OnFileClickListener listener;

    RecentFileListRenderer(JList<String> list, OnFileClickListener listener) {
        if(listener == null) {
            throw new NullPointerException("listener parameter is null");
        }
        list.addMouseListener(this);
        list.addMouseMotionListener(this);

        this.list = list;
        this.listener = listener;
        rolloverRow = -1;

        setHorizontalTextPosition(SwingConstants.LEADING);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, false, false);
        setMargin(8, 15, 8, 15);
        if (rolloverRow == index) {
            setOpaque(true);
            setBackground(ROLLOVER_BACKGROUND);
            setToolTipText(getSelectedFile().getAbsolutePath());
        } else {
            setOpaque(false);
        }
        return this;
    }

    public void setMargin(int top, int left, int bottom, int right) {
        setBorder(new EmptyBorder(top, left, bottom, right));
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
        mouseMoved(e);
    }

    public void mouseExited(MouseEvent e) {
        rolloverRow = -1;
        list.repaint();
    }

    public void mousePressed(MouseEvent e) {
    }

    private File getSelectedFile() {
        if(rolloverRow == -1) {
            return null;
        }
        return ((RecentFileListModel)list.getModel()).getFullElementAt(rolloverRow);
    }

    public void mouseReleased(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || rolloverRow == -1) {
            return;
        }
        listener.onFileSelect(getSelectedFile());
        rolloverRow = -1; // clear selection
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        int newRow;

        // Check that a row boundary contains the mouse point, so that rolloverRow
        // would be -1 if we are below last row
        Rectangle bounds = list.getCellBounds(0, list.getModel().getSize() - 1);
        if (list.getModel().getSize() > 0 && !bounds.contains(e.getPoint())) {
            newRow = -1;
        } else {
            newRow = list.locationToIndex(e.getPoint());
        }

        if (rolloverRow != newRow) {
            rolloverRow = newRow;
            list.repaint();
        }
    }
}
