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

package org.apache.cayenne.modeler.ui.preferences.all;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.*;

public class AllPreferencesView extends JPanel {

    private final JButton copyAllButton;
    private final JButton deleteAllButton;

    public AllPreferencesView() {
        this.copyAllButton = new JButton("Copy All to Clipboard");
        this.deleteAllButton = new JButton("Delete All");

        FormLayout layout = new FormLayout(
                "fill:default:grow, center:pref, fill:default:grow",
                "fill:default:grow, p, 6dlu, p, fill:default:grow");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.add(copyAllButton, cc.xy(2, 2));
        builder.add(deleteAllButton, cc.xy(2, 4));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JButton getCopyAllButton() {
        return copyAllButton;
    }

    public JButton getDeleteAllButton() {
        return deleteAllButton;
    }
}
