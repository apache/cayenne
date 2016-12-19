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

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.cayenne.dbsync.reverse.dbload.DbLoader;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.dialog.db.DbActionOptionsDialog;
import org.apache.cayenne.modeler.util.CayenneAction;

public abstract class DBWizardAction<T extends DbActionOptionsDialog> extends CayenneAction {

    public DBWizardAction(String name, Application application) {
        super(name, application);
    }

    protected DataSourceWizard dataSourceWizardDialog(String title) {
        // connect
        DataSourceWizard connectWizard = new DataSourceWizard(getProjectController(), title);
        if (!connectWizard.startupAction()) {
            return null;
        }

        return connectWizard;
    }

    protected abstract T createDialog(Collection<String> catalogs, Collection<String> schemas, String currentCatalog, String currentSchema);

    protected T loaderOptionDialog(DataSourceWizard connectWizard) {

        // use this catalog as the default...
        List<String> catalogs;
        List<String> schemas;
        String currentCatalog;
        String currentSchema;
        try(Connection connection = connectWizard.getDataSource().getConnection()) {
            catalogs = getCatalogs(connectWizard, connection);
            schemas = getSchemas(connection);
            if (catalogs.isEmpty() && schemas.isEmpty()) {
                return null;
            }
            currentCatalog = connection.getCatalog();
            currentSchema = connection.getSchema();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    Application.getFrame(),
                    ex.getMessage(),
                    "Error loading schemas dialog",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        final T optionsDialog = createDialog(catalogs, schemas, currentCatalog, currentSchema);
        optionsDialog.setVisible(true);
        if (optionsDialog.getChoice() == DbActionOptionsDialog.SELECT) {
            return optionsDialog;
        }

        return null;
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
