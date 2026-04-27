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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action.DbImportActions;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action.LoadDbSchemaAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action.ModelerDbImportAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.ColorTreeRenderer;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.DbImportTreeNode;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.TransferableNode;
import org.apache.cayenne.modeler.ui.action.ModelerAbstractAction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DbImportView extends JPanel {

    private static final int ALL_LINE_SPAN = 5;
    private static final ImageIcon rightArrow = IconFactory.buildIcon("icon-arrow-closed.png");
    private static final ImageIcon downArrow = IconFactory.buildIcon("icon-arrow-open.png");

    private final TreeToolbarPanel treeToolbar;
    private final ReverseEngineeringTreePanel treePanel;
    private final ReverseEngineeringConfigPanel configPanel;
    private final DraggableTreePanel draggableTreePanel;
    private final JProgressBar loadDbSchemaProgress;
    private final JProgressBar reverseEngineeringProgress;
    private final ModelerAbstractAction.CayenneToolbarButton loadDbSchemaButton;

    private final ProjectController controller;

    private boolean initFromModel;

    public DbImportView(ProjectController controller) {
        this.controller = controller;

        DbImportTreeNode root = new DbImportTreeNode(new ReverseEngineering());
        DbImportTreeNode draggableTreeRoot = new DbImportTreeNode(new ReverseEngineering());
        DbImportTree reverseEngineeringTree = new DbImportTree(root);
        DbImportTree draggableTree = new DbImportTree(new TransferableNode(draggableTreeRoot));
        DbImportModel model = new DbImportModel(root);
        model.setCanBeCleaned(true);
        DbImportModel draggableTreeModel = new DbImportModel(draggableTreeRoot);
        draggableTreeModel.setCanBeCleaned(false);

        draggableTree.setRootVisible(false);
        draggableTree.setShowsRootHandles(true);
        draggableTree.setModel(draggableTreeModel);
        reverseEngineeringTree.setRootVisible(false);
        reverseEngineeringTree.setModel(model);
        reverseEngineeringTree.setShowsRootHandles(true);

        DbImportActions actions = new DbImportActions(controller.getApplication(), this, reverseEngineeringTree, draggableTree);

        this.draggableTreePanel = new DraggableTreePanel(draggableTree, reverseEngineeringTree, actions);

        draggableTree.setLoadDbSchemaAction(actions.getLoadDbSchemaAction());
        treeToolbar = new TreeToolbarPanel(reverseEngineeringTree, actions);
        treePanel = new ReverseEngineeringTreePanel(controller, reverseEngineeringTree, draggableTree, actions);
        treePanel.setTreeToolbar(treeToolbar);
        model.setDbSchemaTree(draggableTree);
        draggableTreeModel.setDbSchemaTree(draggableTree);
        ((ColorTreeRenderer) draggableTreePanel.getSourceTree().getCellRenderer()).
                setReverseEngineeringTree(reverseEngineeringTree);

        configPanel = new ReverseEngineeringConfigPanel(controller, this);
        configPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        configPanel.setVisible(false);


        FormLayout buttonPanelLayout = new FormLayout("fill:50dlu");
        DefaultFormBuilder buttonBuilder = new DefaultFormBuilder(buttonPanelLayout);
        buttonBuilder.append(draggableTreePanel.getMoveButton());
        buttonBuilder.append(draggableTreePanel.getMoveInvertButton());

        FormLayout layout = new FormLayout("fill:160dlu:grow, 5dlu, fill:50dlu, 5dlu, fill:160dlu:grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(treeToolbar, ALL_LINE_SPAN);
        FormLayout headerLayout = new FormLayout("fill:80dlu:grow");

        DefaultFormBuilder reverseEngineeringHeaderBuilder = new DefaultFormBuilder(headerLayout);
        JLabel importLabel = new JLabel("Database Import Configuration");
        importLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
        reverseEngineeringHeaderBuilder.append(importLabel);
        builder.append(reverseEngineeringHeaderBuilder.getPanel());

        DefaultFormBuilder databaseHeaderBuilder = new DefaultFormBuilder(headerLayout);
        JLabel schemaLabel = new JLabel("Database Schema");
        schemaLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
        databaseHeaderBuilder.append(schemaLabel);
        LoadDbSchemaAction loadDbSchemaAction = actions.getLoadDbSchemaAction();
        loadDbSchemaButton = (ModelerAbstractAction.CayenneToolbarButton) loadDbSchemaAction.buildButton(0);
        loadDbSchemaButton.setShowingText(false);
        loadDbSchemaButton.setText("Refresh DB Schema");
        treeToolbar.add(loadDbSchemaButton);

        ModelerDbImportAction dbImportAction = actions.getReverseEngineeringAction();
        ModelerAbstractAction.CayenneToolbarButton reverseEngineeringButton = (ModelerAbstractAction.CayenneToolbarButton)
                dbImportAction.buildButton(0);
        reverseEngineeringButton.setShowingText(true);
        reverseEngineeringButton.setText("Run Import");
        JPanel reverseEngineeringButtonPanel = new JPanel();
        reverseEngineeringButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        reverseEngineeringButtonPanel.add(reverseEngineeringButton);
        treeToolbar.addSeparator();
        treeToolbar.add(reverseEngineeringButtonPanel);

        builder.append("");
        builder.append(databaseHeaderBuilder.getPanel());

        builder.append(treePanel);
        builder.append(buttonBuilder.getPanel());
        builder.append(draggableTreePanel);

        loadDbSchemaProgress = new JProgressBar();
        reverseEngineeringProgress = new JProgressBar();
        loadDbSchemaProgress.setIndeterminate(true);
        loadDbSchemaProgress.setVisible(false);
        reverseEngineeringProgress.setIndeterminate(true);
        reverseEngineeringProgress.setVisible(false);
        FormLayout progressLayout = new FormLayout("fill:160dlu:grow, 60dlu, fill:160dlu:grow", "fill:10dlu");
        DefaultFormBuilder progressBarBuilder = new DefaultFormBuilder(progressLayout);
        progressBarBuilder.append(reverseEngineeringProgress);
        progressBarBuilder.append(loadDbSchemaProgress);
        builder.append(progressBarBuilder.getPanel(), ALL_LINE_SPAN);

        createAdvancedOptionsHiderPanel(builder);

        builder.append(configPanel, ALL_LINE_SPAN);
        this.setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        controller.addDataMapDisplayListener(e -> {
            DataMap map = e.getDataMap();
            if (map != null) {
                initFromModel(map);
            }
        });
        draggableTreePanel.getSourceTree().repaint();
    }

    private void createAdvancedOptionsHiderPanel(DefaultFormBuilder builder) {
        JPanel advancedOptionsPanel = new JPanel();
        advancedOptionsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton hideButton = new JButton("Advanced Options");
        hideButton.setIcon(configPanel.isVisible() ? downArrow : rightArrow);
        hideButton.setBorderPainted(false);
        hideButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                configPanel.setVisible(!configPanel.isVisible());
                hideButton.setIcon(configPanel.isVisible() ? downArrow : rightArrow);
            }
        });
        advancedOptionsPanel.add(hideButton);

        builder.append(advancedOptionsPanel, ALL_LINE_SPAN);
    }

    public void initFromModel(DataMap map) {
        treePanel.getReverseEngineeringTree().stopEditing();
        if (map != null) {
            initFromModel = true;
            treeToolbar.unlockButtons();
            ReverseEngineering reverseEngineering = DbImportView.this.controller.getApplication()
                    .getMetaData().get(map, ReverseEngineering.class);
            if (reverseEngineering == null) {
                // create config with default values
                reverseEngineering = new ReverseEngineering();
                DbImportView.this.controller.getApplication().getMetaData().add(map, reverseEngineering);
            }
            configPanel.fillCheckboxes(reverseEngineering);
            configPanel.initializeTextFields(reverseEngineering);
            configPanel.initStrategy(reverseEngineering);
            String[] tableTypes = reverseEngineering.getTableTypes();
            if (tableTypes.length != 0) {
                configPanel.getTableTypes().setText(String.join(",", tableTypes));
            } else {
                configPanel.getTableTypes().setText("TABLE, VIEW");
                configPanel.applyTableTypes("TABLE, VIEW");
            }
            treePanel.updateTree();
            DbImportTreeNode root = draggableTreePanel.getSourceTree().getRootNode();
            root.removeAllChildren();
            draggableTreePanel.updateTree(controller.getSelectedDataMap());
            draggableTreePanel.getMoveButton().setEnabled(false);
            draggableTreePanel.getMoveInvertButton().setEnabled(false);
        }
        initFromModel = false;
    }

    public JProgressBar getLoadDbSchemaProgress() {
        return loadDbSchemaProgress;
    }

    public void lockToolbarButtons() {
        treeToolbar.changeToolbarButtonsState(false);
    }

    public void unlockToolbarButtons() {
        treeToolbar.unlockButtons();
    }

    public JProgressBar getReverseEngineeringProgress() {
        return reverseEngineeringProgress;
    }

    public JButton getLoadDbSchemaButton() {
        return loadDbSchemaButton;
    }

    public DraggableTreePanel getDraggableTreePanel() {
        return draggableTreePanel;
    }

    public boolean isInitFromModel() {
        return initFromModel;
    }

    public String[] getTableTypes() {
        return configPanel
                .getReverseEngineeringBySelectedMap()
                .getTableTypes();
    }
}
