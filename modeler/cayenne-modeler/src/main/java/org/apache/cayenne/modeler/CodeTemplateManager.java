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

package org.apache.cayenne.modeler;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages code generation templates.
 */
public class CodeTemplateManager {

	public static final String STANDARD_SERVER_SUPERCLASS = "Standard Server Superclass";
	public static final String STANDARD_SERVER_SUBCLASS = "Standard Server Subclass";
	public static final String SINGLE_SERVER_CLASS = "Single Server Class";

	private static final String STANDARD_EMBEDDABLE_SUPERCLASS = "Standard Embeddable Superclass";
	private static final String STANDARD_EMBEDDABLE_SUBCLASS = "Standard Embeddable Subclass";
	private static final String SINGLE_EMBEDDABLE_CLASS = "Single Embeddable Class";

	private static final String STANDARD_SERVER_DATAMAP_SUPERCLASS = "Standard Server DataMap Superclass";
	private static final String STANDARD_SERVER_DATAMAP_SUBCLASS = "Standard Server DataMap Subclass";
	private static final String SINGLE_DATAMAP_CLASS = "Single DataMap Class";

	public static final String NODE_NAME = "codeTemplateManager";

	private List<String> defaultSubclassTemplates;
	private List<String> defaultSuperclassTemplates;
	private Map<String, String> customTemplates;
	private Map<String, String> reverseCustomTemplate;
	private Map<String, String> defaultTemplates;

	private List<String> defaultEmbeddableTemplates;
	private List<String> defaultEmbeddableSuperclassTemplates;

	private List<String> defaultServerDataMapTemplates;
	private List<String> defaultServerDataMapSuperclassTemplates;

	private Map<String, String> reverseDefaultsTemplates;

	private final Application application;
	private final Preferences templatePreferences;

	private static final Logger logger = LoggerFactory.getLogger(CodeTemplateManager.class);

	public Preferences getTemplatePreferences(Application application) {
		return application.getPreferencesNode(this.getClass(), NODE_NAME);
	}

	public CodeTemplateManager(Application application) {
		this.application = application;
		this.templatePreferences = getTemplatePreferences(application);
		defaultSuperclassTemplates = new ArrayList<>(2);
		defaultSuperclassTemplates.add(STANDARD_SERVER_SUPERCLASS);

		defaultSubclassTemplates = new ArrayList<>(2);
		defaultSubclassTemplates.add(SINGLE_SERVER_CLASS);
		defaultSubclassTemplates.add(STANDARD_SERVER_SUBCLASS);

		defaultEmbeddableTemplates = new ArrayList<>();
		defaultEmbeddableTemplates.add(STANDARD_EMBEDDABLE_SUBCLASS);
		defaultEmbeddableTemplates.add(SINGLE_EMBEDDABLE_CLASS);

		defaultEmbeddableSuperclassTemplates = new ArrayList<>();
		defaultEmbeddableSuperclassTemplates.add(STANDARD_EMBEDDABLE_SUPERCLASS);

		defaultServerDataMapTemplates = new ArrayList<>();
		defaultServerDataMapTemplates.add(STANDARD_SERVER_DATAMAP_SUBCLASS);
		defaultServerDataMapTemplates.add(SINGLE_DATAMAP_CLASS);

		defaultServerDataMapSuperclassTemplates = new ArrayList<>();
		defaultServerDataMapSuperclassTemplates.add(STANDARD_SERVER_DATAMAP_SUPERCLASS);

		updateCustomTemplates(getTemplatePreferences(application));
		reverseCustomTemplate = new HashMap<>();
		for(Map.Entry<String, String> entry : customTemplates.entrySet()){
			reverseCustomTemplate.put(entry.getValue(), entry.getKey());
		}

		defaultTemplates = new HashMap<>();
		defaultTemplates.put(STANDARD_SERVER_SUPERCLASS, ClassGenerationAction.SUPERCLASS_TEMPLATE);
		defaultTemplates.put(STANDARD_SERVER_SUBCLASS, ClassGenerationAction.SUBCLASS_TEMPLATE);
		defaultTemplates.put(SINGLE_SERVER_CLASS, ClassGenerationAction.SINGLE_CLASS_TEMPLATE);

		defaultTemplates.put(STANDARD_EMBEDDABLE_SUPERCLASS, ClassGenerationAction.EMBEDDABLE_SUPERCLASS_TEMPLATE);
		defaultTemplates.put(STANDARD_EMBEDDABLE_SUBCLASS, ClassGenerationAction.EMBEDDABLE_SUBCLASS_TEMPLATE);
		defaultTemplates.put(SINGLE_EMBEDDABLE_CLASS, ClassGenerationAction.EMBEDDABLE_SINGLE_CLASS_TEMPLATE);

		defaultTemplates.put(STANDARD_SERVER_DATAMAP_SUBCLASS, ClassGenerationAction.DATAMAP_SUBCLASS_TEMPLATE);
		defaultTemplates.put(SINGLE_DATAMAP_CLASS, ClassGenerationAction.DATAMAP_SINGLE_CLASS_TEMPLATE);
		defaultTemplates.put(STANDARD_SERVER_DATAMAP_SUPERCLASS, ClassGenerationAction.DATAMAP_SUPERCLASS_TEMPLATE);

		reverseDefaultsTemplates = new HashMap<>();
		for(Map.Entry<String, String> entry : defaultTemplates.entrySet()){
			reverseDefaultsTemplates.put(entry.getValue(), entry.getKey());
		}
	}

