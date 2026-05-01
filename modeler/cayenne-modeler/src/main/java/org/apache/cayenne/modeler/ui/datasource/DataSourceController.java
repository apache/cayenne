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

package org.apache.cayenne.modeler.ui.datasource;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialogController;
import org.apache.cayenne.modeler.mvc.ChildController;
import org.apache.cayenne.modeler.dbconnector.DBConnector;
import org.apache.cayenne.modeler.dbconnector.DBConnectors;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.pref.GeneralPrefs;

import javax.sql.DataSource;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.cayenne.modeler.dbconnector.DBConnector.*;

/**
 * A subclass of ConnectionWizard that tests configured DataSource, but does not
 * keep an open connection.
 *
 */
public class DataSourceController extends ChildController<ProjectController> {

    private final DataSourceView view;

    private Map<String, DBConnector> connectors;
    private String dataSourceKey;
    // a clone of an object selected from the dropdown, as we need to allow local temporary modifications
    private DBConnector connector;
    private DbAdapter adapter;
    private DataSource dataSource;
    private boolean canceled;

    public DataSourceController(ProjectController parent, String title) {
        this(parent, title, new String[]{"Continue", "Cancel"});
    }

    public DataSourceController(ProjectController parent, String title, String[] buttons) {
        super(parent);

        this.connector = new DBConnector();

        this.view = new DataSourceView(this, buttons);
        this.view.setTitle(title);

        initBindings();
    }

    protected void initBindings() {
        view.getDataSources().addActionListener(e -> {
            Object sel = view.getDataSources().getSelectedItem();
            setDataSourceKey(sel != null ? sel.toString() : null);
        });

        view.getCancelButton().addActionListener(e -> cancelAction());
        view.getOkButton().addActionListener(e -> okAction());
        view.getConfigButton().addActionListener(e -> dataSourceConfigAction());
    }

    private void initFavouriteDataSource() {
        String favouriteDataSource = GeneralPrefs.of().getFavouriteDataSource();
        if (favouriteDataSource != null && connectors.containsKey(favouriteDataSource)) {
            setDataSourceKey(favouriteDataSource);
            view.getDataSources().setSelectedItem(dataSourceKey);
        }
    }

    private DBConnector getConnectionInfoFromPreferences() {
        DBConnector connectionInfo = new DBConnector();
        DataMapDefaults dataMapDefaults = parent.getSelectedDataMapPreferences(parent.getSelectedDataMap());
        connectionInfo.setDbAdapter(dataMapDefaults.getCurrentPreference().get(DB_ADAPTER_PROPERTY, null));
        connectionInfo.setUrl(dataMapDefaults.getCurrentPreference().get(URL_PROPERTY, null));
        connectionInfo.setUserName(dataMapDefaults.getCurrentPreference().get(USER_NAME_PROPERTY, null));
        connectionInfo.setPassword(dataMapDefaults.getCurrentPreference().get(PASSWORD_PROPERTY, null));
        connectionInfo.setJdbcDriver(dataMapDefaults.getCurrentPreference().get(JDBC_DRIVER_PROPERTY, null));
        return connectionInfo;
    }

    private void setDataSourceKey(final String dataSourceKey) {
        this.dataSourceKey = dataSourceKey;

        // update a clone object that will be used to obtain connection...
        DBConnector currentConnector = connectors.get(dataSourceKey);
        if (currentConnector != null) {
            currentConnector.copyTo(connector);
        } else {
            connector = new DBConnector();
        }
        view.getConnectionInfo().setConnectionInfo(connector);
    }

    /**
     * Main action method that pops up a dialog asking for user selection.
     * Returns true if the selection was confirmed, false - if canceled.
     */
    public boolean startupAction() {
        this.canceled = true;
        refreshDataSources();
        initFavouriteDataSource();

        final DataMapDefaults dataMapDefaults = parent.getSelectedDataMapPreferences(parent.getSelectedDataMap());
        if (dataMapDefaults.getCurrentPreference().get(DB_ADAPTER_PROPERTY, null) != null) {
            getConnectionInfoFromPreferences().copyTo(connector);
        }
        view.pack();
        view.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        view.setModal(true);
        view.connectionInfo.setConnectionInfo(connector);
        makeCloseableOnEscape();
        centerView();
        view.setVisible(true);

        return !canceled;
    }

