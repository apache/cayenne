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
package org.apache.cayenne.pref;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;

public class CayenneProjectPreferences {

    // for preferences not dependent on project
    private Map<Class<?>, ChildrenMapPreference> cayennePreferences;

    // for preferences dependent on project
    private Map<Preferences, CayennePreference> projectCayennePreferences;

    public CayenneProjectPreferences() {
        cayennePreferences = new HashMap<>();

        cayennePreferences.put(DBConnectionInfo.class, new ChildrenMapPreference(
                new DBConnectionInfo()));

        projectCayennePreferences = new HashMap<>();

        for (ChildrenMapPreference value : cayennePreferences.values()) {
            value.initChildrenPreferences();
        }
    }

    public ChildrenMapPreference getDetailObject(Class<?> className) {
        return cayennePreferences.get(className);
    }

    public CayennePreference getProjectDetailObject(
            Class<? extends CayennePreference> objectClass,
            Preferences preferences) {

        CayennePreference preference = projectCayennePreferences.get(preferences);

        if (preference == null) {
            try {
                Constructor<? extends CayennePreference> ct = objectClass
                        .getConstructor(Preferences.class);
                preference = ct.newInstance(preferences);
                projectCayennePreferences.put(preferences, preference);
            }
            catch (Throwable e) {
                new CayenneRuntimeException("Error initialzing preference", e);
            }
        }

        return preference;
    }

    // delete property
    public void removeProjectDetailObject(Preferences preference) {
        try {
            preference.removeNode();
            projectCayennePreferences.remove(preference);
        }
        catch (BackingStoreException e) {
            new CayenneRuntimeException("error delete preferences " + e);
        }
    }
}
