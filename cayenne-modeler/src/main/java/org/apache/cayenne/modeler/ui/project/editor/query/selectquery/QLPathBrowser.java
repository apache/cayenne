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
package org.apache.cayenne.modeler.ui.project.editor.query.selectquery;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.cayenne.map.Entity;
import org.apache.cayenne.modeler.toolkit.Renderers;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.toolkit.tree.EntityTreeModel;

/**
 * A tree of the attributes and relationships of the root entity of a query, that inserts the path of a node into the
 * query String on a double click.
 */
class QLPathBrowser extends JPanel {

    private static final String TREE_CARD = "tree";
    private static final String PLACEHOLDER_CARD = "placeholder";

    private final JTree tree;
    private final JLabel hint;
    private final JPanel cards;
    private final CardLayout cardLayout;
    private final Consumer<String> pathConsumer;

    private Entity<?, ?, ?> entity;

    /**
     * @param pathConsumer receives the path of the node chosen by the user
     */
    QLPathBrowser(Consumer<String> pathConsumer) {
        this.pathConsumer = pathConsumer;
        this.tree = new JTree(new DefaultTreeModel(null));
        this.hint = new JLabel("Double-click inserts a path");
        this.cardLayout = new CardLayout();
        this.cards = new JPanel(cardLayout);
        initLayout();
        initBindings();
    }

    private void initLayout() {
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        // a double click inserts a path, the nodes are expanded with their handles
        tree.setToggleClickCount(0);
        tree.setCellRenderer(new PathNodeRenderer());

        hint.setBorder(BorderFactory.createEmptyBorder(3, 2, 0, 0));
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));

        JLabel placeholder = new JLabel("<html>Write \"from &lt;Entity&gt;\" to browse its paths</html>",
                SwingConstants.CENTER);
        placeholder.setForeground(UIManager.getColor("Label.disabledForeground"));

        JScrollPane treeScrollPane = new JScrollPane(tree);
        treeScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        cards.add(treeScrollPane, TREE_CARD);
        cards.add(placeholder, PLACEHOLDER_CARD);

        setLayout(new BorderLayout());
        add(cards, BorderLayout.CENTER);
        add(hint, BorderLayout.SOUTH);

        setEntity(null);
    }

    private void initBindings() {
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        choosePath(path);
                    }
                }
            }
        });

        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "insertPath");
        tree.getActionMap().put("insertPath", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TreePath path = tree.getSelectionPath();
                if (path != null) {
                    choosePath(path);
                }
            }
        });
    }

    /**
     * Shows the paths of the entity, or a placeholder if the entity is null. The tree is rebuilt only if the entity
     * is different from the one shown.
     */
    void setEntity(Entity<?, ?, ?> entity) {
        if (entity != null && entity == this.entity) {
            return;
        }

        this.entity = entity;
        if (entity != null) {
            tree.setModel(new EntityTreeModel(entity));
            cardLayout.show(cards, TREE_CARD);
        } else {
            tree.setModel(new DefaultTreeModel(null));
            cardLayout.show(cards, PLACEHOLDER_CARD);
        }
    }

    /**
     * Rebuilds the tree of the entity shown, to pick up the changes of its attributes and relationships.
     */
    void refresh() {
        if (entity != null) {
            tree.setModel(new EntityTreeModel(entity));
        }
    }

    private void choosePath(TreePath path) {
        String dotPath = EntityTreeModel.toPath(path);
        if (dotPath != null) {
            pathConsumer.accept(dotPath);
        }
    }

    // the nodes of the tree are the attributes and relationships themselves
    private static final class PathNodeRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, Renderers.asString(value), selected, expanded, leaf, row,
                    hasFocus);
            setIcon(IconFactory.iconForObject(value));
            return this;
        }
    }
}
