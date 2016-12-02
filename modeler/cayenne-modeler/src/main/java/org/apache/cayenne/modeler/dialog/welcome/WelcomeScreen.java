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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

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
import org.apache.cayenne.modeler.util.ModelerUtil;

/**
 * Welcome screen (CAY-894) is a panel shown when no project is open. User can quickly
 * create new project or open an existing one.
 */
public class WelcomeScreen extends JPanel implements RecentFileListListener, RecentFileListRenderer.OnFileClickListener {

    /**
     * List of recent projects
     */
    private JList<String> recentsList;

    private JPanel buttonsPane;

    private JPanel fileListPane;

    public WelcomeScreen() {
        initView();
    }

    /**
     * Creates all neccesary components
     */
    protected void initView() {
        setLayout(new GridBagLayout());
        initButtonsPane();
        initFileListPane();
    }

    private void initFileListPane() {
        final ImageIcon rightPaneImg = ModelerUtil.buildIcon("welcome/welcome-screen-right-bg.jpg");
        fileListPane = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(rightPaneImg.getImage(), 0, 0, null);
            }
        };
        fileListPane.setPreferredSize(new Dimension(902, 592));
        fileListPane.setOpaque(false);

        initRecentList();

        add(fileListPane);
    }

    private void initButtonsPane() {
        final ImageIcon leftPaneImg = ModelerUtil.buildIcon("welcome/welcome-screen-left-bg.jpg");
        buttonsPane = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(leftPaneImg.getImage(), 0, 0, null);
            }
        };
        buttonsPane.setPreferredSize(new Dimension(198, 592));
        buttonsPane.setOpaque(false);

        initNewButton();
        initOpenButton();

        add(buttonsPane);
    }


    private void initNewButton() {
        ImageIcon newIcon = ModelerUtil.buildIcon("welcome/welcome-screen-new-btn.png");
        ImageIcon newHoverIcon = ModelerUtil.buildIcon("welcome/welcome-screen-new-btn-hover.png");
        JButton newButton = createButton(newIcon, newHoverIcon);
        newButton.setLocation(24, 488);
        newButton.addActionListener(Application
                .getInstance()
                .getActionManager()
                .getAction(NewProjectAction.class));
        buttonsPane.add(newButton);
    }

    private void initOpenButton() {
        ImageIcon newIcon = ModelerUtil.buildIcon("welcome/welcome-screen-open-btn.png");
        ImageIcon newHoverIcon = ModelerUtil.buildIcon("welcome/welcome-screen-open-btn-hover.png");
        JButton openButton = createButton(newIcon, newHoverIcon);
        openButton.setLocation(24, 532);
        openButton.addActionListener(Application
                .getInstance()
                .getActionManager()
                .getAction(NewProjectAction.class));
        buttonsPane.add(openButton);
    }

    private void initRecentList() {
        recentsList = new JList<>();
        recentsList.setOpaque(false);
        int padding = 20;
        recentsList.setLocation(padding, padding);
        recentsList.setSize(902 - 2 * padding, 592 - 2 * padding);
        Font fontOld = recentsList.getFont();
        Font font = new Font(fontOld.getFontName(), Font.PLAIN, 12);
        recentsList.setFont(font);
//        recentsList.setFixedCellHeight(24);
        RecentFileListRenderer cellRenderer = new RecentFileListRenderer(recentsList, this);
        recentsList.setCellRenderer(cellRenderer);
        fileListPane.add(recentsList);
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
        recentsList.setModel(new RecentFileListModel(arr));
    }

}
