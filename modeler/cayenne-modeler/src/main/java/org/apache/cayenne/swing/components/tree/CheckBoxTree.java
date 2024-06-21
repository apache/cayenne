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
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @since 5.0
 */
public class CheckBoxTree extends JPanel {

    private static final String TOGGLE_ROW_ACTION_KEY = "toggleRow";
    private static final String SELECTION_BACKGROUND_COLOR_KEY = "Tree.selectionBackground";

    protected final JTree labelTree;
    protected final JTree checkBoxTree;

    public CheckBoxTree(TreeModel model) {
        labelTree = new JTree(model);
        checkBoxTree = new JTree(model);

        init();
    }

    public TreeModel getModel() {
        return labelTree.getModel();
    }

    public TreeSelectionModel getSelectionModel() {
        return labelTree.getSelectionModel();
    }

    private void init() {
        initRender();
        initListeners();
    }

    private void initRender() {
        labelTree.setRootVisible(false);
        labelTree.setShowsRootHandles(true);
        labelTree.setUI(new FullWidthPaintTreeUI());
        labelTree.setCellRenderer(new LabelTreeCellRenderer());

        checkBoxTree.setRootVisible(false);
        checkBoxTree.setEditable(true);
        checkBoxTree.setUI(new FullWidthPaintTreeUI() {
            @Override
            protected int getRowX(int row, int depth) {
                return 0;
            }
        });
        checkBoxTree.setCellRenderer(new CheckBoxTreeCellRenderer());
        checkBoxTree.setCellEditor(new CheckBoxTreeCellEditor(checkBoxTree));

        setLayout(new BorderLayout());
        add(labelTree, BorderLayout.CENTER);
        add(checkBoxTree, BorderLayout.EAST);
    }

