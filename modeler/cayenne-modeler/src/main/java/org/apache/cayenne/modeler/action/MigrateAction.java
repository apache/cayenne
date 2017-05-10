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

import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.dialog.db.DbActionOptionsDialog;
import org.apache.cayenne.modeler.dialog.db.merge.MergerOptions;

import java.awt.event.ActionEvent;
import java.util.Collection;

/**
 * Action that alter database schema to match a DataMap.
 */
public class MigrateAction extends DBWizardAction<DbActionOptionsDialog> {

    private boolean dialogShown;

    public MigrateAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName() {
        return "Migrate Database Schema";
    }

    public void performAction(ActionEvent e) {

        DataSourceWizard connectWizard = dataSourceWizardDialog("Migrate DB Schema: Connect to Database");
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

    @Override
    protected DbActionOptionsDialog createDialog(Collection<String> catalogs, Collection<String> schemas,
                                                 String currentCatalog, String currentSchema) {
        dialogShown = true;
        return new DbActionOptionsDialog(Application.getFrame(), "Migrate DB Schema: Select Catalog and Schema",
                catalogs, schemas, currentCatalog, currentSchema);
    }
}
