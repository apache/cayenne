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

import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoader;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.dialog.db.DbActionOptionsDialog;
import org.apache.cayenne.modeler.dialog.db.merge.MergerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Action that alter database schema to match a DataMap.
 */
public class MigrateAction extends DBConnectionAwareAction {

    private static Logger LOGGER = LoggerFactory.getLogger(MigrateAction.class);

    private boolean dialogShown;

    public MigrateAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName() {
        return "Migrate Database Schema";
    }

    public void performAction(ActionEvent e) {

        DataSourceWizard connectWizard = getDataSourceWizard("Migrate DB Schema: Connect to Database");
        if(connectWizard == null) {
            return;
        }

        DataMap map = getProjectController().getCurrentDataMap();
        if (map == null) {
            throw new IllegalStateException("No current DataMap selected.");
        }

        dialogShown = false;
        DbActionOptionsDialog optionsDialog = loaderOptionDialog(connectWizard);
        if(dialogShown && optionsDialog == null) {
            return;
        }

        String selectedCatalog = optionsDialog == null ? null : optionsDialog.getSelectedCatalog();
        String selectedSchema = optionsDialog == null ? null : optionsDialog.getSelectedSchema();

        MergerTokenFactoryProvider mergerTokenFactoryProvider =
                getApplication().getInjector().getInstance(MergerTokenFactoryProvider.class);

        // ... show dialog...
        new MergerOptions(
                getProjectController(),
                "Migrate DB Schema: Options",
                connectWizard.getConnectionInfo(),
                map, selectedCatalog, selectedSchema, mergerTokenFactoryProvider).startupAction();
    }

    protected DbActionOptionsDialog createDialog(Collection<String> catalogs, Collection<String> schemas,
                                                 String currentCatalog, String currentSchema, int command) {
        dialogShown = true;
        if (command == DbActionOptionsDialog.SELECT) {
            return new DbActionOptionsDialog(Application.getFrame(), "Migrate DB Schema: Select Catalog and Schema",
                    catalogs, schemas, currentCatalog, currentSchema);
        }
        return null;
    }

    protected DbActionOptionsDialog loaderOptionDialog(DataSourceWizard connectWizard) {

        // use this catalog as the default...
        List<String> catalogs;
        List<String> schemas;
        String currentCatalog;
        String currentSchema = null;
        try(Connection connection = connectWizard.getDataSource().getConnection()) {
            catalogs = getCatalogs(connectWizard, connection);
            schemas = getSchemas(connection);
            if (catalogs.isEmpty() && schemas.isEmpty()) {
                return null;
            }
            currentCatalog = connection.getCatalog();

            try {
                currentSchema = connection.getSchema();
            } catch (Throwable th) {
                LOGGER.warn("Error getting schema.", th);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    Application.getFrame(),
                    ex.getMessage(),
                    "Error loading schemas dialog",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        DbActionOptionsDialog optionsDialog = getStartDialog(catalogs, schemas, currentCatalog, currentSchema);
        optionsDialog.setVisible(true);
        while ((optionsDialog.getChoice() != DbActionOptionsDialog.CANCEL)) {
            if (optionsDialog.getChoice() == DbActionOptionsDialog.SELECT) {
                return optionsDialog;
            }
            optionsDialog = createDialog(catalogs, schemas, currentCatalog, currentSchema, optionsDialog.getChoice());
            optionsDialog.setVisible(true);
        }

        return null;
    }

    private DbActionOptionsDialog getStartDialog(List<String> catalogs, List<String> schemas, String currentCatalog, String currentSchema) {
        int command = DbActionOptionsDialog.SELECT;
        return createDialog(catalogs, schemas, currentCatalog, currentSchema, command);
    }

    @SuppressWarnings("unchecked")
    private List<String> getCatalogs(DataSourceWizard connectWizard, Connection connection) throws Exception {
        if(!connectWizard.getAdapter().supportsCatalogsOnReverseEngineering()) {
            return (List<String>) Collections.EMPTY_LIST;
        }

        return DbLoader.loadCatalogs(connection);
    }

    private List<String> getSchemas(Connection connection) throws Exception {
        return DbLoader.loadSchemas(connection);
    }
}
