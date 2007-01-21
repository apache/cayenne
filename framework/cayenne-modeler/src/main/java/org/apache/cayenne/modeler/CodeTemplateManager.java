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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.gen.MapClassGenerator;
import org.apache.cayenne.modeler.pref.FSPath;
import org.apache.cayenne.pref.Domain;

/**
 * Manages code generation templates.
 * 
 * @author Andrus Adamchik
 */
public class CodeTemplateManager {

    public static final String STANDARD_SERVER_SUPERCLASS = "Standard Server Superclass";
    public static final String STANDARD_SERVER_SUBCLASS = "Standard Server Subclass";

    protected Map standardSubclassTemplates;
    protected Map standardSuperclassTemplates;
    protected Map customTemplates;

    public static Domain getTemplateDomain(Application application) {
        return application.getPreferenceDomain().getSubdomain(CodeTemplateManager.class);
    }

    public CodeTemplateManager(Application application) {
        standardSuperclassTemplates = new HashMap();

        standardSuperclassTemplates.put(
                STANDARD_SERVER_SUPERCLASS,
                MapClassGenerator.SUPERCLASS_TEMPLATE_1_1);

        standardSuperclassTemplates.put(
                "Standard Client Superclass",
                MapClassGenerator.CLIENT_SUPERCLASS_TEMPLATE_1_2);

        standardSubclassTemplates = new HashMap();
        standardSubclassTemplates.put(
                STANDARD_SERVER_SUBCLASS,
                MapClassGenerator.SUBCLASS_TEMPLATE_1_1);

        standardSubclassTemplates.put(
                "Standard Client Subclass",
                MapClassGenerator.CLIENT_SUBCLASS_TEMPLATE_1_2);

        updateCustomTemplates(getTemplateDomain(application));
    }

    /**
     * Updates custom templates from preferences.
     */
    public void updateCustomTemplates(Domain preferenceDomain) {
        Map templates = preferenceDomain.getDetailsMap(FSPath.class);
        this.customTemplates = new HashMap(templates.size(), 1);
        Iterator it = templates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            FSPath path = (FSPath) entry.getValue();
            customTemplates.put(entry.getKey(), path.getPath());
        }
    }

    public String getTemplatePath(String name) {
        Object value = customTemplates.get(name);
        if (value != null) {
            return value.toString();
        }

        value = standardSuperclassTemplates.get(name);

        if (value != null) {
            return value.toString();
        }

        value = standardSubclassTemplates.get(name);
        return value != null ? value.toString() : null;
    }

    public Map getCustomTemplates() {
        return customTemplates;
    }

    public Map getStandardSubclassTemplates() {
        return standardSubclassTemplates;
    }

    public Map getStandardSuperclassTemplates() {
        return standardSuperclassTemplates;
    }
}
