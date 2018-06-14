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

package org.apache.cayenne.modeler.editor.cgen;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.pref.DataMapDefaults;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CustomPreferencesUpdater {

    enum Property {
        SUBCLASS_TEMPLATE,
        SUPERCLASS_TEMPLATE,
        OVERWRITE,
        PAIRS,
        USE_PACKAGE_PATH,
        MODE,
        OUTPUT_PATTERN,
        CREATE_PROPERTY_NAMES
    }

    private static final String OVERWRITE = "overwrite";
    private static final String PAIRS = "pairs";
    private static final String USE_PACKAGE_PATH = "usePackagePath";
    private static final String MODE = "mode";
    private static final String OUTPUT_PATTERN = "outputPattern";
    private static final String CREATE_PROPERTY_NAMES = "createPropertyNames";

    private Map<DataMap, DataMapDefaults> mapPreferences;


    public CustomPreferencesUpdater(Map<DataMap, DataMapDefaults> mapPreferences) {
        this.mapPreferences = mapPreferences;
    }

    public String getMode() {
        return (String) getProperty(Property.MODE);
    }

    public void setMode(String mode) {
        updatePreferences(Property.MODE, mode);
    }

    public String getSubclassTemplate() {
        return (String) getProperty(Property.SUBCLASS_TEMPLATE);
    }

    public void setSubclassTemplate(String subclassTemplate) {
        updatePreferences(Property.SUBCLASS_TEMPLATE, subclassTemplate);
    }

    public String getSuperclassTemplate() {
        return (String) getProperty(Property.SUPERCLASS_TEMPLATE);
    }

    public void setSuperclassTemplate(String superclassTemplate) {
        updatePreferences(Property.SUPERCLASS_TEMPLATE, superclassTemplate);
    }

    public Boolean getOverwrite() {
        return (Boolean) getProperty(Property.OVERWRITE);
    }

    public void setOverwrite(Boolean overwrite) {
        updatePreferences(Property.OVERWRITE, overwrite);
    }

    public Boolean getPairs() {
        return (Boolean) getProperty(Property.PAIRS);
    }

    public void setPairs(Boolean pairs) {
        updatePreferences(Property.PAIRS, pairs);
    }

    public Boolean getUsePackagePath() {
        return (Boolean) getProperty(Property.USE_PACKAGE_PATH);
    }

    public void setUsePackagePath(Boolean usePackagePath) {
        updatePreferences(Property.USE_PACKAGE_PATH, usePackagePath);
    }

    public String getOutputPattern() {
        return (String) getProperty(Property.OUTPUT_PATTERN);
    }

    public void setOutputPattern(String outputPattern) {
        updatePreferences(Property.OUTPUT_PATTERN, outputPattern);
    }

    public Boolean getCreatePropertyNames() {
        return (Boolean) getProperty(Property.CREATE_PROPERTY_NAMES);
    }

    public void setCreatePropertyNames(Boolean createPropertyNames) {
        updatePreferences(Property.CREATE_PROPERTY_NAMES, createPropertyNames);
    }

    private Object getProperty(Property property) {
        Object obj = null;

        Set<Entry<DataMap, DataMapDefaults>> entities = mapPreferences.entrySet();
        for (Entry<DataMap, DataMapDefaults> entry : entities) {

            switch (property) {
                case MODE:
                    obj = entry.getValue().getProperty(MODE);
                    break;
                case OUTPUT_PATTERN:
                    obj = entry.getValue().getProperty(OUTPUT_PATTERN);
                    break;
                case SUBCLASS_TEMPLATE:
                    obj = entry.getValue().getSubclassTemplate();
                    break;
                case SUPERCLASS_TEMPLATE:
                    obj = entry.getValue().getSuperclassTemplate();
                    break;
                case OVERWRITE:
                    obj = entry.getValue().getBooleanProperty(OVERWRITE);
                    break;
                case PAIRS:
                    obj = entry.getValue().getBooleanProperty(PAIRS);
                    break;
                case USE_PACKAGE_PATH:
                    obj = entry.getValue().getBooleanProperty(USE_PACKAGE_PATH);
                    break;
                case CREATE_PROPERTY_NAMES:
                    obj = entry.getValue().getBooleanProperty(CREATE_PROPERTY_NAMES);
                    break;
                default:
                    throw new IllegalArgumentException("Bad type property: " + property);
            }

        }
        return obj;
    }

    private void updatePreferences(Property property, Object value) {
        Set<Entry<DataMap, DataMapDefaults>> entities = mapPreferences.entrySet();
        for (Entry<DataMap, DataMapDefaults> entry : entities) {

            switch (property) {
                case MODE:
                    entry.getValue().setProperty(MODE, (String) value);
                    break;
                case OUTPUT_PATTERN:
                    entry.getValue().setProperty(OUTPUT_PATTERN, (String) value);
                    break;
                case SUBCLASS_TEMPLATE:
                    entry.getValue().setSubclassTemplate((String) value);
                    break;
                case SUPERCLASS_TEMPLATE:
                    entry.getValue().setSuperclassTemplate((String) value);
                    break;
                case OVERWRITE:
                    entry.getValue().setBooleanProperty(OVERWRITE, (Boolean) value);
                    break;
                case PAIRS:
                    entry.getValue().setBooleanProperty(PAIRS, (Boolean) value);
                    break;
                case USE_PACKAGE_PATH:
                    entry.getValue().setBooleanProperty(USE_PACKAGE_PATH, (Boolean) value);
                    break;
                case CREATE_PROPERTY_NAMES:
                    entry.getValue().setBooleanProperty(CREATE_PROPERTY_NAMES, (Boolean) value);
                    break;
                default:
                    throw new IllegalArgumentException("Bad type property: " + property);
            }
        }
    }
}