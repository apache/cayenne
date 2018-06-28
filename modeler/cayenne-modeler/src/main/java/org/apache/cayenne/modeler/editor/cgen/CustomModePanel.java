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
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.ComboBoxAdapter;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.swing.control.ActionLink;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class CustomModePanel extends GeneratorControllerPanel {

    private ComboBoxAdapter<String> generationMode;
    private ComboBoxAdapter<String> subclassTemplate;
    private ComboBoxAdapter<String> superclassTemplate;
    private ComboBoxAdapter<String> embeddableTemplate;
    private ComboBoxAdapter<String> embeddableSuperTemplate;
    private ComboBoxAdapter<String> dataMapTemplate;
    private ComboBoxAdapter<String> dataMapSuperTemplate;
    private JCheckBox pairs;
    private JCheckBox overwrite;
    private JCheckBox usePackagePath;
    private TextAdapter outputPattern;
    private JCheckBox createPropertyNames;
    private TextAdapter superclassPackage;

    private TextAdapter encoding;

    private JLabel dataMapName;

    private ActionLink manageTemplatesLink;

    CustomModePanel(ProjectController projectController) {
        super(projectController);

        JComboBox<String> modeField = new JComboBox<>();
        this.generationMode = new ComboBoxAdapter<String>(modeField) {
            @Override
            protected void updateModel(String item) throws ValidationException {
                getCgenByDataMap().setArtifactsGenerationMode(CustomModeController.modesByLabel.get(item));
                projectController.setDirty(true);
            }
        };

        JComboBox<String> superclassField = new JComboBox<>();
        this.superclassTemplate = new ComboBoxAdapter<String>(superclassField) {
            @Override
            protected void updateModel(String item) throws ValidationException {
                getCgenByDataMap().setSuperTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(item)));
                projectController.setDirty(true);
            }
        };

        JComboBox<String> subclassField = new JComboBox<>();
        this.subclassTemplate = new ComboBoxAdapter<String>(subclassField) {
            @Override
            protected void updateModel(String item) throws ValidationException {
                getCgenByDataMap().setTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(item)));
                projectController.setDirty(true);
            }
        };

        JComboBox<String> dataMapField = new JComboBox<>();
        this.dataMapTemplate = new ComboBoxAdapter<String>(dataMapField) {
            @Override
            protected void updateModel(String item) throws ValidationException {
                getCgenByDataMap().setQueryTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(item)));
                projectController.setDirty(true);
            }
        };

        JComboBox<String> dataMapSuperField = new JComboBox<>();
        this.dataMapSuperTemplate = new ComboBoxAdapter<String>(dataMapSuperField) {
            @Override
            protected void updateModel(String item) throws ValidationException {
                getCgenByDataMap().setQuerySuperTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(item)));
                projectController.setDirty(true);
            }
        };

        this.pairs = new JCayenneCheckBox();
        this.overwrite = new JCayenneCheckBox();
        this.usePackagePath = new JCayenneCheckBox();

        JTextField outputPatternField = new JTextField();
        this.outputPattern = new TextAdapter(outputPatternField) {
            protected void updateModel(String text) {
                getCgenByDataMap().setOutputPattern(text);
                projectController.setDirty(true);
            }
        };

        this.createPropertyNames = new JCayenneCheckBox();
        this.manageTemplatesLink = new ActionLink("Customize Templates...");
        this.manageTemplatesLink.setFont(manageTemplatesLink.getFont().deriveFont(10f));

        JTextField superclassPackageField = new JTextField();
        this.superclassPackage = new TextAdapter(superclassPackageField) {
            protected void updateModel(String text) {
                getCgenByDataMap().setSuperPkg(text);
                projectController.setDirty(true);
            }
        };

        JTextField encodingField = new JTextField();
        this.encoding = new TextAdapter(encodingField) {
            protected void updateModel(String text) {
                getCgenByDataMap().setEncoding(text);
                projectController.setDirty(true);
            }
        };

        JComboBox<String> embeddableField = new JComboBox<>();
        this.embeddableTemplate = new ComboBoxAdapter<String>(embeddableField) {
            @Override
            protected void updateModel(String item) throws ValidationException {
                getCgenByDataMap().setEmbeddableTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(item)));
                projectController.setDirty(true);
            }
        };

        JComboBox<String> embeddableSuperclassField = new JComboBox<>();
        this.embeddableSuperTemplate = new ComboBoxAdapter<String>(embeddableSuperclassField) {
            @Override
            protected void updateModel(String item) throws ValidationException {
                getCgenByDataMap().setEmbeddableSuperTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(item)));
                projectController.setDirty(true);
            }
        };

        this.dataMapName = new JLabel();
        this.dataMapName.setFont(dataMapName.getFont().deriveFont(1));

        pairs.addChangeListener(e -> {
           setDisableSuperComboBoxes(pairs.isSelected());
            overwrite.setEnabled(!pairs.isSelected());
        });

        // assemble
        FormLayout layout = new FormLayout(
                "right:100dlu, 3dlu, fill:100:grow, 6dlu, fill:50dlu, 3dlu", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.append("Output Directory:", outputFolder.getComponent(), selectOutputFolder);
        builder.nextLine();

        builder.append("Generation Mode:", generationMode.getComboBox());
        builder.nextLine();

        builder.append("DataMap Template:", dataMapTemplate.getComboBox());
        builder.nextLine();

        builder.append("DataMap Superclass Template", dataMapSuperTemplate.getComboBox());
        builder.nextLine();

        builder.append("Subclass Template:", subclassTemplate.getComboBox());
        builder.nextLine();

        builder.append("Superclass Template:", superclassTemplate.getComboBox());
        builder.nextLine();

        builder.append("Embeddable Template", embeddableTemplate.getComboBox());
        builder.nextLine();

        builder.append("Embeddable Super Template", embeddableSuperTemplate.getComboBox());
        builder.nextLine();

        builder.append("Output Pattern:", outputPattern.getComponent());
        builder.nextLine();

        builder.append("Encoding", encoding.getComponent());
        builder.nextLine();

        builder.append("Make Pairs:", pairs);
        builder.nextLine();

        builder.append("Use Package Path:", usePackagePath);
        builder.nextLine();

        builder.append("Overwrite Subclasses:", overwrite);
        builder.nextLine();

        builder.append("Create Property Names:", createPropertyNames);
        builder.nextLine();

        builder.append(dataMapName);
        builder.nextLine();

        builder.append("Superclass package", superclassPackage.getComponent());

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        JPanel links = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        links.add(manageTemplatesLink);
        add(links, BorderLayout.SOUTH);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    public void setDisableSuperComboBoxes(boolean val){
        superclassTemplate.getComboBox().setEnabled(val);
        embeddableSuperTemplate.getComboBox().setEnabled(val);
        dataMapSuperTemplate.getComboBox().setEnabled(val);
    }

    public ComboBoxAdapter<String> getGenerationMode() {
        return generationMode;
    }

    public ActionLink getManageTemplatesLink() {
        return manageTemplatesLink;
    }

    public ComboBoxAdapter<String> getSubclassTemplate() { return subclassTemplate; }

    public ComboBoxAdapter<String> getEmbeddableTemplate() { return embeddableTemplate; }

    public ComboBoxAdapter<String> getEmbeddableSuperTemplate() { return embeddableSuperTemplate; }

    public ComboBoxAdapter<String> getSuperclassTemplate() {
        return superclassTemplate;
    }

    public ComboBoxAdapter<String> getDataMapTemplate() { return dataMapTemplate; }

    public ComboBoxAdapter<String> getDataMapSuperTemplate() { return dataMapSuperTemplate; }

    public JCheckBox getOverwrite() {
        return overwrite;
    }

    public JCheckBox getPairs() {
        return pairs;
    }

    public JCheckBox getUsePackagePath() {
        return usePackagePath;
    }

    public TextAdapter getOutputPattern() {
        return outputPattern;
    }

    public JCheckBox getCreatePropertyNames() {
        return createPropertyNames;
    }

    public TextAdapter getSuperclassPackage() {
        return superclassPackage;
    }

    public TextAdapter getEncoding() { return encoding; }

    public void setDataMapName(String mapName){
        dataMapName.setText(mapName);
    }
}
