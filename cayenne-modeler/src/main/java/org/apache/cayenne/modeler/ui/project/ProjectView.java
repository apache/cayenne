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

import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.pref.adapters.SplitPanePrefs;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.ui.action.CollapseTreeAction;
import org.apache.cayenne.modeler.ui.action.FilterAction;
import org.apache.cayenne.modeler.ui.project.editor.EditorPanelView;
import org.apache.cayenne.modeler.ui.project.tree.ProjectTree;
import org.apache.cayenne.modeler.ui.project.tree.treefilter.TreeFilterPopup;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;

/**
 * Main display area split into the project navigation tree on the left and the selected
 * object editor on the right. Constructed once per opened project; receives the
 * {@link ProjectSession} that backs it.
 */
public class ProjectView extends ProjectPanel {

    private final ProjectTree treePanel;
    private final EditorPanelView editorPanel;
    private final TreeFilterPopup filterPopup;

    public ProjectView(ProjectSession session) {
        super(session);

        GlobalActions globalActions = app.getActionManager();
        globalActions.getAction(CollapseTreeAction.class).setAlwaysOn(true);
        globalActions.getAction(FilterAction.class).setAlwaysOn(true);

        setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 1));

        JToolBar barPanel = new JToolBar();
        barPanel.setFloatable(false);
        barPanel.setMinimumSize(new Dimension(75, 30));
        barPanel.setBorder(BorderFactory.createEmptyBorder());
        barPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JButton collapseButton = globalActions.getAction(CollapseTreeAction.class).buildButton(1);
        JButton filterButton = globalActions.getAction(FilterAction.class).buildButton(3);
        filterButton.setPreferredSize(new Dimension(30, 30));
        collapseButton.setPreferredSize(new Dimension(30, 30));
        barPanel.add(filterButton);
        barPanel.add(collapseButton);

        treePanel = new ProjectTree(session);
        treePanel.setMinimumSize(new Dimension(75, 180));

        JPanel treeNavigatePanel = new JPanel();
        treeNavigatePanel.setMinimumSize(new Dimension(75, 220));
        treeNavigatePanel.setLayout(new BorderLayout());
        treeNavigatePanel.add(treePanel, BorderLayout.CENTER);

        editorPanel = new EditorPanelView(session);
        editorPanel.setMinimumSize(new Dimension(0, 0));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitPane.setDividerSize(2);
        splitPane.setResizeWeight(0.0);
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

        this.filterPopup = new TreeFilterPopup(treePanel);

        new SplitPanePrefs(app.getPrefsManager().uiNode("project/splitPane")).bind(splitPane, 300);
    }

    public EditorPanelView getEditorPanel() {
        return editorPanel;
    }

    public TreeFilterPopup getFilterPopup() {
        return filterPopup;
    }

    public ProjectTree getProjectTreeView() {
        return treePanel;
    }
}
