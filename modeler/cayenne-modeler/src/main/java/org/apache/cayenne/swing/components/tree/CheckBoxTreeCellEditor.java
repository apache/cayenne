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
package org.apache.cayenne.swing.components.tree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @since 5.0
 */
public class CheckBoxTreeCellEditor extends AbstractCellEditor implements TreeCellEditor {

    protected final CheckBoxTreeCellRenderer renderer;
    protected final TreeCellEditor defaultEditor;
    protected final JTree tree;

    private final AbstractAction toggleListener;

    protected CheckBoxTreeCellEditor(JTree tree) {
        this.tree = tree;
        this.renderer = new CheckBoxTreeCellRenderer();
        this.defaultEditor = new DefaultTreeCellEditor(tree, new DefaultTreeCellRenderer());

        this.toggleListener = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object cellEditorValue = getCellEditorValue();
                if (!(cellEditorValue instanceof CheckBoxNodeData) || !(e.getSource() instanceof JCheckBox)) {
                    return;
                }
                TreePath editingPath = tree.getEditingPath();
                JCheckBox checkBox = (JCheckBox) e.getSource();

                CheckBoxNodeData data = (CheckBoxNodeData) cellEditorValue;
                tree.getModel().valueForPathChanged(editingPath, data.withState(checkBox.isSelected()));
                tree.stopEditing();
            }
        };
    }

    @Override
    public Object getCellEditorValue() {
        DefaultMutableTreeNode editedNode = (DefaultMutableTreeNode) tree.getEditingPath().getLastPathComponent();
        return editedNode.getUserObject();
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
                                                boolean leaf, int row) {
        Component component = renderer.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, true);

        if (component instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) component;
            checkBox.setAction(toggleListener);
        }
        return component;
    }
}
