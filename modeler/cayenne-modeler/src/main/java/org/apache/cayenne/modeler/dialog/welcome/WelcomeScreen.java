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

package org.apache.cayenne.modeler.dialog.welcome;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ModelerPreferences;
import org.apache.cayenne.modeler.action.NewProjectAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.event.RecentFileListListener;
import org.apache.cayenne.modeler.util.BackgroundPanel;
import org.apache.cayenne.modeler.util.ModelerUtil;

/**
 * Welcome screen (CAY-894) is a panel shown when no project is open. User can quickly
 * create new project or open an existing one.
 */
public class WelcomeScreen extends JPanel implements RecentFileListListener, RecentFileListRenderer.OnFileClickListener {

    /**
     * List of recent projects
     */
    private JList<String> recentProjectsList;

    private JPanel buttonsPanel;

    public WelcomeScreen() {
        initView();
    }

    /**
     * Creates all necessary components
     */
    protected void initView() {
        setLayout(new GridBagLayout());
        initButtonsPane();
        initFileListPane();
    }

    private void initFileListPane() {
        JPanel fileListPanel = new BackgroundPanel("welcome/welcome-screen-right-bg.jpg");

        final int padding = 20;
        recentProjectsList = new JList<>();
        recentProjectsList.setOpaque(false);
        recentProjectsList.setLocation(padding, padding);
        recentProjectsList.setSize(
                fileListPanel.getWidth() - 2 * padding,
                fileListPanel.getHeight() - 2 * padding
        );
        recentProjectsList.setCellRenderer(new RecentFileListRenderer(recentProjectsList, this));

        fileListPanel.add(recentProjectsList);
        add(fileListPanel);
    }

    private void initButtonsPane() {
        final int padding = 24; // bottom padding for buttons
        final int buttonHeight = 36;

        buttonsPanel = new BackgroundPanel("welcome/welcome-screen-left-bg.jpg");
        int openButtonY = buttonsPanel.getHeight() - padding - buttonHeight; // buttons layout from bottom
        int newButtonY = openButtonY - 10 - buttonHeight; // 10px - space between buttons
        initButton("open", openButtonY, OpenProjectAction.class);
        initButton("new", newButtonY, NewProjectAction.class);

        add(buttonsPanel);
    }

    private void initButton(String name, int y,  Class<? extends Action> actionClass) {
        ImageIcon icon = ModelerUtil.buildIcon("welcome/welcome-screen-"+name+"-btn.png");
        ImageIcon hoverIcon = ModelerUtil.buildIcon("welcome/welcome-screen-"+name+"-btn-hover.png");
        JButton button = createButton(icon, hoverIcon);
        button.setLocation(24, y); // 24px - button left & right padding
        button.addActionListener(Application
                .getInstance()
                .getActionManager()
                .getAction(actionClass));
        buttonsPanel.add(button);
    }

    @Override
    public void onFileSelect(String fileName) {
        File file = new File(fileName);
        ActionEvent event = new ActionEvent(file, 0, null);
        // Fire an action with the file as source
        Application.getInstance()
                .getActionManager()
                .getAction(OpenProjectAction.class)
                .performAction(event);
    }

    /**
     * Creates welcome screen-specific button
     */
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
        List<String> arr = ModelerPreferences.getLastProjFiles();
        recentProjectsList.setModel(new RecentFileListModel(arr));
    }
}
