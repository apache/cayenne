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

package org.apache.cayenne.modeler.action.dbimport;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.DbImportSorter;
import org.apache.cayenne.modeler.editor.dbimport.DbImportTree;
import org.apache.cayenne.modeler.editor.dbimport.DbImportView;
import org.apache.cayenne.modeler.editor.dbimport.DraggableTreePanel;
import org.apache.cayenne.modeler.undo.DbImportTreeUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;

import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @since 4.1
 */
public class MoveImportNodeAction extends CayenneAction {

    private static final String ICON_NAME = "icon-backward.png";
    private static final String ACTION_NAME = "Include";
    private static final String EMPTY_NAME = "";

    private DbImportTree sourceTree;
    private DbImportTree targetTree;
    private DraggableTreePanel panel;
    protected boolean moveInverted;
    private Map<Class, Class> classMap;

    public MoveImportNodeAction(Application application) {
        super(ACTION_NAME, application);
    }

    MoveImportNodeAction(String actionName, Application application) {
        super(actionName, application);
        initMap();
    }

    private void initMap() {
        classMap = new HashMap<>();
        classMap.put(IncludeTable.class, ExcludeTable.class);
        classMap.put(IncludeColumn.class, ExcludeColumn.class);
        classMap.put(IncludeProcedure.class, ExcludeProcedure.class);
        classMap.put(Schema.class, Schema.class);
        classMap.put(Catalog.class, Catalog.class);
    }

    public String getIconName() {
        return ICON_NAME;
    }

