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

package org.apache.cayenne.modeler.editor.cgen;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.swing.control.ActionLink;

import javax.swing.*;
import java.awt.*;

public class CustomModePanel extends GeneratorControllerPanel {

    protected JComboBox generationMode;
    protected JComboBox subclassTemplate;
    protected JComboBox superclassTemplate;
    protected JCheckBox pairs;
    protected JCheckBox overwrite;
    protected JCheckBox usePackagePath;
    protected JTextField outputPattern;
    protected JCheckBox createPropertyNames;
    private JTextField superclassPackage;

    private JTextField additionalMaps;
    private JButton selectAdditionalMaps;
    private JCheckBox client;
    private JTextField encoding;
    private JComboBox embeddableTemplate;
    private JComboBox embeddableSuperTemplate;
    private JLabel dataMapName;

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
        this.manageTemplatesLink.setFont(manageTemplatesLink.getFont().deriveFont(10f));
        this.superclassPackage = new JTextField();

        this.additionalMaps = new JTextField();
        this.selectAdditionalMaps = new JButton("Select");
        this.client = new JCheckBox();
        this.encoding = new JTextField();
        this.embeddableTemplate = new JComboBox();
        this.embeddableSuperTemplate = new JComboBox();
        this.dataMapName = new JLabel();
        this.dataMapName.setFont(dataMapName.getFont().deriveFont(1));

        pairs.addChangeListener(e -> {
            superclassTemplate.setEnabled(pairs.isSelected());
            overwrite.setEnabled(!pairs.isSelected());
        });

        // assemble

        FormLayout layout = new FormLayout(
                "right:77dlu, 3dlu, fill:200:grow, 6dlu, fill:50dlu, 3dlu", "");
        builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append("Output Directory:", outputFolder, selectOutputFolder);
        builder.nextLine();

        builder.append("Additional DataMaps", additionalMaps, selectAdditionalMaps);
        builder.nextLine();

        builder.append("Generation Mode:", generationMode);
        builder.nextLine();

        builder.append("Subclass Template:", subclassTemplate);
        builder.nextLine();

        builder.append("Superclass Template:", superclassTemplate);
        builder.nextLine();

        builder.append("Embeddable Template", embeddableTemplate);
        builder.nextLine();

        builder.append("Embeddable Super Template", embeddableSuperTemplate);
        builder.nextLine();

        builder.append("Output Pattern:", outputPattern);
        builder.nextLine();

        builder.append("Encoding", encoding);
        builder.nextLine();

        builder.append("Make Pairs:", pairs);
        builder.nextLine();

        builder.append("Use Package Path:", usePackagePath);
        builder.nextLine();

        builder.append("Overwrite Subclasses:", overwrite);
        builder.nextLine();

        builder.append("Create Property Names:", createPropertyNames);
        builder.nextLine();

        builder.append("Client", client);
        builder.nextLine();

        builder.append(dataMapName);
        builder.nextLine();

        builder.append("Superclass package", superclassPackage);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        links.add(manageTemplatesLink);
        add(links, BorderLayout.SOUTH);

        add(builder.getPanel(), BorderLayout.CENTER);
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


    public JTextField getSuperclassPackage() {
        return superclassPackage;
    }

    public void setDataMapName(String mapName){
        dataMapName.setText(mapName);
    }

    public void setSuperclassPackage(String pack) {
        superclassPackage.setText(pack);
    }

    public void setPairs(boolean val){
        pairs.setSelected(val);
    }

    public void setOverwrite(boolean val){
        overwrite.setSelected(val);
    }

    public void setUsePackagePath(boolean val) {
        usePackagePath.setSelected(val);
    }

    public void setCreatePropertyNames(boolean val) {
        createPropertyNames.setSelected(val);
    }

    public void setOutputPattern(String pattern){
        outputPattern.setText(pattern);
    }

    public void setSuperclassTemplate(String template){
        superclassTemplate.setSelectedItem(template);
    }

    public void setTemplate(String template) {
        subclassTemplate.setSelectedItem(template);
    }

    public void setGenerationMode(String mode) {
        generationMode.setSelectedItem(mode);
    }
}
