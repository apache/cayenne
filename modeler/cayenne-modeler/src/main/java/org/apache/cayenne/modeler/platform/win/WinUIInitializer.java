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
package org.apache.cayenne.modeler.platform.win;

import com.formdev.flatlaf.FlatLightLaf;
import com.jgoodies.forms.util.LayoutStyle;
import org.apache.cayenne.modeler.platform.FlatLafLayoutStyle;
import org.apache.cayenne.modeler.platform.UIInitializer;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;

import javax.swing.*;
import java.awt.*;

public class WinUIInitializer implements UIInitializer {

    @Override
    public void beforeSwingLaunch() {
        FlatLightLaf.setup();
        LayoutStyle.setCurrent(FlatLafLayoutStyle.INSTANCE);
        // override some default styles and colors
        overrideUIDefaults();
    }

    private void overrideUIDefaults() {
        Color darkGrey = new Color(203, 203, 203);

        UIManager.put("Tree.expandedIcon", IconFactory.buildIcon("icon-arrow-open.png"));
        UIManager.put("Tree.collapsedIcon", IconFactory.buildIcon("icon-arrow-closed.png"));
        UIManager.put("Tree.paintLines", Boolean.FALSE);
        UIManager.put("Tree.drawDashedFocusIndicator", Boolean.FALSE);
        UIManager.put("Tree.selectionBackground", darkGrey);
        UIManager.put("Tree.selectionForeground", Color.BLACK);
        UIManager.put("Tree.selectionBorderColor", UIManager.get("Tree.selectionBackground"));
        UIManager.put("Table.selectionForeground", Color.BLACK);
        UIManager.put("Table.selectionBackground", darkGrey);
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("Table.scrollPaneBorder", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("CheckBoxHeader.border", BorderFactory.createEmptyBorder(0, 15, 0, 0));
        UIManager.put("SplitPane.background", darkGrey);
        UIManager.put("Separator.background", darkGrey);
        UIManager.put("Separator.foreground", darkGrey);
        UIManager.put("Separator.opaque", Boolean.TRUE);
        UIManager.put("MenuItem.selectionBackground", darkGrey);
        UIManager.put("CheckBoxMenuItem.selectionBackground", darkGrey);
        UIManager.put("RadioButtonMenuItem.selectionBackground", darkGrey);
        UIManager.put("MenuItem.selectionForeground", Color.BLACK);
        UIManager.put("CheckBoxMenuItem.selectionForeground", Color.BLACK);
        UIManager.put("RadioButtonMenuItem.selectionForeground", Color.BLACK);
        UIManager.put("ToolBar.buttonMargin", new Insets(4, 8, 4, 8));
        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("Table.showVerticalLines", Boolean.TRUE);
    }
}
