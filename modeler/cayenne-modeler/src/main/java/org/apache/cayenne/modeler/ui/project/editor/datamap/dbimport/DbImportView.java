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
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.ColorTreeRenderer;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.DbImportTreeNode;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.TransferableNode;
import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;

public class DbImportView extends ProjectPanel {

    private static final int ALL_LINE_SPAN = 5;
    private static final ImageIcon rightArrow = IconFactory.buildIcon("icon-arrow-closed.png");
    private static final ImageIcon downArrow = IconFactory.buildIcon("icon-arrow-open.png");

    private final ReverseEngineeringTreePanel configTree;
    private final ConfigToolbar configToolbar;
    private final ReverseEngineeringConfigPanel configPanel;

    private final DbSchemaToolbar dbSchemaToolbar;
    private final DBSchemaPanel dbSchemaPanel;

    private final JProgressBar loadDbSchemaProgress;
    private final JProgressBar reverseEngineeringProgress;

    private boolean initFromModel;

    public DbImportView(ProjectSession session) {
        super(session);

        DbImportTreeNode configRoot = new DbImportTreeNode(new ReverseEngineering());
        DbImportTree configTree = new DbImportTree(configRoot);
        DbImportTreeModel configModel = new DbImportTreeModel(configRoot, true, "Create DB Import Rules");
        configTree.setRootVisible(false);
        configTree.setModel(configModel);
        configTree.setShowsRootHandles(true);

        DbImportTreeNode dbSchemaRootNode = new DbImportTreeNode(new ReverseEngineering());
        DbImportTree dbSchemaTree = new DbImportTree(new TransferableNode(dbSchemaRootNode));
        DbImportTreeModel dbSchemaModel = new DbImportTreeModel(dbSchemaRootNode, false, "Click 'Refresh DB Schema' to load the schema.");
        dbSchemaTree.setRootVisible(false);
        dbSchemaTree.setShowsRootHandles(true);
        dbSchemaTree.setModel(dbSchemaModel);

        DbImportActions actions = new DbImportActions(app, this, configTree, dbSchemaTree);
        this.dbSchemaPanel = new DBSchemaPanel(dbSchemaTree, configTree, actions);
        this.dbSchemaToolbar = new DbSchemaToolbar(actions);

        dbSchemaTree.setLoadDbSchemaAction(actions.getLoadDbSchemaAction());
        this.configToolbar = new ConfigToolbar(configTree, actions);
        this.configTree = new ReverseEngineeringTreePanel(session, configTree, dbSchemaTree, this.dbSchemaPanel, actions);
        this.configTree.setTreeToolbar(configToolbar);

        // repaint the db schema tree whenever the config tree changes so ColorTreeRenderer refreshes
        configModel.addTreeModelListener(new TreeModelListener() {
            public void treeNodesChanged(TreeModelEvent e) { dbSchemaTree.repaint(); }
            public void treeNodesInserted(TreeModelEvent e) { dbSchemaTree.repaint(); }
            public void treeNodesRemoved(TreeModelEvent e) { dbSchemaTree.repaint(); }
            public void treeStructureChanged(TreeModelEvent e) { dbSchemaTree.repaint(); }
        });

        ((ColorTreeRenderer) dbSchemaPanel.getSourceTree().getCellRenderer()).setReverseEngineeringTree(configTree);
        this.configPanel = new ReverseEngineeringConfigPanel(session, this);
        this.loadDbSchemaProgress = new JProgressBar();
        this.reverseEngineeringProgress = new JProgressBar();

        initLayout(actions);
        initBindings();
    }

    private void initLayout(DbImportActions actions) {
        configPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        configPanel.setVisible(false);

        FormLayout buttonPanelLayout = new FormLayout("fill:50dlu");
        DefaultFormBuilder buttonBuilder = new DefaultFormBuilder(buttonPanelLayout);
        buttonBuilder.append(dbSchemaPanel.getMoveButton());
        buttonBuilder.append(dbSchemaPanel.getMoveInvertButton());

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(configToolbar, BorderLayout.NORTH);
        leftPanel.add(configTree, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(dbSchemaToolbar, BorderLayout.NORTH);
        rightPanel.add(dbSchemaPanel, BorderLayout.CENTER);

        FormLayout layout = new FormLayout("fill:160dlu:grow, 5dlu, fill:50dlu, 5dlu, fill:160dlu:grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(leftPanel);
        builder.append(buttonBuilder.getPanel());
        builder.append(rightPanel);

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
        dbSchemaPanel.getSourceTree().repaint();
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
        hideButton.setContentAreaFilled(false);
        hideButton.setFocusPainted(false);
        hideButton.setRolloverEnabled(false);
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
        configTree.getReverseEngineeringTree().stopEditing();
        if (map != null) {
            initFromModel = true;
            configToolbar.unlockButtons();
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
            configTree.updateTree();
            DbImportTreeNode root = dbSchemaPanel.getSourceTree().getRootNode();
            root.removeAllChildren();
            dbSchemaPanel.updateTree(session.getSelectedDataMap());
            dbSchemaPanel.getMoveButton().setEnabled(false);
            dbSchemaPanel.getMoveInvertButton().setEnabled(false);
        }
        initFromModel = false;
    }

    public JProgressBar getLoadDbSchemaProgress() {
        return loadDbSchemaProgress;
    }

    public void lockToolbarButtons() {
        configToolbar.changeToolbarButtonsState(false);
    }

    public void unlockToolbarButtons() {
        configToolbar.unlockButtons();
    }

    public JProgressBar getReverseEngineeringProgress() {
        return reverseEngineeringProgress;
    }

    public JButton getLoadDbSchemaButton() {
        return dbSchemaToolbar.getLoadDbSchemaButton();
    }

    public DBSchemaPanel getDraggableTreePanel() {
        return dbSchemaPanel;
    }

    public boolean isInitFromModel() {
        return initFromModel;
    }

    void invalidateDbSchema() {
        DbImportTree sourceTree = dbSchemaPanel.getSourceTree();
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
