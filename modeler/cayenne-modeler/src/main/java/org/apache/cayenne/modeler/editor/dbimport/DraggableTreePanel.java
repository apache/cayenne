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
import org.apache.cayenne.modeler.action.dbimport.AddCatalogAction;
import org.apache.cayenne.modeler.action.dbimport.AddExcludeColumnAction;
import org.apache.cayenne.modeler.action.dbimport.AddExcludeProcedureAction;
import org.apache.cayenne.modeler.action.dbimport.AddExcludeTableAction;
import org.apache.cayenne.modeler.action.dbimport.AddIncludeColumnAction;
import org.apache.cayenne.modeler.action.dbimport.AddIncludeProcedureAction;
import org.apache.cayenne.modeler.action.dbimport.AddIncludeTableAction;
import org.apache.cayenne.modeler.action.dbimport.AddSchemaAction;
import org.apache.cayenne.modeler.action.dbimport.MoveImportNodeAction;
import org.apache.cayenne.modeler.action.dbimport.MoveInvertNodeAction;
import org.apache.cayenne.modeler.action.dbimport.TreeManipulationAction;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.dialog.db.load.TransferableNode;
import org.apache.cayenne.modeler.util.CayenneAction;

import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 4.1
 */
public class DraggableTreePanel extends JScrollPane {

    private static final int ROOT_LEVEL = 14;
    private static final int FIRST_LEVEL = 11;
    private static final int SECOND_LEVEL = 8;
    private static final int THIRD_LEVEL = 5;
    private static final int FOURTH_LEVEL = 2;
    private static final int FIFTH_LEVEL = 3;
    private static final String MOVE_BUTTON_LABEL = "Include";
    private static final String MOVE_INV_BUTTON_LABEL = "Exclude";

    private DbImportTree sourceTree;
    private DbImportTree targetTree;
    private CayenneAction.CayenneToolbarButton moveButton;
    private CayenneAction.CayenneToolbarButton moveInvertButton;
    private Map<DataMap, ReverseEngineering> databaseStructures;

    private ProjectController projectController;
    private Map<Class, Integer> levels;
    private Map<Class, List<Class>> insertableLevels;
    private Map<Class, Class> actions;

    public DraggableTreePanel(ProjectController projectController, DbImportTree sourceTree, DbImportTree targetTree) {
        super(sourceTree);
        this.targetTree = targetTree;
        this.sourceTree = sourceTree;
        this.projectController = projectController;
        this.databaseStructures = new HashMap<>();
        initLevels();
        initElement();
        initActions();
        initListeners();
    }

    private void initActions() {
        actions = new HashMap<>();
        actions.put(Catalog.class, AddCatalogAction.class);
        actions.put(Schema.class, AddSchemaAction.class);
        actions.put(IncludeTable.class, AddIncludeTableAction.class);
        actions.put(ExcludeTable.class, AddExcludeTableAction.class);
        actions.put(IncludeColumn.class, AddIncludeColumnAction.class);
        actions.put(ExcludeColumn.class, AddExcludeColumnAction.class);
        actions.put(IncludeProcedure.class, AddIncludeProcedureAction.class);
        actions.put(ExcludeProcedure.class, AddExcludeProcedureAction.class);
    }

    public void updateTree(DataMap dataMap) {
        DbImportModel model = (DbImportModel) sourceTree.getModel();
        model.reload();
        if (databaseStructures.get(dataMap) != null) {
            sourceTree.setReverseEngineering(databaseStructures.get(dataMap));
            sourceTree.translateReverseEngineeringToTree(databaseStructures.get(dataMap), true);
            sourceTree.setEnabled(true);
        } else {
            sourceTree.setEnabled(false);
        }
    }

    private void initListeners() {
        sourceTree.addKeyListener(new SourceTreeKeyListener());
        targetTree.addKeyListener(new TargetTreeKeyListener());
        targetTree.addTreeSelectionListener(new TargetTreeSelectionListener());
        targetTree.setTransferHandler(new TargetTreeTransferHandler());
        sourceTree.setTransferHandler(new SourceTreeTransferHandler());
        sourceTree.addTreeSelectionListener(new SourceTreeSelectionListener());
        sourceTree.addMouseListener(new ResetFocusMouseAdapter());
        targetTree.setDragEnabled(true);
    }

    private boolean canBeInverted() {
        if (sourceTree.getSelectionPath() != null) {
            DbImportTreeNode selectedElement = sourceTree.getSelectedNode();
            if (selectedElement == null) {
                return false;
            }
            if (levels.get(selectedElement.getUserObject().getClass()) < SECOND_LEVEL) {
                return true;
            }
        }
        return false;
    }

