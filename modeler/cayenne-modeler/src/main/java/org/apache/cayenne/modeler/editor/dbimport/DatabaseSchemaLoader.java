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

package org.apache.cayenne.modeler.editor.dbimport;

import javax.swing.tree.TreePath;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;

public class DatabaseSchemaLoader {

    private static final String INCLUDE_ALL_PATTERN = "%";
    private static final int TABLE_INDEX = 3;
    private static final int SCHEMA_INDEX = 2;
    private static final int CATALOG_INDEX = 1;

    private ReverseEngineering databaseReverseEngineering;

    public DatabaseSchemaLoader() {
        databaseReverseEngineering = new ReverseEngineering();
    }

    public ReverseEngineering load(DBConnectionInfo connectionInfo,
                                   ClassLoadingService loadingService,
                                   String[] tableTypesFromConfig) throws SQLException {
        try (Connection connection = connectionInfo.makeDataSource(loadingService).getConnection()) {
            String[] types = tableTypesFromConfig != null && tableTypesFromConfig.length != 0 ?
                    tableTypesFromConfig :
                    new String[]{"TABLE", "VIEW", "SYSTEM TABLE",
                            "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"};
            processCatalogs(connection, types);
        }
        return databaseReverseEngineering;
    }

    private void processCatalogs(Connection connection, String[] types) throws SQLException {
        String defaultCatalog = connection.getCatalog();
        try (ResultSet rsCatalog = connection.getMetaData().getCatalogs()) {
            boolean hasCatalogs = false;
            while (rsCatalog.next()) {
                hasCatalogs = true;
                ResultSet resultSet = connection.getMetaData()
                        .getTables(processFilter(rsCatalog, defaultCatalog, CATALOG_INDEX),
                                null,
                                INCLUDE_ALL_PATTERN,
                                types);
                packTable(resultSet);
                packFunctions(connection);
            }
            if(!hasCatalogs) {
                processSchemas(connection, types);
            }
        }
    }

    private void processSchemas(Connection connection, String[] types) throws SQLException {
        String defaultSchema = connection.getSchema();
        try(ResultSet rsSchema = connection.getMetaData().getSchemas()) {
            boolean hasSchemas = false;
            while (rsSchema.next()) {
                hasSchemas = true;
                ResultSet resultSet = connection.getMetaData()
                        .getTables(null,
                                processFilter(rsSchema, defaultSchema, SCHEMA_INDEX),
                                INCLUDE_ALL_PATTERN,
                                types);
                packTable(resultSet);
                packFunctions(connection);
            }
            if(!hasSchemas) {
                ResultSet resultSet = connection.getMetaData()
                        .getTables(null,
                                null,
                                INCLUDE_ALL_PATTERN,
                                types);
                packTable(resultSet);
                packTable(resultSet);
            }
        }
    }

    private void packTable(ResultSet resultSet) throws SQLException {
        while (resultSet.next()) {
            String tableName = resultSet.getString(TABLE_INDEX);
            String schemaName = resultSet.getString(SCHEMA_INDEX);
            String catalogName = resultSet.getString(CATALOG_INDEX);
            packTable(tableName, catalogName, schemaName, null);
        }
    }

    private String processFilter(ResultSet resultSet, String defaultFilter, int filterIndex) throws SQLException {
        return defaultFilter.isEmpty() ?
                resultSet.getString(filterIndex) :
                defaultFilter;
    }

    public ReverseEngineering loadColumns(DBConnectionInfo connectionInfo,
                                          ClassLoadingService loadingService,
                                          TreePath path) throws SQLException {
        Object userObject = ((DbImportTreeNode)path.getPathComponent(1)).getUserObject();
        String catalogName = null, schemaName = null;
        if(userObject instanceof Catalog) {
            catalogName = ((Catalog) userObject).getName();
        } else if(userObject instanceof Schema) {
            schemaName = ((Schema) userObject).getName();
        }
        String tableName = path.getPathComponent(2).toString();

        try (Connection connection = connectionInfo.makeDataSource(loadingService).getConnection()) {
            try (ResultSet rs = connection.getMetaData().getColumns(catalogName, schemaName, tableName, null)) {
                while (rs.next()) {
                    String column = rs.getString(4);
                    packTable(tableName, catalogName, schemaName, column);
                }
            }
        }
        return databaseReverseEngineering;
    }

