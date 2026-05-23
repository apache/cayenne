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

import org.apache.cayenne.modeler.toolkit.combobox.AutoCompletion;
import org.apache.cayenne.modeler.toolkit.combobox.ComboBoxPopup;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.EventObject;

/**
 * Combo box cell editor for the modeler. Handles both auto-completion-aware
 * and plain combos uniformly. Editing is suppressed for ctrl/shift-clicks so
 * the user can extend a multi-row selection without opening the editor.
 */
public class CMComboBoxCellEditor extends AbstractCellEditor
        implements TableCellEditor, ActionListener, Serializable {

    // Auto-complete combos collide with DefaultCellEditor's stop-editing flow,
    // so they need a custom action-listener-based path. This client property
    // is read by Swing's combo UI to keep the popup behavior table-friendly.
    static final String IS_TABLE_CELL_EDITOR_PROPERTY = "JComboBox.isTableCellEditor";

    private final JComboBox<?> comboBox;
    private final boolean autocomplete;

    public CMComboBoxCellEditor(JComboBox<?> comboBox) {
        this.comboBox = comboBox;
        this.autocomplete = Boolean.TRUE.equals(
                comboBox.getClientProperty(AutoCompletion.AUTOCOMPLETION_PROPERTY));

        if (autocomplete) {
            comboBox.putClientProperty(IS_TABLE_CELL_EDITOR_PROPERTY, Boolean.TRUE);
            comboBox.addActionListener(this);
        }

        comboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                // First call: sets popup.preferredSize so the popup *window* is created at the
                // correct width (window size is determined from popup.preferredSize before show()).
                adjustPopupWidth();
                // Second call via invokeLater: BasicComboPopup.show() calls getPopupLocation()
                // *after* firing this listener, which re-constrains scroller.maxSize back to the
                // column width. Running again on the next EDT cycle fixes the scroller, then
                // revalidate/repaint forces the layout to update inside the already-wide window.
                SwingUtilities.invokeLater(() -> {
                    adjustPopupWidth();
                    Object child = comboBox.getUI().getAccessibleChild(comboBox, 0);
                    if (child instanceof JPopupMenu popup) {
                        popup.revalidate();
                        popup.repaint();
                    }
                });
            }
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });
    }

    @Override
    public Object getCellEditorValue() {
        return comboBox.getSelectedItem();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        comboBox.setSelectedItem(value);
        return comboBox;
    }

    private void adjustPopupWidth() {
        Object child = comboBox.getUI().getAccessibleChild(comboBox, 0);
        if (!(child instanceof JPopupMenu)) {
            return;
        }
        JPopupMenu popup = (JPopupMenu) child;
        JScrollPane scrollPane = findScrollPane(popup);
        if (scrollPane == null) {
            return;
        }

        // BasicComboPopup.show() constrains the scroll pane's preferredSize and maximumSize
        // to the column width before firing this listener. Reset them so the scroll pane
        // reports its natural content-based width — which already incorporates item metrics,
        // list insets, scrollbar width, and scroll pane borders — no manual overhead needed.
        scrollPane.setPreferredSize(null);
        scrollPane.setMaximumSize(null);
        popup.setPreferredSize(null);

        int naturalWidth = scrollPane.getPreferredSize().width;
        int targetWidth = Math.min(Math.max(naturalWidth, comboBox.getWidth()), ComboBoxPopup.MAX_WIDTH);

        Dimension scrollSize = new Dimension(targetWidth, scrollPane.getPreferredSize().height);
        scrollPane.setPreferredSize(scrollSize);
        scrollPane.setMaximumSize(scrollSize);
        popup.setPreferredSize(new Dimension(targetWidth, popup.getPreferredSize().height));
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

    @Override
    public boolean stopCellEditing() {
        if (autocomplete && comboBox.isEditable()) {
            // Notify the combo box that editing has stopped (e.g. user pressed F2).
            comboBox.actionPerformed(new ActionEvent(this, 0, ""));
        }
        fireEditingStopped();
        return true;
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) e;
            if (me.isControlDown() || me.isShiftDown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Selecting an item produces "comboBoxChanged" — ignore.
        // Hitting enter produces "comboBoxEdited" — stop editing.
        if (autocomplete && "comboBoxEdited".equals(e.getActionCommand())) {
            stopCellEditing();
        }
    }
}