    public DBConnector getConnector() {
        return connector;
    }

    /**
     * Tests that the entered information is valid and can be used to open a
     * connection. Does not store the open connection.
     */
    public void okAction() {
        DBConnector info = getConnector();
        ModelerClassLoader classLoader = getApplication().getClassLoader();

        // doing connection testing...
        try {
            try {
                this.adapter = info.makeAdapter(classLoader, getApplication().getDbAdapterFactory());
                this.dataSource = info.makeDataSource(classLoader);
            } catch (SQLException ignore) {
                showNoConnectorDialog("Unable to load driver '" + info.getJdbcDriver() + "'");
                return;
            }

            // Test connection
            try (Connection connection = dataSource.getConnection()) {
            }
        } catch (Throwable th) {
            reportError("Connection Error", th);
            return;
        }
        onClose(false);
    }

    public void cancelAction() {
        onClose(true);
    }

    protected void onClose(final boolean canceled) {
        // set success flag, and unblock the caller...
        this.canceled = canceled;
        view.dispose();
        if (!canceled) {
            GeneralPrefs.of().setFavouriteDataSource(dataSourceKey);
        }
    }

    /**
     * Opens preferences panel to allow configuration of DataSource presets.
     */
    public void dataSourceConfigAction() {
        DBConnectors registry = getApplication().getDbConnectors();
        Set<String> before = new HashSet<>(registry.getAll().keySet());

        PreferenceDialogController prefs = new PreferenceDialogController(this);
        prefs.showDataSourceEditorAction(dataSourceKey);

        // auto-select any newly-added DataSource (last new wins, matching prior commit-order behavior)
        for (String name : registry.getAll().keySet()) {
            if (!before.contains(name)) {
                dataSourceKey = name;
            }
        }
        refreshDataSources();
    }

    /**
     * Opens preferences panel to allow configuration of classpath.
     */
    public void classPathConfigAction() {
        final PreferenceDialogController prefs = new PreferenceDialogController(this);
        prefs.showClassPathEditorAction();
        refreshDataSources();
    }

    @Override
    public Component getView() {
        return view;
    }

    private void refreshDataSources() {
        DBConnectors registry = getApplication().getDbConnectors();
        this.connectors = registry.getAll();

        // 1.2 migration fix - update data source adapter names
        final String _12package = "org.objectstyle.cayenne.";
        for (Map.Entry<String, DBConnector> e : new LinkedHashMap<>(connectors).entrySet()) {
            DBConnector info = e.getValue();
            if (info.getDbAdapter() != null && info.getDbAdapter().startsWith(_12package)) {
                info.setDbAdapter("org.apache.cayenne." + info.getDbAdapter().substring(_12package.length()));
                registry.put(e.getKey(), info);
            }
        }

        final String[] keys = connectors.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        view.getDataSources().setModel(new DefaultComboBoxModel<>(keys));

        String key = null;
        if (dataSourceKey == null || !connectors.containsKey(dataSourceKey)) {
            if (keys.length > 0) {
                key = keys[0];
            }
        }

        setDataSourceKey(key != null ? key : dataSourceKey);
        view.getDataSources().setSelectedItem(dataSourceKey);
    }

    protected void showNoConnectorDialog(String message) {
        String[] options = {"Setup driver", "Cancel"};

        int selection = JOptionPane.showOptionDialog(view,
                message,
                "Configuration error",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]);

        if (selection == 0) {
            classPathConfigAction();
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Returns configured DbAdapter.
     */
    public DbAdapter getAdapter() {
        return adapter;
    }
}
