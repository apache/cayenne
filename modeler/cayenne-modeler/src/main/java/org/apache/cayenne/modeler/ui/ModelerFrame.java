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

package org.apache.cayenne.modeler.ui;

import org.apache.cayenne.modeler.ui.project.ProjectView;
import org.apache.cayenne.modeler.action.*;
import org.apache.cayenne.modeler.ui.logconsole.LogConsoleController;
import org.apache.cayenne.modeler.ui.welcome.WelcomeScreen;
import org.apache.cayenne.modeler.event.model.RecentFileListListener;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.RecentFileMenu;
import org.apache.cayenne.swing.components.TopBorder;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main frame of CayenneModeler GUI
 */
public class ModelerFrame extends JFrame {

    private final LogConsoleController logConsoleController;
    private final ActionManager actionManager;
    private final List<RecentFileListListener> recentFileListeners;

    private final JSplitPane splitPane;
    private final JLabel status;
    private final WelcomeScreen welcomePanel;

    private ProjectView projectView;
    private JCheckBoxMenuItem logMenu;
    private Component dockComponent;

    public ModelerFrame(ActionManager actionManager, LogConsoleController logConsoleController) {
        this.actionManager = actionManager;
        this.logConsoleController = logConsoleController;
        this.recentFileListeners = new ArrayList<>();

        setIconImage(ModelerUtil.buildIcon("CayenneModeler.png").getImage());
        initMenus();
        initToolbar();

        status = new JLabel();
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 10));

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(TopBorder.create());
        splitPane.getInsets().left = 5;
        splitPane.getInsets().right = 5;
        splitPane.setResizeWeight(0.7);

        try {
            ComponentGeometry geometry = new ComponentGeometry(this.getClass(), getClass().getSimpleName() + "/splitPane/divider");
            geometry.bindIntProperty(splitPane, JSplitPane.DIVIDER_LOCATION_PROPERTY, 400);
        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass()).error("Cannot bind divider property", ex);
        }

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 1));
        statusBar.setBorder(TopBorder.create());
        // add placeholder
        statusBar.add(Box.createVerticalStrut(16));
        statusBar.add(status);

        if (getContentPane() instanceof JPanel) {
            ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder());
        }
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        this.welcomePanel = new WelcomeScreen();
        recentFileListeners.add(welcomePanel);

        fireRecentFileListChanged(); // start filling list in welcome screen and in menu

        setEditorPanel(null);
    }

    protected void initMenus() {
        getContentPane().setLayout(new BorderLayout());

        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu projectMenu = new JMenu("Project");
        JMenu toolMenu = new JMenu("Tools");
        JMenu helpMenu = new JMenu("Help");

        fileMenu.setMnemonic(KeyEvent.VK_F);
        editMenu.setMnemonic(KeyEvent.VK_E);
        projectMenu.setMnemonic(KeyEvent.VK_P);
        toolMenu.setMnemonic(KeyEvent.VK_T);
        helpMenu.setMnemonic(KeyEvent.VK_H);

        fileMenu.add(actionManager.getAction(NewProjectAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(OpenProjectAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(ProjectAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(ImportDataMapAction.class).buildMenu());
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getAction(SaveAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(SaveAsAction.class).buildMenu());
        fileMenu.add(actionManager.getAction(RevertAction.class).buildMenu());
        fileMenu.addSeparator();

        editMenu.add(actionManager.getAction(UndoAction.class).buildMenu());
        editMenu.add(actionManager.getAction(RedoAction.class).buildMenu());
        editMenu.add(actionManager.getAction(CutAction.class).buildMenu());
        editMenu.add(actionManager.getAction(CopyAction.class).buildMenu());
        editMenu.add(actionManager.getAction(PasteAction.class).buildMenu());

        RecentFileMenu recentFileMenu = new RecentFileMenu("Recent Projects");
        recentFileListeners.add(recentFileMenu);
        fileMenu.add(recentFileMenu);

        fileMenu.addSeparator();
        fileMenu.add(actionManager.getAction(ExitAction.class).buildMenu());

        projectMenu.add(actionManager.getAction(ValidateAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(ShowValidationConfigAction.class).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(actionManager.getAction(CreateNodeAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(CreateDataMapAction.class).buildMenu());

        projectMenu.add(actionManager.getAction(CreateObjEntityAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(CreateEmbeddableAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(CreateDbEntityAction.class).buildMenu());

        projectMenu.add(actionManager.getAction(CreateProcedureAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(CreateQueryAction.class).buildMenu());

        projectMenu.addSeparator();
        projectMenu.add(actionManager.getAction(ObjEntitySyncAction.class).buildMenu());
        projectMenu.add(actionManager.getAction(DbEntitySyncAction.class).buildMenu());
        projectMenu.addSeparator();
        projectMenu.add(actionManager.getAction(RemoveAction.class).buildMenu());

        toolMenu.add(actionManager.getAction(InferRelationshipsAction.class).buildMenu());
        toolMenu.add(actionManager.getAction(ImportEOModelAction.class).buildMenu());
        toolMenu.addSeparator();
        toolMenu.add(actionManager.getAction(GenerateCodeAction.class).buildMenu());
        toolMenu.add(actionManager.getAction(GenerateDBAction.class).buildMenu());
        toolMenu.add(actionManager.getAction(MigrateAction.class).buildMenu());

        // Menu for opening Log console
        toolMenu.addSeparator();
        logMenu = actionManager.getAction(ShowLogConsoleAction.class).buildCheckBoxMenu();

        if (!logConsoleController.getConsoleProperty(LogConsoleController.DOCKED_PROPERTY)
                && logConsoleController.getConsoleProperty(LogConsoleController.SHOW_CONSOLE_PROPERTY)) {
            logConsoleController.setConsoleProperty(LogConsoleController.SHOW_CONSOLE_PROPERTY, false);
        }

        updateLogConsoleMenu();
        toolMenu.add(logMenu);

        toolMenu.addSeparator();
        toolMenu.add(actionManager.getAction(ConfigurePreferencesAction.class).buildMenu());

        helpMenu.add(actionManager.getAction(AboutAction.class).buildMenu());
        helpMenu.add(actionManager.getAction(DocumentationAction.class).buildMenu());

        JMenuBar menuBar = new JMenuBar();

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(projectMenu);
        menuBar.add(toolMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    /**
     * Selects/deselects menu item, depending on status of log console
     */
    public void updateLogConsoleMenu() {
        logMenu.setSelected(logConsoleController.getConsoleProperty(LogConsoleController.SHOW_CONSOLE_PROPERTY));
    }

    /**
     * Plugs a component in the frame, between main area and status bar
     */
    public void setDockComponent(Component c) {
        if (dockComponent == c) {
            return;
        }

        if (dockComponent != null) {
            splitPane.setBottomComponent(null);
        }

        dockComponent = c;

        if (dockComponent != null) {
            splitPane.setBottomComponent(dockComponent);
        }

        splitPane.validate();
    }

    protected void initToolbar() {

        MainToolBar toolBar = new MainToolBar(actionManager);
        getContentPane().add(toolBar, BorderLayout.NORTH);

        // Hide some buttons when frame is too small
        int defaultBtnWidth = toolBar.getDefaultButtonWidth();
        addComponentListener(new ComponentAdapter() {
            private final int[] empty = {};
            private final int[] all = {6, 7, 8, 9, 10, 11, 12, 13, 14};
            private final int[] remove = {6, 7};
            private final int[] removeAndCopy = {6, 7, 8, 9, 10, 11};
            private final int[] undo = {12, 13, 14};
            private final int[] undoAndCopy = {8, 9, 10, 11, 12, 13, 14};

            @Override
            public void componentResized(ComponentEvent e) {
                int[] hidden, shown;
                if (getSize().width < (13 * defaultBtnWidth + 300)) {
                    hidden = all;
                    shown = empty;
                } else if (getSize().width < (16 * defaultBtnWidth + 300)) {
                    hidden = removeAndCopy;
                    shown = undo;
                } else if (getSize().width < (18 * defaultBtnWidth + 300)) {
                    hidden = remove;
                    shown = undoAndCopy;
                } else {
                    hidden = empty;
                    shown = all;
                }

                for (int i : hidden) {
                    toolBar.getComponentAtIndex(i).setVisible(false);
                }
                for (int i : shown) {
                    toolBar.getComponentAtIndex(i).setVisible(true);
                }
            }
        });
    }

    public ProjectView getEditorPanel() {
        return projectView;
    }

    public JLabel getStatus() {
        return status;
    }

    public void setEditorPanel(ProjectView projectView) {
        int oldLocation = splitPane.getDividerLocation();

        this.projectView = projectView;

        if (projectView != null) {
            splitPane.setTopComponent(projectView);
        } else {
            splitPane.setTopComponent(welcomePanel);
        }

        validate();
        splitPane.setDividerLocation(oldLocation);
    }

    /**
     * Notifies all listeners that recent file list has changed
     */
    public void fireRecentFileListChanged() {
        for (RecentFileListListener recentFileListener : recentFileListeners) {
            recentFileListener.recentFileListChanged();
        }
    }

}
