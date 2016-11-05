/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.dbsync.reverse.db;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.filters.CatalogFilter;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.SchemaFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.util.EqualsBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Performs reverse engineering of the database, loading DB metadata in provided DataMap.
 *
 * @since 4.0
 */
public class DbLoader {

    private static final Log LOGGER = LogFactory.getLog(DbLoader.class);

    private static final String WILDCARD = "%";

    private final Connection connection;
    private final DbAdapter adapter;
    private final DbLoaderDelegate delegate;
    private final ObjectNameGenerator nameGenerator;
    private DatabaseMetaData metaData;

    public DbLoader(Connection connection, DbAdapter adapter, DbLoaderDelegate delegate, ObjectNameGenerator nameGenerator) {
        this.adapter = Objects.requireNonNull(adapter);
        this.connection = Objects.requireNonNull(connection);
        this.nameGenerator = Objects.requireNonNull(nameGenerator);
        this.delegate = delegate == null ? new DefaultDbLoaderDelegate() : delegate;
    }

    private static List<String> getStrings(ResultSet rs) throws SQLException {
        List<String> strings = new ArrayList<>();

        while (rs.next()) {
            strings.add(rs.getString(1));
        }

        return strings;
    }

    private static int getDirection(short type) {
        switch (type) {
            case DatabaseMetaData.procedureColumnIn:
                return ProcedureParameter.IN_PARAMETER;
            case DatabaseMetaData.procedureColumnInOut:
                return ProcedureParameter.IN_OUT_PARAMETER;
            case DatabaseMetaData.procedureColumnOut:
                return ProcedureParameter.OUT_PARAMETER;
            default:
                return -1;
        }
    }

