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
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class InspectionCheckBoxTree extends CheckBoxTree {

    private final Map<Inspection, TreePath> inspectionPathCache;

    private InspectionCheckBoxTree(TreeModel model, Map<Inspection, TreePath> inspectionPathCache) {
        super(model);
        this.inspectionPathCache = inspectionPathCache;

        labelTree.addMouseMotionListener(new ToolTipHandler(labelTree));
        checkBoxTree.addMouseMotionListener(new ToolTipHandler(checkBoxTree));
    }

    public void refreshSelectedInspections(Set<Inspection> selectedInspections) {
        TreeModel model = getModel();
        for (var inspectionPath : inspectionPathCache.entrySet()) {
            Inspection inspection = inspectionPath.getKey();
            TreePath path = inspectionPath.getValue();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            CheckBoxNodeData data = (CheckBoxNodeData) node.getUserObject();

            model.valueForPathChanged(path, data.withState(selectedInspections.contains(inspection)));
        }
    }

    public static InspectionCheckBoxTree build() {
        Map<Inspection, TreePath> inspectionPathCache = new HashMap<>();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        Map<Inspection.Group, MutableTreeNode> groupNodes = new EnumMap<>(Inspection.Group.class);
        for (Inspection inspection : Inspection.values()) {
            MutableTreeNode groupNode = groupNodes.computeIfAbsent(inspection.group(), g -> addChild(root, g));
            DefaultMutableTreeNode inspectionNode = addChild(groupNode, inspection);
            inspectionPathCache.put(inspection, new TreePath(inspectionNode.getPath()));
        }

        ChangeOptimizingTreeModel model = new ChangeOptimizingTreeModel(root);
        return new InspectionCheckBoxTree(model, inspectionPathCache);
    }

    private static DefaultMutableTreeNode addChild(MutableTreeNode parent, Object value) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(new CheckBoxNodeData(value, false));
        parent.insert(node, parent.getChildCount());
        return node;
    }

    private static class ToolTipHandler extends MouseMotionAdapter {

        private final JTree tree;
        private Object latestTarget;

        public ToolTipHandler(JTree tree) {
            this.tree = tree;
            ToolTipManager.sharedInstance().registerComponent(tree);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
            TreePath closestPath = tree.getClosestPathForLocation(e.getX(), e.getY());
            if (closestPath == null || !(closestPath.getLastPathComponent() instanceof DefaultMutableTreeNode)) {
                tree.setToolTipText(null);
                toolTipManager.mousePressed(e); // this will hide tooltip no matter the event
                return;
            }

            DefaultMutableTreeNode component = (DefaultMutableTreeNode) closestPath.getLastPathComponent();
            if (component != latestTarget) {
                toolTipManager.mousePressed(e); // this will hide tooltip no matter the event
                latestTarget = component;
            }
            if (!(component.getUserObject() instanceof CheckBoxNodeData)) {
                tree.setToolTipText(null);
                return;
            }

            CheckBoxNodeData data = (CheckBoxNodeData) component.getUserObject();
            if (data.getValue() instanceof Inspection) {
                String description = ((Inspection) data.getValue()).description();
                tree.setToolTipText(description.endsWith(".") ? description : description + ".");
            } else if (data.getValue() instanceof Inspection.Group) {
                tree.setToolTipText(((Inspection.Group) data.getValue()).readableName() + " inspections.");
            } else {
                tree.setToolTipText(null);
            }
        }
    }
}
