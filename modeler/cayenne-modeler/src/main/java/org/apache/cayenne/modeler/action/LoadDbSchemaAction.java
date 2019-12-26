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

import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.prefs.Preferences;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.DatabaseSchemaLoader;
import org.apache.cayenne.modeler.editor.dbimport.DbImportModel;
import org.apache.cayenne.modeler.editor.dbimport.DbImportView;
import org.apache.cayenne.modeler.editor.dbimport.DraggableTreePanel;
import org.apache.cayenne.modeler.editor.dbimport.PrintColumnsBiFunction;
import org.apache.cayenne.modeler.editor.dbimport.PrintTablesBiFunction;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cayenne.modeler.pref.DBConnectionInfo.DB_ADAPTER_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.JDBC_DRIVER_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.PASSWORD_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.URL_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.USER_NAME_PROPERTY;

/**
 * @since 4.1
 */
public class LoadDbSchemaAction extends CayenneAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadDbSchemaAction.class);

    private static final String ICON_NAME = "icon-dbi-refresh.png";
    private static final String ACTION_NAME = "Refresh Db Schema";
    private DraggableTreePanel draggableTreePanel;

    LoadDbSchemaAction(Application application) {
        super(ACTION_NAME, application);
    }

    public String getIconName() {
        return ICON_NAME;
    }

    @Override
    public void performAction(ActionEvent e) {
        performAction(e, null);
    }

    public void performAction(ActionEvent e, TreePath tablePath) {
        final DbImportView rootParent = ((DbImportView) draggableTreePanel.getParent().getParent());
        rootParent.getLoadDbSchemaProgress().setVisible(true);
        rootParent.getLoadDbSchemaButton().setEnabled(false);
        Thread thread = new Thread(() -> {
            LoadDbSchemaAction.this.setEnabled(false);
            rootParent.lockToolbarButtons();
            draggableTreePanel.getMoveButton().setEnabled(false);
            draggableTreePanel.getMoveInvertButton().setEnabled(false);
            try {
                DBConnectionInfo connectionInfo;
                if (datamapPrefNotExist()) {
                    final DataSourceWizard connectWizard = new DataSourceWizard(getProjectController(), "Load Db Schema");
                    if (!connectWizard.startupAction()) {
                        return;
                    }
                    connectionInfo = connectWizard.getConnectionInfo();
                    saveConnectionInfo(connectWizard);
                } else {
                    connectionInfo = getConnectionInfoFromPreferences();
                }

                if (tablePath != null) {
                    Object userObject = ((DbImportTreeNode) tablePath.getLastPathComponent()).getUserObject();
                    if(userObject instanceof Catalog) {
                        Catalog catalog = (Catalog) userObject;
                        if(catalog.getSchemas().isEmpty()) {
                            loadTables(connectionInfo, tablePath, rootParent);
                        }
                    } else if(userObject instanceof Schema) {
                        loadTables(connectionInfo, tablePath, rootParent);
                    } else if(userObject instanceof IncludeTable) {
                        loadColumns(connectionInfo, tablePath);
                    }
                } else {
                    loadDataBase(connectionInfo);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        Application.getFrame(),
                        ex.getMessage(),
                        "Error loading db schema",
                        JOptionPane.ERROR_MESSAGE);
                LOGGER.warn("Error loading db schema", ex);
            } finally {
                rootParent.getLoadDbSchemaButton().setEnabled(true);
                rootParent.getLoadDbSchemaProgress().setVisible(false);
                rootParent.unlockToolbarButtons();
            }
        });
        thread.start();
    }

    private void loadDataBase(DBConnectionInfo connectionInfo) throws Exception {
        ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader()
                .load(connectionInfo,
                        getApplication().getClassLoadingService());
        draggableTreePanel.getSourceTree()
                .setEnabled(true);
        draggableTreePanel.getSourceTree()
                .translateReverseEngineeringToTree(databaseReverseEngineering, true);
        draggableTreePanel
                .bindReverseEngineeringToDatamap(getProjectController().getCurrentDataMap(), databaseReverseEngineering);
        ((DbImportModel) draggableTreePanel.getSourceTree().getModel()).reload();
    }

    private void loadTables(DBConnectionInfo connectionInfo,
                            TreePath tablePath,
                            DbImportView rootParent) throws SQLException {
        ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader()
                .loadTables(connectionInfo,
                        getApplication().getClassLoadingService(),
                        tablePath,
                        rootParent.getTableTypes());
        draggableTreePanel.getSourceTree()
                .update(databaseReverseEngineering,
                        new PrintTablesBiFunction(draggableTreePanel.getSourceTree()));
    }

    private void loadColumns(DBConnectionInfo connectionInfo, TreePath tablePath) throws SQLException {
        ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader()
                .loadColumns(connectionInfo, getApplication().getClassLoadingService(), tablePath);
        draggableTreePanel.getSourceTree()
                .update(databaseReverseEngineering,
                        new PrintColumnsBiFunction(draggableTreePanel.getSourceTree()));
    }

    private boolean datamapPrefNotExist() {
        Preferences dataMapPreference = getProjectController().
                getDataMapPreferences(getProjectController().getCurrentDataMap())
                .getCurrentPreference();
        return dataMapPreference == null || dataMapPreference.get(URL_PROPERTY, null) == null;
    }

    private DBConnectionInfo getConnectionInfoFromPreferences() {
        DBConnectionInfo connectionInfo = new DBConnectionInfo();
        DataMapDefaults dataMapDefaults = getProjectController().
                getDataMapPreferences(getProjectController().getCurrentDataMap());
        connectionInfo.setDbAdapter(dataMapDefaults.getCurrentPreference().get(DB_ADAPTER_PROPERTY, null));
        connectionInfo.setUrl(dataMapDefaults.getCurrentPreference().get(URL_PROPERTY, null));
        connectionInfo.setUserName(dataMapDefaults.getCurrentPreference().get(USER_NAME_PROPERTY, null));
        connectionInfo.setPassword(dataMapDefaults.getCurrentPreference().get(PASSWORD_PROPERTY, null));
        connectionInfo.setJdbcDriver(dataMapDefaults.getCurrentPreference().get(JDBC_DRIVER_PROPERTY, null));
        return connectionInfo;
    }

    private void saveConnectionInfo(DataSourceWizard connectWizard) {
        DataMapDefaults dataMapDefaults = getProjectController().
                getDataMapPreferences(getProjectController().getCurrentDataMap());
        String dbAdapter = connectWizard.getConnectionInfo().getDbAdapter();
        if(dbAdapter != null) {
            dataMapDefaults.getCurrentPreference().put(DB_ADAPTER_PROPERTY, connectWizard.getConnectionInfo().getDbAdapter());
        } else {
            dataMapDefaults.getCurrentPreference().remove(DB_ADAPTER_PROPERTY);
        }
        dataMapDefaults.getCurrentPreference().put(URL_PROPERTY, connectWizard.getConnectionInfo().getUrl());
        dataMapDefaults.getCurrentPreference().put(USER_NAME_PROPERTY, connectWizard.getConnectionInfo().getUserName());
        dataMapDefaults.getCurrentPreference().put(PASSWORD_PROPERTY, connectWizard.getConnectionInfo().getPassword());
        dataMapDefaults.getCurrentPreference().put(JDBC_DRIVER_PROPERTY, connectWizard.getConnectionInfo().getJdbcDriver());
    }

    public void setDraggableTreePanel(DraggableTreePanel draggableTreePanel) {
        this.draggableTreePanel = draggableTreePanel;
    }
}
