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
package org.apache.cayenne.modeler.ui.project.editor.datadomain.validation;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.modeler.event.display.DomainDisplayEvent;
import org.apache.cayenne.modeler.event.display.DomainDisplayListener;
import org.apache.cayenne.modeler.event.display.ValidationConfigDisplayEvent;
import org.apache.cayenne.modeler.event.display.ValidationConfigDisplayListener;
import org.apache.cayenne.modeler.event.model.DomainEvent;
import org.apache.cayenne.modeler.event.model.DomainListener;
import org.apache.cayenne.modeler.toolkit.tree.CheckBoxNodeData;
import org.apache.cayenne.modeler.toolkit.AppPanel;
import org.apache.cayenne.modeler.ui.action.UpdateValidationConfigAction;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ValidationConfig;

import javax.swing.JScrollPane;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.util.EnumSet;
import java.util.Set;

public class ValidationTab extends AppPanel
        implements DomainDisplayListener, DomainListener, ValidationConfigDisplayListener {

    private final ProjectSession session;
    private final DataChannelMetaData metaData;
    private final InspectionCheckBoxTree inspectionTree;
    private final InspectionTreeModelListener inspectionTreeModelListener;

    private Set<Inspection> enabledInspections;

    public ValidationTab(ProjectSession session) {
        super(session.app());
        this.session = session;
        this.metaData = session.app().getMetaData();
        this.inspectionTree = InspectionCheckBoxTree.build();
        this.inspectionTreeModelListener = new InspectionTreeModelListener();

        initLayout();
        initBindings();
    }

    private void initLayout() {
        JScrollPane scrollableInspectionPane = new JScrollPane(inspectionTree);
        scrollableInspectionPane.getVerticalScrollBar().setUnitIncrement(12);

        setLayout(new BorderLayout());
        add(scrollableInspectionPane, BorderLayout.CENTER);
    }

    private void initBindings() {
        session.addDomainDisplayListener(this);
        session.addDomainListener(this);
        session.addValidationConfigDisplayListener(this);
        refreshFromMetadata(session.getSelectedDataDomain());
        inspectionTree.getModel().addTreeModelListener(inspectionTreeModelListener);
    }

    @Override
    public void domainSelected(DomainDisplayEvent e) {
        refreshFromMetadata(e.getDomain());
    }

    @Override
    public void domainChanged(DomainEvent e) {
        refreshFromMetadata(e.getDomain());
    }

    private void refreshFromMetadata(DataChannelDescriptor domain) {
        // suspend tree listener to skip reverberated config updates
        inspectionTree.getModel().removeTreeModelListener(inspectionTreeModelListener);
        try {
            configUpdated(ValidationConfig.fromMetadata(metaData, domain));
        } finally {
            inspectionTree.getModel().addTreeModelListener(inspectionTreeModelListener);
        }
    }

    @Override
    public void validationOptionChanged(ValidationConfigDisplayEvent event) {
        Inspection inspection = event.getInspection();
        if (inspection != null) {
            inspectionTree.selectInspection(inspection);
        }
    }

    private void configUpdated(ValidationConfig updatedConfig) {
        Set<Inspection> configInspections = updatedConfig.getEnabledInspections();
        enabledInspections = configInspections.isEmpty()
                ? EnumSet.noneOf(Inspection.class)
                : EnumSet.copyOf(configInspections);
        inspectionTree.refreshEnabledInspections(enabledInspections);
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
                app().getActionManager()
                        .getAction(UpdateValidationConfigAction.class)
                        .putConfig(new ValidationConfig(enabledInspections))
                        .setUndoable(true)
                        .performAction(this);
            }
        }
    }
}
