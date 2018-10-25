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

package org.apache.cayenne.modeler.editor.dbimport;

import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;

import javax.swing.JTree;
import java.awt.Color;
import java.awt.Component;

import static org.apache.cayenne.modeler.editor.dbimport.DbImportNodeHandler.LABEL_COLOR;
import static org.apache.cayenne.modeler.editor.dbimport.DbImportNodeHandler.NON_INCLUDE_COLOR;

/**
 * @since 4.1
 */
public class ColorTreeRenderer extends DbImportTreeCellRenderer {

    private DbImportNodeHandler handler;
    private DbImportTree reverseEngineeringTree;


    public ColorTreeRenderer() {
        super();
        handler = new DbImportNodeHandler();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel,
                expanded, leaf, row, hasFocus);
        DbImportTree renderedTree = (DbImportTree) tree;
        handler.setDbSchemaNode(node);
        if (node.isLabel()) {
            setForeground(LABEL_COLOR);
            return this;
        }
        if (handler.isContainer(node) || (handler.isFirstNodeIsPrimitive(renderedTree))) {
            handler.setHasEntitiesInEmptyContainer(false);
        }
        if (selected) {
            setForeground(Color.BLACK);
            node.setColorized(node.isColorized());
            return this;
        }
        DbImportTreeNode root;
        handler.findFirstLevelIncludeTable();
        if (!handler.checkTreesLevels(renderedTree)) {
            setForeground(NON_INCLUDE_COLOR);
            node.setColorized(false);
            return this;
        }
        if (reverseEngineeringTree.getSelectionPath() != null) {
            root = reverseEngineeringTree.getSelectedNode();
        } else {
            root = reverseEngineeringTree.getRootNode();
        }
        renderedTree.getRootNode().setColorized(true);

        int traverseResult = handler.traverseTree(root);
        if (traverseResult > 0) {

            if (root.getUserObject().getClass() == IncludeColumn.class) {
                if (handler.nodesIsEqual(root)) {
                    setForeground(handler.getColorByNodeType(root));
                    node.setColorized(true);
                    return this;
                } else {
                    setForeground(NON_INCLUDE_COLOR);
                    node.setColorized(false);
                    return this;
                }
            }
            // Case on IncludeProcedure on zero level is selected
            if (root.getUserObject().getClass() == IncludeProcedure.class) {
                if (handler.nodesIsEqual(root)) {
                    setForeground(handler.getColorByNodeType(root));
                    node.setColorized(true);
                    return this;
                } else {
                    setForeground(NON_INCLUDE_COLOR);
                    node.setColorized(false);
                    return this;
                }
            }
            // If ReverseEngineering doesn't have catalogs or schemas on zero level
            if (!handler.isExistCatalogsOrSchemas()) {
                if ((root.isExcludeTable()) || (root.isExcludeProcedure() || root.isExcludeColumn())) {
                    if (handler.nodesIsEqual(root)) {
                        setForeground(handler.getColorByNodeType(root));
                        node.setColorized(true);
                        return this;
                    }
                    setForeground(NON_INCLUDE_COLOR);
                    node.setColorized(false);
                    return this;
                }
                if (root.equals(node)) {
                    setForeground(handler.getColorByNodeType(root));
                    node.setColorized(true);
                    return this;
                }
            }
            // Recursion painting, if parent is colorized
            if (handler.isParentIncluded()) {
                setForeground(handler.getColorByNodeType(root));
                node.setColorized(true);
                return this;
            }
        } else {
            setForeground(NON_INCLUDE_COLOR);
            node.setColorized(false);
            return this;
        }
        if ((handler.isParentIncluded()) || (reverseEngineeringTree.getSelectionPath() != null)) {
            setForeground(handler.getColorByNodeType(root));
            node.setColorized(true);
            return this;
        } else {
            if (!handler.isExistCatalogsOrSchemas()) {
                setForeground(handler.getColorByNodeType(root));
                node.setColorized(true);
                return this;
            }
            setForeground(NON_INCLUDE_COLOR);
            node.setColorized(false);
            return this;
        }

    }

    public void setReverseEngineeringTree(DbImportTree reverseEngineeringTree) {
        this.reverseEngineeringTree = reverseEngineeringTree;
        handler.setReverseEngineeringTree(reverseEngineeringTree);
    }
}
