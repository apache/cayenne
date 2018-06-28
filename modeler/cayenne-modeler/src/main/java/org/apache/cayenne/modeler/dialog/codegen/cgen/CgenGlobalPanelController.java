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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * @since 4.1
 */
public class CgenGlobalPanelController extends CayenneController{

    private static final String ALL_MODE_LABEL = "Generate all";
    private static final Map<String, String> modesByLabel = new HashMap<>();
    // correspond to non-public constants on MapClassGenerator.
    private static final String MODE_DATAMAP = "datamap";
    private static final String MODE_ENTITY = "entity";
    private static final String MODE_ALL = "all";
    private static final String DATA_MAP_MODE_LABEL = "DataMap generation";
    private static final String ENTITY_MODE_LABEL = "Entity and Embeddable generation";
    private static final Map<String, String> labelByMode = new HashMap<>();
    private static Logger logObj = LoggerFactory.getLogger(CgenGlobalPanelController.class);

    static {
        modesByLabel.put(DATA_MAP_MODE_LABEL, MODE_DATAMAP);
        modesByLabel.put(ENTITY_MODE_LABEL, MODE_ENTITY);
        modesByLabel.put(ALL_MODE_LABEL, MODE_ALL);
        labelByMode.put(MODE_DATAMAP, DATA_MAP_MODE_LABEL);
        labelByMode.put(MODE_ENTITY, ENTITY_MODE_LABEL);
        labelByMode.put(MODE_ALL, ALL_MODE_LABEL);
    }

    protected CgenGlobalPanel view;
    private ProjectController projectController;

    private Collection<ClassGenerationAction> generators;

    CgenGlobalPanelController(CayenneController parent) {
        super(parent);
        this.projectController = Application.getInstance().getFrameController().getProjectController();

        this.view = new CgenGlobalPanel(projectController);
        this.generators = new HashSet<>();
        initSources();

        updateTemplates();
        initButtons();
    }

    private void updateTemplates() {
        String[] modeChoices = new String[]{ENTITY_MODE_LABEL, DATA_MAP_MODE_LABEL, ALL_MODE_LABEL};
        view.getGenerationMode().setModel(new DefaultComboBoxModel<>(modeChoices));

        CodeTemplateManager templateManager = getApplication().getCodeTemplateManager();

        java.util.List<String> customTemplates = new ArrayList<>(templateManager.getCustomTemplates().keySet());
        Collections.sort(customTemplates);

        java.util.List<String> superTemplates = new ArrayList<>(templateManager.getStandardSuperclassTemplates());
        Collections.sort(superTemplates);
        superTemplates.addAll(customTemplates);

        java.util.List<String> subTemplates = new ArrayList<>(templateManager.getStandardSubclassTemplates());
        Collections.sort(subTemplates);
        subTemplates.addAll(customTemplates);

        java.util.List<String> embeddableTemplates = new ArrayList<>(templateManager.getStandartEmbeddableTemplates());
        Collections.sort(embeddableTemplates);
        embeddableTemplates.addAll(customTemplates);

        java.util.List<String> embeddableSuperTemplates = new ArrayList<>(templateManager.getStandartEmbeddableSuperclassTemplates());
        Collections.sort(embeddableSuperTemplates);
        embeddableSuperTemplates.addAll(customTemplates);

        java.util.List<String> dataMapTemplates = new ArrayList<>(templateManager.getStandartDataMapTemplates());
        Collections.sort(dataMapTemplates);
        dataMapTemplates.addAll(customTemplates);

        List<String> dataMapSuperTemplates = new ArrayList<>(templateManager.getStandartDataMapSuperclassTemplates());
        Collections.sort(dataMapSuperTemplates);
        dataMapSuperTemplates.addAll(customTemplates);

        this.view.getSubclassTemplate().setModel(new DefaultComboBoxModel<>(subTemplates.toArray(new String[0])));
        this.view.getSuperclassTemplate().setModel(new DefaultComboBoxModel<>(superTemplates.toArray(new String[0])));

        this.view.getEmbeddableTemplate().setModel(new DefaultComboBoxModel<>(embeddableTemplates.toArray(new String[0])));
        this.view.getEmbeddableSuperTemplate().setModel(new DefaultComboBoxModel<>(embeddableSuperTemplates.toArray(new String[0])));

        this.view.getDataMapTemplate().setModel(new DefaultComboBoxModel<>(dataMapTemplates.toArray(new String[0])));
        this.view.getDataMapSuperTemplate().setModel(new DefaultComboBoxModel<>(dataMapSuperTemplates.toArray(new String[0])));

        this.view.getOutputPattern().setText("*.java");
        this.view.getPairs().setSelected(true);
        this.view.getUsePackagePath().setSelected(true);

        this.view.getOutputFolder().setText(System.getProperty("user.home"));
    }

