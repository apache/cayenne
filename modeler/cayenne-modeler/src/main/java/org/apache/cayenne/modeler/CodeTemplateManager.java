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

package org.apache.cayenne.modeler;

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.modeler.pref.FSPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Manages code generation templates.
 */
public class CodeTemplateManager {

	public static final String STANDARD_SERVER_SUPERCLASS = "Standard Server Superclass";
	public static final String STANDARD_SERVER_SUBCLASS = "Standard Server Subclass";
	public static final String SINGLE_SERVER_CLASS = "Single Server class";
	static final String STANDARD_CLIENT_SUPERCLASS = "Standard Client Superclass";
	static final String STANDARD_CLIENT_SUBCLASS = "Standard Client Subclass";

	public static final String NODE_NAME = "codeTemplateManager";

	protected List<String> standardSubclassTemplates;
	protected List<String> standardSuperclassTemplates;
	protected Map<String, String> customTemplates;
	protected Map<String, String> standardTemplates;

	private Map<String, String> reverseStandartTemplates;

	private static Logger logger = LoggerFactory.getLogger(CodeTemplateManager.class);

	public Preferences getTemplatePreferences(Application application) {
		return application.getPreferencesNode(this.getClass(), NODE_NAME);
	}

	public CodeTemplateManager(Application application) {
		standardSuperclassTemplates = new ArrayList<>(3);

		standardSuperclassTemplates.add(STANDARD_SERVER_SUPERCLASS);
		standardSuperclassTemplates.add(STANDARD_CLIENT_SUPERCLASS);

		standardSubclassTemplates = new ArrayList<>(3);
		standardSubclassTemplates.add(STANDARD_SERVER_SUBCLASS);
		standardSubclassTemplates.add(STANDARD_CLIENT_SUBCLASS);
		standardSubclassTemplates.add(SINGLE_SERVER_CLASS);

		updateCustomTemplates(getTemplatePreferences(application));

		standardTemplates = new HashMap<>();
		standardTemplates.put(STANDARD_SERVER_SUPERCLASS, ClassGenerationAction.SUPERCLASS_TEMPLATE);
		standardTemplates.put(STANDARD_CLIENT_SUPERCLASS, ClientClassGenerationAction.SUPERCLASS_TEMPLATE);
		standardTemplates.put(STANDARD_SERVER_SUBCLASS, ClassGenerationAction.SUBCLASS_TEMPLATE);
		standardTemplates.put(STANDARD_CLIENT_SUBCLASS, ClientClassGenerationAction.SUBCLASS_TEMPLATE);
		standardTemplates.put(SINGLE_SERVER_CLASS, ClassGenerationAction.SINGLE_CLASS_TEMPLATE);

		reverseStandartTemplates = new HashMap<>();
		reverseStandartTemplates.put(ClassGenerationAction.SUBCLASS_TEMPLATE, STANDARD_SERVER_SUBCLASS);
		reverseStandartTemplates.put(ClientClassGenerationAction.SUBCLASS_TEMPLATE, STANDARD_CLIENT_SUBCLASS);
		reverseStandartTemplates.put(ClassGenerationAction.SINGLE_CLASS_TEMPLATE, SINGLE_SERVER_CLASS);
		reverseStandartTemplates.put(ClientClassGenerationAction.SUPERCLASS_TEMPLATE, STANDARD_CLIENT_SUPERCLASS);
		reverseStandartTemplates.put(ClassGenerationAction.SUPERCLASS_TEMPLATE, STANDARD_SERVER_SUPERCLASS);

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
	public String getTemplatePath(String name) {
		Object value = customTemplates.get(name);
		if (value != null) {
			return value.toString();
		}

		value = standardTemplates.get(name);
		return value != null ? value.toString() : null;
	}

	public String getNameByPath(String name) {
		if(customTemplates.containsKey(name)){
			return customTemplates.get(name).toString();
		} else {
			Object value = reverseStandartTemplates.get(name);
			return value != null ? value.toString() : null;
		}
	}

	public Map<String, String> getCustomTemplates() {
		return customTemplates;
	}

	public List<String> getStandardSubclassTemplates() {
		return standardSubclassTemplates;
	}

	public List<String> getStandardSuperclassTemplates() {
		return standardSuperclassTemplates;
	}
}
