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
package org.apache.cayenne.modeler.editor.dbimport;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.LoadDbSchemaAction;
import org.apache.cayenne.modeler.action.ReverseEngineeringAction;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.dialog.db.load.TransferableNode;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.ModelerUtil;

/**
 * @since 4.1
 */
public class DbImportView extends JPanel {

    private static final String MAIN_LAYOUT = "fill:160dlu:grow, 5dlu, fill:50dlu, 5dlu, fill:160dlu:grow";
    private static final String HEADER_LAYOUT = "fill:80dlu:grow";
    private static final String BUTTON_PANEL_LAYOUT = "fill:50dlu";
    private static final String PROGRESS_PANEL_LAYOUT = "fill:160dlu:grow, 60dlu, fill:160dlu:grow";
    private static final int ALL_LINE_SPAN = 5;
    private static final ImageIcon rightArrow = ModelerUtil.buildIcon("icon-arrow-closed.png");
    private static final ImageIcon downArrow = ModelerUtil.buildIcon("icon-arrow-open.png");

    private TreeToolbarPanel treeToolbar;
    private ReverseEngineeringTreePanel treePanel;
    private ReverseEngineeringConfigPanel configPanel;
    private DraggableTreePanel draggableTreePanel;
    private JProgressBar loadDbSchemaProgress;
    private JProgressBar reverseEngineeringProgress;
    private CayenneAction.CayenneToolbarButton loadDbSchemaButton;

    private ProjectController projectController;

    public DbImportView(ProjectController projectController) {
        this.projectController = projectController;
        initFormElements();
        buildForm();
        draggableTreePanel.getSourceTree().repaint();
    }

    private void buildForm() {
        FormLayout buttonPanelLayout = new FormLayout(BUTTON_PANEL_LAYOUT);
        DefaultFormBuilder buttonBuilder = new DefaultFormBuilder(buttonPanelLayout);
        buttonBuilder.append(draggableTreePanel.getMoveButton());
        buttonBuilder.append(draggableTreePanel.getMoveInvertButton());

        FormLayout layout = new FormLayout(MAIN_LAYOUT);
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(treeToolbar, ALL_LINE_SPAN);
        FormLayout headerLayout = new FormLayout(HEADER_LAYOUT);

        DefaultFormBuilder reverseEngineeringHeaderBuilder = new DefaultFormBuilder(headerLayout);
        JLabel importLabel = new JLabel("Database Import Configuration");
        importLabel.setBorder(new EmptyBorder(0, 5, 0,0));
        reverseEngineeringHeaderBuilder.append(importLabel);
        builder.append(reverseEngineeringHeaderBuilder.getPanel());

        DefaultFormBuilder databaseHeaderBuilder = new DefaultFormBuilder(headerLayout);
        JLabel schemaLabel = new JLabel("Database Schema");
        schemaLabel.setBorder(new EmptyBorder(0, 5, 0,0));
        databaseHeaderBuilder.append(schemaLabel);
        LoadDbSchemaAction loadDbSchemaAction = projectController.getApplication().getActionManager().
                getAction(LoadDbSchemaAction.class);
        loadDbSchemaAction.setDraggableTreePanel(draggableTreePanel);
        loadDbSchemaButton = (CayenneAction.CayenneToolbarButton) loadDbSchemaAction.buildButton(0);
        loadDbSchemaButton.setShowingText(false);
        loadDbSchemaButton.setText("Refresh DB Schema");
        treeToolbar.add(loadDbSchemaButton);

        ReverseEngineeringAction reverseEngineeringAction = projectController.getApplication().getActionManager().
                getAction(ReverseEngineeringAction.class);
        reverseEngineeringAction.setView(this);
        CayenneAction.CayenneToolbarButton reverseEngineeringButton = (CayenneAction.CayenneToolbarButton)
                reverseEngineeringAction.buildButton(0);
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
        FormLayout progressLayout = new FormLayout(PROGRESS_PANEL_LAYOUT, "fill:10dlu");
        DefaultFormBuilder progressBarBuilder = new DefaultFormBuilder(progressLayout);
        progressBarBuilder.append(reverseEngineeringProgress);
        progressBarBuilder.append(loadDbSchemaProgress);
        builder.append(progressBarBuilder.getPanel(), ALL_LINE_SPAN);

        createAdvancedOptionsHiderPanel(builder);

        builder.append(configPanel, ALL_LINE_SPAN);
        this.setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
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

    private void initFormElements() {
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

        draggableTreePanel = new DraggableTreePanel(projectController, draggableTree, reverseEngineeringTree);
        treeToolbar = new TreeToolbarPanel(projectController, reverseEngineeringTree, draggableTreePanel);
        treePanel = new ReverseEngineeringTreePanel(projectController, reverseEngineeringTree, draggableTree);
        treePanel.setTreeToolbar(treeToolbar);
        model.setDbSchemaTree(draggableTree);
        draggableTreeModel.setDbSchemaTree(draggableTree);
        ((ColorTreeRenderer) draggableTreePanel.getSourceTree().getCellRenderer()).
                setReverseEngineeringTree(reverseEngineeringTree);

        configPanel = new ReverseEngineeringConfigPanel(projectController);
        configPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        configPanel.setVisible(false);
    }

    public void initFromModel() {
        DataMap map = projectController.getCurrentDataMap();
        treePanel.getReverseEngineeringTree().stopEditing();
        if (map != null) {
            treeToolbar.unlockButtons();
            ReverseEngineering reverseEngineering = DbImportView.this.projectController.getApplication()
                    .getMetaData().get(map, ReverseEngineering.class);
            if (reverseEngineering == null) {
                reverseEngineering = new ReverseEngineering();
                DbImportView.this.projectController.getApplication().getMetaData().add(map, reverseEngineering);
                projectController.setDirty(true);
            }
            configPanel.fillCheckboxes(reverseEngineering);
            configPanel.initializeTextFields(reverseEngineering);
            configPanel.initStrategy(reverseEngineering);
            treePanel.updateTree();
            DbImportTreeNode root = draggableTreePanel.getSourceTree().getRootNode();
            root.removeAllChildren();
            draggableTreePanel.updateTree(projectController.getCurrentDataMap());
            draggableTreePanel.getMoveButton().setEnabled(false);
            draggableTreePanel.getMoveInvertButton().setEnabled(false);
        }
    }

    public boolean isSkipRelationshipsLoading() {
        return configPanel.getSkipRelationshipsLoading().isSelected();
    }

    public boolean isSkipPrimaryKeyLoading() {
        return configPanel.getSkipPrimaryKeyLoading().isSelected();
    }

    public boolean isForceDataMapCatalog() {
        return configPanel.getForceDataMapCatalog().isSelected();
    }

    public boolean isForceDataMapSchema() {
        return configPanel.getForceDataMapSchema().isSelected();
    }

    public boolean isUsePrimitives() {
        return configPanel.getUsePrimitives().isSelected();
    }

    public boolean isUseJava7Typed() {
        return configPanel.getUseJava7Types().isSelected();
    }

    public String getMeaningfulPk() {
        return "".equals(configPanel.getMeaningfulPk().getComponent().getText())
                ? null : configPanel.getMeaningfulPk().getComponent().getText();
    }

    public String getNamingStrategy() {
        return (String) configPanel.getStrategyCombo().getSelectedItem();
    }

    public String getStripFromTableNames() {
        return configPanel.getStripFromTableNames().getComponent().getText();
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
}
