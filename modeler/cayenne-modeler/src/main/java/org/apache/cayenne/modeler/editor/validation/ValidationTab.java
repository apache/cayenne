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
package org.apache.cayenne.modeler.editor.validation;

import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.swing.components.tree.ChangeOptimizingTreeModel;
import org.apache.cayenne.swing.components.tree.CheckBoxNodeData;
import org.apache.cayenne.swing.components.tree.CheckBoxTree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @since 5.0
 */
public class ValidationTab extends JPanel {

    final ValidationTabController controller;
    Map<Inspection, TreePath> inspectionPathMap;
    CheckBoxTree inspectionTree;

    public ValidationTab(ValidationTabController controller) {
        this.controller = controller;
        this.inspectionPathMap = new HashMap<>();
    }

    public void initView() {
        resetView();

        inspectionTree = initTree();
        JScrollPane scrollableInspectionPane = new JScrollPane(inspectionTree);
        scrollableInspectionPane.getVerticalScrollBar().setUnitIncrement(12);

        setLayout(new BorderLayout());
        add(scrollableInspectionPane, BorderLayout.CENTER);

        controller.onViewLoaded();
    }

    CheckBoxTree initTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        Map<Inspection.Group, MutableTreeNode> groupNodes = new EnumMap<>(Inspection.Group.class);
        for (Inspection inspection : Inspection.values()) {
            MutableTreeNode groupNode = groupNodes.computeIfAbsent(inspection.group(), g -> addChildCheckBox(root, g));
            DefaultMutableTreeNode inspectionNode = addChildCheckBox(groupNode, inspection);
            inspectionPathMap.put(inspection, new TreePath(inspectionNode.getPath()));
        }
        TreeModel treeModel = new ChangeOptimizingTreeModel(root);
        return new CheckBoxTree(treeModel);
    }

    void resetView() {
        removeAll();
        inspectionPathMap.clear();
    }

    void refreshSelectedInspections(Set<Inspection> selectedInspections) {
        TreeModel model = inspectionTree.getModel();
        for (var inspectionPath : inspectionPathMap.entrySet()) {
            Inspection inspection = inspectionPath.getKey();
            TreePath path = inspectionPath.getValue();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            CheckBoxNodeData data = (CheckBoxNodeData) node.getUserObject();

            model.valueForPathChanged(path, data.withState(selectedInspections.contains(inspection)));
        }
    }

    static DefaultMutableTreeNode addChildCheckBox(MutableTreeNode parent, Object value) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new CheckBoxNodeData(value, false));
        parent.insert(node, parent.getChildCount());
        return node;
    }
}
