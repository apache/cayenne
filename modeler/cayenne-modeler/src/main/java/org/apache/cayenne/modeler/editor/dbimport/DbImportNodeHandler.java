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

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;

import javax.swing.tree.TreePath;
import java.awt.Color;

/**
 * @since 4.1
 */
class DbImportNodeHandler {

    private static final Color ACCEPT_COLOR = new Color(60,179,113);
    private static final Color EXCLUDE_COLOR = new Color(178, 0, 0);
    static final Color NON_INCLUDE_COLOR = Color.LIGHT_GRAY;
    static final Color LABEL_COLOR = Color.BLACK;
    private static final int EXCLUDE_TABLE_RATE = -10000;

    private boolean existFirstLevelIncludeTable;
    private boolean existCatalogsOrSchemas;
    private boolean hasEntitiesInEmptyContainer;
    private DbImportTreeNode dbSchemaNode;
    private DbImportTree reverseEngineeringTree;

    private boolean namesIsEqual(DbImportTreeNode reverseEngineeringNode) {
        if (isContainer(reverseEngineeringNode)) {
            return dbSchemaNode.getSimpleNodeName().equals(reverseEngineeringNode.getSimpleNodeName());
        } else {
            return (dbSchemaNode.getSimpleNodeName().toLowerCase().matches(reverseEngineeringNode.getSimpleNodeName().toLowerCase()));
        }
    }

    boolean isContainer(DbImportTreeNode node) {
        return (node.getUserObject().getClass() == Schema.class) || (node.getUserObject().getClass() == Catalog.class);
    }

    private boolean isEmptyContainer(DbImportTreeNode rootNode) {
        return ((getChildIncludeTableCount(rootNode) == 0) && (!existFirstLevelIncludeTable));
    }

    boolean isParentIncluded() {
        return ((dbSchemaNode.getParent() != null) && (((DbImportTreeNode) dbSchemaNode.getParent()).isColorized()));
    }

    // Compare node with current rendered node
    public boolean nodesIsEqual(DbImportTreeNode reverseEngineeringNode) {
        TreePath[] paths = reverseEngineeringTree.getSelectionPaths();
        for (TreePath path : paths != null ? paths : new TreePath[0]) {
            DbImportTreeNode node = (DbImportTreeNode) path.getLastPathComponent();
            if ((nodesClassesComparation(node.getUserObject().getClass(), dbSchemaNode.getUserObject().getClass()))
                    && namesIsEqual(node)
                    && (dbSchemaNode.getLevel() >= node.getLevel())
                    && (dbSchemaNode.parentsIsEqual(node))) {
                return true;
            }
        }
        if ((nodesClassesComparation(reverseEngineeringNode.getUserObject().getClass(), dbSchemaNode.getUserObject().getClass()))
                && namesIsEqual(reverseEngineeringNode)
                && (dbSchemaNode.getLevel() >= reverseEngineeringNode.getLevel())
                && (dbSchemaNode.parentsIsEqual(reverseEngineeringNode))) {
            return true;
        }
        return false;
    }

