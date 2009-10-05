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

package org.apache.cayenne.modeler.dialog.codegen;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.cayenne.swing.control.ActionLink;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class CustomModePanel extends GeneratorControllerPanel {

    protected JComboBox generationMode;
    protected JComboBox subclassTemplate;
    protected JComboBox superclassTemplate;
    protected JCheckBox pairs;
    protected JComboBox generatorVersion;
    protected JCheckBox overwrite;
    protected JCheckBox usePackagePath;
    protected JTextField outputPattern;

    protected ActionLink manageTemplatesLink;

    public CustomModePanel() {

        this.generationMode = new JComboBox();
        this.superclassTemplate = new JComboBox();
        this.subclassTemplate = new JComboBox();
        this.pairs = new JCheckBox();
        this.generatorVersion = new JComboBox();
        this.overwrite = new JCheckBox();
        this.usePackagePath = new JCheckBox();
        this.outputPattern = new JTextField();
        this.manageTemplatesLink = new ActionLink("Customize Templates...");
        manageTemplatesLink.setFont(manageTemplatesLink.getFont().deriveFont(10f));

        pairs.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                superclassTemplate.setEnabled(pairs.isSelected());
            }
        });

        // assemble

        PanelBuilder builder = new PanelBuilder(
                new FormLayout(
                        "right:70dlu, 3dlu, fill:150dlu:grow, 3dlu, pref",
                        "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"));
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.addLabel("Output Directory:", cc.xy(1, 1));
        builder.add(outputFolder, cc.xy(3, 1));
        builder.add(selectOutputFolder, cc.xy(5, 1));

        builder.addLabel("Superclass Package:", cc.xy(1, 3));
        builder.add(superclassPackage, cc.xy(3, 3));

        builder.addLabel("Generation Mode:", cc.xy(1, 5));
        builder.add(generationMode, cc.xy(3, 5));

        builder.addLabel("Generator Version:", cc.xy(1, 7));
        builder.add(generatorVersion, cc.xy(3, 7));

        builder.addLabel("Subclass Template:", cc.xy(1, 9));
        builder.add(subclassTemplate, cc.xy(3, 9));

        builder.addLabel("Superclass Template:", cc.xy(1, 11));
        builder.add(superclassTemplate, cc.xy(3, 11));

        builder.addLabel("Output Pattern:", cc.xy(1, 13));
        builder.add(outputPattern, cc.xy(3, 13));

        builder.addLabel("Make Pairs:", cc.xy(1, 15));
        builder.add(pairs, cc.xy(3, 15));

        builder.addLabel("Overwrite Subclasses:", cc.xy(1, 17));
        builder.add(overwrite, cc.xy(3, 17));

        builder.addLabel("Use Package Path:", cc.xy(1, 19));
        builder.add(usePackagePath, cc.xy(3, 19));

        JPanel links = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        links.add(manageTemplatesLink);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
        add(links, BorderLayout.SOUTH);
    }

    public JComboBox getGenerationMode() {
        return generationMode;
    }

    public ActionLink getManageTemplatesLink() {
        return manageTemplatesLink;
    }

    public JComboBox getSubclassTemplate() {
        return subclassTemplate;
    }

    public JComboBox getSuperclassTemplate() {
        return superclassTemplate;
    }

    public JComboBox getGeneratorVersion() {
        return generatorVersion;
    }

    public JCheckBox getOverwrite() {
        return overwrite;
    }

    public JCheckBox getPairs() {
        return pairs;
    }

    public JCheckBox getUsePackagePath() {
        return usePackagePath;
    }

    
    public JTextField getOutputPattern() {
        return outputPattern;
    }
}
