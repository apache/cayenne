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

import org.apache.cayenne.modeler.Application;

import java.util.prefs.Preferences;

public final class GeneralPrefs {

    static final String NODE = "general";

    static final String AUTO_LOAD_PROJECT     = "autoLoadProject";
    static final String DELETE_PROMPT         = "deletePrompt";
    static final String ENCODING              = "encoding";
    static final String FAVOURITE_DATA_SOURCE = "favouriteDataSource";

    private final Preferences prefs;

    private GeneralPrefs(Preferences prefs) {
        this.prefs = prefs;
    }

    public static GeneralPrefs of() {
        return new GeneralPrefs(Application.getInstance().getPreferencesRepository().app(NODE));
    }

    public boolean isAutoLoadProject() {
        return prefs.getBoolean(AUTO_LOAD_PROJECT, false);
    }

    public void setAutoLoadProject(boolean v) {
        prefs.putBoolean(AUTO_LOAD_PROJECT, v);
    }

    public boolean isDeletePrompt() {
        return prefs.getBoolean(DELETE_PROMPT, false);
    }

    public void setDeletePrompt(boolean v) {
        prefs.putBoolean(DELETE_PROMPT, v);
    }

    public String getEncoding() {
        return prefs.get(ENCODING, null);
    }

    public void setEncoding(String v) {
        prefs.put(ENCODING, v == null ? "" : v);
    }

    public String getFavouriteDataSource() {
        return prefs.get(FAVOURITE_DATA_SOURCE, null);
    }

    public void setFavouriteDataSource(String v) {
        prefs.put(FAVOURITE_DATA_SOURCE, v);
    }
}
