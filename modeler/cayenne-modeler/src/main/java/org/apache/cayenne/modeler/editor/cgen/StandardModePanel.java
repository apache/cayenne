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
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

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
    private JButton editSuperclassTemplateBtn;
    private JButton editSubclassTemplateBtn;
    private JButton editEmbeddableTemplateBtn;
    private JButton editEmbeddableSuperTemplateBtn;
    private JButton editDataMapTemplateBtn;
    private JButton editDataMapSuperTemplateBtn;
    private JLabel entityTemplateLbl;
    private JLabel entitySuperTemplateLbl;
    private JLabel embeddableTemplateLbl;
    private JLabel embeddableSuperTemplateLbl;
    private JLabel datamapTemplateLbl;
    private JLabel datamapSuperTemplateLbl;


    public StandardModePanel(CodeGeneratorController codeGeneratorController) {
        super(Application.getInstance().getFrameController().getProjectController(), codeGeneratorController);
        this.codeGeneratorController = codeGeneratorController;
        this.pairs = new JCayenneCheckBox();
        this.overwrite = new JCayenneCheckBox();
        this.usePackagePath = new JCayenneCheckBox();
        this.createPropertyNames = new JCayenneCheckBox();
        this.pkProperties = new JCayenneCheckBox();


        initTextFields();
        initEditTemplateLabels();
        initEditTemplateButtons();
        buildView();
    }

    private void initEditTemplateLabels() {
        this.entityTemplateLbl = new JLabel(TemplateType.ENTITY_SUBCLASS.readableName());
        this.entitySuperTemplateLbl = new JLabel(TemplateType.ENTITY_SUPERCLASS.readableName());
        this.embeddableTemplateLbl = new JLabel(TemplateType.EMBEDDABLE_SUBCLASS.readableName());
        this.embeddableSuperTemplateLbl = new JLabel(TemplateType.EMBEDDABLE_SUPERCLASS.readableName());
        this.datamapTemplateLbl = new JLabel(TemplateType.DATAMAP_SUBCLASS.readableName());
        this.datamapSuperTemplateLbl = new JLabel(TemplateType.DATAMAP_SUPERCLASS.readableName());
    }

    private void initEditTemplateButtons() {
        this.editSubclassTemplateBtn = new JButton("Edit");
        this.editSuperclassTemplateBtn = new JButton("Edit");
        this.editEmbeddableTemplateBtn = new JButton("Edit");
        this.editEmbeddableSuperTemplateBtn = new JButton("Edit");
        this.editDataMapTemplateBtn = new JButton("Edit");
        this.editDataMapSuperTemplateBtn = new JButton("Edit");

    }

    public void setEnableEditSubclassTemplateButtons(Boolean isEnabled) {
        this.editSubclassTemplateBtn.setEnabled(isEnabled);
        this.editEmbeddableTemplateBtn.setEnabled(isEnabled);
        this.editDataMapTemplateBtn.setEnabled(isEnabled);
    }

    public void setEnableEditSuperclassTemplateButtons(Boolean isEnabled) {
        this.editSuperclassTemplateBtn.setEnabled(isEnabled);
        this.editEmbeddableSuperTemplateBtn.setEnabled(isEnabled);
        this.editDataMapSuperTemplateBtn.setEnabled(isEnabled);
    }

    protected void buildView() {
        setLayout(new BorderLayout());
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "left:10dlu, 3dlu, 90dlu, 3dlu, pref, 3dlu, 50dlu, 3dlu, 20dlu",
                "p, 3dlu, p, 10dlu, 11*(p, 3dlu),10dlu,9*(p, 3dlu)");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.addLabel("Output Directory", cc.xyw(1, 1, 3));
        builder.add(outputFolder.getComponent(), cc.xyw(1, 3, 7));
        builder.add(selectOutputFolder, cc.xy(9, 3));

        // Advanced options panel
        builder.addSeparator("Advanced options", cc.xyw(1, 7, 9));

        builder.add(pairs, cc.xy(1, 9));
        builder.addLabel("Make Pairs", cc.xyw(3, 9, 3));

        builder.add(usePackagePath, cc.xy(1, 11));
        builder.addLabel("Use Package Path", cc.xyw(3, 11, 3));

        builder.add(overwrite, cc.xy(1, 13));
        builder.addLabel("Overwrite Subclasses", cc.xyw(3, 13, 3));

        builder.add(createPropertyNames, cc.xy(1, 15));
        builder.addLabel("Create Property Names", cc.xyw(3, 15, 3));

        builder.add(pkProperties, cc.xy(1, 17));
        builder.addLabel("Create PK properties", cc.xyw(3, 17, 3));

        builder.addLabel("Output Pattern", cc.xyw(1, 19, 5));
        builder.add(outputPattern.getComponent(), cc.xyw(1, 21, 5));

        builder.addLabel("Superclass package", cc.xyw(1, 23, 5));
        builder.add(superPkg.getComponent(), cc.xyw(1, 25, 5));


        //Templates options panel
        builder.addSeparator("Templates options", cc.xyw(1, 28, 9));

        builder.add(entityTemplateLbl, cc.xyw(1, 30, 3));
        builder.add(editSubclassTemplateBtn, cc.xy(5, 30));

        builder.add(entitySuperTemplateLbl, cc.xyw(1, 32, 3));
        builder.add(editSuperclassTemplateBtn, cc.xy(5, 32));

        builder.add(embeddableTemplateLbl, cc.xyw(1, 34, 3));
        builder.add(editEmbeddableTemplateBtn, cc.xy(5, 34));

        builder.add(embeddableSuperTemplateLbl, cc.xyw(1, 36, 3));
        builder.add(editEmbeddableSuperTemplateBtn, cc.xy(5, 36));

        builder.add(datamapTemplateLbl, cc.xyw(1, 38, 3));
        builder.add(editDataMapTemplateBtn, cc.xy(5, 38));

        builder.add(datamapSuperTemplateLbl, cc.xyw(1, 40, 3));
        builder.add(editDataMapSuperTemplateBtn, cc.xy(5, 40));

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initTextFields() {
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

    public TextAdapter getSuperPkg() {
        return superPkg;
    }

    public JButton getEditSuperclassTemplateBtn() {
        return editSuperclassTemplateBtn;
    }

    public JButton getEditSubclassTemplateBtn() {
        return editSubclassTemplateBtn;
    }

    public JButton getEditEmbeddableTemplateBtn() {
        return editEmbeddableTemplateBtn;
    }

    public JButton getEditEmbeddableSuperTemplateBtn() {
        return editEmbeddableSuperTemplateBtn;
    }

    public JButton getEditDataMapTemplateBtn() {
        return editDataMapTemplateBtn;
    }

    public JButton getEditDataMapSuperTemplateBtn() {
        return editDataMapSuperTemplateBtn;
    }

    public JLabel getEntityTemplateLbl() {
        return entityTemplateLbl;
    }

    public JLabel getEntitySuperTemplateLbl() {
        return entitySuperTemplateLbl;
    }

    public JLabel getEmbeddableTemplateLbl() {
        return embeddableTemplateLbl;
    }

    public JLabel getEmbeddableSuperTemplateLbl() {
        return embeddableSuperTemplateLbl;
    }

    public JLabel getDatamapTemplateLbl() {
        return datamapTemplateLbl;
    }

    public JLabel getDatamapSuperTemplateLbl() {
        return datamapSuperTemplateLbl;
    }

}