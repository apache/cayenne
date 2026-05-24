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

package org.apache.cayenne.modeler.ui.project.editor.datamap.cgen;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.modeler.project.CgenOps;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.checkbox.CMCheckBox;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.ui.project.editor.datamap.cgen.templateeditor.TemplateEditor;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Right-side options form of the cgen editor — output folder, advanced switches, and the
 * six template-edit buttons that launch a {@link TemplateEditor} on click.
 */
public class CgenConfigPanel extends ProjectPanel {

    static final Border CGEN_PANEL_BORDER = BorderFactory.createEmptyBorder(5, 13, 5, 13);

    private static final String INVALID_PATH_MSG =
            "An invalid path has been detected. It cannot be saved or used until it is corrected.";
    private static final String NEED_TO_SAVE_PROJECT_MSG =
            "You should save project to use relative path as an output directory.";
    private static final String EDITED = " (edited)";

    private final CgenPanel cgen;

    private final CMUndoableTextField outputFolder;
    private final JButton selectOutputFolder;
    private final JCheckBox pairs;
    private final JCheckBox overwrite;
    private final JCheckBox usePackagePath;
    private final JCheckBox createPropertyNames;
    private final JCheckBox pkProperties;
    private final CMUndoableTextField superPkg;
    private final CMUndoableTextField outputPattern;
    private final JButton editSuperclassTemplateBtn;
    private final JButton editSubclassTemplateBtn;
    private final JButton editEmbeddableTemplateBtn;
    private final JButton editEmbeddableSuperTemplateBtn;
    private final JButton editDataMapTemplateBtn;
    private final JButton editDataMapSuperTemplateBtn;
    private final JLabel entityTemplateLbl;
    private final JLabel entitySuperTemplateLbl;
    private final JLabel embeddableTemplateLbl;
    private final JLabel embeddableSuperTemplateLbl;
    private final JLabel datamapTemplateLbl;
    private final JLabel datamapSuperTemplateLbl;

    private CgenConfiguration cgenConfiguration;
    private boolean isDataValid;
    private boolean isEditorOpen;

    public CgenConfigPanel(ProjectSession session, CgenPanel cgen) {
        super(session);
        this.cgen = cgen;

        this.selectOutputFolder = new JButton("..");
        this.pairs = new CMCheckBox(app.getUndoManager());
        this.overwrite = new CMCheckBox(app.getUndoManager());
        this.usePackagePath = new CMCheckBox(app.getUndoManager());
        this.createPropertyNames = new CMCheckBox(app.getUndoManager());
        this.pkProperties = new CMCheckBox(app.getUndoManager());
        this.superPkg = new CMUndoableTextField(app.getUndoManager());
        this.outputPattern = new CMUndoableTextField(app.getUndoManager());
        this.outputFolder = new CMUndoableTextField(app.getUndoManager()) {
            @Override
            public void setText(String text) {
                super.setText(text);
                boolean valid = true;
                try {
                    Paths.get(text == null ? "" : text);
                } catch (InvalidPathException e) {
                    valid = false;
                }
                updateGenerateButton(valid);
            }
        };

        this.editSubclassTemplateBtn = new JButton("Edit");
        this.editSuperclassTemplateBtn = new JButton("Edit");
        this.editEmbeddableTemplateBtn = new JButton("Edit");
        this.editEmbeddableSuperTemplateBtn = new JButton("Edit");
        this.editDataMapTemplateBtn = new JButton("Edit");
        this.editDataMapSuperTemplateBtn = new JButton("Edit");

        this.entityTemplateLbl = new JLabel(TemplateType.ENTITY_SUBCLASS.readableName());
        this.entitySuperTemplateLbl = new JLabel(TemplateType.ENTITY_SUPERCLASS.readableName());
        this.embeddableTemplateLbl = new JLabel(TemplateType.EMBEDDABLE_SUBCLASS.readableName());
        this.embeddableSuperTemplateLbl = new JLabel(TemplateType.EMBEDDABLE_SUPERCLASS.readableName());
        this.datamapTemplateLbl = new JLabel(TemplateType.DATAMAP_SUBCLASS.readableName());
        this.datamapSuperTemplateLbl = new JLabel(TemplateType.DATAMAP_SUPERCLASS.readableName());

        initLayout();
        initBindings();
    }

    public CgenPanel getCodeGeneratorController() {
        return cgen;
    }

    public void initForm(CgenConfiguration cgenConfiguration) {
        this.cgenConfiguration = cgenConfiguration;

        if (cgenConfiguration.getRootPath() != null) {
            outputFolder.setText(cgenConfiguration.buildOutputPath().toString());
            applyOutputFolder(outputFolder.getText());
        } else {
            // unsaved project: no project root to resolve a relative output path against,
            // so leave the field empty rather than running validation on a stale value
            outputFolder.setText("");
        }
        if (cgenConfiguration.getArtifactsGenerationMode().equalsIgnoreCase("all")) {
            cgen.setCurrentClass(cgenConfiguration.getDataMap());
            cgen.setSelected(true);
        }
        outputPattern.setText(cgenConfiguration.getOutputPattern());
        pairs.setSelected(cgenConfiguration.isMakePairs());
        usePackagePath.setSelected(cgenConfiguration.isUsePkgPath());
        overwrite.setSelected(cgenConfiguration.isOverwrite());
        createPropertyNames.setSelected(cgenConfiguration.isCreatePropertyNames());
        pkProperties.setSelected(cgenConfiguration.isCreatePKProperties());
        superPkg.setText(cgenConfiguration.getSuperPkg());
        updateTemplatesLabels(cgenConfiguration);
    }