    private void packFunctions(Connection connection) throws SQLException {
        Collection<Catalog> catalogs = databaseReverseEngineering.getCatalogs();
        for (Catalog catalog : catalogs) {
            ResultSet procResultSet = connection.getMetaData().getProcedures(
                    catalog.getName(), null, "%"
            );
            while (procResultSet.next()) {
                IncludeProcedure includeProcedure = new IncludeProcedure(procResultSet.getString(3));
                if (!catalog.getIncludeProcedures().contains(includeProcedure)) {
                    catalog.addIncludeProcedure(includeProcedure);
                }
            }
        }
        for (Schema schema : databaseReverseEngineering.getSchemas()) {
            ResultSet procResultSet = connection.getMetaData().getProcedures(
                    null, schema.getName(), "%"
            );
            while (procResultSet.next()) {
                IncludeProcedure includeProcedure = new IncludeProcedure(procResultSet.getString(3));
                if (!schema.getIncludeProcedures().contains(includeProcedure)) {
                    schema.addIncludeProcedure(includeProcedure);
                }
            }
        }
        for (Catalog catalog : catalogs) {
            for (Schema schema : catalog.getSchemas()) {
                ResultSet procResultSet = connection.getMetaData().getProcedures(
                        catalog.getName(), schema.getName(), "%"
                );
                while (procResultSet.next()) {
                    IncludeProcedure includeProcedure = new IncludeProcedure(procResultSet.getString(3));
                    if (!schema.getIncludeProcedures().contains(includeProcedure)) {
                        schema.addIncludeProcedure(includeProcedure);
                    }
                }
            }
        }
    }

    private void packTable(String tableName, String catalogName, String schemaName, String columnName) {
        IncludeTable table = new IncludeTable();
        table.setPattern(tableName);

        if (catalogName == null && schemaName == null) {
            if (!databaseReverseEngineering.getIncludeTables().contains(table)) {
                databaseReverseEngineering.addIncludeTable(table);
            }
            return;
        }

        FilterContainer filterContainer;
        if (catalogName != null && schemaName == null) {
            Catalog parentCatalog = getCatalogByName(databaseReverseEngineering.getCatalogs(), catalogName);

            if(parentCatalog == null) {
                parentCatalog = new Catalog();
                parentCatalog.setName(catalogName);
                databaseReverseEngineering.addCatalog(parentCatalog);
            }
            filterContainer = parentCatalog;
        } else if (catalogName == null) {
            Schema parentSchema = getSchemaByName(databaseReverseEngineering.getSchemas(), schemaName);

            if(parentSchema == null) {
                parentSchema = new Schema();
                parentSchema.setName(schemaName);
                databaseReverseEngineering.addSchema(parentSchema);
            }
            filterContainer = parentSchema;
        } else {
            Catalog parentCatalog = getCatalogByName(databaseReverseEngineering.getCatalogs(), catalogName);
            Schema parentSchema;
            if (parentCatalog != null) {
                parentSchema = getSchemaByName(parentCatalog.getSchemas(), schemaName);
                if(parentSchema == null) {
                    parentSchema = new Schema();
                    parentSchema.setName(schemaName);
                    parentCatalog.addSchema(parentSchema);
                }
            } else {
                parentCatalog = new Catalog();
                parentCatalog.setName(catalogName);
                parentSchema = new Schema();
                parentSchema.setName(schemaName);
                databaseReverseEngineering.addCatalog(parentCatalog);
            }
            filterContainer = parentSchema;
        }

        addTable(filterContainer, table);
        addColumn(filterContainer, table, columnName);
    }

    private void addTable(FilterContainer parentFilter, IncludeTable table) {
        if (!parentFilter.getIncludeTables().contains(table)) {
            parentFilter.addIncludeTable(table);
        }
    }

    private void addColumn(FilterContainer filterContainer, IncludeTable table, String columnName) {
        IncludeTable foundTable = getTableByName(filterContainer.getIncludeTables(), table.getPattern());
        table = foundTable != null ? foundTable : table;
        if (columnName != null ) {
            IncludeColumn includeColumn = new IncludeColumn(columnName);
            table.addIncludeColumn(includeColumn);
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

    private IncludeTable getTableByName(Collection<IncludeTable> tables, String catalogName) {
        for (IncludeTable table : tables) {
            if (table.getPattern().equals(catalogName)) {
                return table;
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
}
