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
package org.apache.cayenne.modeler.osx;

import java.awt.Color;
import java.awt.Component;
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
import javax.swing.border.Border;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.modeler.action.AboutAction;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.init.platform.PlatformInitializer;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent;
import com.apple.eawt.Application;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

public class OSXPlatformInitializer implements PlatformInitializer {

    @Inject
    protected ActionManager actionManager;

    public void initLookAndFeel() {

        // override some default styles and colors, assuming that Aqua theme will be used
        overrideUIDefaults();

        // configure special Mac menu handlers
        Application app = Application.getApplication();
        app.setAboutHandler(new AboutHandler() {
            @Override
            public void handleAbout(AppEvent.AboutEvent aboutEvent) {
                actionManager.getAction(AboutAction.class).showAboutDialog();
            }
        });

        app.setPreferencesHandler(new PreferencesHandler() {
            @Override
            public void handlePreferences(AppEvent.PreferencesEvent preferencesEvent) {
                actionManager.getAction(ConfigurePreferencesAction.class).showPreferencesDialog();
            }
        });

        app.setQuitHandler(new QuitHandler() {
            @Override
            public void handleQuitRequestWith(AppEvent.QuitEvent quitEvent, QuitResponse quitResponse) {
                if(!actionManager.getAction(ExitAction.class).exit()) {
                    quitResponse.cancelQuit();
                }
            }
        });
    }

    private void overrideUIDefaults() {
        Color lightGrey = new Color(0xEEEEEE);
        Color darkGrey  = new Color(225, 225, 225);
        Border darkBorder = BorderFactory.createLineBorder(darkGrey);

        UIManager.put("ToolBarSeparatorUI",          OSXToolBarSeparatorUI.class.getName());
        UIManager.put("PanelUI",                     OSXPanelUI.class.getName());
        // next two is custom made for Cayenne's MainToolBar
        UIManager.put("MainToolBar.background",      UIManager.get("ToolBar.background"));
        UIManager.put("MainToolBar.border",          BorderFactory.createEmptyBorder(0, 7, 0, 7));
        UIManager.put("ToolBar.background",          lightGrey);
        UIManager.put("ToolBar.border",              darkBorder);
        UIManager.put("ScrollPane.border",           darkBorder);
        UIManager.put("Table.scrollPaneBorder",      darkBorder);
        UIManager.put("SplitPane.border",            BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.background",        darkGrey);
        UIManager.put("Tree.rendererFillBackground", Boolean.TRUE);
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
