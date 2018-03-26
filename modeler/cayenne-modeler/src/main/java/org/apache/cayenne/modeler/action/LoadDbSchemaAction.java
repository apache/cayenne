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

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.editor.DbImportModel;
import org.apache.cayenne.modeler.editor.DbImportView;
import org.apache.cayenne.modeler.editor.DraggableTreePanel;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.util.CayenneAction;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

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
    private static final String INCLUDE_ALL_PATTERN = "%";
    private static final String EMPTY_DEFAULT_CATALOG = "";
    private static final int TABLE_INDEX = 3;
    private static final int SCHEMA_INDEX = 2;
    private static final int CATALOG_INDEX = 1;

    private ReverseEngineering databaseReverseEngineering;
    private DraggableTreePanel draggableTreePanel;

    public LoadDbSchemaAction(Application application) {
        super(ACTION_NAME, application);
    }

    public String getIconName() {
        return ICON_NAME;
    }

    @Override
    public void performAction(ActionEvent e) {
        final DbImportView rootParent = ((DbImportView) draggableTreePanel.getParent().getParent());
        rootParent.getLoadDbSchemaProgress().setVisible(true);
        rootParent.getLoadDbSchemaButton().setEnabled(false);
        Thread thread = new Thread(() -> {
            LoadDbSchemaAction.this.setEnabled(false);
            draggableTreePanel.getMoveButton().setEnabled(false);
            draggableTreePanel.getMoveInvertButton().setEnabled(false);
            rootParent.lockToolbarButtons();
            DBConnectionInfo connectionInfo;
            if (!datamapPreferencesExist()) {
                final DataSourceWizard connectWizard = new DataSourceWizard(getProjectController(), "Load Db Schema");
                connectWizard.setProjectController(getProjectController());
                if (!connectWizard.startupAction()) {
                    return;
                }
                connectionInfo = connectWizard.getConnectionInfo();
                saveConnectionInfo(connectWizard);
            } else {
                connectionInfo = getConnectionInfoFromPreferences();
            }

            databaseReverseEngineering = new ReverseEngineering();

            try(Connection connection = connectionInfo.makeDataSource(getApplication().getClassLoadingService()).getConnection()) {
                String[] types = {"TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"};
                try (ResultSet rs = connection.getMetaData().getCatalogs()) {
                    String defaultCatalog = connection.getCatalog();
                    while (rs.next()) {
                        ResultSet resultSet;
                        if (defaultCatalog.equals(EMPTY_DEFAULT_CATALOG)) {
                            resultSet = connection.getMetaData().getTables(rs.getString(1), null, INCLUDE_ALL_PATTERN, types);
                        } else {
                            resultSet = connection.getMetaData().getTables(defaultCatalog, null, INCLUDE_ALL_PATTERN, types);
                        }
                        String tableName = "";
                        String schemaName = "";
                        String catalogName = "";
                        while (resultSet.next()) {
                            tableName = resultSet.getString(TABLE_INDEX);
                            schemaName = resultSet.getString(SCHEMA_INDEX);
                            catalogName = resultSet.getString(CATALOG_INDEX);
                            packTable(tableName, catalogName, schemaName);
                        }
                        packFunctions(connection);
                    }
                }

                draggableTreePanel.getSourceTree().setEnabled(true);
                draggableTreePanel.getSourceTree().translateReverseEngineeringToTree(databaseReverseEngineering, true);
                draggableTreePanel.bindReverseEngineeringToDatamap(getProjectController().getCurrentDataMap(), databaseReverseEngineering);
                ((DbImportModel) draggableTreePanel.getSourceTree().getModel()).reload();
            } catch (SQLException exception) {
                JOptionPane.showMessageDialog(
                        Application.getFrame(),
                        exception.getMessage(),
                        "Error db schema loading",
                        JOptionPane.ERROR_MESSAGE);
            }
            finally {
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

    private void packFunctions(Connection connection) throws SQLException {
        Collection<Catalog> catalogs = databaseReverseEngineering.getCatalogs();
        for (Catalog catalog : catalogs) {
            ResultSet procResultSet = connection.getMetaData().getProcedures(catalog.getName(), null, "%");
            while (procResultSet.next()) {
                IncludeProcedure includeProcedure = new IncludeProcedure(procResultSet.getString(3));
                if (!catalog.getIncludeProcedures().contains(includeProcedure)) {
                    catalog.addIncludeProcedure(includeProcedure);
                }
            }
        }
        for (Schema schema : databaseReverseEngineering.getSchemas()) {
            ResultSet procResultSet = connection.getMetaData().getProcedures(null, schema.getName(), "%");
            while (procResultSet.next()) {
                IncludeProcedure includeProcedure = new IncludeProcedure(procResultSet.getString(3));
                if (!schema.getIncludeProcedures().contains(includeProcedure)) {
                    schema.addIncludeProcedure(includeProcedure);
                }
            }
        }
        for (Catalog catalog : catalogs) {
            for (Schema schema : catalog.getSchemas()) {
                ResultSet procResultSet = connection.getMetaData().getProcedures(catalog.getName(), schema.getName(), "%");
                while (procResultSet.next()) {
                    IncludeProcedure includeProcedure = new IncludeProcedure(procResultSet.getString(3));
                    if (!schema.getIncludeProcedures().contains(includeProcedure)) {
                        schema.addIncludeProcedure(includeProcedure);
                    }
                }
            }
        }
    }

    private void packTable(String tableName, String catalogName, String schemaName) {
        IncludeTable newTable = new IncludeTable();
        newTable.setPattern(tableName);
        if ((catalogName == null) && (schemaName == null)) {
            if (!databaseReverseEngineering.getIncludeTables().contains(newTable)) {
                databaseReverseEngineering.addIncludeTable(newTable);
            }
        }
        if ((catalogName != null) && (schemaName == null)) {
            Catalog parentCatalog = getCatalogByName(databaseReverseEngineering.getCatalogs(), catalogName);
            if (parentCatalog != null) {
                if (!parentCatalog.getIncludeTables().contains(newTable)) {
                    parentCatalog.addIncludeTable(newTable);
                }
            } else {
                parentCatalog = new Catalog();
                parentCatalog.setName(catalogName);
                if (!parentCatalog.getIncludeTables().contains(newTable)) {
                    parentCatalog.addIncludeTable(newTable);
                }
                databaseReverseEngineering.addCatalog(parentCatalog);
            }
        }
        if ((catalogName == null) && (schemaName != null)) {
            Schema parentSchema = getSchemaByName(databaseReverseEngineering.getSchemas(), schemaName);
            if (parentSchema != null) {
                if (!parentSchema.getIncludeTables().contains(newTable)) {
                    parentSchema.addIncludeTable(newTable);
                }
            } else {
                parentSchema = new Schema();
                parentSchema.setName(schemaName);
                if (!parentSchema.getIncludeTables().contains(newTable)) {
                    parentSchema.addIncludeTable(newTable);
                }
                databaseReverseEngineering.addSchema(parentSchema);
            }
        }
        if ((catalogName != null) && (schemaName != null)) {
            Catalog parentCatalog = getCatalogByName(databaseReverseEngineering.getCatalogs(), catalogName);
            Schema parentSchema;
            if (parentCatalog != null) {
                parentSchema = getSchemaByName(parentCatalog.getSchemas(), schemaName);
                if (parentSchema != null) {
                    if (!parentSchema.getIncludeTables().contains(newTable)) {
                        parentSchema.addIncludeTable(newTable);
                    }
                } else {
                    parentSchema = new Schema();
                    parentSchema.setName(schemaName);
                    if (!parentSchema.getIncludeTables().contains(newTable)) {
                        parentSchema.addIncludeTable(newTable);
                    }
                    parentCatalog.addSchema(parentSchema);
                }
            } else {
                parentCatalog = new Catalog();
                parentCatalog.setName(catalogName);
                parentSchema = new Schema();
                parentSchema.setName(schemaName);
                if (!parentSchema.getIncludeTables().contains(newTable)) {
                    parentSchema.addIncludeTable(newTable);
                }
                databaseReverseEngineering.addCatalog(parentCatalog);
            }
        }
    }

    private Catalog getCatalogByName(Collection<Catalog> catalogs, String catalogName) {
        for (Catalog catalog : catalogs) {
            if (catalog.getName().equals(catalogName)) {
                return catalog;
            }
        }
        return null;
    }

    private Schema getSchemaByName(Collection<Schema> schemas, String schemaName) {
        for (Schema schema : schemas) {
            if (schema.getName().equals(schemaName)) {
                return schema;
            }
        }
        return null;
    }

    public void setDraggableTreePanel(DraggableTreePanel draggableTreePanel) {
        this.draggableTreePanel = draggableTreePanel;
    }
}
