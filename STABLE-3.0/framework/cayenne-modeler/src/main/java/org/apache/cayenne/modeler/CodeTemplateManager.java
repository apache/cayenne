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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationAction1_1;
import org.apache.cayenne.gen.ClassGenerator;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.pref.Domain;

/**
 * Manages code generation templates.
 * 
 */
public class CodeTemplateManager {

    public static final String STANDARD_SERVER_SUPERCLASS = "Standard Server Superclass";
    public static final String STANDARD_SERVER_SUBCLASS = "Standard Server Subclass";
    static final String STANDARD_CLIENT_SUPERCLASS = "Standard Client Superclass";
    static final String STANDARD_CLIENT_SUBCLASS = "Standard Client Subclass";

    protected List<String> standardSubclassTemplates;
    protected List<String> standardSuperclassTemplates;
    protected Map<String, String> customTemplates;
    protected Map<String, String> standardTemplates;
    protected Map<String, String> standardTemplates1_1;

    public static Domain getTemplateDomain(Application application) {
        return application.getPreferenceDomain().getSubdomain(CodeTemplateManager.class);
    }

    public CodeTemplateManager(Application application) {
        standardSuperclassTemplates = new ArrayList<String>(3);

        standardSuperclassTemplates.add(STANDARD_SERVER_SUPERCLASS);
        standardSuperclassTemplates.add(STANDARD_CLIENT_SUPERCLASS);

        standardSubclassTemplates = new ArrayList<String>(3);
        standardSubclassTemplates.add(STANDARD_SERVER_SUBCLASS);
        standardSubclassTemplates.add(STANDARD_CLIENT_SUBCLASS);

        updateCustomTemplates(getTemplateDomain(application));

        standardTemplates = new HashMap<String, String>();
        standardTemplates.put(
                STANDARD_SERVER_SUPERCLASS,
                ClassGenerationAction.SUPERCLASS_TEMPLATE);
        standardTemplates.put(
                STANDARD_CLIENT_SUPERCLASS,
                ClientClassGenerationAction.SUPERCLASS_TEMPLATE);
        standardTemplates.put(
                STANDARD_SERVER_SUBCLASS,
                ClassGenerationAction.SUBCLASS_TEMPLATE);
        standardTemplates.put(
                STANDARD_CLIENT_SUBCLASS,
                ClientClassGenerationAction.SUBCLASS_TEMPLATE);

        standardTemplates1_1 = new HashMap<String, String>();
        standardTemplates1_1.put(
                STANDARD_SERVER_SUPERCLASS,
                ClassGenerationAction1_1.SUPERCLASS_TEMPLATE);
        standardTemplates1_1.put(
                STANDARD_CLIENT_SUPERCLASS,
                ClientClassGenerationAction.SUPERCLASS_TEMPLATE);
        standardTemplates1_1.put(
                STANDARD_SERVER_SUBCLASS,
                ClassGenerationAction1_1.SUBCLASS_TEMPLATE);
        standardTemplates1_1.put(
                STANDARD_CLIENT_SUBCLASS,
                ClientClassGenerationAction.SUBCLASS_TEMPLATE);
    }

    /**
     * Updates custom templates from preferences.
     */
    public void updateCustomTemplates(Domain preferenceDomain) {
        Map<String, FSPath> templates = preferenceDomain.getDetailsMap(FSPath.class);
        this.customTemplates = new HashMap<String, String>(templates.size(), 1);

        for (Map.Entry<String, FSPath> entry : templates.entrySet()) {
            FSPath path = entry.getValue();
            customTemplates.put(entry.getKey(), path.getPath());
        }
    }

    // TODO: andrus, 12/5/2007 - this should also take a "pairs" parameter to correctly
    // assign standard templates
    public String getTemplatePath(String name, String version) {
        Object value = customTemplates.get(name);
        if (value != null) {
            return value.toString();
        }

        Map<String, String> templates;
        if (ClassGenerator.VERSION_1_1.equals(version)) {
            templates = standardTemplates1_1;
        }
        else {
            templates = standardTemplates;
        }

        value = templates.get(name);
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
