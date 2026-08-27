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

import org.apache.cayenne.modeler.ui.project.tree.ProjectTree;
import org.apache.cayenne.modeler.ui.project.tree.ProjectTreeModel;

import javax.swing.JCheckBox;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

public class TreeFilterPopup extends JPopupMenu {

    private final ProjectTree treeView;
    private final ProjectTreeModel treeModel;

    private final JCheckBox dbEntity;
    private final JCheckBox objEntity;
    private final JCheckBox embeddable;
    private final JCheckBox procedure;
    private final JCheckBox query;
    private final JCheckBox all;

    private boolean showDbEntity;
    private boolean showObjEntity;
    private boolean showEmbeddable;
    private boolean showProcedure;
    private boolean showQuery;

    public TreeFilterPopup(ProjectTree treeView) {
        this.treeView = treeView;
        this.treeModel = treeView.getProjectModel();

        this.all = new JCheckBox("Show all");
        this.dbEntity = new JCheckBox("DbEntity");
        this.objEntity = new JCheckBox("ObjEntity");
        this.embeddable = new JCheckBox("Embeddable");
        this.procedure = new JCheckBox("Procedure");
        this.query = new JCheckBox("Query");

        add(all);
        addSeparator();
        add(dbEntity);
        add(objEntity);
        add(embeddable);
        add(procedure);
        add(query);

        selectAll();

        dbEntity.addActionListener(e -> {
            showDbEntity = dbEntity.isSelected();
            applyFilter();
        });
        objEntity.addActionListener(e -> {
            showObjEntity = objEntity.isSelected();
            applyFilter();
        });
        embeddable.addActionListener(e -> {
            showEmbeddable = embeddable.isSelected();
            applyFilter();
        });
        procedure.addActionListener(e -> {
            showProcedure = procedure.isSelected();
            applyFilter();
        });
        query.addActionListener(e -> {
            showQuery = query.isSelected();
            applyFilter();
        });
        all.addActionListener(e -> removeFilter());
    }

    public void treeExpOrCollPath(String action) {
        TreeNode root = (TreeNode) treeModel.getRoot();
        expandAll(treeView, new TreePath(root), action);
    }

    private void selectAll() {
        showDbEntity = showObjEntity = showEmbeddable = showProcedure = showQuery = true;
        dbEntity.setSelected(true);
        objEntity.setSelected(true);
        embeddable.setSelected(true);
        procedure.setSelected(true);
        query.setSelected(true);
        all.setEnabled(false);
    }

    private void removeFilter() {
        selectAll();
        treeModel.setFiltered(true, true, true, true, true);
        treeView.updateUI();
    }

    private void applyFilter() {
        treeModel.setFiltered(showDbEntity, showObjEntity, showEmbeddable, showProcedure, showQuery);
        treeView.updateUI();

        boolean allSelected = showDbEntity && showObjEntity && showEmbeddable && showProcedure && showQuery;
        all.setSelected(allSelected);
        all.setEnabled(!allSelected);
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
