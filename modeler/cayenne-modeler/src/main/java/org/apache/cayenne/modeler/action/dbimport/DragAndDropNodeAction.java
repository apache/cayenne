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

import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.DbImportModel;
import org.apache.cayenne.modeler.editor.dbimport.DbImportSorter;

import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 5.0
 */
public class DragAndDropNodeAction extends TreeManipulationAction {

    private static final String ACTION_NAME = "DragAndDrop";
    private DbImportTreeNode[] nodes;
    private DbImportTreeNode dropLocationParentNode;
    private DbImportTreeNode sourceParentNode;
    private JTree.DropLocation dropLocation;

    public DragAndDropNodeAction(Application application) {
        super(ACTION_NAME, application);
    }

    @Override
    public void performAction(ActionEvent e) {
        if (dropLocationDuplicateFound()) {
            return;
        }
        DbImportModel model = (DbImportModel) tree.getModel();
        ReverseEngineering reverseEngineeringOldCopy = new ReverseEngineering(tree.getReverseEngineering());
        List<DbImportTreeNode> nodesToExpand = Arrays.stream(nodes)
                .filter(node -> tree.isExpanded(new TreePath(node.getPath())))
                .collect(Collectors.toList());

        for (DbImportTreeNode node : nodes) {
            if (checkDropPossibility(node)) {
                int index = calculateDropIndex();
                model.removeNodeFromParent(node);
                model.insertNodeInto(node, dropLocationParentNode, index);
            }
        }
        getProjectController().setDirty(true);
        DbImportSorter.syncUserObjectItems(dropLocationParentNode);
        DbImportSorter.syncUserObjectItems(sourceParentNode);
        putReverseEngineeringToUndoManager(reverseEngineeringOldCopy);
        tree.reloadModelKeepingExpanded(dropLocationParentNode);
        tree.expandTree(nodesToExpand);
    }

    private boolean dropLocationDuplicateFound() {
        for (DbImportTreeNode node : nodes) {
            if (dropLocationParentNode.isNodeChild(node)) {
                // we are fine about this
                continue;
            }
            int duplicateIndex = dropLocationParentNode.getChildNodes().indexOf(node);
            if (duplicateIndex >= 0) {
                JOptionPane.showMessageDialog(
                        Application.getFrame(),
                        dropLocationParentNode.getSimpleNodeName() + " already contains " + node.getSimpleNodeName(),
                        "Error moving",
                        JOptionPane.ERROR_MESSAGE);
                return true;
            }
        }
        return false;
    }

    private int calculateDropIndex() {
        int index = dropLocation.getChildIndex();
        //node moving inside a one node
        if (sourceParentNode == dropLocationParentNode) {
            int childCount = dropLocationParentNode.getChildCount();
            int childIndex = dropLocation.getChildIndex();
            if (childIndex == childCount) {
                index = childCount - 1;
            }
        }
        //If target node is collapsed
        if (index == -1 && sourceParentNode != dropLocationParentNode) {
            index = dropLocationParentNode.getChildCount();
        }

        //If the target node is an expanded parent node, we place the node in the first position
        if (index == -1 && sourceParentNode == dropLocationParentNode) {
            index = 0;
        }
        return index;
    }

    private boolean checkDropPossibility(DbImportTreeNode node) {
        // Don't allow a node to be dropped onto itself
        if (node == dropLocationParentNode) {
            return false;
        }
        // Don't allow a node to be dropped onto one of its descendants
        for (DbImportTreeNode childNode : node.getChildNodes()) {
            if (isNodeAncestor(childNode, dropLocationParentNode)) {
                return false;
            }
        }
        return true;
    }

    private boolean isNodeAncestor(DbImportTreeNode node1, DbImportTreeNode node2) {
        if (node2 == null) {
            return false;
        }
        if (node2.getParent() == node1) {
            return true;
        }
        return isNodeAncestor(node1, node2.getParent());
    }

    public void setNodes(DbImportTreeNode[] nodes) {
        this.nodes = nodes;
    }

    public void setDropLocationParentNode(DbImportTreeNode dropLocationParentNode) {
        this.dropLocationParentNode = dropLocationParentNode;
    }

    public void setSourceParentNode(DbImportTreeNode sourceParentNode) {
        this.sourceParentNode = sourceParentNode;
    }

    public void setDropLocation(JTree.DropLocation dropLocation) {
        this.dropLocation = dropLocation;
    }
}
