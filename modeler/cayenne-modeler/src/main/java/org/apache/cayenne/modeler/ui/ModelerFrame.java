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

import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.project.ProjectView;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.logconsole.LogConsoleController;
import org.apache.cayenne.modeler.ui.welcome.WelcomeScreen;
import org.apache.cayenne.modeler.pref.ComponentGeometry;
import org.apache.cayenne.modeler.toolkit.border.TopBorder;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Main frame of CayenneModeler GUI
 */
public class ModelerFrame extends JFrame {

    private final GlobalActions globalActions;

    private final JSplitPane splitPane;
    private final JLabel status;
    private final WelcomeScreen welcomePanel;
    private final ModelerMenuBar menuBar;

    private ProjectView projectView;
    private Component dockComponent;

    public ModelerFrame(GlobalActions globalActions, LogConsoleController logConsoleController) {
        this.globalActions = globalActions;

        setIconImage(IconFactory.buildIcon("CayenneModeler.png").getImage());
        getContentPane().setLayout(new BorderLayout());
        this.menuBar = new ModelerMenuBar(globalActions, logConsoleController);
        setJMenuBar(menuBar);
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
        this.menuBar.addRecentFileListener(welcomePanel);

        fireRecentFileListChanged(); // start filling list in welcome screen and in menu

        setProjectView(null);
    }

    /**
     * Selects/deselects menu item, depending on status of log console
     */
    public void updateLogConsoleMenu() {
        menuBar.updateLogConsoleMenu();
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

        MainToolBar toolBar = new MainToolBar(globalActions);
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

    public JLabel getStatus() {
        return status;
    }

    public ProjectView getProjectView() {
        return projectView;
    }

    public void setProjectView(ProjectView projectView) {
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
        menuBar.fireRecentFileListChanged();
    }
}
