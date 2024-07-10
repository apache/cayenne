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

package org.apache.cayenne.modeler.editor.dbimport;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.dbimport.DeleteNodeAction;
import org.apache.cayenne.modeler.action.dbimport.EditNodeAction;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.util.Util;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.util.EventObject;
import java.util.regex.Pattern;

/**
 * @since 4.1
 */
public class DbImportTreeCellEditor extends DefaultTreeCellEditor {

    private ProjectController projectController;

    public DbImportTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
        super(tree, renderer);
        setFont(UIManager.getFont("Tree.font"));
        this.addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent e) {
                DbImportTreeCellEditor.this.cancelCellEditing();
            }

            @Override
            public void editingCanceled(ChangeEvent e) {
                editingStopped(e);
            }
        });

    }

    @Override
    public Object getCellEditorValue() {
        if (tree.getSelectionPath() == null) {
            return "";
        }
        DbImportTreeNode node = (DbImportTreeNode) tree.getSelectionPath().getLastPathComponent();
        return node.getUserObject();
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value,
                                                boolean isSelected, boolean expanded, boolean leaf, int row) {
        if (value instanceof DbImportTreeNode) {
            value = ((DbImportTreeNode) value).getSimpleNodeName();
        }
        return super.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        if (tree.getSelectionPath() != null) {
            // Disable label nodes editing
            if (((DbImportTreeNode) tree.getSelectionPath().getLastPathComponent()).getUserObject().getClass() == String.class) {
                return false;
            }
            if (tree.getSelectionPath().getLastPathComponent() == tree.getModel().getRoot()) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidReverseEngineering() {
        try {
            Pattern.compile(super.getCellEditorValue().toString());
        } catch (Exception exception) {
            return false;
        }
        return true;
    }

    @Override
    public void cancelCellEditing() {
        if (tree.getSelectionPath() == null) {
            return;
        }
        if (!Util.isEmptyString(super.getCellEditorValue().toString()) && !insertableNodeExist() && (isValidReverseEngineering())) {
            EditNodeAction action = projectController.getApplication().getActionManager().getAction(EditNodeAction.class);
            action.setActionName(super.getCellEditorValue().toString());
            action.actionPerformed(null);
        } else {
            DbImportTreeNode selectedNode = (DbImportTreeNode) tree.getSelectionPath().getLastPathComponent();
            if (Util.isEmptyString(selectedNode.getSimpleNodeName()) || (insertableNodeExist())) {
                DeleteNodeAction action = projectController.getApplication().getActionManager().getAction(DeleteNodeAction.class);
                TreePath parentPath = tree.getSelectionPath().getParentPath();
                action.actionPerformed(null);
                tree.setSelectionPath(parentPath);
            } else {
                tree.startEditingAtPath(tree.getSelectionPath());
            }
        }
        if (tree.getSelectionPath() != null) {
            DbImportTreeNode selectedNode = (DbImportTreeNode) tree.getSelectionPath().getLastPathComponent();
            ((DbImportTree) tree).reloadModelKeepingExpanded(selectedNode);
        }
    }

    private boolean equalNodes(int i, DbImportTreeNode parent, DbImportTreeNode selectedElement) {
        return super.getCellEditorValue().toString().equals(((DbImportTreeNode) parent.getChildAt(i)).getSimpleNodeName()) &&
                selectedElement.getUserObject().getClass().equals(((DbImportTreeNode) parent.getChildAt(i)).getUserObject().getClass());
    }

    private boolean insertableNodeExist() {
        DbImportTreeNode selectedElement;
        if (tree.getSelectionPath() == null) {
            selectedElement = (DbImportTreeNode) tree.getModel().getRoot();
        } else {
            selectedElement = (DbImportTreeNode) tree.getSelectionPath().getLastPathComponent();
        }
        int childCount = selectedElement.getParent().getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (equalNodes(i, (DbImportTreeNode) selectedElement.getParent(), selectedElement)) {
                if (selectedElement.getParent().getChildAt(i) != selectedElement) {
                    return true;
                }
            }

        }
        return false;
    }

    public void setProjectController(ProjectController projectController) {
        this.projectController = projectController;
    }
}
