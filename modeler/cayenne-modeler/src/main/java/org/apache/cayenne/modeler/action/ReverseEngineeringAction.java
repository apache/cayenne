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

import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportAction;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dbimport.ModelerDbImportModule;
import org.apache.cayenne.modeler.dbimport.ModelerDbLoaderContext;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.ui.dbloadresult.DbLoadResultDialog;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportView;
import org.apache.cayenne.modeler.util.LongRunningTask;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Action that imports database structure into a DataMap.
 */
public class ReverseEngineeringAction extends DBConnectionAwareAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReverseEngineeringAction.class);

    private static final String ACTION_NAME = "Reengineer Database Schema";
    private static final String ICON_NAME = "icon-dbi-runImport.png";
    private static final String DIALOG_TITLE = "Reengineer DB Schema: Connect to Database";

    private DbImportView view;
    private AtomicInteger dataMapCount;
    protected Set<DataMap> dataMaps;

    public ReverseEngineeringAction(Application application) {
        super(ACTION_NAME, application);
    }

    @Override
    public String getIconName() {
        return ICON_NAME;
    }

    public void performAction(Set<DataMap> dataMapSet) {
        resetParams();
        dataMaps.addAll(dataMapSet);
        dataMapCount.set(dataMaps.size());
        dataMapSet.forEach(this::startImport);
    }

    private void startImport(DataMap dataMap) {
        ModelerDbLoaderContext context = new ModelerDbLoaderContext(
                getProjectController(),
                application.getMetaData(),
                dataMap);

        DBConnectionInfo connectionInfo = getConnectionInfo(DIALOG_TITLE, dataMap);
        if (connectionInfo == null) {
            return;
        }

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

        if (!context.buildConfig(connectionInfo, view, true)) {
            try {
                context.getConnection().close();
            } catch (SQLException ignored) {
            }
            return;
        }

        DbImportController dbImportController = application.getFrameController().getDbImportController();
        DbLoadResultDialog dbLoadResultDialog = dbImportController.createDialog();

        runLoaderInThread(context, () -> {
            application.getUndoManager().discardAllEdits();
            try {
                context.getConnection().close();
                if (dataMapCount.decrementAndGet() <= 0 && !context.isInterrupted()) {
                    if (!dbLoadResultDialog.isVisible() && !dbLoadResultDialog.getTableForMap().isEmpty()) {
                        dbImportController.showDialog();
                    }
                }
            } catch (SQLException ignored) {
            }
        });
    }

    /**
     * Connects to DB and delegates processing to DbLoaderController, starting it asynchronously.
     */
    @Override
    public void performAction(ActionEvent event) {
        resetParams();
        dataMaps.add(application.getFrameController().getProjectController().getSelectedDataMap());
        dataMapCount.set(dataMaps.size());
        startImport(application.getFrameController().getProjectController().getSelectedDataMap());
    }

    private void resetParams() {
        dataMapCount = new AtomicInteger();
        this.dataMaps = new HashSet<>();
    }

    private void runLoaderInThread(final ModelerDbLoaderContext context, final Runnable callback) {
        Thread th = new Thread(() -> {
            LoadDataMapTask task = new LoadDataMapTask(Application.getFrame(), "Reengineering DB", context);
            task.startAndWait();
            SwingUtilities.invokeLater(callback);
        });
        th.start();
    }

    public void setView(DbImportView view) {
        this.view = view;
    }

    static class LoadDataMapTask extends LongRunningTask<Object> {

        private final ModelerDbLoaderContext context;

        public LoadDataMapTask(JFrame frame, String title, ModelerDbLoaderContext context) {
            super(frame, title);
            setMinValue(0);
            setMaxValue(10);
            this.context = context;
        }

        @Override
        protected void execute() {
            context.setStatusNote("Preparing...");
            try {
                createAction().execute(context.getConfig());
            } catch (Exception e) {
                context.processException(e, "Error importing database schema.");
            }
            ProjectUtil.cleanObjMappings(context.getDataMap());
        }

        private DbImportAction createAction() {
            Injector injector = DIBootstrap.createInjector(new DbSyncModule(),
                    new ToolsModule(LOGGER),
                    new DbImportModule(),
                    new ModelerDbImportModule(context));
            return injector.getInstance(DbImportAction.class);
        }

        @Override
        protected String getCurrentNote() {
            return context.getStatusNote();
        }

        @Override
        protected int getCurrentValue() {
            return getMinValue();
        }

        @Override
        protected boolean isIndeterminate() {
            return true;
        }

        @Override
        public boolean isCanceled() {
            return context.isStopping();
        }

        @Override
        public void setCanceled(boolean canceled) {
            if (canceled) {
                context.setStatusNote("Canceling..");
            }
            context.setStopping(canceled);
        }
    }

}