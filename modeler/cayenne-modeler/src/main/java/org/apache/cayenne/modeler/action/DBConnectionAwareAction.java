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

package org.apache.cayenne.modeler.action;

import java.util.prefs.Preferences;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.util.CayenneAction;

import static org.apache.cayenne.modeler.pref.DBConnectionInfo.*;

/**
 * Base action that provides DBConnectionInfo for the current DataMap or calls {@link DataSourceWizard} dialog to
 * create one.
 *
 * @since 4.2
 */
public abstract class DBConnectionAwareAction extends CayenneAction {

    public DBConnectionAwareAction(String name, Application application) {
        super(name, application);
    }

    protected DBConnectionInfo getConnectionInfo(String title) {
        DBConnectionInfo connectionInfo;
        if (datamapPrefNotExist()) {
            DataSourceWizard connectWizard = getDataSourceWizard(title);
            if (connectWizard == null) {
                return null;
            }
            connectionInfo = connectWizard.getConnectionInfo();
            saveConnectionInfo(connectWizard);
        } else {
            connectionInfo = getConnectionInfoFromPreferences();
        }
        return connectionInfo;
    }

    protected DataSourceWizard getDataSourceWizard(String title, String[] buttons) {
        DataSourceWizard connectWizard = new DataSourceWizard(getProjectController(), title, buttons);
        if (!connectWizard.startupAction()) {
            return null;
        }
        return connectWizard;
    }

    protected DataSourceWizard getDataSourceWizard(String title) {
        DataSourceWizard connectWizard = new DataSourceWizard(getProjectController(), title);
        if (!connectWizard.startupAction()) {
            return null;
        }
        return connectWizard;
    }

    protected boolean datamapPrefNotExist() {
        Preferences dataMapPreference = getProjectController().
                getDataMapPreferences(getProjectController().getCurrentDataMap())
                .getCurrentPreference();
        return dataMapPreference == null || dataMapPreference.get(URL_PROPERTY, null) == null;
    }

    protected DBConnectionInfo getConnectionInfoFromPreferences() {
        DBConnectionInfo connectionInfo = new DBConnectionInfo();
        DataMapDefaults dataMapDefaults = getProjectController()
                .getDataMapPreferences(getProjectController().getCurrentDataMap());
        connectionInfo.setDbAdapter(dataMapDefaults.getCurrentPreference().get(DB_ADAPTER_PROPERTY, null));
        connectionInfo.setUrl(dataMapDefaults.getCurrentPreference().get(URL_PROPERTY, null));
        connectionInfo.setUserName(dataMapDefaults.getCurrentPreference().get(USER_NAME_PROPERTY, null));
        connectionInfo.setPassword(dataMapDefaults.getCurrentPreference().get(PASSWORD_PROPERTY, null));
        connectionInfo.setJdbcDriver(dataMapDefaults.getCurrentPreference().get(JDBC_DRIVER_PROPERTY, null));
        return connectionInfo;
    }

    protected void saveConnectionInfo(DataSourceWizard connectWizard) {
        DataMapDefaults dataMapDefaults = getProjectController().
                getDataMapPreferences(getProjectController().getCurrentDataMap());

        String dbAdapter = connectWizard.getConnectionInfo().getDbAdapter();
        if(dbAdapter != null) {
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
