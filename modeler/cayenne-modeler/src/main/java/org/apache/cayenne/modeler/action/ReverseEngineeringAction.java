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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.dialog.db.DbActionOptionsDialog;
import org.apache.cayenne.modeler.dialog.db.load.DbLoadResultDialog;
import org.apache.cayenne.modeler.dialog.db.load.DbLoaderContext;
import org.apache.cayenne.modeler.dialog.db.load.LoadDataMapTask;
import org.apache.cayenne.modeler.editor.DbImportController;
import org.apache.cayenne.modeler.editor.dbimport.DbImportView;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataMapDefaults;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.cayenne.modeler.pref.DBConnectionInfo.*;

/**
 * Action that imports database structure into a DataMap.
 */
public class ReverseEngineeringAction extends DBWizardAction<DbActionOptionsDialog> {

    private static final String ACTION_NAME = "Reengineer Database Schema";
    private static final String ICON_NAME = "icon-dbi-runImport.png";
    private static final String DIALOG_TITLE = "Reengineer DB Schema: Connect to Database";

    private DbImportView view;
    private AtomicInteger dataMapCount;
    protected Set<DataMap> dataMaps;

    public String getIconName() {
        return ICON_NAME;
    }

    ReverseEngineeringAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName() {
        return ACTION_NAME;
    }

    public void performAction(Set<DataMap> dataMapSet) {
        resetParams();
        dataMaps.addAll(dataMapSet);
        dataMapCount.set(dataMaps.size());
        ProjectController projectController = Application.getInstance().getFrameController().getProjectController();
        for(DataMap dataMap : dataMapSet) {
            projectController.setCurrentDataMap(dataMap);
            startImport();
        }
    }

    private void startImport(){
        final DbLoaderContext context = new DbLoaderContext(application.getMetaData());
        DBConnectionInfo connectionInfo;
        if (!datamapPreferencesExist()) {
            final DataSourceWizard connectWizard = dataSourceWizardDialog(DIALOG_TITLE);
            if (connectWizard == null) {
                return;
            }
            connectionInfo = connectWizard.getConnectionInfo();
            saveConnectionInfo(connectWizard);
        } else {
            connectionInfo = getConnectionInfoFromPreferences();
        }
        context.setProjectController(getProjectController());
        try {
            context.setConnection(connectionInfo.makeDataSource(getApplication().getClassLoadingService()).getConnection());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    Application.getFrame(),
                    ex.getMessage(),
                    "Error loading schemas dialog",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if(!context.buildConfig(connectionInfo, view)) {
            try {
                context.getConnection().close();
            } catch (SQLException ignored) {}
            return;
        }

        runLoaderInThread(context, () -> {
            application.getUndoManager().discardAllEdits();
            try {
                context.getConnection().close();
                if(dataMapCount.decrementAndGet() <= 0 && !context.isInterrupted()) {
                    DbImportController dbImportController = Application.getInstance().getFrameController().getDbImportController();
                    DbLoadResultDialog dbLoadResultDialog = dbImportController.createDialog();
                    if (!dbLoadResultDialog.isVisible()) {
                        dbImportController.showDialog();
                    }
                }
            } catch (SQLException ignored) {}
        });
    }

    /**
     * Connects to DB and delegates processing to DbLoaderController, starting it asynchronously.
     */
    @Override
    public void performAction(ActionEvent event) {
        resetParams();
        dataMaps.add(Application.getInstance().getFrameController().getProjectController().getCurrentDataMap());
        dataMapCount.set(dataMaps.size());
        startImport();
    }

    private void resetParams() {
        dataMapCount = new AtomicInteger();
        this.dataMaps = new HashSet<>();
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

    private boolean datamapPreferencesExist() {
        DataMapDefaults dataMapDefaults = getProjectController().
                getDataMapPreferences(getProjectController().getCurrentDataMap());
        return dataMapDefaults.getCurrentPreference().get(DB_ADAPTER_PROPERTY, null) != null;
    }

    private void runLoaderInThread(final DbLoaderContext context, final Runnable callback) {
        Thread th = new Thread(() -> {
            LoadDataMapTask task = new LoadDataMapTask(Application.getFrame(), "Reengineering DB", context);
            task.startAndWait();
            SwingUtilities.invokeLater(callback);
        });
        th.start();
    }

    @Override
    protected DbActionOptionsDialog createDialog(Collection<String> catalogs, Collection<String> schemas,
                                                 String currentCatalog, String currentSchema, int command) {
        // NOOP
        return null;
    }

    public void setView(DbImportView view) {
        this.view = view;
    }
}