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
package org.apache.cayenne.modeler.platform.mac;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.platform.UIInitializer;
import org.apache.cayenne.modeler.ui.action.AboutAction;
import org.apache.cayenne.modeler.ui.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.ui.action.ExitAction;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class MacUIInitializer implements UIInitializer {

    @Override
    public void beforeSwingLaunch() {
        // must be set before Aqua L&F initializes — it reads this property during init
        System.setProperty("apple.awt.application.name", "CayenneModeler");

        // override some default styles and colors, assuming that Aqua theme will be used

        Color lightGrey = new Color(0xEEEEEE);
        Color darkGrey = new Color(225, 225, 225);
        Border darkBorder = BorderFactory.createLineBorder(darkGrey);

        UIManager.put("ToolBarSeparatorUI", MacToolBarSeparatorUI.class.getName());
        UIManager.put("PanelUI", MacPanelUI.class.getName());
        // next two is custom-made for Cayenne's MainToolBar
        UIManager.put("ToolBar.background", lightGrey);
        UIManager.put("MainToolBar.background", lightGrey);
        UIManager.put("MainToolBar.border", BorderFactory.createEmptyBorder(0, 7, 0, 7));
        UIManager.put("ToolBar.border", darkBorder);
        UIManager.put("ScrollPane.border", darkBorder);
        UIManager.put("Table.scrollPaneBorder", darkBorder);
        UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.background", darkGrey);
        UIManager.put("Tree.rendererFillBackground", Boolean.TRUE);
        UIManager.put("Tree.paintLines", Boolean.FALSE);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", darkGrey);
        UIManager.put("ComboBox.selectionForeground", Color.BLACK);
        UIManager.put("CheckBox.background", Color.WHITE);
        UIManager.put("Tree.background", Color.WHITE);
        UIManager.put("Tree.selectionForeground", Color.BLACK);
        UIManager.put("Tree.selectionBackground", lightGrey);
        UIManager.put("Tree.selectionBorderColor", lightGrey);
        UIManager.put("Table.selectionForeground", Color.BLACK);
        UIManager.put("Table.selectionBackground", lightGrey);
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
        UIManager.put("CheckBoxHeader.border", BorderFactory.createEmptyBorder(0, 9, 0, 0));

        // MacOS BigSur needs additional style tweaking for the tabs active state
        MacOSVersion version = MacOSVersion.fromSystemProperties();
        if (version.gt(MacOSVersion.CATALINA)) {
            UIManager.put("TabbedPane.selectedTabTitlePressedColor", Color.BLACK);
            UIManager.put("TabbedPane.selectedTabTitleNormalColor", Color.BLACK);
            UIManager.put("TabbedPane.selectedTabTitleShadowDisabledColor", new Color(0, 0, 0, 0));
            UIManager.put("TabbedPane.selectedTabTitleShadowNormalColor", new Color(0, 0, 0, 0));
        }

        Border backgroundPainter = new AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(lightGrey);
                g.fillRect(0, 0, width - 1, height - 1);
            }
        };
        UIManager.put("MenuItem.selectedBackgroundPainter", backgroundPainter);
        UIManager.put("MenuItem.selectionForeground", Color.BLACK);
    }

    @Override
    public void afterFrameCreated(Application app) {

        // set additional look and feel for the window
        app.getFrame().getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);

        // Only relocate About/Preferences/Quit to the macOS application menu when the
        // screen menu bar is actually in use. Otherwise the JMenuBar stays inside the
        // window frame, and stripping these items would make them inaccessible.
        if (!Boolean.getBoolean("apple.laf.useScreenMenuBar")) {
            return;
        }

        GlobalActions globalActions = app.getActionManager();

        Desktop desktop = Desktop.getDesktop();
        desktop.setAboutHandler(e -> globalActions.getAction(AboutAction.class).showAboutDialog());
        desktop.setPreferencesHandler(e -> globalActions.getAction(ConfigurePreferencesAction.class).showPreferencesDialog());
        desktop.setQuitHandler((e, r) -> {
            if (!globalActions.getAction(ExitAction.class).exit()) {
                r.cancelQuit();
            } else {
                r.performQuit();
            }
        });

        Set<Action> removeActions = new HashSet<>();
        removeActions.add(globalActions.getAction(ExitAction.class));
        removeActions.add(globalActions.getAction(AboutAction.class));
        removeActions.add(globalActions.getAction(ConfigurePreferencesAction.class));

        JMenuBar menuBar = app.getFrame().getJMenuBar();
        for (Component c : menuBar.getComponents()) {
            if (c instanceof JMenu menu) {

                Component[] menuItems = menu.getPopupMenu().getComponents();
                for (int i = 0; i < menuItems.length; i++) {

                    if (menuItems[i] instanceof JMenuItem menuItem) {

                        if (removeActions.contains(menuItem.getAction())) {
                            menu.remove(menuItem);

                            // this algorithm is pretty lame, but it works for
                            // the current (as of 08.2010) menu layout
                            if (i > 0
                                    && i == menuItems.length - 1
                                    && menuItems[i - 1] instanceof JPopupMenu.Separator) {
                                menu.remove(i - 1);
                            }
                        }
                    }
                }
            }
        }
    }
}
