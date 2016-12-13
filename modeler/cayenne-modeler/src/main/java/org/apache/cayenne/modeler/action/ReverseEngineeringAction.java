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

import org.apache.cayenne.dbsync.reverse.db.DbLoader;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.dialog.db.load.DbLoaderContext;
import org.apache.cayenne.modeler.dialog.db.load.DbLoaderOptionsDialog;
import org.apache.cayenne.modeler.dialog.db.load.LoadDataMapTask;

import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Action that imports database structure into a DataMap.
 */
public class ReverseEngineeringAction extends DBWizardAction {

    ReverseEngineeringAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName() {
        return "Reengineer Database Schema";
    }

    /**
     * Connects to DB and delegates processing to DbLoaderController, starting it asynchronously.
     */
    @Override
    public void performAction(ActionEvent event) {
        final DbLoaderContext context = new DbLoaderContext();
        final DataSourceWizard connectWizard = dataSourceWizardDialog("Reengineer DB Schema: Connect to Database");
        if(connectWizard == null) {
            return;
        }

        context.setProjectController(getProjectController());
        try {
            context.setConnection(connectWizard.getDataSource().getConnection());
        } catch (SQLException ex) {
            return;
        }

        final DbLoaderOptionsDialog loaderOptionsDialog = loaderOptionDialog(connectWizard, context);
        if(!context.buildConfig(connectWizard, loaderOptionsDialog)) {
            return;
        }

        runLoaderInThread(context, new Runnable() {
            @Override
            public void run() {
                application.getUndoManager().discardAllEdits();
                try {
                    context.getConnection().close();
                } catch (SQLException ignored) {
                }
            }
        });
    }

    private void runLoaderInThread(final DbLoaderContext context,
                                   final Runnable callback) {
        Thread th = new Thread(new Runnable() {
            public void run() {
                LoadDataMapTask task = new LoadDataMapTask(Application.getFrame(), "Reengineering DB", context);
                task.startAndWait();
                SwingUtilities.invokeLater(callback);
            }
        });
        th.start();
    }

    private DbLoaderOptionsDialog loaderOptionDialog(DataSourceWizard connectWizard, DbLoaderContext context) {
        if(connectWizard == null) {
            return null;
        }

        List<String> catalogs = Collections.emptyList();
        List<String> schemas = Collections.emptyList();
        try {
            schemas = DbLoader.loadSchemas(context.getConnection());
            if (connectWizard.getAdapter().supportsCatalogsOnReverseEngineering()) {
                catalogs = DbLoader.loadCatalogs(context.getConnection());
            }
        } catch (SQLException ex) {
            context.processWarn(ex, "Error Loading catalogs and schemas");
        }

        if (context.isStopping()) {
            return null;
        }

        // use this catalog as the default...
        String currentCatalog = null;
        String currentSchema = null;
        try {
            currentCatalog = context.getConnection().getCatalog();
            currentSchema = context.getConnection().getSchema();
        } catch (SQLException e) {
            context.processWarn(e, "Error getting catalog or schema");
        }

        final DbLoaderOptionsDialog dialog = new DbLoaderOptionsDialog(
                schemas, catalogs,
                currentSchema, currentCatalog
        );
        dialog.setVisible(true);
        dialog.dispose();

        if (dialog.getChoice() == DbLoaderOptionsDialog.CANCEL) {
            return null;
        }

        return dialog;
    }
}
