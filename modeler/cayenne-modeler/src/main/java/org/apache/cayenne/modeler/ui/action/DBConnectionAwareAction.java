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
import org.apache.cayenne.modeler.ui.datasource.DataSourceController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataMapDefaults;

import static org.apache.cayenne.modeler.pref.DBConnectionInfo.*;

/**
 * Base action that provides DBConnectionInfo for the current DataMap or calls {@link DataSourceController} dialog to
 * create one.
 */
public abstract class DBConnectionAwareAction extends ModelerAbstractAction {

    public DBConnectionAwareAction(String name, Application application) {
        super(name, application);
    }

    protected DBConnectionInfo getConnectionInfo(String title, DataMap dataMap) {

        DBConnectionInfo connectionInfo = getConnectionInfoFromPreferences(dataMap);
        if (connectionInfo == null) {
            DataSourceController connectWizard = getDataSourceWizard(title);
            if (connectWizard == null) {
                return null;
            }
            connectionInfo = connectWizard.getConnectionInfo();
            saveConnectionInfo(dataMap, connectWizard);
        }

        return connectionInfo;
    }

    protected DataSourceController getDataSourceWizard(String title, String[] buttons) {
        DataSourceController connectWizard = new DataSourceController(getProjectController(), title, buttons);
        if (!connectWizard.startupAction()) {
            return null;
        }
        return connectWizard;
    }

    protected DataSourceController getDataSourceWizard(String title) {
        DataSourceController connectWizard = new DataSourceController(getProjectController(), title);
        if (!connectWizard.startupAction()) {
            return null;
        }
        return connectWizard;
    }

    private DBConnectionInfo getConnectionInfoFromPreferences(DataMap dataMap) {

        DataMapDefaults defaults = getProjectController().getSelectedDataMapPreferences(dataMap);
        if (defaults == null
                || defaults.getCurrentPreference() == null
                || defaults.getCurrentPreference().get(URL_PROPERTY, null) == null) {
            return null;
        }

        DBConnectionInfo connectionInfo = new DBConnectionInfo();
        connectionInfo.setDbAdapter(defaults.getCurrentPreference().get(DB_ADAPTER_PROPERTY, null));
        connectionInfo.setUrl(defaults.getCurrentPreference().get(URL_PROPERTY, null));
        connectionInfo.setUserName(defaults.getCurrentPreference().get(USER_NAME_PROPERTY, null));
        connectionInfo.setPassword(defaults.getCurrentPreference().get(PASSWORD_PROPERTY, null));
        connectionInfo.setJdbcDriver(defaults.getCurrentPreference().get(JDBC_DRIVER_PROPERTY, null));
        return connectionInfo;
    }

    protected void saveConnectionInfo(DataMap dataMap, DataSourceController connectWizard) {
        DataMapDefaults dataMapDefaults = getProjectController().getSelectedDataMapPreferences(dataMap);

        String dbAdapter = connectWizard.getConnectionInfo().getDbAdapter();
        if (dbAdapter != null) {
            dataMapDefaults.getCurrentPreference().put(DB_ADAPTER_PROPERTY, dbAdapter);
        } else {
            dataMapDefaults.getCurrentPreference().remove(DB_ADAPTER_PROPERTY);
        }
        dataMapDefaults.getCurrentPreference().put(URL_PROPERTY, connectWizard.getConnectionInfo().getUrl());
        dataMapDefaults.getCurrentPreference().put(USER_NAME_PROPERTY, connectWizard.getConnectionInfo().getUserName());
        dataMapDefaults.getCurrentPreference().put(PASSWORD_PROPERTY, connectWizard.getConnectionInfo().getPassword());
        dataMapDefaults.getCurrentPreference().put(JDBC_DRIVER_PROPERTY, connectWizard.getConnectionInfo().getJdbcDriver());
    }
}
