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
package org.apache.cayenne.modeler.win;

import com.jgoodies.looks.windows.WindowsLookAndFeel;
import org.apache.cayenne.modeler.init.platform.PlatformInitializer;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.BorderFactory;
import java.awt.Color;

public class WinPlatformInitializer implements PlatformInitializer {

    private static Logger logger = LoggerFactory.getLogger(WinPlatformInitializer.class);

    public void setupMenus(JFrame frame) {
    }

    public void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(WindowsLookAndFeel.class.getName());
            // override some default styles and colors
            overrideUIDefaults();
        } catch (Exception e) {
            logger.warn("Error installing L&F: " + WindowsLookAndFeel.class.getName(), e);
        }
    }

    private void overrideUIDefaults() {
        Color darkGrey = new Color(225, 225, 225);

        UIManager.put("Tree.expandedIcon",      ModelerUtil.buildIcon("icon-arrow-open.png"));
        UIManager.put("Tree.collapsedIcon",     ModelerUtil.buildIcon("icon-arrow-closed.png"));
        UIManager.put("Tree.paintLines",        Boolean.FALSE);
        UIManager.put("Tree.drawDashedFocusIndicator",  Boolean.FALSE);
        UIManager.put("Tree.selectionBorderColor",      UIManager.get("Tree.selectionBackground"));
        UIManager.put("ScrollPane.border",      BorderFactory.createEmptyBorder());
        UIManager.put("Table.scrollPaneBorder", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.border",       BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.background",   darkGrey);
        UIManager.put("Separator.background",   darkGrey);
        UIManager.put("Separator.foreground",   darkGrey);
        UIManager.put("Separator.opaque",       Boolean.TRUE);
    }
}
