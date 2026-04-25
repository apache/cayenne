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

import org.apache.cayenne.CayenneRuntimeException;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class CayenneProjectPreferences {

    private final Map<Class<?>, ChildrenMapPreference> modelerPreferences;
    private final Map<Preferences, CayennePreference> modelerProjectPreferences;

    public CayenneProjectPreferences() {
        modelerPreferences = new HashMap<>();
        modelerPreferences.put(DBConnectionInfo.class, new ChildrenMapPreference(new DBConnectionInfo()));
        modelerProjectPreferences = new HashMap<>();

        for (ChildrenMapPreference value : modelerPreferences.values()) {
            value.initChildrenPreferences();
        }
    }

    public ChildrenMapPreference getDetailObject(Class<?> className) {
        return modelerPreferences.get(className);
    }

    public CayennePreference getProjectDetailObject(
            Class<? extends CayennePreference> objectClass,
            Preferences preferences) {

        CayennePreference preference = modelerProjectPreferences.get(preferences);

        if (preference == null) {
            try {
                Constructor<? extends CayennePreference> ct = objectClass.getConstructor(Preferences.class);
                preference = ct.newInstance(preferences);
                modelerProjectPreferences.put(preferences, preference);
            } catch (Throwable e) {
                throw new CayenneRuntimeException("Error initializing preferences", e);
            }
        }

        return preference;
    }
}
