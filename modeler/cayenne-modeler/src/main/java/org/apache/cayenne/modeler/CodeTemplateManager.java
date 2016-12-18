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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	static final String STANDARD_CLIENT_SUPERCLASS = "Standard Client Superclass";
	static final String STANDARD_CLIENT_SUBCLASS = "Standard Client Subclass";

	public static final String NODE_NAME = "codeTemplateManager";

	protected List<String> standardSubclassTemplates;
	protected List<String> standardSuperclassTemplates;
	protected Map<String, String> customTemplates;
	protected Map<String, String> standardTemplates;

	private static Log logger = LogFactory.getLog(CodeTemplateManager.class);

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

		updateCustomTemplates(getTemplatePreferences(application));

		standardTemplates = new HashMap<>();
		standardTemplates.put(STANDARD_SERVER_SUPERCLASS, ClassGenerationAction.SUPERCLASS_TEMPLATE);
		standardTemplates.put(STANDARD_CLIENT_SUPERCLASS, ClientClassGenerationAction.SUPERCLASS_TEMPLATE);
		standardTemplates.put(STANDARD_SERVER_SUBCLASS, ClassGenerationAction.SUBCLASS_TEMPLATE);
		standardTemplates.put(STANDARD_CLIENT_SUBCLASS, ClientClassGenerationAction.SUBCLASS_TEMPLATE);
	}

	/**
	 * Updates custom templates from preferences.
	 */
	public void updateCustomTemplates(Preferences preference) {
		String[] keys = null;
		try {
			keys = preference.childrenNames();
		} catch (BackingStoreException e) {
			logger.warn("Error reading preferences");
		}
		this.customTemplates = new HashMap<>(keys.length, 1);

		for (int j = 0; j < keys.length; j++) {
			FSPath path = new FSPath(preference.node(keys[j]));
			customTemplates.put(keys[j], path.getPath());
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
