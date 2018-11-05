/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.editor.dbimport.DatabaseSchemaLoader;
import org.apache.cayenne.modeler.editor.dbimport.DbImportModel;
import org.apache.cayenne.modeler.editor.dbimport.DbImportView;
import org.apache.cayenne.modeler.editor.dbimport.DraggableTreePanel;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.util.CayenneAction;

import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

import static org.apache.cayenne.modeler.pref.DBConnectionInfo.DB_ADAPTER_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.URL_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.USER_NAME_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.PASSWORD_PROPERTY;
import static org.apache.cayenne.modeler.pref.DBConnectionInfo.JDBC_DRIVER_PROPERTY;

/**
 * @since 4.1
 */
public class LoadDbSchemaAction extends CayenneAction {

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
                if (!datamapPreferencesExist()) {
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
                    ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader()
                            .loadColumns(connectionInfo, getApplication().getClassLoadingService(), tablePath);
                    draggableTreePanel.getSourceTree().updateTableColumns(databaseReverseEngineering);
                } else {
                    ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader()
                            .load(connectionInfo, getApplication().getClassLoadingService());
                    draggableTreePanel.getSourceTree()
                            .setEnabled(true);
                    draggableTreePanel.getSourceTree()
                            .translateReverseEngineeringToTree(databaseReverseEngineering, true);
                    draggableTreePanel
                            .bindReverseEngineeringToDatamap(getProjectController().getCurrentDataMap(), databaseReverseEngineering);
                    ((DbImportModel) draggableTreePanel.getSourceTree().getModel()).reload();
                }


            } catch (SQLException exception) {
                JOptionPane.showMessageDialog(
                        Application.getFrame(),
                        exception.getMessage(),
                        "Error db schema loading",
                        JOptionPane.ERROR_MESSAGE);
            } finally {
                rootParent.getLoadDbSchemaButton().setEnabled(true);
                rootParent.getLoadDbSchemaProgress().setVisible(false);
                rootParent.unlockToolbarButtons();
            }
        });
        thread.start();
    }

    private boolean datamapPreferencesExist() {
        DataMapDefaults dataMapDefaults = getProjectController().
                getDataMapPreferences(getProjectController().getCurrentDataMap());
        return dataMapDefaults.getCurrentPreference().get(DB_ADAPTER_PROPERTY, null) != null;
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
        dataMapDefaults.getCurrentPreference().put(DB_ADAPTER_PROPERTY, connectWizard.getConnectionInfo().getDbAdapter());
        dataMapDefaults.getCurrentPreference().put(URL_PROPERTY, connectWizard.getConnectionInfo().getUrl());
        dataMapDefaults.getCurrentPreference().put(USER_NAME_PROPERTY, connectWizard.getConnectionInfo().getUserName());
        dataMapDefaults.getCurrentPreference().put(PASSWORD_PROPERTY, connectWizard.getConnectionInfo().getPassword());
        dataMapDefaults.getCurrentPreference().put(JDBC_DRIVER_PROPERTY, connectWizard.getConnectionInfo().getJdbcDriver());
    }

    public void setDraggableTreePanel(DraggableTreePanel draggableTreePanel) {
        this.draggableTreePanel = draggableTreePanel;
    }
}
