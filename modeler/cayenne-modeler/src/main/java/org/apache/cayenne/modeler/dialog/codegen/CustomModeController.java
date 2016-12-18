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

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.CodeTemplateManager;
import org.apache.cayenne.modeler.dialog.pref.PreferenceDialog;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.util.Util;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * A controller for the custom generation mode.
 */
public class CustomModeController extends GeneratorController {

	// correspond to non-public constants on MapClassGenerator.
	static final String MODE_DATAMAP = "datamap";
	static final String MODE_ENTITY = "entity";
	static final String MODE_ALL = "all";

	static final String DATA_MAP_MODE_LABEL = "DataMap generation";
	static final String ENTITY_MODE_LABEL = "Entity and Embeddable generation";
	static final String ALL_MODE_LABEL = "Generate all";

	static final Map<String, String> modesByLabel = new HashMap<>();

	static {
		modesByLabel.put(DATA_MAP_MODE_LABEL, MODE_DATAMAP);
		modesByLabel.put(ENTITY_MODE_LABEL, MODE_ENTITY);
		modesByLabel.put(ALL_MODE_LABEL, MODE_ALL);
	}

	protected CustomModePanel view;
	protected CodeTemplateManager templateManager;

	protected ObjectBinding superTemplate;
	protected ObjectBinding subTemplate;

	private CustomPreferencesUpdater preferencesUpdater;

	public CustomPreferencesUpdater getCustomPreferencesUpdater() {
		return preferencesUpdater;
	}

	public CustomModeController(CodeGeneratorControllerBase parent) {
		super(parent);

		Object[] modeChoices = new Object[] { ENTITY_MODE_LABEL, DATA_MAP_MODE_LABEL, ALL_MODE_LABEL };
		view.getGenerationMode().setModel(new DefaultComboBoxModel(modeChoices));

		// bind preferences and init defaults...

		Set<Entry<DataMap, DataMapDefaults>> entities = getMapPreferences().entrySet();

		for (Entry<DataMap, DataMapDefaults> entry : entities) {

			if (Util.isEmptyString(entry.getValue().getSuperclassTemplate())) {
				entry.getValue().setSuperclassTemplate(CodeTemplateManager.STANDARD_SERVER_SUPERCLASS);
			}

			if (Util.isEmptyString(entry.getValue().getSubclassTemplate())) {
				entry.getValue().setSubclassTemplate(CodeTemplateManager.STANDARD_SERVER_SUBCLASS);
			}

			if (Util.isEmptyString(entry.getValue().getProperty("mode"))) {
				entry.getValue().setProperty("mode", MODE_ENTITY);
			}

			if (Util.isEmptyString(entry.getValue().getProperty("overwrite"))) {
				entry.getValue().setBooleanProperty("overwrite", false);
			}

			if (Util.isEmptyString(entry.getValue().getProperty("pairs"))) {
				entry.getValue().setBooleanProperty("pairs", true);
			}

			if (Util.isEmptyString(entry.getValue().getProperty("usePackagePath"))) {
				entry.getValue().setBooleanProperty("usePackagePath", true);
			}

			if (Util.isEmptyString(entry.getValue().getProperty("outputPattern"))) {
				entry.getValue().setProperty("outputPattern", "*.java");
			}
		}

		BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);

		builder.bindToAction(view.getManageTemplatesLink(), "popPreferencesAction()");

		builder.bindToComboSelection(view.getGenerationMode(), "customPreferencesUpdater.mode").updateView();

		builder.bindToStateChange(view.getOverwrite(), "customPreferencesUpdater.overwrite").updateView();

		builder.bindToStateChange(view.getPairs(), "customPreferencesUpdater.pairs").updateView();

		builder.bindToStateChange(view.getUsePackagePath(), "customPreferencesUpdater.usePackagePath").updateView();

		subTemplate = builder.bindToComboSelection(view.getSubclassTemplate(),
				"customPreferencesUpdater.subclassTemplate");

		superTemplate = builder.bindToComboSelection(view.getSuperclassTemplate(),
				"customPreferencesUpdater.superclassTemplate");

		builder.bindToTextField(view.getOutputPattern(), "customPreferencesUpdater.outputPattern").updateView();

