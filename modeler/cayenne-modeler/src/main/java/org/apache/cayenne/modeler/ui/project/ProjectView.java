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

package org.apache.cayenne.modeler.ui.project;

import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CollapseTreeAction;
import org.apache.cayenne.modeler.action.FilterAction;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.ui.project.editor.EditorPanelView;
import org.apache.cayenne.modeler.ui.project.tree.ProjectTreeView;
import org.apache.cayenne.modeler.ui.project.tree.treefilter.TreeFilterController;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Main display area split into the project navigation tree on the left and selected object editor on the right.
 */
public class ProjectView extends JPanel {

    private final ProjectTreeView treePanel;
    private final EditorPanelView editorPanel;
    private final TreeFilterController filterController;

    public ProjectView(ProjectController controller) {

        ActionManager actionManager = controller.getApplication().getActionManager();
        actionManager.getAction(CollapseTreeAction.class).setAlwaysOn(true);
        actionManager.getAction(FilterAction.class).setAlwaysOn(true);

        setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 1));

        JToolBar barPanel = new JToolBar();
        barPanel.setFloatable(false);
        barPanel.setMinimumSize(new Dimension(75, 30));
        barPanel.setBorder(BorderFactory.createEmptyBorder());
        barPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton collapseButton = actionManager.getAction(CollapseTreeAction.class).buildButton(1);
        JButton filterButton = actionManager.getAction(FilterAction.class).buildButton(3);
        filterButton.setPreferredSize(new Dimension(30, 30));
        collapseButton.setPreferredSize(new Dimension(30, 30));
        barPanel.add(filterButton);
        barPanel.add(collapseButton);

        treePanel = new ProjectTreeView(controller);
        treePanel.setMinimumSize(new Dimension(75, 180));

        JPanel treeNavigatePanel = new JPanel();
        treeNavigatePanel.setMinimumSize(new Dimension(75, 220));
        treeNavigatePanel.setLayout(new BorderLayout());
        treeNavigatePanel.add(treePanel, BorderLayout.CENTER);

        editorPanel = new EditorPanelView(controller);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitPane.setDividerSize(2);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(barPanel, BorderLayout.NORTH);
        leftPanel.setBorder(BorderFactory.createEmptyBorder());
        JScrollPane scrollPane = new JScrollPane(treeNavigatePanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(editorPanel);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);

        this.filterController = new TreeFilterController(treePanel);

        // Moving this to try-catch block per CAY-940. Exception will be stack-traced
        try {
            ComponentGeometry geometry = new ComponentGeometry(this.getClass(), getClass().getSimpleName() + "/splitPane/divider");
            geometry.bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, 300);
        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass()).error("Cannot bind divider property", ex);
        }
    }

    public EditorPanelView getEditorPanel() {
        return editorPanel;
    }

    public TreeFilterController getFilterController() {
        return filterController;
    }

    public ProjectTreeView getProjectTreeView() {
        return treePanel;
    }
}