    public boolean checkTreesLevels(DbImportTree dbTree) {
        if (dbTree.getRootNode().getChildCount() == 0) {
            return false;
        }
        DbImportTreeNode dbNode = (DbImportTreeNode) dbTree.getRootNode().getChildAt(0);
        int childCount = reverseEngineeringTree.getRootNode().getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (((DbImportTreeNode) reverseEngineeringTree.getRootNode().getChildAt(i)).
                    getUserObject().getClass() == Catalog.class) {
                if (dbNode.getUserObject().getClass() == Catalog.class || dbNode.getUserObject().getClass() == IncludeTable.class) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    // Compare reverseEngineeringNode with node.getParent()
    private boolean compareWithParent(DbImportTreeNode reverseEngineeringNode) {
        if ((reverseEngineeringNode == null) || (dbSchemaNode.getParent() == null)) {
            return false;
        }
        if ((((DbImportTreeNode)dbSchemaNode.getParent()).getUserObject().getClass() == reverseEngineeringNode.getUserObject().getClass())
                && (((DbImportTreeNode)dbSchemaNode.getParent()).getSimpleNodeName().equals(reverseEngineeringNode.getSimpleNodeName()))
                && (((DbImportTreeNode)dbSchemaNode.getParent()).getLevel() >= reverseEngineeringNode.getLevel())
                && (((DbImportTreeNode)dbSchemaNode.getParent())).parentsIsEqual(reverseEngineeringNode)) {
            return true;
        }
        return false;
    }

    // Get child IncludeTable's count in node, if exists
    private int getChildIncludeTableCount(DbImportTreeNode parentNode) {
        if (parentNode.isIncludeTable()) {
            return 1;
        }
        int childCount = parentNode.getChildCount();
        int result = 0;
        for (int i = 0; i < childCount; i++) {
            DbImportTreeNode tmpNode = (DbImportTreeNode) parentNode.getChildAt(i);
            if (tmpNode.isIncludeTable()) {
                result++;
            }
        }
        return result;
    }

    // Find Exclude-node in configuration
    private boolean foundExclude(DbImportTreeNode rootNode) {
        int childCount = rootNode.getChildCount();
        for (int i = 0; i < childCount; i++) {
            DbImportTreeNode tmpNode = (DbImportTreeNode) rootNode.getChildAt(i);
            if (tmpNode.getChildCount() > 0) {
                if (tmpNode.isExcludeTable() || tmpNode.isExcludeProcedure()) {
                    return true;
                }
            }
            if (dbSchemaNode.getParent() != null) {
                if (nodesIsEqual(tmpNode)) {
                    if (tmpNode.isExcludeTable() || tmpNode.isExcludeProcedure()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /*
    *  Recursively traverse DbImportTree,
    *  Increment result if rendered node exists in configuration tree,
    *  Subtract EXCLUDE_TABLE_RATE from result, if found Exclude-node for rendered node,
    *  Return 0, if rendered node not found.
    */
    int traverseTree(DbImportTreeNode rootNode) {
        hasEntitiesInEmptyContainer = false;
        int traverseResult = 0;
        int childCount = rootNode.getChildCount();
        boolean hasProcedures = false;

        // Case for empty reverse engineering, which has a include/exclude tables/procedures
        if ((childCount == 0) && (nodesIsEqual(rootNode))) {
            traverseResult++;
        }

        if (nodesIsEqual(rootNode)) {
            traverseResult++;
        }

        ReverseEngineering reverseEngineering = reverseEngineeringTree.getReverseEngineering();
        if ((reverseEngineering.getCatalogs().isEmpty()) && (reverseEngineering.getSchemas().isEmpty())
                && (reverseEngineering.getIncludeTables().isEmpty())
                && (!dbSchemaNode.isIncludeProcedure())) {
            traverseResult++;
        }

        if (nodesIsEqual(rootNode) && isEmptyContainer(rootNode)) {
            hasEntitiesInEmptyContainer = true;
            if (foundExclude(rootNode)) {
                return EXCLUDE_TABLE_RATE;
            }
            return 1;
        }

        if (compareWithParent(rootNode) && (!rootNode.isReverseEngineering()) &&
                isEmptyContainer(rootNode) && (dbSchemaNode.isIncludeTable())) {
            hasEntitiesInEmptyContainer = true;
            if (foundExclude(rootNode)) {
                return EXCLUDE_TABLE_RATE;
            }
            return 1;
        }

        if (hasEntitiesInEmptyContainer) {
            for (int i = 0; i < childCount; i++) {
                DbImportTreeNode tmpNode = (DbImportTreeNode) rootNode.getChildAt(i);
                if (dbSchemaNode.isIncludeProcedure() && (nodesIsEqual(tmpNode))) {
                    int tmpNodeChildCount = tmpNode.getChildCount();
                    if (tmpNodeChildCount > 0) {
                        traverseResult += traverseTree((DbImportTreeNode) rootNode.getChildAt(i));
                    }
                    traverseResult++;
                    hasProcedures = true;
                }
            }
            if ((!rootNode.isExcludeTable()) && (!nodesIsEqual(rootNode))
                    && (!dbSchemaNode.isIncludeProcedure()) &&(!dbSchemaNode.isIncludeColumn())) {
                traverseResult++;
            } else {
                if ((!hasProcedures) && (!dbSchemaNode.isIncludeProcedure())) {
                    traverseResult += EXCLUDE_TABLE_RATE;
                }
            }
        }

        for (int i = 0; i < childCount; i++) {
            DbImportTreeNode tmpNode = (DbImportTreeNode) rootNode.getChildAt(i);
            if (tmpNode.getChildCount() > 0) {
                traverseResult += traverseTree(tmpNode);
                if (tmpNode.isExcludeTable() || tmpNode.isExcludeProcedure()) {
                    traverseResult += EXCLUDE_TABLE_RATE;
                }
            } else if (compareWithParent(tmpNode) && !(existFirstLevelIncludeTable)) {
                if (!dbSchemaNode.isIncludeProcedure()) {
                    traverseResult++;
                }
            }
            if (dbSchemaNode.getParent() != null) {
                if (nodesIsEqual(tmpNode)) {
                    if (tmpNode.isExcludeTable() || tmpNode.isExcludeProcedure()) {
                        traverseResult += EXCLUDE_TABLE_RATE;
                    }
                    traverseResult++;
                }
            }
        }
        return traverseResult;
    }

    Color getColorByNodeType(DbImportTreeNode node) {
        if ((reverseEngineeringTree.getSelectionPaths() != null) &&(reverseEngineeringTree.getSelectionPaths().length > 1)) {
            for (TreePath path : reverseEngineeringTree.getSelectionPaths()) {
                DbImportTreeNode pathNode = (DbImportTreeNode) path.getLastPathComponent();
                if (pathNode.getSimpleNodeName().equals(dbSchemaNode.getSimpleNodeName())) {
                    if (pathNode.isExcludeTable() || pathNode.isExcludeProcedure() || node.isExcludeColumn()) {
                        return EXCLUDE_COLOR;
                    } else {
                        return ACCEPT_COLOR;
                    }
                }
            }
        }
        if (node.isExcludeTable() || node.isExcludeProcedure() || node.isExcludeColumn()) {
            return EXCLUDE_COLOR;
        } else {
            return ACCEPT_COLOR;
        }
    }

    void findFirstLevelIncludeTable() {
        DbImportTreeNode root = reverseEngineeringTree.getRootNode();
        int childCount = root.getChildCount();
        existFirstLevelIncludeTable = false;
        existCatalogsOrSchemas = false;
        for (int i = 0; i < childCount; i++) {
            DbImportTreeNode tmpNode = (DbImportTreeNode) root.getChildAt(i);
            if (tmpNode.isIncludeTable()) {
                existFirstLevelIncludeTable = true;
            }
            if (isContainer(tmpNode)) {
                existCatalogsOrSchemas = true;
            }
        }
    }

    // Check, is DatabaseTree started with IncludeTable or IncludeProcedure
    boolean isFirstNodeIsPrimitive(DbImportTree tree) {
        final int firstChildIndex = 0;
        DbImportTreeNode root = tree.getRootNode();
        if (root.getChildCount() == 0) {
            return false;
        }
        DbImportTreeNode firstElement = (DbImportTreeNode) root.getChildAt(firstChildIndex);
        if (firstElement.isIncludeTable() || firstElement.isIncludeProcedure()) {
            return true;
        }
        return false;
    }

    public boolean nodesClassesComparation(Class firstClass, Class secondClass) {
        if (firstClass.equals(secondClass)) {
            return true;
        }
        if ((firstClass.equals(IncludeTable.class)) && (secondClass.equals(ExcludeTable.class))) {
            return true;
        }
        if ((firstClass.equals(ExcludeTable.class)) && (secondClass.equals(IncludeTable.class))) {
            return true;
        }
        if ((firstClass.equals(IncludeProcedure.class)) && (secondClass.equals(ExcludeProcedure.class))) {
            return true;
        }
        if ((firstClass.equals(ExcludeProcedure.class)) && (secondClass.equals(IncludeProcedure.class))) {
            return true;
        }
        if ((firstClass.equals(ExcludeColumn.class)) && (secondClass.equals(IncludeColumn.class))) {
            return true;
        }
        if ((firstClass.equals(IncludeColumn.class)) && (secondClass.equals(ExcludeColumn.class))) {
            return true;
        }
        return false;
    }

    public boolean isExistCatalogsOrSchemas() {
        return existCatalogsOrSchemas;
    }

    public boolean getHasEntitiesInEmptyContainer() {
        return hasEntitiesInEmptyContainer;
    }

    public void setHasEntitiesInEmptyContainer(boolean newFlag) {
        hasEntitiesInEmptyContainer = newFlag;
    }

    public void setDbSchemaNode(DbImportTreeNode dbSchemaNode) {
        this.dbSchemaNode = dbSchemaNode;
    }

    public void setReverseEngineeringTree(DbImportTree reverseEngineeringTree) {
        this.reverseEngineeringTree = reverseEngineeringTree;
    }
}
