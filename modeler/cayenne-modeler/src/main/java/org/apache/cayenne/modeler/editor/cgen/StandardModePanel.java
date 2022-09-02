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

package org.apache.cayenne.modeler.editor.cgen;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.ComboBoxAdapter;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.function.BiConsumer;

/**
 * @since 4.1
 */
public class StandardModePanel extends GeneratorControllerPanel {

    private final JCheckBox pairs;
    private final JCheckBox overwrite;
    private final JCheckBox usePackagePath;
    private final JCheckBox createPropertyNames;
    private final JCheckBox pkProperties;
    private TextAdapter superPkg;
    protected TextAdapter outputPattern;
    private ComboBoxAdapter<String> subclassTemplate;
    private ComboBoxAdapter<String> superclassTemplate;
    private ComboBoxAdapter<String> embeddableTemplate;
    private ComboBoxAdapter<String> embeddableSuperTemplate;
    private ComboBoxAdapter<String> dataMapTemplate;
    private ComboBoxAdapter<String> dataMapSuperTemplate;
    private JButton templateManagerButton;


    public StandardModePanel(CodeGeneratorController codeGeneratorController) {
        super(Application.getInstance().getFrameController().getProjectController(), codeGeneratorController);
        this.codeGeneratorController = codeGeneratorController;
        this.pairs = new JCayenneCheckBox();
        this.overwrite = new JCayenneCheckBox();
        this.usePackagePath = new JCayenneCheckBox();
        this.createPropertyNames = new JCayenneCheckBox();
        this.pkProperties = new JCayenneCheckBox();
        this.templateManagerButton = new JButton("Templates manager");
        this.templateManagerButton.setFont(templateManagerButton.getFont().deriveFont(14f));

        initTextFields();
        initTemplatesSelector();
        buildView();
    }

    protected void buildView() {
        setLayout(new BorderLayout());
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "left:10dlu,3dlu, 140dlu, 3dlu, 50dlu, 3dlu, 20dlu",
                "p, 3dlu, p, 10dlu, 11*(p, 3dlu),10dlu,9*(p, 3dlu)");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.addLabel("Output Directory", cc.xyw(1, 1, 3));
        builder.add(outputFolder.getComponent(), cc.xyw(1, 3, 6));
        builder.add(selectOutputFolder, cc.xy(7, 3));

        // Advanced options panel
        builder.addSeparator("Advanced options", cc.xyw(1, 7, 7 ));

        builder.add(pairs, cc.xy(1, 9));
        builder.addLabel("Make Pairs", cc.xy(3, 9));

        builder.add(usePackagePath, cc.xy(1, 11));
        builder.addLabel("Use Package Path", cc.xy(3, 11));

        builder.add(overwrite, cc.xy(1, 13));
        builder.addLabel("Overwrite Subclasses", cc.xy(3, 13));

        builder.add(createPropertyNames, cc.xy(1, 15));
        builder.addLabel("Create Property Names", cc.xy(3, 15));

        builder.add(pkProperties, cc.xy(1, 17));
        builder.addLabel("Create PK properties", cc.xy(3, 17));

        builder.addLabel("Output Pattern", cc.xyw(1, 19, 3));
        builder.add(outputPattern.getComponent(), cc.xyw(1, 21, 3));

        builder.addLabel("Superclass package", cc.xyw(1, 23, 3));
        builder.add(superPkg.getComponent(), cc.xyw(1, 25, 3));

        //Templates options panel
        builder.addSeparator("Templates options", cc.xyw(1, 28, 7 ));

        builder.add(subclassTemplate.getComboBox(), cc.xyw(1, 30,3));
        builder.addLabel("Subclass", cc.xyw(5, 30,3));

        builder.add(superclassTemplate.getComboBox(), cc.xyw(1, 32,3));
        builder.addLabel("Superclass", cc.xyw(5, 32,3));

        builder.add(embeddableTemplate.getComboBox(), cc.xyw(1, 34,3));
        builder.addLabel("Embeddable", cc.xyw(5, 34,3));

        builder.add(embeddableSuperTemplate.getComboBox(), cc.xyw(1, 36,3));
        builder.addLabel("Embeddable Superclass", cc.xyw(5, 36,3));

