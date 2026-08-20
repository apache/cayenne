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
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.adapters.GeneralPrefs;
import org.apache.cayenne.modeler.toolkit.AppPanel;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GeneralPrefsPanel extends AppPanel {

    static final String[] STANDARD_ENCODINGS = {
            "ISO-8859-1", "US-ASCII", "UTF-8", "UTF-16", "UTF-16BE", "UTF-16LE"
    };

    private final JComboBox<String> encodingChoices;
    private final JCheckBox autoLoadProject;
    private final JCheckBox noDeletePrompt;
    private final String systemEncoding;
    private final String defaultLabel;

    public GeneralPrefsPanel(Application app) {
        super(app);

        this.systemEncoding = Charset.defaultCharset().name();
        this.defaultLabel = systemEncoding + " (default)";

        GeneralPrefs prefs = new GeneralPrefs(app.getPrefsLocator().appNode(GeneralPrefs.NODE));

        this.encodingChoices = new JComboBox<>(new DefaultComboBoxModel<>(encodingLabels()));
        selectEncoding(prefs.getEncoding());

        this.autoLoadProject = new JCheckBox();
        this.autoLoadProject.setSelected(prefs.isAutoLoadProject());

        this.noDeletePrompt = new JCheckBox();
        this.noDeletePrompt.setSelected(prefs.isNoDeletePrompt());

        initLayout();
    }

    public void commit() {
        GeneralPrefs prefs = new GeneralPrefs(app.getPrefsLocator().appNode(GeneralPrefs.NODE));
        prefs.setEncoding(selectedEncoding());
        prefs.setAutoLoadProject(autoLoadProject.isSelected());
        prefs.setNoDeletePrompt(noDeletePrompt.isSelected());
    }

    private void initLayout() {
        FormLayout layout = new FormLayout(
                "right:pref, $lcgap, fill:120dlu, default:grow",
                "p, $rgap, p, $rgap, p, $rgap, p, fill:40dlu:grow");

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.addSeparator("General Preferences", cc.xyw(1, 1, 4));

        builder.add(new JLabel("File Encoding:"), cc.xy(1, 3));
        builder.add(encodingChoices, cc.xy(3, 3));
        builder.add(new JLabel("Auto-Load Last Project:"), cc.xy(1, 5));
        builder.add(autoLoadProject, cc.xy(3, 5, CellConstraints.LEFT, CellConstraints.DEFAULT));
        builder.add(new JLabel("Delete Without Prompt:"), cc.xy(1, 7));
        builder.add(noDeletePrompt, cc.xy(3, 7, CellConstraints.LEFT, CellConstraints.DEFAULT));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private String selectedEncoding() {
        Object selected = encodingChoices.getSelectedItem();
        return (selected == null || defaultLabel.equals(selected)) ? systemEncoding : selected.toString();
    }

    private String[] encodingLabels() {
        List<String> labels = new ArrayList<>(Arrays.asList(STANDARD_ENCODINGS));
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
