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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.swing.control.ActionLink;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class CustomModePanel extends GeneratorControllerPanel {

    protected JComboBox generationMode;
    protected JComboBox subclassTemplate;
    protected JComboBox superclassTemplate;
    protected JCheckBox pairs;
    protected JCheckBox overwrite;
    protected JCheckBox usePackagePath;
    protected JTextField outputPattern;
    protected JCheckBox createPropertyNames;

    private DefaultFormBuilder builder;

    protected ActionLink manageTemplatesLink;

    public CustomModePanel() {

        this.generationMode = new JComboBox();
        this.superclassTemplate = new JComboBox();
        this.subclassTemplate = new JComboBox();
        this.pairs = new JCheckBox();
        this.overwrite = new JCheckBox();
        this.usePackagePath = new JCheckBox();
        this.outputPattern = new JTextField();
        this.createPropertyNames = new JCheckBox();
        this.manageTemplatesLink = new ActionLink("Customize Templates...");
        manageTemplatesLink.setFont(manageTemplatesLink.getFont().deriveFont(10f));

        pairs.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                superclassTemplate.setEnabled(pairs.isSelected());
                overwrite.setEnabled(!pairs.isSelected());
            }
        });

        // assemble

        FormLayout layout = new FormLayout(
                "right:77dlu, 3dlu, fill:200:grow, 6dlu, fill:50dlu, 3dlu", "");
        builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append("Output Directory:", outputFolder, selectOutputFolder);
        builder.nextLine();

        builder.append("Generation Mode:", generationMode);
        builder.nextLine();

        builder.append("Subclass Template:", subclassTemplate);
        builder.nextLine();

        builder.append("Superclass Template:", superclassTemplate);
        builder.nextLine();

        builder.append("Output Pattern:", outputPattern);
        builder.nextLine();

        builder.append("Make Pairs:", pairs);
        builder.nextLine();

        builder.append("Use Package Path:", usePackagePath);
        builder.nextLine();

        builder.append("Overwrite Subclasses:", overwrite);
        builder.nextLine();

        builder.append("Create Property Names:", createPropertyNames);
        builder.nextLine();

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        links.add(manageTemplatesLink);
        add(links, BorderLayout.SOUTH);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void addDataMapLine(StandardPanelComponent dataMapLine) {
        dataMapLines.add(dataMapLine);
        builder.append(dataMapLine, 4);
        builder.nextLine();
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

    public JCheckBox getCreatePropertyNames() {
        return createPropertyNames;
    }
}
