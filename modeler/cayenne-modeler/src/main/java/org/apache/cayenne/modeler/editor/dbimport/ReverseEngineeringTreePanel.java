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
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.db.load.CatalogPopUpMenu;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.dialog.db.load.DefaultPopUpMenu;
import org.apache.cayenne.modeler.dialog.db.load.IncludeTablePopUpMenu;
import org.apache.cayenne.modeler.dialog.db.load.RootPopUpMenu;
import org.apache.cayenne.modeler.dialog.db.load.SchemaPopUpMenu;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneLayout;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.1
 */
class ReverseEngineeringTreePanel extends JScrollPane {

    private DbImportTree reverseEngineeringTree;
    private DbImportTree dbSchemaTree;

    private ProjectController projectController;
    private TreeToolbarPanel treeToolbar;
    private Map<Class, DefaultPopUpMenu> popups;

    ReverseEngineeringTreePanel(ProjectController projectController, DbImportTree reverseEngineeringTree,
                                DbImportTree dbSchemaTree) {
        super(reverseEngineeringTree);
        this.projectController = projectController;
        this.reverseEngineeringTree = reverseEngineeringTree;
        this.dbSchemaTree = dbSchemaTree;
        reverseEngineeringTree.setEditable(true);
        reverseEngineeringTree.setCellRenderer(new DbImportTreeCellRenderer());
        DbImportTreeCellEditor editor = new DbImportTreeCellEditor(reverseEngineeringTree,
                (DefaultTreeCellRenderer) reverseEngineeringTree.getCellRenderer());
        editor.setProjectController(projectController);
        reverseEngineeringTree.setCellEditor(editor);
        initListeners();
        initPopupMenus();
        changeIcons();
    }

    void updateTree() {
        reverseEngineeringTree.translateReverseEngineeringToTree(getReverseEngineeringBySelectedMap(), false);
    }

    private void initPopupMenus() {
        popups = new HashMap<>();
        popups.put(Catalog.class, new CatalogPopUpMenu());
        popups.put(Schema.class, new SchemaPopUpMenu());
        popups.put(ReverseEngineering.class, new RootPopUpMenu());
        popups.put(String.class, new RootPopUpMenu());
        popups.put(IncludeTable.class, new IncludeTablePopUpMenu());
        popups.put(ExcludeTable.class, new DefaultPopUpMenu());
        popups.put(IncludeColumn.class, new DefaultPopUpMenu());
        popups.put(ExcludeColumn.class, new DefaultPopUpMenu());
        popups.put(IncludeProcedure.class, new DefaultPopUpMenu());
        popups.put(ExcludeProcedure.class, new DefaultPopUpMenu());
    }

    private void initListeners() {
        reverseEngineeringTree.addTreeSelectionListener(e -> treeToolbar.lockButtons());
        reverseEngineeringTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (reverseEngineeringTree.getRowForLocation(e.getX(), e.getY()) == -1) {
                    reverseEngineeringTree.setSelectionRow(-1);
                }

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (reverseEngineeringTree.isEditing()) {
                        return;
                    }
                    int row = reverseEngineeringTree.getClosestRowForLocation(e.getX(), e.getY());
                    reverseEngineeringTree.setSelectionRow(row);
                    DefaultPopUpMenu popupMenu;
                    DbImportTreeNode selectedElement;
                    if (reverseEngineeringTree.getSelectionPath() != null) {
                        selectedElement = reverseEngineeringTree.getSelectedNode();
                        popupMenu = popups.get(selectedElement.getUserObject().getClass());
                    } else {
                        selectedElement = reverseEngineeringTree.getRootNode();
                        popupMenu = popups.get(ReverseEngineering.class);
                    }
                    if (popupMenu != null) {
                        popupMenu.setProjectController(projectController);
                        popupMenu.setSelectedElement(selectedElement);
                        popupMenu.setParentElement((DbImportTreeNode) selectedElement.getParent());
                        popupMenu.setTree(reverseEngineeringTree);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                } else if (reverseEngineeringTree.getSelectionPath() != null) {
                    DbImportTreeNode selectedNode = reverseEngineeringTree.getSelectedNode();

                    if( selectedNode.isExcludeColumn() || selectedNode.isIncludeColumn()) {
                        DbImportTreeNode expandNode = (DbImportTreeNode) selectedNode.getParent();

                        DbImportTreeNode parentPath = dbSchemaTree.findNode(
                                dbSchemaTree.getRootNode(), expandNode, 0
                        );

                        if (parentPath != null) {
                            dbSchemaTree.expandPath(new TreePath((parentPath).getPath()));
                            scrollToNode(dbSchemaTree, parentPath);
                        }

                    } else {

                        DbImportTreeNode findNode = dbSchemaTree.findNode(
                                dbSchemaTree.getRootNode(), selectedNode, 0
                        );


                        if (findNode != null) {
                            dbSchemaTree.expandPath(new TreePath(((DbImportTreeNode) findNode.getParent()).getPath()));
                            scrollToNode(dbSchemaTree, findNode);
                        }
                    }
                }
            }
        });
    }

    private void scrollToNode(JTree tree, DbImportTreeNode node) {
        TreePath path = new TreePath(node.getPath());
        tree.scrollPathToVisible(path);
        DraggableTreePanel parentPanel = ((DraggableTreePanel) dbSchemaTree.getParent().getParent());
        parentPanel.getHorizontalScrollBar().setValue(0);
    }

    private ReverseEngineering getReverseEngineeringBySelectedMap() {
        DataMap dataMap = projectController.getCurrentDataMap();
        return projectController.getApplication().getMetaData().get(dataMap, ReverseEngineering.class);
    }

    private void changeIcons() {
        // Deleting standard tree icons
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) reverseEngineeringTree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
    }

    public DbImportTree getReverseEngineeringTree() {
        return reverseEngineeringTree;
    }

    void setTreeToolbar(TreeToolbarPanel treeToolbar) {
        this.treeToolbar = treeToolbar;
    }
}
