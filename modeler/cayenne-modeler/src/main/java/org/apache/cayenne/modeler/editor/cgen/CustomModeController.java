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

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.dialog.cgen.TemplateDialog;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.swing.BindingBuilder;

import javax.swing.DefaultComboBoxModel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * @since 4.1
 * A controller for the custom generation mode.
 */
public class CustomModeController extends GeneratorController<CustomModePanel> {

    public CustomModeController(CodeGeneratorController parent) {
        super(parent);
        bind();
        initClientModeListener();
    }

    @Override
    protected CustomModePanel createView() {
        return new CustomModePanel(getParentController());
    }


    @Override
    public CustomModePanel getView() {
        return view;
    }

    private void bind() {
        BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);
        builder.bindToAction(view.getTemplateManagerButton(), "popPreferencesAction()");

    }

    private void updateTemplates() {
        boolean isClient = cgenConfiguration.isClient();
        CodeTemplateManager templateManager = getApplication().getCodeTemplateManager();

        List<String> customTemplates = new ArrayList<>(templateManager.getCustomTemplates().keySet());
        Collections.sort(customTemplates);

        List<String> superTemplates = isClient ?
                new ArrayList<>(templateManager.getStandardClientSuperclassTemplates()) :
                new ArrayList<>(templateManager.getStandardSuperclassTemplates());
        Collections.sort(superTemplates);
        superTemplates.addAll(customTemplates);

        List<String> subTemplates = isClient ?
                new ArrayList<>(templateManager.getStandardClientSubclassTemplates()) :
                new ArrayList<>(templateManager.getStandardSubclassTemplates());
        Collections.sort(subTemplates);
        subTemplates.addAll(customTemplates);

        List<String> querySuperTemplates = isClient ?
                new ArrayList<>(templateManager.getStandardClientDataMapSuperclassTemplates()) :
                new ArrayList<>(templateManager.getStandartDataMapSuperclassTemplates());
        Collections.sort(querySuperTemplates);
        querySuperTemplates.addAll(customTemplates);

        List<String> queryTemplates = isClient ?
                new ArrayList<>(templateManager.getStandardClientDataMapTemplates()) :
                new ArrayList<>(templateManager.getStandartDataMapTemplates());
        Collections.sort(queryTemplates);
        queryTemplates.addAll(customTemplates);

        List<String> embeddableSuperTemplates = new ArrayList<>(templateManager.getStandartEmbeddableSuperclassTemplates());
		Collections.sort(embeddableSuperTemplates);
		embeddableSuperTemplates.addAll(customTemplates);

		List<String> embeddableTemplates = new ArrayList<>(templateManager.getStandartEmbeddableTemplates());
		Collections.sort(embeddableTemplates);
		embeddableTemplates.addAll(customTemplates);

        this.view.getSubclassTemplate().getComboBox().setModel(new DefaultComboBoxModel<>(subTemplates.toArray(new String[0])));
        this.view.getSuperclassTemplate().getComboBox().setModel(new DefaultComboBoxModel<>(superTemplates.toArray(new String[0])));
        this.view.getQueryTemplate().getComboBox().setModel(new DefaultComboBoxModel<>(queryTemplates.toArray(new String[0])));
        this.view.getQuerySuperTemplate().getComboBox().setModel(new DefaultComboBoxModel<>(querySuperTemplates.toArray(new String[0])));
        this.view.getEmbeddableTemplate().getComboBox().setModel(new DefaultComboBoxModel<>(embeddableTemplates.toArray(new String[0])));
        this.view.getEmbeddableSuperTemplate().getComboBox().setModel(new DefaultComboBoxModel<>(embeddableSuperTemplates.toArray(new String[0])));
    }

    public void popPreferencesAction() {
        new PreferenceDialog(getApplication().getFrameController()).startupAction(PreferenceDialog.TEMPLATES_KEY);
        updateTemplates();
        updateComboBoxes();
    }

    private void missTemplateDialog(CgenConfiguration cgenConfiguration, String template, String superTemplate) {
        new TemplateDialog(this, cgenConfiguration, template, superTemplate).startupAction();
        updateComboBoxes();
    }

    public void addTemplateAction(String template, String superTemplate) {
        new PreferenceDialog(getApplication().getFrameController()).startupToCreateTemplate(template, superTemplate);
        updateTemplates();
    }

    private String getTemplateName(Supplier<String> supplier, Path path) {
        return getApplication().getCodeTemplateManager().getNameByPath(
                supplier.get(), path);
    }

    private String getPath(Supplier<String> supplier, Path rootPath) {
        if (rootPath != null) {
            return rootPath.resolve(Paths.get(supplier.get())).normalize().toString();
        } else {
            return (Paths.get(supplier.get()).normalize().toString());
        }
    }


    private void updateComboBoxes() {
        Path rootPath = cgenConfiguration.getRootPath();

        String templateName = getTemplateName(cgenConfiguration::getTemplate, rootPath);
        String superTemplateName = getTemplateName(cgenConfiguration::getSuperTemplate, rootPath);
        String embeddableTemplateName = getTemplateName(cgenConfiguration::getEmbeddableTemplate, rootPath);
        String embeddableSuperTemplateName = getTemplateName(cgenConfiguration::getEmbeddableSuperTemplate, rootPath);
        String queryTemplateName = getTemplateName(cgenConfiguration::getQueryTemplate, rootPath);
        String querySuperTemplateName = getTemplateName(cgenConfiguration::getQuerySuperTemplate, rootPath);

        String path = getPath(cgenConfiguration::getTemplate, rootPath);
        String superPath = getPath(cgenConfiguration::getSuperTemplate, rootPath);
        String embeddableTemplatePath = getPath(cgenConfiguration::getEmbeddableTemplate, rootPath);
        String embeddableSuperTemplatePath = getPath(cgenConfiguration::getEmbeddableSuperTemplate, rootPath);
        String queryTemplatePath = getPath(cgenConfiguration::getQueryTemplate, rootPath);
        String querySuperTemplatePath = getPath(cgenConfiguration::getQuerySuperTemplate, rootPath);

        if (templateName == null && superTemplateName == null) {
            view.getSubclassTemplate().setItem(null);
            view.getSuperclassTemplate().setItem(null);
            missTemplateDialog(cgenConfiguration, path, superPath);
        } else if (templateName == null) {
            view.getSubclassTemplate().setItem(null);
            missTemplateDialog(cgenConfiguration, path, null);
        } else if (superTemplateName == null) {
            view.getSuperclassTemplate().setItem(null);
            missTemplateDialog(cgenConfiguration, null, superPath);
        } else {
            view.getSubclassTemplate().setItem(templateName);
            view.getSuperclassTemplate().setItem(superTemplateName);
        }

        if(embeddableTemplateName == null && embeddableSuperTemplateName == null) {
        	missTemplateDialog(cgenConfiguration, embeddableTemplatePath, embeddableSuperTemplatePath);
        } else if(embeddableTemplateName == null) {
        	missTemplateDialog(cgenConfiguration, embeddableTemplatePath, null);
        } else if(embeddableSuperTemplateName == null) {
        	missTemplateDialog(cgenConfiguration, null, embeddableSuperTemplatePath);
        }
    	view.getEmbeddableTemplate().setItem(embeddableTemplateName);
    	view.getEmbeddableSuperTemplate().setItem(embeddableSuperTemplateName);

    	if(queryTemplateName == null && querySuperTemplateName == null) {
    		missTemplateDialog(cgenConfiguration, queryTemplatePath, querySuperTemplatePath);
    	} else if(queryTemplateName == null) {
    		missTemplateDialog(cgenConfiguration, queryTemplatePath, null);
    	} else if(querySuperTemplateName == null) {
    		missTemplateDialog(cgenConfiguration, null, querySuperTemplatePath);
    	}
    	view.getQueryTemplate().setItem(queryTemplateName);
    	view.getQuerySuperTemplate().setItem(querySuperTemplateName);

        view.setDisableSuperComboBoxes(view.getPairs().isSelected());
    }


    private void initClientModeListener() {
        view.getClientMode().addActionListener(val -> {
            boolean isSelected = view.getClientMode().isSelected();
            cgenConfiguration.setClient(isSelected);
            if (isSelected) {
                cgenConfiguration.setTemplate(ClientClassGenerationAction.SUBCLASS_TEMPLATE);
                cgenConfiguration.setSuperTemplate(ClientClassGenerationAction.SUPERCLASS_TEMPLATE);
                cgenConfiguration.setQueryTemplate(ClientClassGenerationAction.DMAP_SUBCLASS_TEMPLATE);
                cgenConfiguration.setQuerySuperTemplate(ClientClassGenerationAction.DMAP_SUPERCLASS_TEMPLATE);
            } else {
                cgenConfiguration.setTemplate(ClassGenerationAction.SUBCLASS_TEMPLATE);
                cgenConfiguration.setSuperTemplate(ClassGenerationAction.SUPERCLASS_TEMPLATE);
                cgenConfiguration.setQueryTemplate(ClassGenerationAction.DATAMAP_SUBCLASS_TEMPLATE);
                cgenConfiguration.setQuerySuperTemplate(ClassGenerationAction.DATAMAP_SUPERCLASS_TEMPLATE);
            }
            updateTemplates();
            String templateName = getApplication().getCodeTemplateManager().getNameByPath(
                    isSelected ?
                            ClientClassGenerationAction.SUBCLASS_TEMPLATE :
                            ClassGenerationAction.SUBCLASS_TEMPLATE,
                    cgenConfiguration.getRootPath());
            String superTemplateName = getApplication().getCodeTemplateManager().getNameByPath(
                    isSelected ?
                            ClientClassGenerationAction.SUBCLASS_TEMPLATE :
                            ClassGenerationAction.SUBCLASS_TEMPLATE,
                    cgenConfiguration.getRootPath());
            String queryTemplateName = getApplication().getCodeTemplateManager().getNameByPath(
            		isSelected ?
            				ClientClassGenerationAction.DMAP_SUBCLASS_TEMPLATE :
            					ClassGenerationAction.DATAMAP_SUBCLASS_TEMPLATE,
            					cgenConfiguration.getRootPath());
            String querySuperTemplateName = getApplication().getCodeTemplateManager().getNameByPath(
            		isSelected ?
            				ClientClassGenerationAction.DMAP_SUPERCLASS_TEMPLATE :
            					ClassGenerationAction.DATAMAP_SUPERCLASS_TEMPLATE,
            					cgenConfiguration.getRootPath());
            view.getSubclassTemplate().setItem(templateName);
            view.getSuperclassTemplate().setItem(superTemplateName);
            view.getQueryTemplate().setItem(queryTemplateName);
            view.getQuerySuperTemplate().setItem(querySuperTemplateName);
            getParentController().checkCgenConfigDirty();
        });
    }

    @Override
    public void initForm(CgenConfiguration cgenConfiguration) {
        super.initForm(cgenConfiguration);
        view.getClientMode().setSelected(cgenConfiguration.isClient());
        updateTemplates();
        view.getOutputPattern().setText(cgenConfiguration.getOutputPattern());
        updateComboBoxes();
    }

    @Override
    public void updateConfiguration(CgenConfiguration cgenConfiguration) {
        // Do nothing
    }
}
