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
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action.DbImportActions;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action.ModelerDbImportAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.ColorTreeRenderer;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.DbImportTreeNode;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.TransferableNode;
import org.apache.cayenne.modeler.toolkit.AppAction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DbImportView extends ProjectPanel {

    private static final int ALL_LINE_SPAN = 5;
    private static final ImageIcon rightArrow = IconFactory.buildIcon("icon-arrow-closed.png");
    private static final ImageIcon downArrow = IconFactory.buildIcon("icon-arrow-open.png");

    private final TreeToolbarPanel treeToolbar;
    private final ReverseEngineeringTreePanel treePanel;
    private final ReverseEngineeringConfigPanel configPanel;
    private final SourceTargetPanel sourceTargetPanel;
    private final JProgressBar loadDbSchemaProgress;
    private final JProgressBar reverseEngineeringProgress;
    private final AppAction.CayenneToolbarButton loadDbSchemaButton;

    private boolean initFromModel;

    public DbImportView(ProjectSession session) {
        super(session);

        DbImportTreeNode configRoot = new DbImportTreeNode(new ReverseEngineering());
        DbImportTree configTree = new DbImportTree(configRoot);
        DbImportTreeModel configModel = new DbImportTreeModel(configRoot, true, "Configuration is empty.");
        configTree.setRootVisible(false);
        configTree.setModel(configModel);
        configTree.setShowsRootHandles(true);

        DbImportTreeNode dbRoot = new DbImportTreeNode(new ReverseEngineering());
        DbImportTree dbTree = new DbImportTree(new TransferableNode(dbRoot));
        DbImportTreeModel dbModel = new DbImportTreeModel(dbRoot, false, "Click 'Refresh DB Schema' above to load the schema.");
        dbTree.setRootVisible(false);
        dbTree.setShowsRootHandles(true);
        dbTree.setModel(dbModel);

        DbImportActions actions = new DbImportActions(app, this, configTree, dbTree);
        this.sourceTargetPanel = new SourceTargetPanel(dbTree, configTree, actions);
        dbTree.setLoadDbSchemaAction(actions.getLoadDbSchemaAction());
        this.treeToolbar = new TreeToolbarPanel(configTree, actions);
        this.treePanel = new ReverseEngineeringTreePanel(session, configTree, dbTree, this.sourceTargetPanel, actions);
        treePanel.setTreeToolbar(treeToolbar);

        configModel.setDbSchemaTree(dbTree);
        dbModel.setDbSchemaTree(dbTree);

        ((ColorTreeRenderer) sourceTargetPanel.getSourceTree().getCellRenderer()).setReverseEngineeringTree(configTree);
        this.configPanel = new ReverseEngineeringConfigPanel(session, this);
        this.loadDbSchemaProgress = new JProgressBar();
        this.reverseEngineeringProgress = new JProgressBar();
        this.loadDbSchemaButton = (AppAction.CayenneToolbarButton) actions.getLoadDbSchemaAction().buildButton(0);

        initLayout(actions);
        initBindings();
    }

    private void initLayout(DbImportActions actions) {
        configPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        configPanel.setVisible(false);

        FormLayout buttonPanelLayout = new FormLayout("fill:50dlu");
        DefaultFormBuilder buttonBuilder = new DefaultFormBuilder(buttonPanelLayout);
        buttonBuilder.append(sourceTargetPanel.getMoveButton());
        buttonBuilder.append(sourceTargetPanel.getMoveInvertButton());

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

        loadDbSchemaButton.setShowingText(false);
        loadDbSchemaButton.setText("Refresh DB Schema");
        treeToolbar.add(loadDbSchemaButton);

        ModelerDbImportAction dbImportAction = actions.getReverseEngineeringAction();
        AppAction.CayenneToolbarButton reverseEngineeringButton =
                (AppAction.CayenneToolbarButton) dbImportAction.buildButton(0);
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
        builder.append(sourceTargetPanel);

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
        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
        sourceTargetPanel.getSourceTree().repaint();
    }

    private void initBindings() {
        session.addDataMapDisplayListener(e -> {
            DataMap map = e.getDataMap();
            if (map != null) {
                initFromModel(map);
            }
        });
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
            ReverseEngineering reverseEngineering = DbImportView.this.app
                    .getMetaData().get(map, ReverseEngineering.class);
            if (reverseEngineering == null) {
                // create config with default values
                reverseEngineering = new ReverseEngineering();
                DbImportView.this.app.getMetaData().add(map, reverseEngineering);
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
            DbImportTreeNode root = sourceTargetPanel.getSourceTree().getRootNode();
            root.removeAllChildren();
            sourceTargetPanel.updateTree(session.getSelectedDataMap());
            sourceTargetPanel.getMoveButton().setEnabled(false);
            sourceTargetPanel.getMoveInvertButton().setEnabled(false);
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

    public SourceTargetPanel getDraggableTreePanel() {
        return sourceTargetPanel;
    }

    public boolean isInitFromModel() {
        return initFromModel;
    }

    void invalidateDbSchema() {
        DbImportTree sourceTree = sourceTargetPanel.getSourceTree();
        DbImportTreeNode root = sourceTree.getRootNode();
        root.removeAllChildren();
        sourceTree.setEnabled(false);
        ((DbImportTreeModel) sourceTree.getModel()).reload();
    }

    public String[] getTableTypes() {
        return configPanel
                .getReverseEngineeringBySelectedMap()
                .getTableTypes();
    }
}