    private void initElement() {
        sourceTree.setDragEnabled(true);
        sourceTree.setCellRenderer(new ColorTreeRenderer());
        sourceTree.setDropMode(DropMode.INSERT);

        MoveImportNodeAction action = projectController.getApplication().
                getActionManager().getAction(MoveImportNodeAction.class);
        action.setPanel(this);
        action.setSourceTree(sourceTree);
        action.setTargetTree(targetTree);
        moveButton = (CayenneAction.CayenneToolbarButton) action.buildButton();
        moveButton.setShowingText(true);
        moveButton.setText(MOVE_BUTTON_LABEL);
        MoveInvertNodeAction actionInv = projectController.getApplication().
                getActionManager().getAction(MoveInvertNodeAction.class);
        actionInv.setPanel(this);
        actionInv.setSourceTree(sourceTree);
        actionInv.setTargetTree(targetTree);
        moveInvertButton = (CayenneAction.CayenneToolbarButton) actionInv.buildButton();
        moveInvertButton.setShowingText(true);
        moveInvertButton.setText(MOVE_INV_BUTTON_LABEL);


        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) sourceTree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
    }

    private void initLevels() {
        levels = new HashMap<>();
        levels.put(ReverseEngineering.class, ROOT_LEVEL);
        levels.put(Catalog.class, FIRST_LEVEL);
        levels.put(Schema.class, SECOND_LEVEL);
        levels.put(IncludeTable.class, THIRD_LEVEL);
        levels.put(IncludeColumn.class, FOURTH_LEVEL);
        levels.put(ExcludeColumn.class, FOURTH_LEVEL);
        levels.put(ExcludeTable.class, FIFTH_LEVEL);
        levels.put(IncludeProcedure.class, FIFTH_LEVEL);
        levels.put(ExcludeProcedure.class, FIFTH_LEVEL);

        insertableLevels = new HashMap<>();
        List<Class> rootLevelClasses = new ArrayList<>();
        rootLevelClasses.add(Catalog.class);
        rootLevelClasses.add(Schema.class);
        rootLevelClasses.add(IncludeTable.class);
        rootLevelClasses.add(ExcludeTable.class);
        rootLevelClasses.add(IncludeColumn.class);
        rootLevelClasses.add(ExcludeColumn.class);
        rootLevelClasses.add(IncludeProcedure.class);
        rootLevelClasses.add(ExcludeProcedure.class);

        List<Class> catalogLevelClasses = new ArrayList<>();
        catalogLevelClasses.add(Schema.class);
        catalogLevelClasses.add(IncludeTable.class);
        catalogLevelClasses.add(ExcludeTable.class);
        catalogLevelClasses.add(IncludeColumn.class);
        catalogLevelClasses.add(ExcludeColumn.class);
        catalogLevelClasses.add(IncludeProcedure.class);
        catalogLevelClasses.add(ExcludeProcedure.class);

        List<Class> schemaLevelClasses = new ArrayList<>();
        schemaLevelClasses.add(IncludeTable.class);
        schemaLevelClasses.add(ExcludeTable.class);
        schemaLevelClasses.add(IncludeColumn.class);
        schemaLevelClasses.add(ExcludeColumn.class);
        schemaLevelClasses.add(IncludeProcedure.class);
        schemaLevelClasses.add(ExcludeProcedure.class);

        List<Class> includeTableLevelClasses = new ArrayList<>();
        includeTableLevelClasses.add(IncludeColumn.class);
        includeTableLevelClasses.add(ExcludeColumn.class);

        insertableLevels.put(ReverseEngineering.class, rootLevelClasses);
        insertableLevels.put(Catalog.class, catalogLevelClasses);
        insertableLevels.put(Schema.class, schemaLevelClasses);
        insertableLevels.put(IncludeTable.class, includeTableLevelClasses);
    }

    private boolean canBeMoved() {
        if (sourceTree.getSelectionPath() != null) {
            DbImportTreeNode selectedElement = sourceTree.getSelectedNode();
            if (selectedElement == null) {
                return false;
            }
            if (selectedElement.isIncludeColumn() || selectedElement.isExcludeColumn()) {
                DbImportTreeNode node = targetTree.findNode(targetTree.getRootNode(), (DbImportTreeNode) selectedElement.getParent(), 0);
                if(node != null && node.isExcludeTable()) {
                    return false;
                }
            }
            Class draggableElementClass = selectedElement.getUserObject().getClass();
            Class reverseEngineeringElementClass;
            if (targetTree.getSelectionPath() != null) {
                selectedElement = targetTree.getSelectedNode();
                DbImportTreeNode parent = (DbImportTreeNode) selectedElement.getParent();
                if (parent != null) {
                    reverseEngineeringElementClass = parent.getUserObject().getClass();
                } else {
                    reverseEngineeringElementClass = selectedElement.getUserObject().getClass();
                }
            } else {
                reverseEngineeringElementClass = ReverseEngineering.class;
            }
            List<Class> containsList = insertableLevels.get(reverseEngineeringElementClass);
            return containsList.contains(draggableElementClass);
        }
        return false;
    }

    public JButton getMoveButton() {
        return moveButton;
    }

    public JButton getMoveInvertButton() {
        return moveInvertButton;
    }

    public TreeManipulationAction getActionByNodeType(Class nodeType) {
        Class actionClass = actions.get(nodeType);
        if (actionClass != null) {
            TreeManipulationAction action = (TreeManipulationAction) projectController.getApplication().
                    getActionManager().getAction(actionClass);
            return action;
        }
        return null;
    }

    public void bindReverseEngineeringToDatamap(DataMap dataMap, ReverseEngineering reverseEngineering) {
        databaseStructures.put(dataMap, reverseEngineering);
    }

    public DbImportTree getSourceTree() {
        return sourceTree;
    }

    private static class SourceTreeTransferHandler extends TransferHandler {

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        public Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            TreePath[] paths = tree.getSelectionPaths();
            DbImportTreeNode[] nodes = new DbImportTreeNode[paths.length];
            for (int i = 0; i < paths.length; i++) {
                nodes[i] = (DbImportTreeNode) paths[i].getLastPathComponent();
            }
            return new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return TransferableNode.flavors;
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return true;
                }

                @Override
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                    return nodes;
                }
            };
        }
    }

    private class SourceTreeKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                sourceTree.setSelectionRow(-1);
                moveButton.setEnabled(false);
                moveInvertButton.setEnabled(false);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {}
    }

    private class TargetTreeKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                targetTree.setSelectionRow(-1);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {}
    }

    private class TargetTreeSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            DbImportModel model = (DbImportModel) sourceTree.getModel();
            DbImportTreeNode root = (DbImportTreeNode) model.getRoot();
            sourceTree.repaint();
            if (root.getChildCount() > 0) {
                model.nodesChanged(root, new int[]{root.getChildCount() - 1});
            }
            if (canBeMoved()) {
                moveButton.setEnabled(true);
                if (canBeInverted()) {
                    moveInvertButton.setEnabled(true);
                } else {
                    moveInvertButton.setEnabled(false);
                }
            } else {
                moveButton.setEnabled(false);
                moveInvertButton.setEnabled(false);
            }
        }
    }

    private class TargetTreeTransferHandler extends TransferHandler {

        @Override
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }
            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            if (!canBeMoved()) {
                return false;
            }
            Transferable transferable = support.getTransferable();
            DbImportTreeNode[] transferData = null;
            try {
                for (DataFlavor dataFlavor : transferable.getTransferDataFlavors()) {
                    transferData = (DbImportTreeNode[]) transferable.getTransferData(dataFlavor);
                }
            } catch (IOException | UnsupportedFlavorException e) {
                return false;
            }
            if (transferData != null) {
                MoveImportNodeAction action = projectController.getApplication().
                        getActionManager().getAction(MoveImportNodeAction.class);
                action.setSourceTree(sourceTree);
                action.setTargetTree(targetTree);
                action.setPanel(DraggableTreePanel.this);
                action.performAction(null);
                return true;
            }
            return false;
        }
    }

    private class SourceTreeSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (sourceTree.getLastSelectedPathComponent() != null) {
                if (canBeMoved()) {
                    moveButton.setEnabled(true);
                    if (canBeInverted()) {
                        moveInvertButton.setEnabled(true);
                    } else {
                        moveInvertButton.setEnabled(false);
                    }
                } else {
                    moveInvertButton.setEnabled(false);
                    moveButton.setEnabled(false);
                }
            }
        }
    }

    private class ResetFocusMouseAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (sourceTree.getRowForLocation(e.getX(),e.getY()) == -1) {
                sourceTree.setSelectionRow(-1);
                moveInvertButton.setEnabled(false);
                moveButton.setEnabled(false);
            }
        }
    }
}
