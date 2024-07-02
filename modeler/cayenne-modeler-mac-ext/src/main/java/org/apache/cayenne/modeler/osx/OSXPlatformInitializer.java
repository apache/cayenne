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
package org.apache.cayenne.modeler.osx;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Graphics;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.modeler.action.AboutAction;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.init.platform.PlatformInitializer;

public class OSXPlatformInitializer implements PlatformInitializer {

    @Inject
    protected ActionManager actionManager;

    public void initLookAndFeel() {

        // override some default styles and colors, assuming that Aqua theme will be used
        overrideUIDefaults();

        Desktop desktop = Desktop.getDesktop();

        desktop.setAboutHandler(e -> actionManager.getAction(AboutAction.class).showAboutDialog());
        desktop.setPreferencesHandler(e -> actionManager.getAction(ConfigurePreferencesAction.class).showPreferencesDialog());
        desktop.setQuitHandler((e, r) -> {
            if(!actionManager.getAction(ExitAction.class).exit()) {
                r.cancelQuit();
            } else {
                r.performQuit();
            }
        });
    }

    private void overrideUIDefaults() {
        Color lightGrey = new Color(0xEEEEEE);
        Color darkGrey  = new Color(225, 225, 225);
        Border darkBorder = BorderFactory.createLineBorder(darkGrey);

        UIManager.put("ToolBarSeparatorUI",           OSXToolBarSeparatorUI.class.getName());
        UIManager.put("PanelUI",                      OSXPanelUI.class.getName());
        // next two is custom-made for Cayenne's MainToolBar
        UIManager.put("ToolBar.background",           lightGrey);
        UIManager.put("MainToolBar.background",       lightGrey);
        UIManager.put("MainToolBar.border",           BorderFactory.createEmptyBorder(0, 7, 0, 7));
        UIManager.put("ToolBar.border",               darkBorder);
        UIManager.put("ScrollPane.border",            darkBorder);
        UIManager.put("Table.scrollPaneBorder",       darkBorder);
        UIManager.put("SplitPane.border",             BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.background",         darkGrey);
        UIManager.put("Tree.rendererFillBackground",  Boolean.TRUE);
        UIManager.put("Tree.paintLines",              Boolean.FALSE);
        UIManager.put("ComboBox.background",          Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", darkGrey);
        UIManager.put("ComboBox.selectionForeground", Color.BLACK);
        UIManager.put("CheckBox.background",          Color.WHITE);
        UIManager.put("Tree.background",              Color.WHITE);
        UIManager.put("Tree.selectionForeground",     Color.BLACK);
        UIManager.put("Tree.selectionBackground",     lightGrey);
        UIManager.put("Tree.selectionBorderColor",    lightGrey);
        UIManager.put("Table.selectionForeground",    Color.BLACK);
        UIManager.put("Table.selectionBackground",    lightGrey);
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
        UIManager.put("CheckBoxHeader.border",          BorderFactory.createEmptyBorder(0, 9, 0, 0));

        // MacOS BigSur needs additional style tweaking for the tabs active state
        OSXVersion version = OSXVersion.fromSystemProperties();
        if(version.gt(OSXVersion.CATALINA)) {
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
        UIManager.put("MenuItem.selectionForeground",       Color.BLACK);
    }

    public void setupMenus(JFrame frame) {
        // set additional look and feel for the window
        frame.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);

        Set<Action> removeActions = new HashSet<>();
        removeActions.add(actionManager.getAction(ExitAction.class));
        removeActions.add(actionManager.getAction(AboutAction.class));
        removeActions.add(actionManager.getAction(ConfigurePreferencesAction.class));

        JMenuBar menuBar = frame.getJMenuBar();
        for (Component menu : menuBar.getComponents()) {
            if (menu instanceof JMenu) {
                JMenu jMenu = (JMenu) menu;

                Component[] menuItems = jMenu.getPopupMenu().getComponents();
                for (int i = 0; i < menuItems.length; i++) {

                    if (menuItems[i] instanceof JMenuItem) {
                        JMenuItem jMenuItem = (JMenuItem) menuItems[i];

                        if (removeActions.contains(jMenuItem.getAction())) {
                            jMenu.remove(jMenuItem);

                            // this algorithm is pretty lame, but it works for
                            // the current (as of 08.2010) menu layout
                            if (i > 0
                                    && i == menuItems.length - 1
                                    && menuItems[i - 1] instanceof JPopupMenu.Separator) {
                                jMenu.remove(i - 1);
                            }
                        }
                    }
                }
            }
        }
    }
}