    private void initListeners() {
        labelTree.getModel().addTreeModelListener(new CheckBoxTreeModelEventHandler());
        checkBoxTree.setModel(labelTree.getModel());

        labelTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        labelTree.addMouseListener(new TreeFullWidthMouseClickHandler(labelTree));
        checkBoxTree.setSelectionModel(labelTree.getSelectionModel());

        labelTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), TOGGLE_ROW_ACTION_KEY);
        checkBoxTree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), TOGGLE_ROW_ACTION_KEY);
        labelTree.getActionMap().put(TOGGLE_ROW_ACTION_KEY, new ToggleRowAction());
        checkBoxTree.getActionMap().put(TOGGLE_ROW_ACTION_KEY, new ToggleRowAction());

        labelTree.addTreeWillExpandListener(new ShareExpandListener(checkBoxTree));
    }

    private static class CheckBoxTreeModelEventHandler implements TreeModelListener {

        private boolean listening;

        private CheckBoxTreeModelEventHandler() {
            this.listening = true;
        }

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            if (listening) {
                handleEvent(e);
            }
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            if (listening) {
                handleEvent(e);
            }
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            if (listening) {
                handleEvent(e);
            }
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            if (listening) {
                handleEvent(e);
            }
        }

        private void handleEvent(TreeModelEvent e) {
            Object parentComponent = e.getTreePath().getLastPathComponent();
            if (!(e.getSource() instanceof TreeModel) || !(parentComponent instanceof DefaultMutableTreeNode)) {
                return;
            }
            TreeModel model = (TreeModel) e.getSource();
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentComponent;

            validateParentCheckBoxState(model, e.getTreePath(), parentNode);
            listening = false;
            for (Object child : e.getChildren()) {
                propagateCheckBoxState(model, e.getTreePath().pathByAddingChild(child), null);
            }
            listening = true;
        }

        private void validateParentCheckBoxState(TreeModel model, TreePath parentPath,
                                                 DefaultMutableTreeNode parentNode) {
            if (!(parentNode.getUserObject() instanceof CheckBoxNodeData)) {
                return;
            }
            CheckBoxNodeData parentData = (CheckBoxNodeData) parentNode.getUserObject();

            int childCount = model.getChildCount(parentNode);
            int checkCount = countCheckedChildren(model, parentNode);

            CheckBoxNodeData parentDataChanged =
                    checkCount == childCount ? parentData.withState(CheckBoxNodeData.State.SELECTED)
                            : checkCount == 0 ? parentData.withState(CheckBoxNodeData.State.DESELECTED)
                            : parentData.withState(CheckBoxNodeData.State.INDETERMINATE);
            model.valueForPathChanged(parentPath, parentDataChanged);
        }

        private static int countCheckedChildren(TreeModel model, DefaultMutableTreeNode parentNode) {
            int childCount = model.getChildCount(parentNode);
            int checkCount = 0;
            for (int i = 0; i < childCount; i++) {
                Object childComponent = model.getChild(parentNode, i);
                if (!(childComponent instanceof DefaultMutableTreeNode)) {
                    continue;
                }
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) childComponent;
                if (!(childNode.getUserObject() instanceof CheckBoxNodeData)) {
                    continue;
                }
                CheckBoxNodeData childData = (CheckBoxNodeData) childNode.getUserObject();
                checkCount += childData.isSelected() ? 1 : 0;
            }
            return checkCount;
        }

        private void propagateCheckBoxState(TreeModel model, TreePath childPath, CheckBoxNodeData.State parentState) {
            if (parentState == CheckBoxNodeData.State.INDETERMINATE) {
                return;
            }
            Object component = childPath.getLastPathComponent();
            if (!(component instanceof DefaultMutableTreeNode)) {
                return;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) component;
            if (!(node.getUserObject() instanceof CheckBoxNodeData)) {
                return;
            }
            CheckBoxNodeData data = (CheckBoxNodeData) node.getUserObject();

            if (parentState != null) {
                data = data.withState(parentState);
                model.valueForPathChanged(childPath, data);
            }

            CheckBoxNodeData finalData = data;
            node.children().asIterator().forEachRemaining(child -> {
                propagateCheckBoxState(model, childPath.pathByAddingChild(child), finalData.getState());
            });
        }
    }

    private static class ShareExpandListener implements TreeWillExpandListener {

        private final JTree otherTree;

        private ShareExpandListener(JTree otherTree) {
            this.otherTree = otherTree;
        }

        @Override
        public void treeWillExpand(TreeExpansionEvent event) {
            otherTree.expandPath(event.getPath());
        }

        @Override
        public void treeWillCollapse(TreeExpansionEvent event) {
            otherTree.collapsePath(event.getPath());
        }
    }

    private static class TreeFullWidthMouseClickHandler extends MouseAdapter {

        private final JTree tree;

        public TreeFullWidthMouseClickHandler(JTree tree) {
            this.tree = tree;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }
            selectFullWidth(e);

            if (e.getClickCount() < 2) {
                return;
            }
            toggleTargetRow(e);
        }

        private void selectFullWidth(MouseEvent e) {
            int closestRow = tree.getClosestRowForLocation(e.getX(), e.getY());
            Rectangle closestRowBounds = tree.getRowBounds(closestRow);
            if (e.getY() >= closestRowBounds.getY()
                    && e.getY() < closestRowBounds.getY() + closestRowBounds.getHeight()
                    && !tree.isRowSelected(closestRow)) {
                tree.setSelectionRow(closestRow);
            }
        }

        private void toggleTargetRow(MouseEvent e) {
            int closestRow = tree.getClosestRowForLocation(e.getX(), e.getY());
            if (tree.isExpanded(closestRow)) {
                tree.collapseRow(closestRow);
            } else {
                tree.expandRow(closestRow);
            }
        }
    }

    private static class FullWidthPaintTreeUI extends BasicTreeUI {

        @Override
        public void paint(Graphics g, JComponent c) {
            if (tree.getSelectionCount() > 0) {
                g.setColor(UIManager.getColor(SELECTION_BACKGROUND_COLOR_KEY));
                int[] rows = tree.getSelectionRows();
                if (rows != null) {
                    for (int i : rows) {
                        Rectangle r = tree.getRowBounds(i);
                        g.fillRect(0, r.y, tree.getWidth(), r.height);
                    }
                }
            }
            super.paint(g, c);
        }
    }

    private static class ToggleRowAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!(e.getSource() instanceof JTree)) {
                return;
            }
            JTree tree = (JTree) e.getSource();
            if (tree.getSelectionPath() == null) {
                return;
            }
            Object component = tree.getSelectionPath().getLastPathComponent();
            if (tree.getSelectionPath() == null || !(component instanceof DefaultMutableTreeNode)) {
                return;
            }
            Object value = ((DefaultMutableTreeNode) component).getUserObject();
            if (!(value instanceof CheckBoxNodeData)) {
                return;
            }
            CheckBoxNodeData data = (CheckBoxNodeData) value;

            tree.getModel().valueForPathChanged(tree.getSelectionPath(), data.toggleState());
        }
    }
}
