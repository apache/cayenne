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

package org.apache.cayenne.modeler.ui.project.tree;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.modeler.toolkit.Renderers;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Panel displaying a Cayenne project as a tree. Passive view driven by
 * {@link ProjectTreeController}.
 */
public class ProjectTreeView extends JTree {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectTreeView.class);

    private static final Color SELECTION_COLOR = UIManager.getColor("Tree.selectionBackground");

    ProjectTreeView() {
        setCellRenderer(Renderers.treeRenderer());
        setOpaque(false);
        setBorder(TopBorder.create());
        setRootVisible(true);
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    }

    /**
     * Returns tree model cast to ProjectTreeModel.
     */
    public ProjectTreeModel getProjectModel() {
        return (ProjectTreeModel) getModel();
    }

    /**
     * Returns a "name" property of the tree node.
     */
    @Override
    public String convertValueToText(
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        // unwrap
        while (value instanceof DefaultMutableTreeNode) {
            value = ((DefaultMutableTreeNode) value).getUserObject();
        }

        // String - just return it
        if (value instanceof String) {
            return value.toString();
        }

        // Project - return the name of top file
        if (value instanceof Project) {
            Resource resource = ((Project) value).getConfigurationResource();
            return (resource != null) ? resource.getURL().getPath() : "";
        }

        // read name property
        try {
            if (value instanceof Embeddable) {
                return String.valueOf(PropertyUtils.getProperty(value, "className"));
            }

            return (value != null) ? String.valueOf(PropertyUtils.getProperty(value, "name")) : "";
        } catch (Exception e) {
            LOGGER.warn("Exception reading property 'name', class " + value.getClass().getName(), e);
            return "";
        }
    }

    /**
     * Makes node current, visible and selected.
     */
    void navigateTo(DefaultMutableTreeNode node) {
        TreePath path = new TreePath(node.getPath());

        if (!isVisible(path)) {
            makeVisible(path);

            Rectangle bounds = getPathBounds(path);
            if (bounds != null) {
                bounds.height = getVisibleRect().height;
                scrollRectToVisible(bounds);
            }
        }

        setSelectionPath(path);
    }

    /**
     * Makes node current, visible but not selected.
     */
    void setSelected(DefaultMutableTreeNode node) {
        TreePath path = new TreePath(node.getPath());
        if (!isVisible(path)) {
            makeVisible(path);
        }
    }

    void updateNode(DefaultMutableTreeNode node) {
        if (node != null) {
            getProjectModel().nodeChanged(node);
        }
    }

    /**
     * Removes current node from the tree. Selects a new node adjacent to the currently
     * selected node instead.
     */
    void removeNode(DefaultMutableTreeNode toBeRemoved) {

        // lookup for the new selected node
        Object selectedNode = null;

        TreePath selectionPath = getSelectionPath();
        if (selectionPath != null) {
            selectedNode = selectionPath.getLastPathComponent();
        }

        if (toBeRemoved == selectedNode) {

            // first search siblings
            DefaultMutableTreeNode newSelection = toBeRemoved.getNextSibling();
            if (newSelection == null) {
                newSelection = toBeRemoved.getPreviousSibling();

                // try parent
                if (newSelection == null) {
                    newSelection = (DefaultMutableTreeNode) toBeRemoved.getParent();

                    // search the whole tree
                    if (newSelection == null) {

                        newSelection = toBeRemoved.getNextNode();
                        if (newSelection == null) {

                            newSelection = toBeRemoved.getPreviousNode();
                        }
                    }
                }
            }

            navigateTo(newSelection);
        }

        getProjectModel().removeNodeFromParent(toBeRemoved);
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(Color.white);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (getSelectionCount() > 0) {
            g.setColor(SELECTION_COLOR);
            int[] rows = getSelectionRows();
            if (rows != null) {
                for (int i : rows) {
                    Rectangle r = getRowBounds(i);
                    g.fillRect(0, r.y, getWidth(), r.height);
                }
            }
        }
        super.paintComponent(g);
    }
}
