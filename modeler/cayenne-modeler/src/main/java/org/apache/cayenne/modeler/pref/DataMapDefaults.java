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
package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.pref.RenamedPreferences;
import org.apache.cayenne.util.Util;

import java.util.prefs.Preferences;

public class DataMapDefaults extends RenamedPreferences {

    private boolean generatePairs;
    private String outputPath;
    private String subclassTemplate;
    private String superclassPackage;
    private String superclassTemplate;

    private boolean initGeneratePairs;

    public static final String GENERATE_PAIRS_PROPERTY = "generatePairs";
    public static final String OUTPUT_PATH_PROPERTY = "outputPath";
    public static final String SUBCLASS_TEMPLATE_PROPERTY = "subclassTemplate";
    public static final String SUPERCLASS_PACKAGE_PROPERTY = "superclassPackage";
    public static final String SUPERCLASS_TEMPLATE_PROPERTY = "superclassTemplate";

    public static final String DEFAULT_SUPERCLASS_PACKAGE_SUFFIX = "auto";

    public DataMapDefaults(Preferences pref) {
        super(pref);
    }

    /**
     * Returns a superclass package tailored for a given DataMap.
     */
    public void updateSuperclassPackage(DataMap dataMap, boolean isClient) {

        String storedPackage = getSuperclassPackage();
        if (Util.isEmptyString(storedPackage)
                || DEFAULT_SUPERCLASS_PACKAGE_SUFFIX.equals(storedPackage)) {
            String mapPackage = (isClient) ? dataMap.getDefaultClientPackage() : dataMap
                    .getDefaultPackage();
            if (!Util.isEmptyString(mapPackage)) {

                if (mapPackage.endsWith(".")) {
                    mapPackage = mapPackage.substring(mapPackage.length() - 1);
                }

                if (!Util.isEmptyString(mapPackage)) {
                    String newPackage = mapPackage
                            + "."
                            + DEFAULT_SUPERCLASS_PACKAGE_SUFFIX;
                    if (!Util.nullSafeEquals(newPackage, storedPackage)) {
                        setSuperclassPackage(newPackage);
                    }
                }
            }
        }

        if (DEFAULT_SUPERCLASS_PACKAGE_SUFFIX.equals(getSuperclassPackage())) {
            setSuperclassPackage(null);
        }
    }

    /**
     * An initialization callback.
     */
    public void prePersist() {
        setGeneratePairs(Boolean.TRUE);
    }

    /**
     * Sets superclass package, building it by "normalizing" and concatenating prefix and
     * suffix.
     */
    public void setSuperclassPackage(String prefix, String suffix) {
        if (prefix == null) {
            prefix = "";
        }
        else if (prefix.endsWith(".")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        if (suffix == null) {
            suffix = "";
        }
        else if (suffix.startsWith(".")) {
            suffix = suffix.substring(1);
        }

        String dot = (suffix.length() > 0 && prefix.length() > 0) ? "." : "";
        setSuperclassPackage(prefix + dot + suffix);
    }

    public boolean getGeneratePairs() {
        if (!initGeneratePairs) {
            generatePairs = getCurrentPreference().getBoolean(
                    GENERATE_PAIRS_PROPERTY,
                    false);
            initGeneratePairs = true;
        }
        return generatePairs;
    }

    public void setGeneratePairs(Boolean bool) {
        if (getCurrentPreference() != null) {
            this.generatePairs = bool;
            getCurrentPreference().putBoolean(GENERATE_PAIRS_PROPERTY, bool);
        }
    }

    public String getOutputPath() {
        if (outputPath == null) {
            outputPath = getCurrentPreference().get(OUTPUT_PATH_PROPERTY, null);
        }
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        if (getCurrentPreference() != null) {
            this.outputPath = outputPath;
            if(outputPath == null) {
                outputPath = "";
            }
            getCurrentPreference().put(OUTPUT_PATH_PROPERTY, outputPath);
        }
    }

    public String getSubclassTemplate() {
        if (subclassTemplate == null) {
            subclassTemplate = getCurrentPreference().get(
                    SUBCLASS_TEMPLATE_PROPERTY,
                    null);
        }
        return subclassTemplate;
    }

    public void setSubclassTemplate(String subclassTemplate) {
        if (getCurrentPreference() != null) {
            this.subclassTemplate = subclassTemplate;
            if(subclassTemplate == null) {
                subclassTemplate = "";
            }
            getCurrentPreference().put(SUBCLASS_TEMPLATE_PROPERTY, subclassTemplate);
        }
    }

    public String getSuperclassPackage() {
        if (superclassPackage == null) {
            superclassPackage = getCurrentPreference().get(
                    SUPERCLASS_PACKAGE_PROPERTY,
                    null);
        }
        return superclassPackage;
    }

    public void setSuperclassPackage(String superclassPackage) {
        if (getCurrentPreference() != null) {
            this.superclassPackage = superclassPackage;
            if(superclassPackage == null) {
                superclassPackage = "";
            }
            getCurrentPreference().put(SUPERCLASS_PACKAGE_PROPERTY, superclassPackage);
        }
    }

    public String getSuperclassTemplate() {
        if (superclassTemplate == null) {
            superclassTemplate = getCurrentPreference().get(
                    SUPERCLASS_TEMPLATE_PROPERTY,
                    null);
        }
        return superclassTemplate;
    }

    public void setSuperclassTemplate(String superclassTemplate) {
        if (getCurrentPreference() != null) {
            this.superclassTemplate = superclassTemplate;
            if(superclassTemplate == null) {
                superclassTemplate = "";
            }
            getCurrentPreference().put(SUPERCLASS_TEMPLATE_PROPERTY, superclassTemplate);
        }
    }

    public String getProperty(String property) {
        if (property != null && getCurrentPreference() != null) {
            return getCurrentPreference().get(property, null);
        }
        return null;
    }

    public void setProperty(String property, String value) {
        if (getCurrentPreference() != null) {
            if(value == null) {
                value = "";
            }
            getCurrentPreference().put(property, value);
        }
    }

    public void setBooleanProperty(String property, boolean value) {
        if (getCurrentPreference() != null) {
            getCurrentPreference().putBoolean(property, value);
        }
    }

    public boolean getBooleanProperty(String property) {
        if (property != null && getCurrentPreference() != null) {
            return getCurrentPreference().getBoolean(property, false);
        }
        return false;
    }
}