        builder.add(dataMapTemplate.getComboBox(), cc.xyw(1, 38,3));
        builder.addLabel("DataMap", cc.xyw(5, 38,3));

        builder.add(dataMapSuperTemplate.getComboBox(), cc.xyw(1, 40,3));
        builder.addLabel("DataMap Superclass", cc.xyw(5, 40,3));

        builder.add(templateManagerButton, cc.xyw(1, 44,3));

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initTextFields(){
        JTextField superPkgField = new JTextField();
        this.superPkg = new TextAdapter(superPkgField) {
            @Override
            protected void updateModel(String text) throws ValidationException {
                getCgenConfig().setSuperPkg(text);
                checkConfigDirty();
            }
        };

        JTextField outputPatternField = new JTextField();
        this.outputPattern = new TextAdapter(outputPatternField) {
            protected void updateModel(String text) {
                getCgenConfig().setOutputPattern(text);
                checkConfigDirty();
            }
        };
    }

    private void initTemplatesSelector() {
        JComboBox<String> subclassField = new JComboBox<>();
        this.subclassTemplate = new StringComboBoxAdapter(subclassField, CgenConfiguration::setTemplate);
        JComboBox<String> superclassField = new JComboBox<>();
        this.superclassTemplate = new StringComboBoxAdapter(superclassField, CgenConfiguration::setSuperTemplate);
        JComboBox<String> embeddableField = new JComboBox<>();
        this.embeddableTemplate = new StringComboBoxAdapter(embeddableField, CgenConfiguration::setEmbeddableTemplate);
        JComboBox<String> embeddableSuperField = new JComboBox<>();
        this.embeddableSuperTemplate = new StringComboBoxAdapter(embeddableSuperField, CgenConfiguration::setEmbeddableSuperTemplate);
        JComboBox<String> queryField = new JComboBox<>();
        this.dataMapTemplate = new StringComboBoxAdapter(queryField, CgenConfiguration::setDataMapTemplate);
        JComboBox<String> querySuper = new JComboBox<>();
        this.dataMapSuperTemplate = new StringComboBoxAdapter(querySuper, CgenConfiguration::setDataMapSuperTemplate);
    }

    class StringComboBoxAdapter extends ComboBoxAdapter<String> {
        private final BiConsumer<CgenConfiguration, String> setTemplate;

        public StringComboBoxAdapter(JComboBox<String> subclassField, BiConsumer<CgenConfiguration, String> setter) {
            super(subclassField);
            this.setTemplate = setter;
        }

        @Override
        protected void updateModel(String item) throws ValidationException {
            CgenConfiguration cgenConfiguration = getCgenConfig();
            String templatePath = Application.getInstance()
                    .getCodeTemplateManager()
                    .getTemplatePath(item, cgenConfiguration.getDataMap().getConfigurationSource());
            setTemplate.accept(cgenConfiguration, templatePath);
            checkConfigDirty();
        }
    }

    public void setDisableSuperComboBoxes(boolean val) {
        superclassTemplate.getComboBox().setEnabled(val);
        embeddableSuperTemplate.getComboBox().setEnabled(val);
        dataMapSuperTemplate.getComboBox().setEnabled(val);
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

    public JCheckBox getCreatePropertyNames() {
        return createPropertyNames;
    }

    public JCheckBox getPkProperties() {
        return pkProperties;
    }

    public TextAdapter getOutputPattern() {
        return outputPattern;
    }

    public JButton getTemplateManagerButton() {
        return templateManagerButton;
    }

    public ComboBoxAdapter<String> getSubclassTemplate() {
        return subclassTemplate;
    }

    public ComboBoxAdapter<String> getSuperclassTemplate() {
        return superclassTemplate;
    }

    public ComboBoxAdapter<String> getEmbeddableTemplate() {
        return embeddableTemplate;
    }

    public ComboBoxAdapter<String> getEmbeddableSuperTemplate() {
        return embeddableSuperTemplate;
    }

    public ComboBoxAdapter<String> getDataMapTemplate() {
        return dataMapTemplate;
    }

    public ComboBoxAdapter<String> getDataMapSuperTemplate() {
        return dataMapSuperTemplate;
    }

}