    /**
     * Retrieves catalogs for a given connection.
     *
     * @return List with the catalog names; empty list if none found.
     */
    // using a static method for catalog loading as we don't need a full DbLoader for this operation
    public static List<String> loadCatalogs(Connection connection) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getCatalogs()) {
            return getStrings(rs);
        }
    }

    /**
     * Retrieves the schemas for the given connection.
     *
     * @return List with the schema names; empty list if none found.
     */
    // using a static method for catalog loading as we don't need a full DbLoader for this operation
    public static List<String> loadSchemas(Connection connection) throws SQLException {

        try (ResultSet rs = connection.getMetaData().getSchemas()) {
            return getStrings(rs);
        }
    }

    /**
     * Returns DatabaseMetaData object associated with this DbLoader.
     */
    private DatabaseMetaData getMetaData() throws SQLException {
        if (metaData == null) {
            metaData = connection.getMetaData();
        }
        return metaData;
    }

    protected void loadDbRelationships(DbLoaderConfiguration config, String catalog, String schema,
                                       List<DbEntity> tables) throws SQLException {
        if (config.isSkipRelationshipsLoading()) {
            return;
        }

        // Get all the foreign keys referencing this table
        Map<String, DbEntity> tablesMap = new HashMap<>();
        for (DbEntity table : tables) {
            tablesMap.put(table.getName(), table);
        }

        Map<String, Set<ExportedKey>> keys = loadExportedKeys(config, catalog, schema, tablesMap);
        for (Map.Entry<String, Set<ExportedKey>> entry : keys.entrySet()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Process keys for: " + entry.getKey());
            }

            Set<ExportedKey> exportedKeys = entry.getValue();
            ExportedKey key = exportedKeys.iterator().next();
            if (key == null) {
                throw new IllegalStateException();
            }

            DbEntity pkEntity = tablesMap.get(key.getPKTableName());
            if (pkEntity == null) {
                skipRelationLog(key, key.getPKTableName());
                continue;
            }

            DbEntity fkEntity = tablesMap.get(key.getFKTableName());
            if (fkEntity == null) {
                skipRelationLog(key, key.getFKTableName());
                continue;
            }

            if (!new EqualsBuilder().append(pkEntity.getCatalog(), key.getPkCatalog())
                    .append(pkEntity.getSchema(), key.getPkSchema()).append(fkEntity.getCatalog(), key.getFkCatalog())
                    .append(fkEntity.getSchema(), key.getFkSchema()).isEquals()) {

                LOGGER.info("Skip relation: '" + key + "' because it related to objects from other catalog/schema");
                LOGGER.info("     relation primary key: '" + key.getPkCatalog() + "." + key.getPkSchema() + "'");
                LOGGER.info("       primary key entity: '" + pkEntity.getCatalog() + "." + pkEntity.getSchema() + "'");
                LOGGER.info("     relation foreign key: '" + key.getFkCatalog() + "." + key.getFkSchema() + "'");
                LOGGER.info("       foreign key entity: '" + fkEntity.getCatalog() + "." + fkEntity.getSchema() + "'");
                continue;
            }

            // forwardRelationship is a reference from table with primary key
            DbRelationship forwardRelationship = new DbRelationship();

            forwardRelationship.setSourceEntity(pkEntity);
            forwardRelationship.setTargetEntityName(fkEntity);

            // forwardRelationship is a reference from table with foreign key,
            // it is what exactly we load from db

            // TODO: dirty and non-transparent... using DbRelationshipDetected for the benefit of the merge package.
            // This info is available from joins....
            DbRelationshipDetected reverseRelationship = new DbRelationshipDetected();


            reverseRelationship.setFkName(key.getFKName());
            reverseRelationship.setSourceEntity(fkEntity);
            reverseRelationship.setTargetEntityName(pkEntity);
            reverseRelationship.setToMany(false);

            createAndAppendJoins(exportedKeys, pkEntity, fkEntity, forwardRelationship, reverseRelationship);

            boolean toDependentPK = isToDependentPK(forwardRelationship);
            forwardRelationship.setToDependentPK(toDependentPK);

            boolean isOneToOne = toDependentPK
                    && fkEntity.getPrimaryKeys().size() == forwardRelationship.getJoins().size();

            forwardRelationship.setToMany(!isOneToOne);

            // set relationship names only after their joins are ready ... generator logic is based on relationship
            // state...
            forwardRelationship.setName(NameBuilder
                    .builder(forwardRelationship, pkEntity)
                    .baseName(nameGenerator.relationshipName(forwardRelationship))
                    .name());

            reverseRelationship.setName(NameBuilder
                    .builder(reverseRelationship, fkEntity)
                    .baseName(nameGenerator.relationshipName(reverseRelationship))
                    .name());

            if (delegate.dbRelationshipLoaded(fkEntity, reverseRelationship)) {
                fkEntity.addRelationship(reverseRelationship);
            }
            if (delegate.dbRelationshipLoaded(pkEntity, forwardRelationship)) {
                pkEntity.addRelationship(forwardRelationship);
            }
        }
    }

    private boolean isToDependentPK(DbRelationship forwardRelationship) {
        for (DbJoin dbJoin : forwardRelationship.getJoins()) {
            if (!dbJoin.getTarget().isPrimaryKey()) {
                return false;
            }
        }

        return true;
    }

    private void createAndAppendJoins(Set<ExportedKey> exportedKeys, DbEntity pkEntity, DbEntity fkEntity,
                                      DbRelationship forwardRelationship, DbRelationship reverseRelationship) {
        for (ExportedKey exportedKey : exportedKeys) {
            // Create and append joins
            String pkName = exportedKey.getPKColumnName();
            String fkName = exportedKey.getFKColumnName();

            // skip invalid joins...
            DbAttribute pkAtt = pkEntity.getAttribute(pkName);
            if (pkAtt == null) {
                LOGGER.info("no attribute for declared primary key: " + pkName);
                continue;
            }

            DbAttribute fkAtt = fkEntity.getAttribute(fkName);
            if (fkAtt == null) {
                LOGGER.info("no attribute for declared foreign key: " + fkName);
                continue;
            }

            forwardRelationship.addJoin(new DbJoin(forwardRelationship, pkName, fkName));
            reverseRelationship.addJoin(new DbJoin(reverseRelationship, fkName, pkName));
        }
    }

    private Map<String, Set<ExportedKey>> loadExportedKeys(DbLoaderConfiguration config, String catalog, String schema,
                                                           Map<String, DbEntity> tables) throws SQLException {
        Map<String, Set<ExportedKey>> keys = new HashMap<>();

        for (DbEntity dbEntity : tables.values()) {
            if (!delegate.dbRelationship(dbEntity)) {
                continue;
            }

            ResultSet rs;
            try {
                rs = getMetaData().getExportedKeys(catalog, schema, dbEntity.getName());
            } catch (SQLException cay182Ex) {
                // Sybase-specific - the line above blows on VIEWS, see CAY-182.
                LOGGER.info("Error getting relationships for '" + catalog + "." + schema + "', ignoring. "
                        + cay182Ex.getMessage(), cay182Ex);
                return new HashMap<>();
            }

            try {
                while (rs.next()) {
                    ExportedKey key = ExportedKey.extractData(rs);

                    DbEntity fkEntity = tables.get(key.getFKTableName());
                    if (fkEntity == null) {
                        skipRelationLog(key, key.getFKTableName());
                        continue;
                    }

                    if (config.getFiltersConfig().tableFilter(fkEntity.getCatalog(), fkEntity.getSchema())
                            .isIncludeTable(fkEntity.getName()) == null) {
                        continue;
                    }

                    Set<ExportedKey> exportedKeys = keys.get(key.getStrKey());
                    if (exportedKeys == null) {
                        exportedKeys = new TreeSet<ExportedKey>();

                        keys.put(key.getStrKey(), exportedKeys);
                    }
                    exportedKeys.add(key);
                }

            } finally {
                rs.close();
            }
        }
        return keys;
    }

    private void skipRelationLog(ExportedKey key, String tableName) {
        LOGGER.info("Skip relation: '" + key + "' because table '" + tableName + "' not found");
    }

    protected String[] getTableTypes(DbLoaderConfiguration config) {

        String[] configTypes = config.getTableTypes();
        if (configTypes != null && configTypes.length > 0) {
            return configTypes;
        }

        List<String> list = new ArrayList<>(2);

        String viewType = adapter.tableTypeForView();
        if (viewType != null) {
            list.add(viewType);
        }

        String tableType = adapter.tableTypeForTable();
        if (tableType != null) {
            list.add(tableType);
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Performs database reverse engineering based on provided configuration. Stores the resulting {@link DbEntity}
     * and {@link Procedure} objects in provided DataMap.
     *
     * @since 4.0
     */
    public void load(DataMap dataMap, DbLoaderConfiguration config) throws SQLException {
        loadDbEntities(dataMap, config);
        loadProcedures(dataMap, config);
    }

    protected void loadDbEntities(DataMap dataMap, DbLoaderConfiguration config) throws SQLException {

        String[] types = getTableTypes(config);

        for (CatalogFilter catalog : config.getFiltersConfig().getCatalogs()) {
            for (SchemaFilter schema : catalog.schemas) {
                List<DbEntity> entities = createTableLoader(catalog.name, schema.name, schema.tables).loadDbEntities(
                        dataMap, config, types);
                loadDbRelationships(config, catalog.name, schema.name, entities);
            }
        }
    }

    protected DbTableLoader createTableLoader(String catalog, String schema, TableFilter filter) throws SQLException {
        return new DbTableLoader(catalog,
                schema,
                getMetaData(),
                delegate,
                new DbAttributesPerSchemaLoader(catalog, schema, getMetaData(), adapter, filter));
    }


    /**
     * Loads database stored procedures into the DataMap.
     * <p>
     * <i>As of 1.1 there is no boolean property or delegate method to make
     * procedure loading optional or to implement custom merging logic, so
     * currently this method is NOT CALLED from "loadDataMapFromDB" and should
     * be invoked explicitly by the user. </i>
     * </p>
     *
     * @since 4.0
     */
    protected Map<String, Procedure> loadProcedures(DataMap dataMap, DbLoaderConfiguration config) throws SQLException {

        Map<String, Procedure> procedures = loadProcedures(config);
        if (procedures.isEmpty()) {
            return procedures;
        }

        loadProceduresColumns(config, procedures);

        for (Procedure procedure : procedures.values()) {
            dataMap.addProcedure(procedure);
        }

        return procedures;
    }

    private void loadProceduresColumns(DbLoaderConfiguration config, Map<String, Procedure> procedures)
            throws SQLException {

        for (CatalogFilter catalog : config.getFiltersConfig().getCatalogs()) {
            for (SchemaFilter schema : catalog.schemas) {
                loadProceduresColumns(procedures, catalog.name, schema.name);
            }
        }
    }

    private void loadProceduresColumns(Map<String, Procedure> procedures, String catalog, String schema)
            throws SQLException {

        try (ResultSet columnsRS = getMetaData().getProcedureColumns(catalog, schema, null, null);) {
            while (columnsRS.next()) {
                String procSchema = columnsRS.getString("PROCEDURE_SCHEM");
                String procCatalog = columnsRS.getString("PROCEDURE_CAT");
                String name = columnsRS.getString("PROCEDURE_NAME");
                String key = Procedure.generateFullyQualifiedName(procCatalog, procSchema, name);
                Procedure procedure = procedures.get(key);
                if (procedure == null) {
                    continue;
                }

                ProcedureParameter column = loadProcedureParams(columnsRS, key, procedure);
                if (column == null) {
                    continue;
                }
                procedure.addCallParameter(column);
            }
        }
    }

    private ProcedureParameter loadProcedureParams(ResultSet columnsRS, String key, Procedure procedure)
            throws SQLException {
        String columnName = columnsRS.getString("COLUMN_NAME");

        // skip ResultSet columns, as they are not described in Cayenne
        // procedures yet...
        short type = columnsRS.getShort("COLUMN_TYPE");
        if (type == DatabaseMetaData.procedureColumnResult) {
            LOGGER.debug("skipping ResultSet column: " + key + "." + columnName);
        }

        if (columnName == null) {
            if (type == DatabaseMetaData.procedureColumnReturn) {
                LOGGER.debug("null column name, assuming result column: " + key);
                columnName = "_return_value";
                procedure.setReturningValue(true);
            } else {
                LOGGER.info("invalid null column name, skipping column : " + key);
                return null;
            }
        }

        int columnType = columnsRS.getInt("DATA_TYPE");

        // ignore precision of non-decimal columns
        int decimalDigits = -1;
        if (TypesMapping.isDecimal(columnType)) {
            decimalDigits = columnsRS.getShort("SCALE");
            if (columnsRS.wasNull()) {
                decimalDigits = -1;
            }
        }

        ProcedureParameter column = new ProcedureParameter(columnName);
        int direction = getDirection(type);
        if (direction != -1) {
            column.setDirection(direction);
        }

        column.setType(columnType);
        column.setMaxLength(columnsRS.getInt("LENGTH"));
        column.setPrecision(decimalDigits);

        column.setProcedure(procedure);
        return column;
    }

    private Map<String, Procedure> loadProcedures(DbLoaderConfiguration config) throws SQLException {
        Map<String, Procedure> procedures = new HashMap<>();

        FiltersConfig filters = config.getFiltersConfig();
        for (CatalogFilter catalog : filters.getCatalogs()) {
            for (SchemaFilter schema : catalog.schemas) {
                if (filters.proceduresFilter(catalog.name, schema.name).isEmpty()) {
                    continue;
                }

                procedures.putAll(loadProcedures(filters, catalog.name, schema.name));
            }
        }

        return procedures;
    }

    private Map<String, Procedure> loadProcedures(FiltersConfig filters, String catalog, String schema)
            throws SQLException {
        Map<String, Procedure> procedures = new HashMap<>();
        // get procedures

        try (ResultSet rs = getMetaData().getProcedures(catalog, schema, WILDCARD);) {
            while (rs.next()) {

                String name = rs.getString("PROCEDURE_NAME");
                Procedure procedure = new Procedure(name);
                procedure.setCatalog(rs.getString("PROCEDURE_CAT"));
                procedure.setSchema(rs.getString("PROCEDURE_SCHEM"));

                if (!filters.proceduresFilter(procedure.getCatalog(), procedure.getSchema()).isIncluded(
                        procedure.getName())) {
                    LOGGER.info("skipping Cayenne PK procedure: " + name);
                    continue;
                }

                switch (rs.getShort("PROCEDURE_TYPE")) {
                    case DatabaseMetaData.procedureNoResult:
                    case DatabaseMetaData.procedureResultUnknown:
                        procedure.setReturningValue(false);
                        break;
                    case DatabaseMetaData.procedureReturnsResult:
                        procedure.setReturningValue(true);
                        break;
                }

                procedures.put(procedure.getFullyQualifiedName(), procedure);
            }
        }
        return procedures;
    }
}