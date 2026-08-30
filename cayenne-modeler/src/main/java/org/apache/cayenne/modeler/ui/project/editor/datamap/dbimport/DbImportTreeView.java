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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeNode;
import java.awt.CardLayout;
import java.awt.Color;

/**
 * A scrollable {@link DbImportTree} that is replaced with a centered message while the tree is empty.
 */
class DbImportTreeView extends JPanel {

    private static final String TREE_CARD = "tree";
    private static final String EMPTY_CARD = "empty";

    private final DbImportTree tree;
    private final JScrollPane scrollPane;
    private final JLabel emptyLabel;
    private final CardLayout cards;

    DbImportTreeView(DbImportTree tree) {
        this.tree = tree;
        this.cards = new CardLayout();
        this.scrollPane = new JScrollPane(tree);
        this.emptyLabel = new JLabel("", SwingConstants.CENTER);

        emptyLabel.setOpaque(true);
        emptyLabel.setBackground(tree.getBackground());
        Color disabled = UIManager.getColor("Label.disabledForeground");
        emptyLabel.setForeground(disabled != null ? disabled : Color.GRAY);

        setLayout(cards);
        add(scrollPane, TREE_CARD);
        add(emptyLabel, EMPTY_CARD);

        tree.getModel().addTreeModelListener(new TreeModelListener() {

            public void treeNodesChanged(TreeModelEvent e) {
                showRelevantCard();
            }

            public void treeNodesInserted(TreeModelEvent e) {
                showRelevantCard();
            }

            public void treeNodesRemoved(TreeModelEvent e) {
                showRelevantCard();
            }

            public void treeStructureChanged(TreeModelEvent e) {
                showRelevantCard();
            }
        });

        showRelevantCard();
    }

    JScrollPane getScrollPane() {
        return scrollPane;
    }

    private void showRelevantCard() {
        if (((TreeNode) tree.getModel().getRoot()).getChildCount() == 0) {
            emptyLabel.setText(tree.getEmptyText());
            cards.show(this, EMPTY_CARD);
        } else {
            cards.show(this, TREE_CARD);
        }
    }
}
