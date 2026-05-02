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
import org.apache.cayenne.modeler.dbconnector.DBConnector;

import java.util.prefs.Preferences;

public final class DataMapPrefs implements PreferenceAdapter {

    public static final String SUPERCLASS_PACKAGE_PROPERTY = "superclassPackage";
    public static final String DEFAULT_SUPERCLASS_PACKAGE_SUFFIX = "auto";

    public static DataMapPrefs of(PreferencesRepository repository, DataMap dataMap) {
        return new DataMapPrefs(repository.dataMapPref(dataMap, null));
    }

    private final Preferences pref;

    private DataMapPrefs(Preferences pref) {
        this.pref = pref;
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
        if (pref != null) {
            if(superclassPackage == null) {
                superclassPackage = "";
            }
            pref.put(SUPERCLASS_PACKAGE_PROPERTY, superclassPackage);
        }
    }

    /**
     * Returns connection info stored in this DataMap's preferences, or null if no
     * connection has been configured (URL not set).
     */
    public DBConnector getConnector() {
        if (pref == null || pref.get(DBConnector.URL_PROPERTY, null) == null) {
            return null;
        }
        DBConnector connector = new DBConnector();
        connector.setDbAdapter(pref.get(DBConnector.DB_ADAPTER_PROPERTY, null));
        connector.setUrl(pref.get(DBConnector.URL_PROPERTY, null));
        connector.setUserName(pref.get(DBConnector.USER_NAME_PROPERTY, null));
        connector.setPassword(pref.get(DBConnector.PASSWORD_PROPERTY, null));
        connector.setJdbcDriver(pref.get(DBConnector.JDBC_DRIVER_PROPERTY, null));
        return connector;
    }

    public void setConnector(DBConnector connector) {
        if (pref == null) {
            return;
        }
        if (connector.getDbAdapter() != null) {
            pref.put(DBConnector.DB_ADAPTER_PROPERTY, connector.getDbAdapter());
        } else {
            pref.remove(DBConnector.DB_ADAPTER_PROPERTY);
        }
        pref.put(DBConnector.URL_PROPERTY, connector.getUrl());
        pref.put(DBConnector.USER_NAME_PROPERTY, connector.getUserName());
        pref.put(DBConnector.PASSWORD_PROPERTY, connector.getPassword());
        pref.put(DBConnector.JDBC_DRIVER_PROPERTY, connector.getJdbcDriver());
    }

    public boolean hasDbAdapter() {
        return pref != null && pref.get(DBConnector.DB_ADAPTER_PROPERTY, null) != null;
    }
}
