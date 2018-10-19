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
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class CustomModePanel extends GeneratorControllerPanel {

    private JComboBox<String> subclassTemplate;
    private JComboBox<String> superclassTemplate;
    protected JCheckBox pairs;
    private JCheckBox overwrite;
    private JCheckBox usePackagePath;
    private JTextField outputPattern;
    private JCheckBox createPropertyNames;
    private JCheckBox createPKProperties;

    private ActionLink manageTemplatesLink;

    CustomModePanel() {

        this.superclassTemplate = new JComboBox<>();
        this.subclassTemplate = new JComboBox<>();
        this.pairs = new JCheckBox();
        this.overwrite = new JCheckBox();
        this.usePackagePath = new JCheckBox();
        this.outputPattern = new JTextField();
        this.createPropertyNames = new JCheckBox();
        this.createPKProperties = new JCheckBox();
        this.manageTemplatesLink = new ActionLink("Customize Templates...");
        manageTemplatesLink.setFont(manageTemplatesLink.getFont().deriveFont(10f));

        // assemble
        FormLayout layout = new FormLayout(
                "right:77dlu, 1dlu, fill:100:grow, 1dlu, left:80dlu, 1dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append("Output Directory:", outputFolder, selectOutputFolder);
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

        builder.append("Create PK Properties:", createPKProperties);
        builder.nextLine();

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        links.add(manageTemplatesLink);
        add(links, BorderLayout.SOUTH);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public ActionLink getManageTemplatesLink() {
        return manageTemplatesLink;
    }

    public JComboBox<String> getSubclassTemplate() {
        return subclassTemplate;
    }

    public JComboBox<String> getSuperclassTemplate() {
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

    public JCheckBox getCreatePKProperties() {
        return createPKProperties;
    }
}
