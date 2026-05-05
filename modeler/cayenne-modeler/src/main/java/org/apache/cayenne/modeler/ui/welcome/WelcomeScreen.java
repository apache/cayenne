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

package org.apache.cayenne.modeler.ui.welcome;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.RecentProjectsPrefs;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.action.NewProjectAction;
import org.apache.cayenne.modeler.ui.action.OpenProjectAction;
import org.apache.cayenne.modeler.event.model.RecentFileListListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

/**
 * A panel shown when no project is open. User can quickly create new project or open an existing one.
 */
public class WelcomeScreen extends JScrollPane implements RecentFileListListener, RecentFileListRenderer.OnFileClickListener {

    private final Application application;
    private final GlobalActions actionManager;

    private final JList<String> recentProjectsList;
    private final JPanel buttonsPanel;
    private final JPanel mainPanel;

    public WelcomeScreen(Application application) {
        this.application = application;
        this.actionManager = application.getActionManager();
        this.mainPanel = new WelcomeScreenMainPanel();
        this.buttonsPanel = new BackgroundPanel("welcome/welcome-screen-left-bg.jpg");
        this.recentProjectsList = new JList<>();

        initLayout();
    }

    private void initLayout() {
        setBorder(BorderFactory.createEmptyBorder());
        initButtonsPane();
        initFileListPane();
        setViewportView(mainPanel);
    }

    private void initFileListPane() {
        JPanel fileListPanel = new BackgroundPanel("welcome/welcome-screen-right-bg.jpg");

        int padding = 20;
        recentProjectsList.setOpaque(false);
        recentProjectsList.setLocation(padding, padding);
        recentProjectsList.setSize(
                fileListPanel.getWidth() - 2 * padding,
                fileListPanel.getHeight() - 2 * padding
        );
        recentProjectsList.setCellRenderer(new RecentFileListRenderer(recentProjectsList, this));

        fileListPanel.add(recentProjectsList);
        mainPanel.add(fileListPanel);
    }

    private void initButtonsPane() {
        int padding = 24;
        int buttonHeight = 36;

        int openButtonY = buttonsPanel.getHeight() - padding - buttonHeight; // buttons layout from bottom
        int newButtonY = openButtonY - 10 - buttonHeight; // 10px - space between buttons
        initButton("open", openButtonY, OpenProjectAction.class);
        initButton("new", newButtonY, NewProjectAction.class);

        mainPanel.add(buttonsPanel);
    }

    private void initButton(String name, int y, Class<? extends Action> actionClass) {
        ImageIcon icon = IconFactory.buildIcon("welcome/welcome-screen-" + name + "-btn.png");
        ImageIcon hoverIcon = IconFactory.buildIcon("welcome/welcome-screen-" + name + "-btn-hover.png");
        JButton button = createButton(icon, hoverIcon);
        button.setLocation(24, y); // 24px - button left & right padding
        button.addActionListener(actionManager.getAction(actionClass));
        buttonsPanel.add(button);
    }

    @Override
    public void onFileSelect(File file) {
        actionManager.getAction(OpenProjectAction.class).performAction(new ActionEvent(file, 0, null));
    }

    private JButton createButton(Icon outIcon, Icon overIcon) {
        JButton button = new JButton();

        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);

        button.setPressedIcon(overIcon);
        button.setRolloverIcon(overIcon);
        button.setIcon(outIcon);

        button.setSize(outIcon.getIconWidth(), outIcon.getIconHeight());

        return button;
    }

    @Override
    public void recentFileListChanged() {
        List<File> arr = RecentProjectsPrefs.of(application.getPreferencesRepository()).getFiles();
        recentProjectsList.setModel(new RecentFileListModel(arr));
    }
}
