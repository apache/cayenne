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
package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.util.Util;

public class DataMapDefaults extends _DataMapDefaults {

    public static final String DEFAULT_SUPERCLASS_PACKAGE_SUFFIX = "auto";

    /**
     * Returns a superlcass package tailored for a given DataMap.
     */
    public void updateSuperclassPackage(DataMap dataMap, boolean isClient) {

        String storedPackage = super.getSuperclassPackage();
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
}