    public void updateTemplatesLabels(CgenConfiguration configuration) {
        updateTemplateLabel(entityTemplateLbl, TemplateType.ENTITY_SUBCLASS, configuration.getTemplate().getData());
        updateTemplateLabel(entitySuperTemplateLbl, TemplateType.ENTITY_SUPERCLASS, configuration.getSuperTemplate().getData());
        updateTemplateLabel(embeddableTemplateLbl, TemplateType.EMBEDDABLE_SUBCLASS, configuration.getEmbeddableTemplate().getData());
        updateTemplateLabel(embeddableSuperTemplateLbl, TemplateType.EMBEDDABLE_SUPERCLASS, configuration.getEmbeddableSuperTemplate().getData());
        updateTemplateLabel(datamapTemplateLbl, TemplateType.DATAMAP_SUBCLASS, configuration.getDataMapTemplate().getData());
        updateTemplateLabel(datamapSuperTemplateLbl, TemplateType.DATAMAP_SUPERCLASS, configuration.getDataMapSuperTemplate().getData());
    }

    private void updateTemplateLabel(JLabel label, TemplateType type, String template) {
        if (!TemplateType.isDefault(template)) {
            label.setText(type.readableName() + EDITED);
        } else {
            label.setText(type.readableName());
        }
    }

    /**
     * Locks or unlocks template-edit buttons depending on the state of the cgen panel
     * (entities/embeddables/datamap selection, MakePairs flag, and template editor window state).
     */
    public void updateTemplateEditorButtons() {
        boolean isMakePairs = pairs.isSelected();
        boolean isEntitiesSelected = cgen.isEntitiesSelected();
        boolean isEmbeddableSelected = cgen.isEmbeddableSelected();
        boolean isDataMapSelected = cgen.isDataMapSelected();

        editSubclassTemplateBtn.setEnabled(isEntitiesSelected && !isEditorOpen);
        editSuperclassTemplateBtn.setEnabled(isMakePairs && isEntitiesSelected && !isEditorOpen);

        editEmbeddableTemplateBtn.setEnabled(isEmbeddableSelected && !isEditorOpen);
        editEmbeddableSuperTemplateBtn.setEnabled(isMakePairs && isEmbeddableSelected && !isEditorOpen);

        editDataMapTemplateBtn.setEnabled(isDataMapSelected && !isEditorOpen);
        editDataMapSuperTemplateBtn.setEnabled(isMakePairs && isDataMapSelected && !isEditorOpen);

        setToolTipText(editSubclassTemplateBtn);
        setToolTipText(editSuperclassTemplateBtn);
        setToolTipText(editEmbeddableTemplateBtn);
        setToolTipText(editEmbeddableSuperTemplateBtn);
        setToolTipText(editDataMapTemplateBtn);
        setToolTipText(editDataMapSuperTemplateBtn);
    }

    public void setEditorOpen(boolean editorOpen) {
        isEditorOpen = editorOpen;
    }

    public boolean isDataValid() {
        return isDataValid;
    }

    public CMUndoableTextField getOutputFolder() {
        return outputFolder;
    }

    private void initLayout() {
        setLayout(new BorderLayout());
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "left:10dlu, 3dlu, 97dlu, 3dlu, 40dlu, 3dlu, 50dlu, 3dlu, 20dlu",
                "p, 3dlu, p, 10dlu, 11*(p, 3dlu),10dlu,9*(p, 3dlu)");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();
        builder.addLabel("Output Directory", cc.xyw(1, 1, 3));
        builder.add(outputFolder, cc.xyw(1, 3, 7));
        builder.add(selectOutputFolder, cc.xy(9, 3));

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
        builder.add(outputPattern, cc.xyw(1, 21, 5));

        builder.addLabel("Superclass package", cc.xyw(1, 23, 5));
        builder.add(superPkg, cc.xyw(1, 25, 5));

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
        builder.getPanel().setBorder(CGEN_PANEL_BORDER);

        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        superPkg.addCommitListener(text -> {
            cgenConfiguration.setSuperPkg(text);
            cgen.checkCgenConfigDirty();
        });

        outputPattern.addCommitListener(text -> {
            cgenConfiguration.setOutputPattern(text);
            cgen.checkCgenConfigDirty();
        });

        outputFolder.addCommitListener(this::applyOutputFolder);

