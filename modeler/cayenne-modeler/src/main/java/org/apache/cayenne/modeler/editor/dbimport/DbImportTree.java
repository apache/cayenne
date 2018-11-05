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
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.PatternParam;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.LoadDbSchemaAction;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.dialog.db.load.TransferableNode;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


/**
 * @since 4.1
 */
public class DbImportTree extends JTree {

    private boolean isTransferable;
    private ReverseEngineering reverseEngineering;

    public DbImportTree(TreeNode node) {
        super(node);
        createTreeExpandListener();
    }

    public void translateReverseEngineeringToTree(ReverseEngineering reverseEngineering, boolean isTransferable) {
        this.isTransferable = isTransferable;
        this.reverseEngineering = reverseEngineering;
        DbImportModel model = (DbImportModel) this.getModel();
        DbImportTreeNode root = (DbImportTreeNode) model.getRoot();
        root.removeAllChildren();
        root.setUserObject(reverseEngineering);
        printCatalogs(reverseEngineering.getCatalogs(), root);
        printSchemas(reverseEngineering.getSchemas(), root);
        printIncludeTables(reverseEngineering.getIncludeTables(), root);
        printParams(reverseEngineering.getExcludeTables(), root);
        printParams(reverseEngineering.getIncludeColumns(), root);
        printParams(reverseEngineering.getExcludeColumns(), root);
        printParams(reverseEngineering.getIncludeProcedures(), root);
        printParams(reverseEngineering.getExcludeProcedures(), root);
        model.reload();
    }

    public void updateTableColumns(ReverseEngineering reverseEngineering) {
        DbImportModel model = (DbImportModel) this.getModel();

        DbImportTreeNode root = (DbImportTreeNode) model.getRoot();
        reverseEngineering.getCatalogs().forEach(newCatalog -> {
            DbImportTreeNode catalog = findNodeInParent(root, newCatalog);

            if (catalog == null) {
                return;
            }

            newCatalog.getIncludeTables().forEach(newTable -> {
                DbImportTreeNode table = findNodeInParent(catalog, newTable);
                if (table == null) {
                    return;
                }
                if (table.getChildCount() != 0) {
                    table.removeAllChildren();
                }
                newTable.getIncludeColumns().forEach(column ->
                        table.add(new DbImportTreeNode(column)));
                table.setLoaded(true);
                model.reload(table);
            });
        });
    }

    private DbImportTreeNode findNodeInParent(DbImportTreeNode parent, Object object) {
        for (int i = 0; i <= parent.getChildCount(); i++) {
            DbImportTreeNode node = (DbImportTreeNode) parent.getChildAt(i);
            Object userObject = node.getUserObject();

            if (object instanceof Catalog) {
                Catalog catalog = (Catalog) object;
                if (!(userObject instanceof Catalog)) {
                    continue;
                }

                Catalog currentCatalog = (Catalog) userObject;
                if (currentCatalog.getName().equals(catalog.getName())) {
                    return node;
                }
            }

            if (object instanceof IncludeTable) {
                IncludeTable table = (IncludeTable) object;
                if (!(userObject instanceof IncludeTable)) {
                    continue;
                }

                IncludeTable currentTable = (IncludeTable) userObject;
                if (currentTable.getPattern().equals(table.getPattern())) {
                    return node;
                }
            }

            if (object instanceof ExcludeTable) {
                ExcludeTable table = (ExcludeTable) object;
                if (!(userObject instanceof ExcludeTable)) {
                    continue;
                }

                ExcludeTable currentTable = (ExcludeTable) userObject;
                if (currentTable.getPattern().equals(table.getPattern())) {
                    return node;
                }
            }
        }

        return null;
    }

    public DbImportTreeNode findNodeByParentsChain(DbImportTreeNode rootNode, DbImportTreeNode movedNode, int depth) {
        String parentName = ((DbImportTreeNode) movedNode.getParent()).getSimpleNodeName();
        if ((rootNode.parentsIsEqual(((DbImportTreeNode) movedNode.getParent())))
                && (rootNode.getSimpleNodeName().equals(parentName))
                && ((rootNode.isCatalog()) || (rootNode.isSchema()) || (rootNode.isIncludeTable()))) {
            return rootNode;
        }
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DbImportTreeNode childNode = (DbImportTreeNode) rootNode.getChildAt(i);
            DbImportTreeNode node = findNodeByParentsChain(childNode, movedNode, depth++);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    public DbImportTreeNode findNode(DbImportTreeNode rootNode, DbImportTreeNode movedNode, int depth) {
        String parentName = movedNode.getSimpleNodeName();
        if ((rootNode.parentsIsEqual(movedNode))
                && (rootNode.getSimpleNodeName().equals(parentName))) {
            return rootNode;
        }
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DbImportTreeNode childNode = (DbImportTreeNode) rootNode.getChildAt(i);
            DbImportTreeNode node = findNode(childNode, movedNode, depth++);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    // Create list of expanded elements
    private ArrayList<DbImportTreeNode> createTreeExpandList(DbImportTreeNode rootNode, ArrayList<DbImportTreeNode> resultList) {
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DbImportTreeNode childNode = (DbImportTreeNode) rootNode.getChildAt(i);
            TreePath childPath = new TreePath(childNode.getPath());
            if (isExpanded(childPath)) {
                resultList.add(childNode);
            }
            if (childNode.getChildCount() > 0) {
                createTreeExpandList(childNode, resultList);
            }
        }
        return resultList;
    }

    public ArrayList<DbImportTreeNode> getTreeExpandList() {
        ArrayList<DbImportTreeNode> resultList = new ArrayList<>();
        return createTreeExpandList(getRootNode(), resultList);
    }

    private void expandBeginningWithNode(DbImportTreeNode rootNode, ArrayList<DbImportTreeNode> list) {
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DbImportTreeNode childNode = (DbImportTreeNode) rootNode.getChildAt(i);
            list.forEach((element) -> {
                if (element.equals(childNode)) {
                    this.expandPath(new TreePath(childNode.getPath()));
                }
            });
            if (childNode.getChildCount() > 0) {
                expandBeginningWithNode(childNode, list);
            }
        }
    }