		builder.bindToStateChange(view.getCreatePropertyNames(), "customPreferencesUpdater.createPropertyNames")
				.updateView();

		updateTemplates();
	}

	protected void createDefaults() {
		TreeMap<DataMap, DataMapDefaults> map = new TreeMap<DataMap, DataMapDefaults>();
		Collection<DataMap> dataMaps = getParentController().getDataMaps();
		for (DataMap dataMap : dataMaps) {
			DataMapDefaults preferences;
			preferences = getApplication().getFrameController().getProjectController()
					.getDataMapPreferences(this.getClass().getName().replace(".", "/"), dataMap);
			preferences.setSuperclassPackage("");
			preferences.updateSuperclassPackage(dataMap, false);

			map.put(dataMap, preferences);

			if (getOutputPath() == null) {
				setOutputPath(preferences.getOutputPath());
			}
		}

		setMapPreferences(map);
		preferencesUpdater = new CustomPreferencesUpdater(map);
	}

	protected GeneratorControllerPanel createView() {
		this.view = new CustomModePanel();

		Set<Entry<DataMap, DataMapDefaults>> entities = getMapPreferences().entrySet();
		for (Entry<DataMap, DataMapDefaults> entry : entities) {
			StandardPanelComponent dataMapLine = createDataMapLineBy(entry.getKey(), entry.getValue());
			dataMapLine.getDataMapName().setText(dataMapLine.getDataMap().getName());
			BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), dataMapLine);
			builder.bindToTextField(dataMapLine.getSuperclassPackage(), "preferences.superclassPackage").updateView();
			this.view.addDataMapLine(dataMapLine);
		}
		return view;
	}

	private StandardPanelComponent createDataMapLineBy(DataMap dataMap, DataMapDefaults preferences) {
		StandardPanelComponent dataMapLine = new StandardPanelComponent();
		dataMapLine.setDataMap(dataMap);
		dataMapLine.setPreferences(preferences);

		return dataMapLine;
	}

	protected void updateTemplates() {
		this.templateManager = getApplication().getCodeTemplateManager();

		List<String> customTemplates = new ArrayList<>(templateManager.getCustomTemplates().keySet());
		Collections.sort(customTemplates);

		List<String> superTemplates = new ArrayList<>(templateManager.getStandardSuperclassTemplates());
		Collections.sort(superTemplates);
		superTemplates.addAll(customTemplates);

		List<String> subTemplates = new ArrayList<>(templateManager.getStandardSubclassTemplates());
		Collections.sort(subTemplates);
		subTemplates.addAll(customTemplates);

		this.view.getSubclassTemplate().setModel(new DefaultComboBoxModel(subTemplates.toArray()));
		this.view.getSuperclassTemplate().setModel(new DefaultComboBoxModel(superTemplates.toArray()));

		superTemplate.updateView();
		subTemplate.updateView();
	}

	public Component getView() {
		return view;
	}

	public Collection<ClassGenerationAction> createGenerator() {

		mode = modesByLabel.get(view.getGenerationMode().getSelectedItem()).toString();

		Collection<ClassGenerationAction> generators = super.createGenerator();

		String superKey = view.getSuperclassTemplate().getSelectedItem().toString();
		String superTemplate = templateManager.getTemplatePath(superKey);

		String subKey = view.getSubclassTemplate().getSelectedItem().toString();
		String subTemplate = templateManager.getTemplatePath(subKey);

		for (ClassGenerationAction generator : generators) {
			generator.setSuperTemplate(superTemplate);
			generator.setTemplate(subTemplate);
			generator.setOverwrite(view.getOverwrite().isSelected());
			generator.setUsePkgPath(view.getUsePackagePath().isSelected());
			generator.setMakePairs(view.getPairs().isSelected());
			generator.setCreatePropertyNames(view.getCreatePropertyNames().isSelected());

			if (!Util.isEmptyString(view.getOutputPattern().getText())) {
				generator.setOutputPattern(view.getOutputPattern().getText());
			}
		}

		return generators;
	}

	public void popPreferencesAction() {
		new PreferenceDialog(getApplication().getFrameController()).startupAction(PreferenceDialog.TEMPLATES_KEY);
		updateTemplates();
	}

	@Override
	protected ClassGenerationAction newGenerator() {
		return new ClassGenerationAction();
	}
}
