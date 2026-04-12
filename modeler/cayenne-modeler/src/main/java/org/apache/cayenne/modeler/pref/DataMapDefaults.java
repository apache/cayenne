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

import org.apache.cayenne.pref.RenamedPreferences;

import java.util.prefs.Preferences;

public class DataMapDefaults extends RenamedPreferences {

    public static final String SUPERCLASS_PACKAGE_PROPERTY = "superclassPackage";
    public static final String DEFAULT_SUPERCLASS_PACKAGE_SUFFIX = "auto";

    public DataMapDefaults(Preferences pref) {
        super(pref);
    }

    /**
     * Sets superclass package, building it by "normalizing" and concatenating prefix and suffix.
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

        String dot = (!suffix.isEmpty() && !prefix.isEmpty()) ? "." : "";
        setSuperclassPackage(prefix + dot + suffix);
    }

    public void setSuperclassPackage(String superclassPackage) {
        if (getCurrentPreference() != null) {
            if(superclassPackage == null) {
                superclassPackage = "";
            }
            getCurrentPreference().put(SUPERCLASS_PACKAGE_PROPERTY, superclassPackage);
        }
    }

    public String getProperty(String property) {
        if (property != null && getCurrentPreference() != null) {
            return getCurrentPreference().get(property, null);
        }
        return null;
    }
}