    private void initSources() {
        DataChannelMetaData metaData = getApplication().getMetaData();
        Project project = projectController.getProject();
        Collection<DataMap> dataMaps = ((DataChannelDescriptor) project.getRootNode()).getDataMaps();
        for (DataMap dataMap : dataMaps) {
            ClassGenerationAction classGenerationAction = metaData.get(dataMap, ClassGenerationAction.class);
            if (classGenerationAction != null) {
                generators.add(classGenerationAction);
            }
        }
    }

    private void initButtons() {
        this.view.getSelectOutputFolder().addActionListener(action -> {

            JTextField outputFolder = view.getOutputFolder();

            String currentDir = outputFolder.getText();

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogType(JFileChooser.OPEN_DIALOG);

            // guess start directory
            if (!Util.isEmptyString(currentDir)) {
                chooser.setCurrentDirectory(new File(currentDir));
            } else {
                FSPath lastDir = Application.getInstance().getFrameController().getLastDirectory();
                lastDir.updateChooser(chooser);
            }

                int result = chooser.showOpenDialog(getView());
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selected = chooser.getSelectedFile();

                    // update model
                    String path = selected.getAbsolutePath();
                    view.getOutputFolder().setText(path);
                }
        });

        this.view.getGenerateButton().addActionListener(action -> {
            try {
            for(ClassGenerationAction generator : generators) {
                generator.prepareArtifacts();
                generator.execute();
            }
            JOptionPane.showMessageDialog(
                    this.getApplication().getFrameController().getView(),
                    "Class generation finished");
            } catch (Exception ex) {
            logObj.error("Error generating classes", ex);
            JOptionPane.showMessageDialog(
                    this.getApplication().getFrameController().getView(),
                    "Error generating classes - " + ex.getMessage());
            }
        });

        this.view.getResetFolder().addActionListener(action ->
                generators.forEach(val -> {
                    val.setDestDir(new File(view.getOutputFolder().getText()));
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetMode().addActionListener(action ->
                generators.forEach(val -> {
                    val.setArtifactsGenerationMode(modesByLabel.get(String.valueOf(view.getGenerationMode().getSelectedItem())));
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetDataMapTemplate().addActionListener(action ->
                generators.forEach(val -> {
                    val.setQueryTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(view.getDataMapTemplate().getSelectedItem())));
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetDataMapSuperTemplate().addActionListener(action ->
                generators.forEach(val -> {
                    val.setQuerySuperTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(view.getDataMapSuperTemplate().getSelectedItem())));
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetTemplate().addActionListener(action ->
                generators.forEach(val -> {
                    val.setTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(view.getSubclassTemplate().getSelectedItem())));
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetSuperTemplate().addActionListener(action ->
                generators.forEach(val -> {
                    val.setSuperTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(view.getSuperclassTemplate().getSelectedItem())));
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetEmbeddableTemplate().addActionListener(action ->
                generators.forEach(val -> {
                    val.setEmbeddableTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(view.getEmbeddableTemplate().getSelectedItem())));
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetEmbeddableSuperTemplate().addActionListener(action ->
                generators.forEach(val -> {
                    val.setEmbeddableSuperTemplate(Application.getInstance().getCodeTemplateManager().getTemplatePath(String.valueOf(view.getEmbeddableSuperTemplate().getSelectedItem())));
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetEncoding().addActionListener(action ->
                generators.forEach(val -> {
                    val.setEncoding(view.getEncoding().getText());
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetPattern().addActionListener(action ->
                generators.forEach(val -> {
                    val.setOutputPattern(view.getOutputPattern().getText());
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetPairs().addActionListener(action ->
                generators.forEach(val -> {
                    val.setMakePairs(view.getPairs().isSelected());
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getUsePackagePath().addActionListener(action ->
                generators.forEach(val -> {
                    val.setUsePkgPath(view.getUsePackagePath().isSelected());
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetOverwrite().addActionListener(action ->
                generators.forEach(val -> {
                    val.setUsePkgPath(view.getUsePackagePath().isSelected());
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetNames().addActionListener(action ->
                generators.forEach(val -> {
                    val.setCreatePropertyNames(view.getCreatePropertyNames().isSelected());
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));

        this.view.getResetPackage().addActionListener(action ->
                generators.forEach(val -> {
                    val.setSuperPkg(view.getSuperclassPackage().getText());
                    update(val.getDataMap());
                    projectController.setDirty(true);
                }));
    }

    private void update(DataMap dataMap) {
        projectController.fireDataMapDisplayEvent(new DataMapDisplayEvent(this, dataMap, (DataChannelDescriptor)projectController.getProject().getRootNode()));

    }

    @Override
    public Component getView() {
        return view;
    }
}