    private boolean canInsert(TreePath path, DbImportTreeNode foundNode) {
        DbImportTreeNode sourceElement = (DbImportTreeNode) path.getLastPathComponent();
        DbImportTreeNode selectedElement;
        if (foundNode == null) {
            if (targetTree.getSelectionPath() != null) {
                DbImportTreeNode node = targetTree.getSelectedNode();
                if ((node.getUserObject().getClass() == Catalog.class)
                        || (node.getUserObject().getClass() == Schema.class)
                        || (node.getUserObject().getClass() == ReverseEngineering.class)) {
                    selectedElement = targetTree.getSelectedNode();
                } else {
                    selectedElement = (DbImportTreeNode) targetTree.getSelectionPath().
                            getParentPath().getLastPathComponent();
                }
            } else {
                selectedElement = targetTree.getRootNode();
            }
        } else {
            selectedElement = foundNode;
        }
        if ((nodeClassesIsSameTypes(sourceElement, selectedElement))
                && (sourceElement.getSimpleNodeName().equals(selectedElement.getSimpleNodeName()))) {
            return false;
        }
        int childCount = selectedElement.getChildCount();
        for (int i = 0; i < childCount; i++) {
            DbImportTreeNode child = (DbImportTreeNode) selectedElement.getChildAt(i);
            if ((nodeClassesIsSameTypes(sourceElement, child))
                && (sourceElement.getSimpleNodeName().equals(child.getSimpleNodeName()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void performAction(ActionEvent e) {
        TreePath[] paths = sourceTree.getSelectionPaths();
        TreeManipulationAction action = null;
        DbImportTreeNode foundNode = null;
        String insertableName = EMPTY_NAME;
        DbImportView rootParent = (DbImportView) panel.getParent().getParent();
        rootParent.getReverseEngineeringProgress().setVisible(true);
        if (paths != null) {
            boolean isChanged = false;
            ReverseEngineering reverseEngineeringOldCopy = new ReverseEngineering(targetTree.getReverseEngineering());
            try {
                for (TreePath path : paths) {
                    DbImportTreeNode selectedElement = (DbImportTreeNode) path.getLastPathComponent();
                    DbImportTreeNode previousNode;
                    foundNode = targetTree.findNodeByParentsChain(
                            targetTree.getRootNode(), selectedElement, 0
                    );
                    // If parent nodes from db schema doesn't exist, create it
                    if (foundNode == null) {
                        for (int i = selectedElement.getParents().size() - 2; i >= 0; i--) {
                            DbImportTreeNode insertedNode = selectedElement.getParents().get(i);
                            previousNode = targetTree.findNodeByParentsChain(targetTree.getRootNode(), insertedNode, 0);
                            if (previousNode == null) {
                                previousNode = targetTree.getRootNode();
                            }
                            TreeManipulationAction manipulationAction = panel.getActionByNodeType(
                                    insertedNode.getUserObject().getClass()
                            );
                            if (canInsert(new TreePath(insertedNode.getPath()), previousNode)) {
                                manipulationAction.setFoundNode(previousNode);
                                manipulationAction.setInsertableNodeName(insertedNode.getSimpleNodeName());
                                manipulationAction.setTree(targetTree);
                                manipulationAction.setMovedFromDbSchema(true);
                                manipulationAction.actionPerformed(e);
                                manipulationAction.setFoundNode(null);
                                manipulationAction.setMultipleAction(false);
                            }
                        }
                    }
                    // Again find node where we insert our node
                    foundNode = targetTree.findNodeByParentsChain(targetTree.getRootNode(), selectedElement, 0);
                    if (!moveInverted) {
                        action = panel.getActionByNodeType(selectedElement.getUserObject().getClass());
                    } else {
                        action = panel.getActionByNodeType(classMap.get(selectedElement.getUserObject().getClass()));
                    }
                    if (action != null) {
                        if (paths.length > 1) {
                            action.setMultipleAction(true);
                        } else {
                            action.setMultipleAction(false);
                        }
                        if (canInsert(path, foundNode)) {
                            insertableName = selectedElement.getSimpleNodeName();
                            action.setFoundNode(foundNode);
                            action.setInsertableNodeName(Matcher.quoteReplacement(insertableName));
                            action.setTree(targetTree);
                            action.setMovedFromDbSchema(true);
                            action.actionPerformed(e);
                            action.setFoundNode(null);
                            action.resetActionFlags();
                            isChanged = true;
                            sourceTree.setSelectionRow(-1);
                            panel.getMoveButton().setEnabled(false);
                            panel.getMoveInvertButton().setEnabled(false);
                        }
                    }
                }
                if ((paths.length > 1) && (targetTree.getSelectionPath() != null)) {
                    getProjectController().setDirty(true);
                    List<DbImportTreeNode> expandList = targetTree.getTreeExpandList();
                    targetTree.translateReverseEngineeringToTree(targetTree.getReverseEngineering(), false);
                    targetTree.expandTree(expandList);
                }
                if ((isChanged) && (!insertableName.equals(EMPTY_NAME))) {
                    ReverseEngineering reverseEngineeringNewCopy = new ReverseEngineering(targetTree.getReverseEngineering());
                    DbImportTreeUndoableEdit undoableEdit = new DbImportTreeUndoableEdit(
                            reverseEngineeringOldCopy, reverseEngineeringNewCopy, targetTree, getProjectController()
                    );
                    getProjectController().getApplication().getUndoManager().addEdit(undoableEdit);
                }
                if (foundNode != null) {
                    DbImportSorter.sortSubtree((DbImportTreeNode) foundNode.getRoot(),DbImportSorter.NODE_COMPARATOR_BY_TYPE);
                    targetTree.reloadModel();
                    targetTree.setSelectionPath(new TreePath(foundNode.getLastChild().getPath()));
                }
            } finally {
                rootParent.getReverseEngineeringProgress().setVisible(false);
                if (action != null) {
                    action.resetActionFlags();
                }
            }
        }
    }

    private boolean nodeClassesIsSameTypes(DbImportTreeNode sourceElement, DbImportTreeNode selectedElement) {
        if (sourceElement.getUserObject().getClass() == selectedElement.getUserObject().getClass()) {
            return true;
        }
        if (sourceElement.getUserObject().getClass() == IncludeTable.class) {
            if ((selectedElement.getUserObject().getClass() == IncludeTable.class)
                || (selectedElement.getUserObject().getClass() == ExcludeTable.class)) {
                return true;
            }
        }
        if (sourceElement.getUserObject().getClass() == IncludeProcedure.class) {
            if ((selectedElement.getUserObject().getClass() == IncludeProcedure.class)
                    || (selectedElement.getUserObject().getClass() == ExcludeProcedure.class)) {
                return true;
            }
        }
        return false;
    }

    public void setSourceTree(DbImportTree sourceTree) {
        this.sourceTree = sourceTree;
    }

    public void setTargetTree(DbImportTree targetTree) {
        this.targetTree = targetTree;
    }

    public void setPanel(DraggableTreePanel panel) {
        this.panel = panel;
    }
}
