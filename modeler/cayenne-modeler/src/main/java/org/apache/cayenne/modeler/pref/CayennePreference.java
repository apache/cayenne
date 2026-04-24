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

import java.util.prefs.Preferences;

public class CayennePreference implements Preference {

    private static final String CAYENNE_PREFERENCES_PATH = "org/apache/cayenne";

    protected Preferences currentPreference;

    public static Preferences getRoot() {
        return Preferences.userRoot().node(CAYENNE_PREFERENCES_PATH);
    }

    @Override
    public Preferences getCurrentPreference() {
        return currentPreference;
    }


    public Preferences getNode(Class<?> aClass, String path) {
        Preferences pkgNode = Preferences.userNodeForPackage(aClass);
        return path == null || path.isEmpty() ? pkgNode : pkgNode.node(path);
    }

    public void saveObjectPreference() {
    }
}
