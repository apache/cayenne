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
package org.apache.cayenne.modeler.dialog.codegen.cgen;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.ProjectController;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

/**
 * @since 4.1
 */
public class CgenGlobalPanel extends JPanel {

    private JButton generateButton;

    private JTextField outputFolder;
    private JButton selectOutputFolder;
    private JComboBox<String> generationMode;
    private JComboBox<String> subclassTemplate;
    private JComboBox<String> superclassTemplate;
    private JComboBox<String> embeddableTemplate;
    private JComboBox<String> embeddableSuperTemplate;
    private JComboBox<String> dataMapTemplate;
    private JComboBox<String> dataMapSuperTemplate;
    private JCheckBox pairs;
    private JCheckBox overwrite;
    private JCheckBox usePackagePath;
    private JTextField outputPattern;
    private JCheckBox createPropertyNames;
    private JTextField superclassPackage;
    private JTextField encoding;

    private JButton resetFolder;
    private JButton resetMode;
    private JButton resetDataMapTemplate;
    private JButton resetDataMapSuperTemplate;
    private JButton resetTemplate;
    private JButton resetSuperTemplate;
    private JButton resetEmbeddableTemplate;
    private JButton resetEmbeddableSuperTemplate;
    private JButton resetPattern;
    private JButton resetEncoding;
    private JButton resetPairs;
    private JButton resetPath;
    private JButton resetOverwrite;
    private JButton resetNames;
    private JButton resetPackage;

    CgenGlobalPanel(ProjectController projectController) {
        this.generateButton = new JButton("Generate All classes");
        this.outputFolder = new JTextField();
        this.selectOutputFolder = new JButton("Select");
        this.generationMode = new JComboBox<>();
        this.subclassTemplate = new JComboBox<>();
        this.superclassTemplate = new JComboBox<>();
        this.embeddableTemplate = new JComboBox<>();
        this.embeddableSuperTemplate = new JComboBox<>();
        this.dataMapTemplate = new JComboBox<>();
        this.dataMapSuperTemplate = new JComboBox<>();
        this.pairs = new JCheckBox();
        this.overwrite = new JCheckBox();
        this.usePackagePath = new JCheckBox();
        this.outputPattern = new JTextField();
        this.createPropertyNames = new JCheckBox();
        this.encoding = new JTextField();
        this.superclassPackage = new JTextField();

        // assemble
        FormLayout layout = new FormLayout(
                "right:100dlu, 3dlu, fill:50:grow, 6dlu, fill:70dlu, 3dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append("Generate Classes for dataMaps", generateButton);
        builder.nextLine();

        builder.appendSeparator();

        resetFolder = new JButton("Reset Folder");
        builder.append("Output Directory:", outputFolder, selectOutputFolder);
        builder.nextLine();
        builder.append(resetFolder);
        builder.nextLine();
        resetMode = new JButton("Reset Mode");
        builder.append("Generation Mode:", generationMode, resetMode);
        builder.nextLine();

        resetDataMapTemplate = new JButton("Reset Template");
        builder.append("DataMap Template:", dataMapTemplate, resetDataMapTemplate);
        builder.nextLine();

        resetDataMapSuperTemplate = new JButton("Reset Template");
        builder.append("DataMap Superclass Template", dataMapSuperTemplate, resetDataMapSuperTemplate);
        builder.nextLine();

        resetTemplate = new JButton("Reset Template");
        builder.append("Subclass Template:", subclassTemplate, resetTemplate);
        builder.nextLine();

        resetSuperTemplate = new JButton("Reset Template");
        builder.append("Superclass Template:", superclassTemplate, resetSuperTemplate);
        builder.nextLine();

        resetEmbeddableTemplate = new JButton("Reset Template");
        builder.append("Embeddable Template", embeddableTemplate, resetEmbeddableTemplate);
        builder.nextLine();

        resetEmbeddableSuperTemplate = new JButton("Reset Template");
        builder.append("Embeddable Super Template", embeddableSuperTemplate, resetEmbeddableSuperTemplate);
        builder.nextLine();

        resetPattern = new JButton("Reset pattern");
        builder.append("Output Pattern:", outputPattern, resetPattern);
        builder.nextLine();

        resetEncoding = new JButton("Reset encoding");
        builder.append("Encoding", encoding, resetEncoding);
        builder.nextLine();

        resetPairs = new JButton("Reset pairs");
        builder.append("Make Pairs:", pairs, resetPairs);
        builder.nextLine();

        resetPath = new JButton("Reset path");
        builder.append("Use Package Path:", usePackagePath, resetPath);
        builder.nextLine();

        resetOverwrite = new JButton("Reset overwrite");
        builder.append("Overwrite Subclasses:", overwrite, resetOverwrite);
        builder.nextLine();

        resetNames = new JButton("Reset Names");
        builder.append("Create Property Names:", createPropertyNames, resetNames);
        builder.nextLine();

        resetPackage = new JButton("Reset Package");
        builder.append("Superclass package", superclassPackage, resetPackage);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JButton getGenerateButton() {
        return generateButton;
    }

    public JTextField getOutputFolder() {
        return outputFolder;
    }

    public JButton getSelectOutputFolder() {
        return selectOutputFolder;
    }

    public JComboBox<String> getGenerationMode() { return generationMode; }

    public JComboBox<String> getSubclassTemplate() {
        return subclassTemplate;
    }

    public JComboBox<String> getSuperclassTemplate() {
        return superclassTemplate;
    }

    public JComboBox<String> getEmbeddableTemplate() {
        return embeddableTemplate;
    }

    public JComboBox<String> getEmbeddableSuperTemplate() {
        return embeddableSuperTemplate;
    }

    public JComboBox<String> getDataMapTemplate() {
        return dataMapTemplate;
    }

    public JComboBox<String> getDataMapSuperTemplate() {
        return dataMapSuperTemplate;
    }

    public JCheckBox getPairs() {
        return pairs;
    }

    public JCheckBox getOverwrite() {
        return overwrite;
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

    public JTextField getEncoding() {
        return encoding;
    }

    public JButton getResetFolder() {
        return resetFolder;
    }

    public JButton getResetMode() {
        return resetMode;
    }

    public JButton getResetDataMapTemplate() {
        return resetDataMapTemplate;
    }

    public JButton getResetDataMapSuperTemplate() {
        return resetDataMapSuperTemplate;
    }

    public JButton getResetTemplate() {
        return resetTemplate;
    }

    public JButton getResetSuperTemplate() {
        return resetSuperTemplate;
    }

    public JButton getResetEmbeddableTemplate() {
        return resetEmbeddableTemplate;
    }

    public JButton getResetEmbeddableSuperTemplate() {
        return resetEmbeddableSuperTemplate;
    }

    public JButton getResetPattern() {
        return resetPattern;
    }

    public JButton getResetEncoding() {
        return resetEncoding;
    }

    public JButton getResetPairs() {
        return resetPairs;
    }

    public JButton getResetPath() {
        return resetPath;
    }

    public JButton getResetOverwrite() {
        return resetOverwrite;
    }

    public JButton getResetNames() {
        return resetNames;
    }

    public JButton getResetPackage() {
        return resetPackage;
    }
}
