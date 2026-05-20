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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.ui.datasource.DataSourceDialog;
import org.apache.cayenne.modeler.dbconnector.DBConnector;
import org.apache.cayenne.modeler.pref.adapters.DataMapPrefs;

/**
 * Base action that provides DBConnectionInfo for the current DataMap or calls {@link DataSourceDialog} dialog to
 * create one.
 */
public abstract class DBConnectionAwareAction extends AppAction {

    public DBConnectionAwareAction(String name, Application application) {
        super(application, name);
    }

    protected DBConnector getConnector(String title, DataMap dataMap) {

        DBConnector connector = getConnectionInfoFromPreferences(dataMap);
        if (connector == null) {
            DataSourceDialog controller = getDataSourceController(title);
            if (controller == null) {
                return null;
            }
            connector = controller.getConnector();
            saveConnector(dataMap, controller);
        }

        return connector;
    }

    protected DataSourceDialog getDataSourceController(String title) {
        DataSourceDialog connectWizard = new DataSourceDialog(
                getProjectSession(),
                app.getFrame(),
                title);
        connectWizard.open();
        if (connectWizard.isCanceled()) {
            return null;
        }
        return connectWizard;
    }

    private DBConnector getConnectionInfoFromPreferences(DataMap dataMap) {
        DataMapPrefs defaults = dataMapPrefs(dataMap);
        return defaults != null ? defaults.getConnector() : null;
    }

    protected void saveConnector(DataMap dataMap, DataSourceDialog controller) {
        dataMapPrefs(dataMap).setConnector(controller.getConnector());
    }

    private DataMapPrefs dataMapPrefs(DataMap dataMap) {
        return new DataMapPrefs(app.getPrefsManager().dataMapPref(dataMap, null));
    }
}
