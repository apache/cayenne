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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.DomainEvent;
import org.apache.cayenne.configuration.event.DomainListener;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.UpdateValidationConfigAction;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ValidationConfig;
import org.apache.cayenne.swing.components.tree.CheckBoxNodeData;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * @since 5.0
 */
public class ValidationTabController implements DomainListener {

    private final ProjectController projectController;
    private final DataChannelMetaData metaData;
    private final ValidationTab view;

    private Set<Inspection> enabledInspections;

    public ValidationTabController(ProjectController projectController) {
        this.projectController = projectController;
        this.metaData = projectController.getApplication().getInjector().getInstance(DataChannelMetaData.class);
        this.view = new ValidationTab(this);
    }

    @Override
    public void domainChanged(DomainEvent e) {
        updateConfig(e.getDomain());
    }

    public ValidationTab getView() {
        return view;
    }

    void onViewLoaded() {
        projectController.addDomainListener(this);
        updateConfig(projectController.getCurrentDataChanel());
        initListeners();
    }

    private void initListeners() {
        view.inspectionTree.getModel().addTreeModelListener(new InspectionTreeModelListener());
        view.inspectionTree.getSelectionModel().addTreeSelectionListener(e -> {
            TreePath path = e.getNewLeadSelectionPath();
            if (path == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode)) {
                return;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            if (!(node.getUserObject() instanceof CheckBoxNodeData)) {
                return;
            }

            view.showDescription((CheckBoxNodeData) node.getUserObject());
        });
    }

    private void updateConfig(DataChannelDescriptor dataChannel) {
        ValidationConfig config = Optional.ofNullable(metaData.get(dataChannel, ValidationConfig.class))
                .orElseGet(ValidationConfig::new);
        Set<Inspection> configInspections = config.getEnabledInspections();
        enabledInspections = configInspections.isEmpty()
                ? EnumSet.noneOf(Inspection.class)
                : EnumSet.copyOf(configInspections);
        view.refreshSelectedInspections(enabledInspections);
    }

    private class InspectionTreeModelListener implements TreeModelListener {

        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            handleEvent(e);
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            handleEvent(e);
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            handleEvent(e);
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            handleEvent(e);
        }

        private void handleEvent(TreeModelEvent e) {
            boolean inspectionsUpdated = false;
            for (Object child : e.getChildren()) {
                if (!(child instanceof DefaultMutableTreeNode)) {
                    continue;
                }
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) child;
                if (!(node.getUserObject() instanceof CheckBoxNodeData)) {
                    continue;
                }
                CheckBoxNodeData data = (CheckBoxNodeData) node.getUserObject();
                if (!(data.getValue() instanceof Inspection)) {
                    continue;
                }
                Inspection inspection = (Inspection) data.getValue();

                if (data.isSelected()) {
                    inspectionsUpdated = enabledInspections.add(inspection);
                } else {
                    inspectionsUpdated = enabledInspections.remove(inspection);
                }
            }
            if (inspectionsUpdated) {
                projectController.getApplication()
                        .getActionManager()
                        .getAction(UpdateValidationConfigAction.class)
                        .putDataChannel(projectController.getCurrentDataChanel())
                        .putConfig(new ValidationConfig(enabledInspections))
                        .setUndoable(true)
                        .performAction(null);
            }
        }
    }
}
