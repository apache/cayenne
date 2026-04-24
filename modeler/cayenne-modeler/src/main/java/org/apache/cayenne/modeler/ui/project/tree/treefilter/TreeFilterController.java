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
package org.apache.cayenne.modeler.ui.project.tree.treefilter;

import org.apache.cayenne.modeler.ui.project.tree.ProjectTreeModel;
import org.apache.cayenne.modeler.ui.project.tree.ProjectTreeView;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

public class TreeFilterController {

    private boolean showDbEntity;
    private boolean showObjEntity;
    private boolean showEmbeddable;
    private boolean showProcedure;
    private boolean showQuery;

    private final ProjectTreeView treeView;
    private final ProjectTreeModel treeModel;
    private final TreeFilterPopup view;

    public TreeFilterController(ProjectTreeView treeView) {

        this.view = new TreeFilterPopup();
        this.treeView = treeView;
        this.treeModel = treeView.getProjectModel();

        selectAll();

        view.getDbEntity().addActionListener(e -> {
            showDbEntity = view.getDbEntity().isSelected();
            applyFilter();
        });
        view.getObjEntity().addActionListener(e -> {
            showObjEntity = view.getObjEntity().isSelected();
            applyFilter();
        });
        view.getEmbeddable().addActionListener(e -> {
            showEmbeddable = view.getEmbeddable().isSelected();
            applyFilter();
        });
        view.getProcedure().addActionListener(e -> {
            showProcedure = view.getProcedure().isSelected();
            applyFilter();
        });
        view.getQuery().addActionListener(e -> {
            showQuery = view.getQuery().isSelected();
            applyFilter();
        });

        view.getAll().addActionListener(e -> removeFilter());
    }

    private void selectAll() {
        showDbEntity = showObjEntity = showEmbeddable = showProcedure = showQuery = true;
        view.getDbEntity().setSelected(true);
        view.getObjEntity().setSelected(true);
        view.getEmbeddable().setSelected(true);
        view.getProcedure().setSelected(true);

        view.getQuery().setSelected(true);
        view.getAll().setEnabled(false);
    }

    private void removeFilter() {
        selectAll();
        treeModel.setFiltered(true, true, true, true, true);
        treeView.updateUI();
    }

    private void applyFilter() {
        treeModel.setFiltered(showDbEntity, showObjEntity, showEmbeddable, showProcedure, showQuery);
        treeView.updateUI();

        boolean all = showDbEntity && showObjEntity && showEmbeddable && showProcedure && showQuery;
        view.getAll().setSelected(all);
        view.getAll().setEnabled(!all);
    }

    public TreeFilterPopup getView() {
        return view;
    }

    public void treeExpOrCollPath(String action) {
        TreeNode root = (TreeNode) treeModel.getRoot();
        expandAll(treeView, new TreePath(root), action);
    }

    private void expandAll(JTree tree, TreePath parent, String action) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();

        if (node.getChildCount() >= 0) {
            for (Enumeration<? extends TreeNode> e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, action);
            }
        }

        if ("expand".equals(action)) {
            tree.expandPath(parent);
        } else if ("collapse".equals(action)) {
            treeModel.reload(treeModel.getRootNode());
        }
    }
}
