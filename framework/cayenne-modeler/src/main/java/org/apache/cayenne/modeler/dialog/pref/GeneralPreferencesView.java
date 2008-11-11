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

package org.apache.cayenne.modeler.dialog.pref;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.*;

/**
 */
public class GeneralPreferencesView extends JPanel {

    protected JTextField saveInterval;
    protected JLabel saveIntervalLabel;
    protected EncodingSelectorView encodingSelector;
    protected JLabel encodingSelectorLabel;
    protected JCheckBox autoLoadProjectBox;
    protected JCheckBox deletePromptBox;

    public GeneralPreferencesView() {
        this.saveInterval = new JTextField();
        this.encodingSelector = new EncodingSelectorView();
        this.saveIntervalLabel = new JLabel("Preferences Save Interval (sec):");
        this.encodingSelectorLabel = new JLabel("File Encoding:");
        this.autoLoadProjectBox = new JCheckBox("Automatically Load Last Opened Project");
        this.deletePromptBox = new JCheckBox("Always Delete Items Without Prompt");

        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, 30dlu, 3dlu, fill:70dlu",
                "p, 3dlu, p, 12dlu, p, 30dlu, p, 12dlu, p, 3dlu, p, fill:40dlu:grow");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.addSeparator("General Preferences", cc.xyw(1, 1, 5));
        builder.add(saveIntervalLabel, cc.xy(1, 3));
        builder.add(saveInterval, cc.xy(3, 3));
        builder.add(encodingSelectorLabel, cc.xy(1, 5));
        builder.add(encodingSelector, cc.xywh(3, 5, 3, 3));
        builder.add(autoLoadProjectBox, cc.xy(1, 7, CellConstraints.LEFT, CellConstraints.DEFAULT));

        builder.addSeparator("Editor Preferences", cc.xyw(1, 9, 5));
        builder.add(deletePromptBox, cc.xy(1, 11, CellConstraints.LEFT, CellConstraints.DEFAULT));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void setEnabled(boolean b) {
        super.setEnabled(b);
        saveInterval.setEnabled(b);
        saveIntervalLabel.setEnabled(b);
        encodingSelector.setEnabled(b);
        encodingSelectorLabel.setEnabled(b);
        autoLoadProjectBox.setEnabled(b);
        deletePromptBox.setEnabled(b);
    }

    public JTextField getSaveInterval() {
        return saveInterval;
    }

    public EncodingSelectorView getEncodingSelector() {
        return encodingSelector;
    }

    public JCheckBox getAutoLoadProject() {
        return autoLoadProjectBox;
    }

    public JCheckBox getDeletePrompt() {
        return deletePromptBox;
    }
}