	/**
	 * Updates custom templates from preferences.
	 */
	public void updateCustomTemplates(Preferences preference) {
		String[] keys = {};
		try {
			keys = preference.childrenNames();
		} catch (BackingStoreException e) {
			logger.warn("Error reading preferences");
		}
		this.customTemplates = new HashMap<>(keys.length, 1);
		for (String key : keys) {
			FSPath path = new FSPath(preference.node(key));
			customTemplates.put(key, path.getPath());
		}
	}

	// TODO: andrus, 12/5/2007 - this should also take a "pairs" parameter to
	// correctly
	// assign standard templates
	public String getTemplatePath(String name, Resource rootPath) {
		Object value = customTemplates.get(name);
		if (value != null) {
			try {
				if(rootPath != null) {
						Path path = Paths.get(rootPath.getURL().toURI()).getParent();
						value = path.relativize(Paths.get((String) value));
				}
				return value.toString();
			} catch (URISyntaxException e) {
				logger.warn("Path for template named '{}' could not be resolved", name);
			}
		}
		value = defaultTemplates.get(name);
		return value != null ? value.toString() : null;
	}

	public String getNameByPath(String name, Path rootPath) {
		Path path = Paths.get(name);
		String normalizedPath = (rootPath == null) ?
				path.normalize().toString() :
				rootPath.resolve(path).normalize().toString();
		if (reverseCustomTemplate.containsKey(normalizedPath)) {
			return reverseCustomTemplate.get(normalizedPath);
		} else {
			Object value = reverseDefaultsTemplates.get(name);
			if (value != null) {
				return value.toString();
			} else {
				String preparedName = prepareName(Paths.get(normalizedPath), templatePreferences);
				return addTemplate(normalizedPath, preparedName).getKey();
			}
		}
	}

	public FSPath addTemplate(String path, String name) {
		Preferences newNode = templatePreferences.node(name);
		FSPath fsPath = (FSPath) application
				.getCayenneProjectPreferences()
				.getProjectDetailObject(FSPath.class, newNode);
		fsPath.setPath(path);
		return fsPath;
	}

	/**
	 * Prepares template name from the path. First, remove the extension and add "_0" to the name.
	 * Next step is check for unique. If the same name is exist, increases number at the end to the next one.
	 */
	private String prepareName(Path path, Preferences templatePreferences) {
		String preparedName = path.getFileName().toString().replaceAll(".vm$", "");
		preparedName += "_0";
		int j = 1;
		try {
			while (templatePreferences.nodeExists(preparedName)) {
				preparedName = preparedName.replaceAll("_.*[0-9]", "_" + j);
				j++;
			}
		} catch (BackingStoreException e) {
			logger.warn("Error reading preferences");
		}
		return preparedName;
	}

	public Map<String, String> getCustomTemplates() {
		return customTemplates;
	}

	public List<String> getDefaultSubclassTemplates() {
		return defaultSubclassTemplates;
	}

	public List<String> getDefaultSuperclassTemplates() {
		return defaultSuperclassTemplates;
	}

	public List<String> getDefaultEmbeddableTemplates() {
		return defaultEmbeddableTemplates;
	}

	public List<String> getDefaultEmbeddableSuperclassTemplates() {
		return defaultEmbeddableSuperclassTemplates;
	}

	public List<String> getDefaultDataMapTemplates() {
		return defaultServerDataMapTemplates;
	}

	public List<String> getDefaultDataMapSuperclassTemplates() {
		return defaultServerDataMapSuperclassTemplates;
	}
}
