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
package org.apache.cayenne.modeler.util;

import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.modeler.Application;

import java.util.Arrays;
import java.util.Vector;
import java.util.prefs.Preferences;

/**
 * Helper class to store/read information about naming strategies have been used
 */
public class NameGeneratorPreferences {

    private static final String STRATEGIES_PREFERENCE = "name.generators.recent";

    /**
     * Naming strategies to appear in combobox by default
     */
    private static final Vector<String> PREDEFINED_STRATEGIES = new Vector<String>();
    static {
        PREDEFINED_STRATEGIES.add(DefaultObjectNameGenerator.class.getCanonicalName());
    }

    static final NameGeneratorPreferences instance = new NameGeneratorPreferences();

    public static NameGeneratorPreferences getInstance() {
        return instance;
    }

    Preferences getPreference() {
        return Application.getInstance().getMainPreferenceForProject();
    }

    /**
     * @return last used strategies, PREDEFINED_STRATEGIES by default
     */
    public Vector<String> getLastUsedStrategies() {

        String prop = null;

        if (getPreference() != null) {
            prop = getPreference().get(STRATEGIES_PREFERENCE, null);
        }

        if (prop == null) {
            return PREDEFINED_STRATEGIES;
        }

        return new Vector<String>(Arrays.asList(prop.split(",")));
    }

    /**
     * Adds strategy to history
     */
    public void addToLastUsedStrategies(String strategy) {
        Vector<String> strategies = getLastUsedStrategies();

        // move to top
        strategies.remove(strategy);
        strategies.add(0, strategy);

        StringBuilder res = new StringBuilder();
        for (String str : strategies) {
            res.append(str).append(",");
        }
        if (!strategies.isEmpty()) {
            res.deleteCharAt(res.length() - 1);
        }

        getPreference().put(STRATEGIES_PREFERENCE, res.toString());
    }

    public ObjectNameGenerator createNamingStrategy(Application application)
            throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        return application.getClassLoadingService()
                .loadClass(ObjectNameGenerator.class, getLastUsedStrategies().get(0)).newInstance();
    }

    public static ObjectNameGenerator defaultNameGenerator() {
        return new DefaultObjectNameGenerator(NoStemStemmer.getInstance());
    }
}