        pairs.addActionListener(val -> {
            cgenConfiguration.setMakePairs(pairs.isSelected());
            if (!pairs.isSelected()) {
                setSingleclassForDefaults();
            } else {
                setSubclassForDefaults();
            }
            updateTemplateEditorButtons();
            initForm(cgenConfiguration);
            cgen.checkCgenConfigDirty();
        });

        overwrite.addActionListener(val -> {
            cgenConfiguration.setOverwrite(overwrite.isSelected());
            cgen.checkCgenConfigDirty();
        });

        createPropertyNames.addActionListener(val -> {
            cgenConfiguration.setCreatePropertyNames(createPropertyNames.isSelected());
            cgen.checkCgenConfigDirty();
        });

        usePackagePath.addActionListener(val -> {
            cgenConfiguration.setUsePkgPath(usePackagePath.isSelected());
            cgen.checkCgenConfigDirty();
        });

        pkProperties.addActionListener(val -> {
            cgenConfiguration.setCreatePKProperties(pkProperties.isSelected());
            cgen.checkCgenConfigDirty();
        });

        editSubclassTemplateBtn.addActionListener(val ->
                new TemplateEditor(app, this, TemplateType.ENTITY_SUBCLASS).open());

        editSuperclassTemplateBtn.addActionListener(val ->
                new TemplateEditor(app, this, TemplateType.ENTITY_SUPERCLASS).open());

        editEmbeddableTemplateBtn.addActionListener(val ->
                new TemplateEditor(app, this, TemplateType.EMBEDDABLE_SUBCLASS).open());

        editEmbeddableSuperTemplateBtn.addActionListener(val ->
                new TemplateEditor(app, this, TemplateType.EMBEDDABLE_SUPERCLASS).open());

        editDataMapTemplateBtn.addActionListener(val ->
                new TemplateEditor(app, this, TemplateType.DATAMAP_SUBCLASS).open());

        editDataMapSuperTemplateBtn.addActionListener(val ->
                new TemplateEditor(app, this, TemplateType.DATAMAP_SUPERCLASS).open());

        selectOutputFolder.addActionListener(e -> selectOutputFolderAction());
    }

    void applyOutputFolder(String text) {
        if (cgenConfiguration == null) {
            return;
        }
        Path path;
        try {
            path = Paths.get(text == null ? "" : text);
        } catch (InvalidPathException e) {
            updateGenerateButton(false);
            throw new ValidationException(INVALID_PATH_MSG);
        }
        if (cgenConfiguration.getRootPath() == null && !path.isAbsolute()) {
            updateGenerateButton(false);
            throw new ValidationException(NEED_TO_SAVE_PROJECT_MSG);
        }
        cgenConfiguration.updateOutputPath(path);
        updateGenerateButton(true);
        cgen.checkCgenConfigDirty();
    }

    private void updateGenerateButton(boolean isDataValid) {
        this.isDataValid = isDataValid;
        cgen.updateGenerateButton();
    }

    private void setToolTipText(JButton button) {
        if (button.isEnabled()) {
            button.setToolTipText("Open template editor");
        } else {
            button.setToolTipText("At least one artefact of appropriate type must be selected." +
                    " The Make Pairs checkbox can also affect the blocking");
        }
    }

    private void setSubclassForDefaults() {
        if (TemplateType.isDefault(cgenConfiguration.getTemplate().getData())) {
            cgenConfiguration.setTemplate(TemplateType.ENTITY_SUBCLASS.defaultTemplate());
        }
        if (TemplateType.isDefault(cgenConfiguration.getEmbeddableTemplate().getData())) {
            cgenConfiguration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SUBCLASS.defaultTemplate());
        }
        if (TemplateType.isDefault(cgenConfiguration.getDataMapTemplate().getData())) {
            cgenConfiguration.setDataMapTemplate(TemplateType.DATAMAP_SUBCLASS.defaultTemplate());
        }
    }

    private void setSingleclassForDefaults() {
        if (TemplateType.isDefault(cgenConfiguration.getTemplate().getData())) {
            cgenConfiguration.setTemplate(TemplateType.ENTITY_SINGLE_CLASS.defaultTemplate());
        }
        if (TemplateType.isDefault(cgenConfiguration.getEmbeddableTemplate().getData())) {
            cgenConfiguration.setEmbeddableTemplate(TemplateType.EMBEDDABLE_SINGLE_CLASS.defaultTemplate());
        }
        if (TemplateType.isDefault(cgenConfiguration.getDataMapTemplate().getData())) {
            cgenConfiguration.setDataMapTemplate(TemplateType.DATAMAP_SINGLE_CLASS.defaultTemplate());
        }
    }

    private void selectOutputFolderAction() {
        String currentDir = outputFolder.getText();
        File initialDir = !Util.isEmptyString(currentDir)
                ? new File(currentDir)
                : CgenOps.baseDir(session).toFile();

        File selected = app.getFileChooser(this, "Select Output Folder").openDir(initialDir);
        if (selected != null) {
            String path = selected.getAbsolutePath();
            outputFolder.setText(path);
            applyOutputFolder(path);
        }
    }
}
