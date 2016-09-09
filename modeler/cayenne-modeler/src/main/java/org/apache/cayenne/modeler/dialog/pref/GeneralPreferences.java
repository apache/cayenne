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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.pref.CayennePreferenceEditor;
import org.apache.cayenne.pref.PreferenceEditor;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;

public class GeneralPreferences extends CayenneController {

	public static final String AUTO_LOAD_PROJECT_PREFERENCE = "autoLoadProject";
	public static final String DELETE_PROMPT_PREFERENCE = "deletePrompt";
	public static final String ENCODING_PREFERENCE = "encoding";

	protected GeneralPreferencesView view;
	protected CayennePreferenceEditor editor;

	protected boolean autoLoadProjectPreference;
	protected String encoding;
	protected boolean deletePromptPreference;

	protected Preferences preferences;

	protected ObjectBinding encodingBinding;
	protected ObjectBinding autoLoadProjectBinding;
	protected ObjectBinding deletePromptBinding;

	public GeneralPreferences(PreferenceDialog parentController) {
		super(parentController);
		this.view = new GeneralPreferencesView();

		PreferenceEditor editor = parentController.getEditor();
		if (editor instanceof CayennePreferenceEditor) {
			this.editor = (CayennePreferenceEditor) editor;
			this.view.setEnabled(true);
			initBindings();

			encodingBinding.updateView();
			autoLoadProjectBinding.updateView();
			deletePromptBinding.updateView();
		} else {
			this.view.setEnabled(false);
		}
	}

	public Component getView() {
		return view;
	}

	protected void initBindings() {
		// init model objects
		preferences = application.getPreferencesNode(GeneralPreferences.class, "");

		this.encoding = preferences.get(ENCODING_PREFERENCE, null);

		this.autoLoadProjectPreference = preferences.getBoolean(AUTO_LOAD_PROJECT_PREFERENCE, false);
		this.deletePromptPreference = preferences.getBoolean(DELETE_PROMPT_PREFERENCE, false);

		// build child controllers...
		EncodingSelector encodingSelector = new EncodingSelector(this, view.getEncodingSelector());

		// create bindings...
		BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);

		this.encodingBinding = builder.bindToProperty(encodingSelector, "encoding",
				EncodingSelector.ENCODING_PROPERTY_BINDING);

		this.autoLoadProjectBinding = builder.bindToCheckBox(view.getAutoLoadProject(), "autoLoadProject");

		this.deletePromptBinding = builder.bindToCheckBox(view.getDeletePrompt(), "deletePrompt");
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		addChangedPreferences(ENCODING_PREFERENCE, encoding);
		this.encoding = encoding;
	}

	public boolean getAutoLoadProject() {
		return autoLoadProjectPreference;
	}

	public void setAutoLoadProject(boolean autoLoadProject) {

		addChangedBooleanPreferences(AUTO_LOAD_PROJECT_PREFERENCE, autoLoadProject);
		this.autoLoadProjectPreference = autoLoadProject;
	}

	public boolean getDeletePrompt() {
		return deletePromptPreference;
	}

	public void setDeletePrompt(boolean deletePrompt) {

		addChangedBooleanPreferences(DELETE_PROMPT_PREFERENCE, deletePrompt);
		this.deletePromptPreference = deletePrompt;
	}

	public void addChangedBooleanPreferences(String key, boolean value) {
		Map<String, Boolean> map = editor.getChangedBooleanPreferences().get(preferences);
		if (map == null) {
			map = new HashMap<>();
		}
		map.put(key, value);
		editor.getChangedBooleanPreferences().put(preferences, map);
	}

	public void addChangedPreferences(String key, String value) {
		Map<String, String> map = editor.getChangedPreferences().get(preferences);
		if (map == null) {
			map = new HashMap<>();
		}
		map.put(key, value);
		editor.getChangedPreferences().put(preferences, map);
	}
}
