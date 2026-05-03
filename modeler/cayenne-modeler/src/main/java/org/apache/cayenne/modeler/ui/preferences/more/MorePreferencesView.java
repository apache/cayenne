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

package org.apache.cayenne.modeler.ui.preferences.more;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.*;

public class MorePreferencesView extends JPanel {

    public MorePreferencesView(MorePreferencesController controller) {
        JButton copyAllButton = new JButton("Copy All to Clipboard");
        JButton resetToDefaultsButton = new JButton("Reset to Defaults");
        JCheckBox importLegacyPreferencesCheckBox =
                new JCheckBox("Import older Modeler preferences if available", true);
        importLegacyPreferencesCheckBox.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        copyAllButton.addActionListener(e -> controller.copyAllClicked());
        resetToDefaultsButton.addActionListener(
                e -> controller.resetToDefaultsClicked(importLegacyPreferencesCheckBox.isSelected()));

        FormLayout layout = new FormLayout(
                "12dlu, default:grow",
                "p, 6dlu, p, 12dlu, p, 6dlu, p, 3dlu, p, fill:default:grow");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("Export Preferences", cc.xyw(1, 1, 2));
        builder.add(copyAllButton, cc.xy(2, 3, CellConstraints.LEFT, CellConstraints.DEFAULT));

        builder.addSeparator("Reset Preferences", cc.xyw(1, 5, 2));
        builder.add(resetToDefaultsButton, cc.xy(2, 7, CellConstraints.LEFT, CellConstraints.DEFAULT));
        builder.add(importLegacyPreferencesCheckBox, cc.xy(2, 9, CellConstraints.LEFT, CellConstraints.DEFAULT));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }
}
