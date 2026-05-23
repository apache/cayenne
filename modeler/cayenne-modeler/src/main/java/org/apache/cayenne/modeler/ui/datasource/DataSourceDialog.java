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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.apache.cayenne.modeler.dbconnector.DBConnectorFactory;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnectors;
import org.apache.cayenne.modeler.pref.adapters.DataMapPrefs;
import org.apache.cayenne.modeler.pref.adapters.GeneralPrefs;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.toolkit.ProjectDialog;
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialog;
import org.apache.cayenne.modeler.ui.preferences.dbconnector.DBConnectorEditor;
import org.apache.cayenne.modeler.project.ProjectSession;

import javax.sql.DataSource;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Modal dialog for selecting/editing a saved DB Connector and testing the resulting
 * connection. After {@link #open()} returns, call {@link #isCanceled()} to check whether
 * the user confirmed a working connection or cancelled.
 */
public class DataSourceDialog extends ProjectDialog {

    private final DBConnectorEditor editor;
    private final JComboBox<String> dataSources;
    private final JButton configButton;
    private final JButton okButton;
    private final JButton cancelButton;

    private Map<String, DBConnector> connectors;
    private String dataSourceKey;
    // a clone of the object selected from the dropdown — local edits don't bleed into the registry
    private DBConnector connector = new DBConnector();
    private DbAdapter adapter;
    private DataSource dataSource;
    private boolean canceled;

    public DataSourceDialog(ProjectSession session, Window owner, String title) {
        super(session, owner, title, ModalityType.APPLICATION_MODAL);
        this.editor = new DBConnectorEditor();
        this.dataSources = new JComboBox<>();
        this.configButton = new JButton("...");
        this.configButton.setToolTipText("configure local DataSource");
        this.okButton = new JButton("Continue");
        this.cancelButton = new JButton("Cancel");

        initLayout();
        initBindings();

        refreshDataSources();
        initFavouriteDataSource();

        DataMapPrefs dataMapPrefs = new DataMapPrefs(app.getPrefsManager().dataMapPref(session.getSelectedDataMap(), null));
        if (dataMapPrefs.hasDbAdapter()) {
            getConnectionInfoFromPreferences().copyTo(connector);
        }

        editor.setConnector(connector);

        this.canceled = true;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public DBConnector getConnector() {
        return connector;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    private void initLayout() {
        getRootPane().setDefaultButton(okButton);

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "20dlu:grow, pref, 3dlu, fill:max(150dlu;pref), 3dlu, fill:20dlu",
                "p"));
        builder.setDefaultDialogBorder();
        builder.addLabel("Saved DataSources:", cc.xy(2, 1));
        builder.add(dataSources, cc.xy(4, 1));
        builder.add(configButton, cc.xy(6, 1));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(okButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.NORTH);
        getContentPane().add(editor, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    private void initBindings() {
        dataSources.addActionListener(e -> {
            Object sel = dataSources.getSelectedItem();
            setSelectedDataSource(sel != null ? sel.toString() : null);
        });
        cancelButton.addActionListener(e -> onClose(true));
        okButton.addActionListener(e -> okClicked());
        configButton.addActionListener(e -> dataSourceConfigClicked());
    }

    private void setSelectedDataSource(String dataSourceKey) {
        this.dataSourceKey = dataSourceKey;

        DBConnector currentConnector = connectors.get(dataSourceKey);
        if (currentConnector != null) {
            currentConnector.copyTo(connector);
        } else {
            connector = new DBConnector();
        }
        editor.setConnector(connector);
    }

    private void initFavouriteDataSource() {
        String favourite = new GeneralPrefs(app.getPrefsLocator().appNode(GeneralPrefs.NODE)).getFavouriteDataSource();
        if (favourite != null && connectors.containsKey(favourite)) {
            setSelectedDataSource(favourite);
            dataSources.setSelectedItem(dataSourceKey);
        }
    }

    private DBConnector getConnectionInfoFromPreferences() {
        DataMapPrefs dataMapPrefs = new DataMapPrefs(app.getPrefsManager().dataMapPref(session.getSelectedDataMap(), null));
        DBConnector c = dataMapPrefs.getConnector();
        return c != null ? c : new DBConnector();
    }

    private void okClicked() {
        DBConnector info = getConnector();
        ModelerClassLoader classLoader = app.getClassLoader();

        // doing connection testing...
        try {
            try {
                DBConnectorFactory factory = new DBConnectorFactory(classLoader);
                this.adapter = factory.makeAdapter(info, app.getDbAdapterFactory());
                this.dataSource = factory.makeDataSource(info);
            } catch (SQLException ignore) {
                showNoConnectorDialog("Unable to load driver '" + info.getJdbcDriver() + "'");
                return;
            }

            // test connection
            try (Connection connection = dataSource.getConnection()) {
                // no-op, just verify open
            }
        } catch (Throwable th) {
            reportError("Connection Error", th);
            return;
        }
        onClose(false);
    }

    private void onClose(boolean canceled) {
        this.canceled = canceled;
        dispose();
        if (!canceled) {
            new GeneralPrefs(app.getPrefsLocator().appNode(GeneralPrefs.NODE)).setFavouriteDataSource(dataSourceKey);
        }
    }

    private void dataSourceConfigClicked() {
        DBConnectors registry = app.getDbConnectors();
        java.util.Set<String> before = new java.util.HashSet<>(registry.getAll().keySet());

        new PreferenceDialog(app, app.getFrame()).showDBConnectorEditorAction(dataSourceKey);

        // auto-select any newly-added DataSource (last new wins, matching prior commit-order behavior)
        for (String name : registry.getAll().keySet()) {
            if (!before.contains(name)) {
                dataSourceKey = name;
            }
        }
        refreshDataSources();
    }

    private void classPathConfigAction() {
        new PreferenceDialog(app, app.getFrame()).showClassPathEditorAction();
        refreshDataSources();
    }

    private void refreshDataSources() {
        DBConnectors registry = app.getDbConnectors();
        this.connectors = registry.getAll();

        // 1.2 migration fix - update data source adapter names
        String oldPackage = "org.objectstyle.cayenne.";
        for (Map.Entry<String, DBConnector> e : new LinkedHashMap<>(connectors).entrySet()) {
            DBConnector info = e.getValue();
            if (info.getDbAdapter() != null && info.getDbAdapter().startsWith(oldPackage)) {
                info.setDbAdapter("org.apache.cayenne." + info.getDbAdapter().substring(oldPackage.length()));
                registry.put(e.getKey(), info);
            }
        }

        String[] keys = connectors.keySet().toArray(new String[0]);
        Arrays.sort(keys);
        dataSources.setModel(new DefaultComboBoxModel<>(keys));

        String key = null;
        if (dataSourceKey == null || !connectors.containsKey(dataSourceKey)) {
            if (keys.length > 0) {
                key = keys[0];
            }
        }

        setSelectedDataSource(key != null ? key : dataSourceKey);
        dataSources.setSelectedItem(dataSourceKey);
    }

    private void showNoConnectorDialog(String message) {
        String[] options = {"Setup driver", "Cancel"};

        int selection = JOptionPane.showOptionDialog(this,
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
}
