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

package org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport;

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
import org.apache.cayenne.modeler.ui.action.ModelerAbstractAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action.DbImportActions;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action.DragAndDropNodeAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action.TreeManipulationAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.ColorTreeRenderer;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.DbImportTreeNode;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.TransferableNode;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DraggableTreePanel extends JScrollPane {

    private static final int ROOT_LEVEL = 14;
    private static final int FIRST_LEVEL = 11;
    private static final int SECOND_LEVEL = 8;
    private static final int THIRD_LEVEL = 5;
    private static final int FOURTH_LEVEL = 2;
    private static final int FIFTH_LEVEL = 3;
    private static final String MOVE_BUTTON_LABEL = "Include";
    private static final String MOVE_INV_BUTTON_LABEL = "Exclude";

    private final DbImportTree sourceTree;
    private final DbImportTree targetTree;
    private final DbImportActions actions;
    private final Map<DataMap, ReverseEngineering> databaseStructures;
    private final Map<Class<?>, Integer> levels;
    private final Map<Class<?>, List<Class<?>>> insertableLevels;

    private ModelerAbstractAction.CayenneToolbarButton moveButton;
    private ModelerAbstractAction.CayenneToolbarButton moveInvertButton;
    private ImportSourceTree importSourceTree;

    public DraggableTreePanel(DbImportTree sourceTree, DbImportTree targetTree, DbImportActions actions) {
        super(sourceTree);
        this.targetTree = targetTree;
        this.sourceTree = sourceTree;
        this.actions = actions;
        this.databaseStructures = new HashMap<>();
        this.levels = new HashMap<>();
        this.insertableLevels = new HashMap<>();

        initLevels();
        initElement();
        initListeners();
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
        sourceTree.setTransferHandler(new SourceTreeTransferHandler());
        sourceTree.addTreeSelectionListener(new SourceTreeSelectionListener());
        sourceTree.addMouseListener(new ResetFocusMouseAdapter());

        targetTree.addKeyListener(new TargetTreeKeyListener());
        targetTree.setTransferHandler(new TargetTreeTransferHandler());
        targetTree.addTreeSelectionListener(new TargetTreeSelectionListener());
        targetTree.setDragEnabled(true);
        targetTree.setDropMode(DropMode.INSERT);
    }

    private boolean canBeInverted() {
        DbImportTreeNode selectedElement = sourceTree.getSelectedNode();
        if (selectedElement == null) {
            return false;
        }
        return levels.get(selectedElement.getUserObject().getClass()) < SECOND_LEVEL;
    }

    private void initElement() {
        sourceTree.setDragEnabled(true);
        sourceTree.setCellRenderer(new ColorTreeRenderer());
        sourceTree.setDropMode(DropMode.INSERT);

        moveButton = (ModelerAbstractAction.CayenneToolbarButton) actions.getMoveImportNodeAction().buildButton();
        moveButton.setShowingText(true);
        moveButton.setText(MOVE_BUTTON_LABEL);
        moveInvertButton = (ModelerAbstractAction.CayenneToolbarButton) actions.getMoveInvertNodeAction().buildButton();
        moveInvertButton.setShowingText(true);
        moveInvertButton.setText(MOVE_INV_BUTTON_LABEL);
    }

    private void initLevels() {
        levels.put(ReverseEngineering.class, ROOT_LEVEL);
        levels.put(Catalog.class, FIRST_LEVEL);
        levels.put(Schema.class, SECOND_LEVEL);
        levels.put(IncludeTable.class, THIRD_LEVEL);
        levels.put(IncludeColumn.class, FOURTH_LEVEL);
        levels.put(ExcludeColumn.class, FOURTH_LEVEL);
        levels.put(ExcludeTable.class, FIFTH_LEVEL);
        levels.put(IncludeProcedure.class, FIFTH_LEVEL);
        levels.put(ExcludeProcedure.class, FIFTH_LEVEL);

        insertableLevels.put(ReverseEngineering.class, Arrays.asList(
                Catalog.class, Schema.class,
                IncludeTable.class, ExcludeTable.class,
                IncludeColumn.class, ExcludeColumn.class,
                IncludeProcedure.class, ExcludeProcedure.class
        ));

        insertableLevels.put(Catalog.class, Arrays.asList(
                Schema.class,
                IncludeTable.class, ExcludeTable.class,
                IncludeColumn.class, ExcludeColumn.class,
                IncludeProcedure.class, ExcludeProcedure.class
        ));

        insertableLevels.put(Schema.class, Arrays.asList(
                IncludeTable.class, ExcludeTable.class,
                IncludeColumn.class, ExcludeColumn.class,
                IncludeProcedure.class, ExcludeProcedure.class
        ));

        insertableLevels.put(IncludeTable.class, Arrays.asList(
                IncludeColumn.class, ExcludeColumn.class
        ));
    }

    private boolean canBeMoved() {
        DbImportTreeNode selectedElement = sourceTree.getSelectedNode();
        if (selectedElement == null) {
            return false;
        }

        if (selectedElement.isIncludeColumn() || selectedElement.isExcludeColumn()) {
            DbImportTreeNode node = targetTree.findNode(targetTree.getRootNode(), selectedElement.getParent(), 0);
            if (node != null && node.isExcludeTable()) {
                return false;
            }
        }

        Class<?> draggableElementClass = selectedElement.getUserObject().getClass();
        Class<?> reverseEngineeringElementClass;
        if (targetTree.getSelectionPath() != null) {
            selectedElement = targetTree.getSelectedNode();
            DbImportTreeNode parent = selectedElement.getParent();
            if (parent != null) {
                reverseEngineeringElementClass = parent.getUserObject().getClass();
            } else {
                reverseEngineeringElementClass = selectedElement.getUserObject().getClass();
            }
        } else {
            reverseEngineeringElementClass = ReverseEngineering.class;
        }

        List<Class<?>> containsList = insertableLevels.get(reverseEngineeringElementClass);
        return containsList.contains(draggableElementClass);
    }

    public JButton getMoveButton() {
        return moveButton;
    }

    public JButton getMoveInvertButton() {
        return moveInvertButton;
    }

    public TreeManipulationAction getActionByNodeType(Class<?> nodeType) {
        return actions.getAction(nodeType);
    }

    public void bindReverseEngineeringToDatamap(DataMap dataMap, ReverseEngineering reverseEngineering) {
        databaseStructures.put(dataMap, reverseEngineering);
    }

    public DbImportTree getSourceTree() {
        return sourceTree;
    }

    private class SourceTreeTransferHandler extends TransferHandler {

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        public Transferable createTransferable(JComponent c) {
            importSourceTree = ImportSourceTree.SOURCE_TREE;
            JTree tree = (JTree) c;
            TreePath[] paths = tree.getSelectionPaths();
            int pathLength = paths == null ? 0 : paths.length;
            DbImportTreeNode[] nodes = new DbImportTreeNode[pathLength];
            for (int i = 0; i < pathLength; i++) {
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
                public Object getTransferData(DataFlavor flavor) {
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
            boolean canBeMoved = canBeMoved();
            moveButton.setEnabled(canBeMoved);
            moveInvertButton.setEnabled(canBeMoved && canBeInverted());
        }
    }

    private class TargetTreeTransferHandler extends TransferHandler {
        private DbImportTreeNode sourceParentNode;

        @Override
        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            importSourceTree = ImportSourceTree.TARGET_TREE;
            TreePath[] paths = tree.getSelectionPaths();
            DbImportTreeNode lastSelectedNode = (DbImportTreeNode) tree.getLastSelectedPathComponent();
            sourceParentNode = lastSelectedNode.getParent();
            if (paths != null && paths.length > 0) {
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
                    public Object getTransferData(DataFlavor flavor) {
                        return nodes;
                    }
                };
            }
            return null;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            DbImportTreeNode dropLocationParentNode = (DbImportTreeNode) dropLocation.getPath().getLastPathComponent();

            List<Class<?>> allowedItemsList = insertableLevels.get(dropLocationParentNode.getUserObject().getClass());
            DbImportTreeNode[] nodes = getNodesFromSupport(support);
            if (nodes != null && allowedItemsList != null) {
                for (DbImportTreeNode node : nodes) {
                    if (!allowedItemsList.contains(node.getUserObject().getClass())) {
                        return false;
                    }
                }
            }
            return support.isDrop();
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            if (importSourceTree == ImportSourceTree.TARGET_TREE) {
                return importDataFromTargetTree(support);
            }
            if (importSourceTree == ImportSourceTree.SOURCE_TREE) {
                return importDataFromSourceTree(support);
            }
            return false;
        }

        private boolean importDataFromSourceTree(TransferSupport support) {
            if (!canBeMoved()) {
                return false;
            }
            DbImportTreeNode[] nodes = getNodesFromSupport(support);
            if (nodes != null) {
                actions.getMoveImportNodeAction().performAction(null);
                return true;
            }
            return false;
        }

        private boolean importDataFromTargetTree(TransferSupport support) {
            JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
            DbImportTreeNode dropLocationParentNode = (DbImportTreeNode) dropLocation.getPath().getLastPathComponent();

            DbImportTreeNode[] nodes = getNodesFromSupport(support);
            if (nodes != null) {
                DragAndDropNodeAction action = actions.getDragAndDropNodeAction();
                action.setDropLocationParentNode(dropLocationParentNode);
                action.setSourceParentNode(sourceParentNode);
                action.setDropLocation(dropLocation);
                action.setNodes(nodes);
                action.performAction(null);
                return true;
            }
            return false;
        }

        private DbImportTreeNode[] getNodesFromSupport(TransferSupport support) {
            Transferable transferable = support.getTransferable();
            DbImportTreeNode[] nodes = null;
            try {
                for (DataFlavor dataFlavor : transferable.getTransferDataFlavors()) {
                    nodes = (DbImportTreeNode[]) transferable.getTransferData(dataFlavor);
                }
            } catch (IOException | UnsupportedFlavorException e) {
                return null;
            }
            return nodes;
        }
    }

    private class SourceTreeSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            if (sourceTree.getLastSelectedPathComponent() != null) {
                boolean canBeMoved = canBeMoved();
                moveButton.setEnabled(canBeMoved);
                moveInvertButton.setEnabled(canBeMoved && canBeInverted());
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

    private enum ImportSourceTree {
        TARGET_TREE,
        SOURCE_TREE
    }
}