    public void expandTree(ArrayList<DbImportTreeNode> expandIndexesList) {
        expandBeginningWithNode(getRootNode(), expandIndexesList);
    }

    private <T extends PatternParam> void printParams(Collection<T> collection, DbImportTreeNode parent) {
        for (T element : collection) {
            DbImportTreeNode node = !isTransferable ? new DbImportTreeNode(element) : new TransferableNode(element);
            if (!node.getSimpleNodeName().equals("")) {
                parent.add(node);
            }
        }
    }

    private void printIncludeTables(Collection<IncludeTable> collection, DbImportTreeNode parent) {
        for (IncludeTable includeTable : collection) {
            DbImportTreeNode node = !isTransferable ? new DbImportTreeNode(includeTable) : new TransferableNode(includeTable);
            if (!node.getSimpleNodeName().equals("")) {

                if (isTransferable && includeTable.getIncludeColumns().isEmpty() && includeTable.getExcludeColumns().isEmpty()) {
                    printParams(Collections.singletonList(new IncludeColumn("Loading...")), node);
                }

                printParams(includeTable.getIncludeColumns(), node);
                printParams(includeTable.getExcludeColumns(), node);
                parent.add(node);
            }
        }
    }

    private void printChildren(FilterContainer container, DbImportTreeNode parent) {
        printIncludeTables(container.getIncludeTables(), parent);
        printParams(container.getExcludeTables(), parent);
        printParams(container.getIncludeColumns(), parent);
        printParams(container.getExcludeColumns(), parent);
        printParams(container.getIncludeProcedures(), parent);
        printParams(container.getExcludeProcedures(), parent);
    }

    private void printSchemas(Collection<Schema> schemas, DbImportTreeNode parent) {
        for (Schema schema : schemas) {
            DbImportTreeNode node = !isTransferable ? new DbImportTreeNode(schema) : new TransferableNode(schema);
            if (!node.getSimpleNodeName().equals("")) {
                printChildren(schema, node);
                parent.add(node);
            }
        }
    }

    private void printCatalogs(Collection<Catalog> catalogs, DbImportTreeNode parent) {
        for (Catalog catalog : catalogs) {
            DbImportTreeNode node = !isTransferable ? new DbImportTreeNode(catalog) : new TransferableNode(catalog);
            if (!node.getSimpleNodeName().equals("")) {
                printSchemas(catalog.getSchemas(), node);
                printChildren(catalog, node);
                parent.add(node);
            }
        }
    }

    private void createTreeExpandListener() {
        TreeExpansionListener treeExpansionListener = new TreeExpansionListener() {

            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                TreePath path = event.getPath();
                Object lastPathComponent = path.getLastPathComponent();
                if (!(lastPathComponent instanceof TransferableNode)) {
                    return;
                }

                DbImportTreeNode node = (DbImportTreeNode) lastPathComponent;
                if (node.isIncludeTable() && !node.isLoaded()) {
                    //reload columns action.

                    LoadDbSchemaAction action = Application.getInstance().getActionManager().getAction(LoadDbSchemaAction.class);
                    action.performAction(null, path);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {

            }
        };
        this.addTreeExpansionListener(treeExpansionListener);
    }


    public DbImportTreeNode getSelectedNode() {
        return (DbImportTreeNode) this.getSelectionPath().getLastPathComponent();
    }

    public DbImportTreeNode getRootNode() {
        return (DbImportTreeNode) this.getModel().getRoot();
    }

    public ReverseEngineering getReverseEngineering() {
        return reverseEngineering;
    }

    public void setReverseEngineering(ReverseEngineering reverseEngineering) {
        this.reverseEngineering = reverseEngineering;
    }

    public boolean isTransferable() {
        return isTransferable;
    }
}
