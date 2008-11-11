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
package org.apache.cayenne.modeler.util.combo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.CellRendererPane;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

/**
 * EditorTextField is a text field to be used in combobox editor. It paints self normally
 * when focused, otherwise combo's renderer is used.
 * 
 */
public class EditorTextField extends JTextField implements FocusListener {

    /**
     * Edited combobox
     */
    protected JComboBox combo;

    /**
     * Panel to draw renderer component
     */
    private final CellRendererPane rendererPane;

    /**
     * Combo's popup list is unaccessible, so we use our own default list
     */
    private final JList list = new JList();

    /**
     * True if editor has focus.
     */
    private boolean hasFocus;

    public EditorTextField(JComboBox edited) {
        combo = edited;
        rendererPane = new CellRendererPane();

        addFocusListener(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        if (hasFocus)
            super.paintComponent(g);
        else {
            list.setEnabled(combo.isEnabled());

            ListCellRenderer renderer = combo.getRenderer();
            Component c = renderer.getListCellRendererComponent(list, combo
                    .getSelectedItem(), -1, false, false);
            
            //fill background first
            Color oldColor = g.getColor();
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(oldColor);
            
            Insets insets = getInsets();
            rendererPane.paintComponent(g, c, combo, insets.left, insets.top, 
                    getWidth() - insets.right - insets.left, getHeight() - insets.bottom - insets.top);
        }
    }

    public void focusGained(FocusEvent e) {
        hasFocus = true;

        combo.repaint();
    }

    public void focusLost(FocusEvent e) {
        hasFocus = false;
        combo.repaint();
    }
}
