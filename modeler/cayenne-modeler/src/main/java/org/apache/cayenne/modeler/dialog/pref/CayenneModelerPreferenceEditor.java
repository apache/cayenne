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

package org.apache.cayenne.modeler.dialog.pref;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.pref.CayennePreferenceEditor;

/**
 * Specialized preferences editor for CayenneModeler.
 * 
 */
public class CayenneModelerPreferenceEditor extends CayennePreferenceEditor {

    protected boolean refreshingClassLoader;
    protected Application application;

    public CayenneModelerPreferenceEditor(Application application) {
        super(application.getCayenneProjectPreferences());
        this.application = application;
    }

    public boolean isRefreshingClassLoader() {
        return refreshingClassLoader;
    }

    public void setRefreshingClassLoader(boolean refreshingClassLoader) {
        this.refreshingClassLoader = refreshingClassLoader;
    }

    public void save() {
        super.save();
        
        if (isRefreshingClassLoader()) {
            application.initClassLoader();
            refreshingClassLoader = false;
        }
    }

    @Override
    protected void restart() {
    }
}
