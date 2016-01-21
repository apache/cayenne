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
package org.apache.cayenne.modeler.dialog.pref;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.Dimension;

/**
 * @since 4.0
 */
public class TreeView extends JPanel {
    protected JTree tree;
    protected JScrollPane scrollPane;

    public JTree getTree() {
        return tree;
    }

    public void setTree(JTree tree) {
        this.tree = tree;
    }

    public TreeView(DefaultMutableTreeNode root) {
        this.tree = new JTree(root);

        TreeCellRenderer renderer = new FilteredTreeCellRenderer();
        tree.setCellRenderer(renderer);

        scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(210, 300));
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }
}
