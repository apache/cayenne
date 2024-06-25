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
package org.apache.cayenne.modeler.generic;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.UIManager;

import org.apache.cayenne.modeler.init.platform.PlatformInitializer;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

public class GenericPlatformInitializer implements PlatformInitializer {

    private static Logger logger = LoggerFactory.getLogger(GenericPlatformInitializer.class);

    static final String DEFAULT_LAF_NAME = PlasticXPLookAndFeel.class.getName();

    // note that another theme - "Desert Blue" doesn't support Chinese and
    // Japanese chars
    static final String DEFAULT_THEME_NAME = "Sky Bluer";

    public void setupMenus(JFrame frame) {
        // noop - default menus are fine
    }

    public void initLookAndFeel() {

        PlasticTheme theme = findTheme();

        if (theme != null) {
            PlasticLookAndFeel.setCurrentTheme(theme);
        }

        try {
            UIManager.setLookAndFeel(DEFAULT_LAF_NAME);
            // override some default styles and colors
            overrideUIDefaults();
        } catch (Exception e) {
            logger.warn("Error installing L&F: " + DEFAULT_LAF_NAME, e);
        }
    }

    private void overrideUIDefaults() {
        Color greyHighlight = new Color(0xCBCBCB);

        UIManager.put("ButtonUI",                       GenericButtonUI.class.getName());
        UIManager.put("HiResGrayFilterEnabled",         Boolean.TRUE);
        UIManager.put("Tree.expandedIcon",              ModelerUtil.buildIcon("icon-arrow-open.png"));
        UIManager.put("Tree.collapsedIcon",             ModelerUtil.buildIcon("icon-arrow-closed.png"));
        UIManager.put("Tree.paintLines",                Boolean.FALSE);
        UIManager.put("Tree.selectionForeground",       Color.BLACK);
        UIManager.put("Tree.selectionBackground",       greyHighlight);
        UIManager.put("Tree.selectionBorderColor",      UIManager.get("Tree.selectionBackground"));
        UIManager.put("Table.selectionForeground",      Color.BLACK);
        UIManager.put("Table.selectionBackground",      greyHighlight);
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.border",              BorderFactory.createEmptyBorder());
        UIManager.put("Table.scrollPaneBorder",         BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.border",               BorderFactory.createEmptyBorder());
        UIManager.put("ToolBar.border",                 BorderFactory.createEmptyBorder(1, 1, 1, 1));
        UIManager.put("CheckBoxHeader.border",          BorderFactory.createEmptyBorder(0, 15, 0, 0));
        UIManager.put("MenuItem.selectionBackground",            greyHighlight);
        UIManager.put("CheckBoxMenuItem.selectionBackground",    greyHighlight);
        UIManager.put("RadioButtonMenuItem.selectionBackground", greyHighlight);
        UIManager.put("MenuItem.selectionForeground",            Color.BLACK);
        UIManager.put("CheckBoxMenuItem.selectionForeground",    Color.BLACK);
        UIManager.put("RadioButtonMenuItem.selectionForeground", Color.BLACK);
        // this one is custom for MainToolBar
        UIManager.put("MainToolBar.border",             BorderFactory.createLineBorder(Color.GRAY));
    }

    protected PlasticTheme findTheme() {

        for (Object object : PlasticLookAndFeel.getInstalledThemes()) {
            PlasticTheme theme = (PlasticTheme) object;
            if (DEFAULT_THEME_NAME.equals(theme.getName())) {
                return theme;
            }
        }
        return null;
    }

}
