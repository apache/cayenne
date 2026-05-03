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

package org.apache.cayenne.modeler.ui.preferences.general;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class GeneralPreferencesView extends JPanel {

    private final JComboBox<String> encodingChoices;
    private final JCheckBox autoLoadProject;
    private final JCheckBox noDeletePrompt;
    private final String systemEncoding;
    private final String defaultLabel;

    public GeneralPreferencesView(
            String[] supportedEncodings,
            String currentEncoding,
            boolean autoLoadProject,
            boolean noDeletePrompt) {

        this.systemEncoding = Charset.defaultCharset().name();
        this.defaultLabel = systemEncoding + " (default)";

        String[] encodingLabels = createEncodingLabels(supportedEncodings);
        this.encodingChoices = new JComboBox<>(new DefaultComboBoxModel<>(encodingLabels));
        selectEncoding(currentEncoding);

        this.autoLoadProject = new JCheckBox();
        this.autoLoadProject.setSelected(autoLoadProject);
        this.noDeletePrompt = new JCheckBox();
        this.noDeletePrompt.setSelected(noDeletePrompt);

        JLabel encodingSelectorLabel = new JLabel("File Encoding:");
        JLabel autoLoadProjectLabel = new JLabel("Auto-Load Last Project:");
        JLabel noDeletePromptLabel = new JLabel("Delete Without Prompt:");

        FormLayout layout = new FormLayout(
                "right:pref, 3dlu, fill:120dlu, default:grow",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, fill:40dlu:grow");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.addSeparator("General Preferences", cc.xyw(1, 1, 4));

        builder.add(encodingSelectorLabel, cc.xy(1, 3));
        builder.add(encodingChoices, cc.xy(3, 3));
        builder.add(autoLoadProjectLabel, cc.xy(1, 5));
        builder.add(this.autoLoadProject, cc.xy(3, 5, CellConstraints.LEFT, CellConstraints.DEFAULT));
        builder.add(noDeletePromptLabel, cc.xy(1, 7));
        builder.add(this.noDeletePrompt, cc.xy(3, 7, CellConstraints.LEFT, CellConstraints.DEFAULT));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public String getSelectedEncoding() {
        Object selected = encodingChoices.getSelectedItem();
        return (selected == null || defaultLabel.equals(selected)) ? systemEncoding : selected.toString();
    }

    public boolean isAutoLoadProjectSelected() {
        return autoLoadProject.isSelected();
    }

    public boolean isNoDeletePromptSelected() {
        return noDeletePrompt.isSelected();
    }

    private String[] createEncodingLabels(String[] supportedEncodings) {
        List<String> labels = new ArrayList<>(Arrays.asList(supportedEncodings));
        labels.remove(systemEncoding);
        labels.add(defaultLabel);
        Collections.sort(labels);
        return labels.toArray(new String[0]);
    }

    private void selectEncoding(String encoding) {
        if (encoding == null || encoding.isEmpty() || encoding.equals(systemEncoding)) {
            encodingChoices.setSelectedItem(defaultLabel);
        } else {
            encodingChoices.setSelectedItem(encoding);
        }
    }
}
