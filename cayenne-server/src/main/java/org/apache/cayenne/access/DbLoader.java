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
package org.apache.cayenne.access;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.access.loader.DbLoaderConfiguration;
import org.apache.cayenne.access.loader.ManyToManyCandidateEntity;
import org.apache.cayenne.access.loader.filters.*;
import org.apache.cayenne.access.loader.filters.CatalogFilter;
import org.apache.cayenne.access.loader.filters.SchemaFilter;
import org.apache.cayenne.access.loader.filters.TableFilter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DbRelationshipDetected;
import org.apache.cayenne.map.DetectedDbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.ExportedKey;
import org.apache.cayenne.map.naming.LegacyNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.map.naming.ObjectNameGenerator;
import org.apache.cayenne.util.EntityMergeSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class that does reverse engineering of the database. It can create
 * DataMaps using database meta data obtained via JDBC driver.
 */
public class DbLoader {

    private static final Log LOGGER = LogFactory.getLog(DbLoader.class);

    public static final String WILDCARD = "%";
    public static final String WILDCARD_PATTERN = ".*";

    /**
     * CAY-479 - need to track which entities which are skipped during loading from db since it it already present in
     * dataMap and haven't marked for overriding so that relationships to non-skipped entities can be loaded
     */
    private Set<DbEntity> skippedEntities = new HashSet<DbEntity>();

    private final Connection connection;
    private final DbAdapter adapter;
    private final DbLoaderDelegate delegate;

    private boolean creatingMeaningfulPK;

    private DatabaseMetaData metaData;


    /**
     * Strategy for choosing names for entities, attributes and relationships
     */
    private ObjectNameGenerator nameGenerator;

    /**
     * Creates new DbLoader.
     */
    public DbLoader(Connection connection, DbAdapter adapter, DbLoaderDelegate delegate) {
        this(connection, adapter, delegate, new LegacyNameGenerator());
    }

    /**
     * Creates new DbLoader with specified naming strategy.
     * 
     * @since 3.0
     */
    public DbLoader(Connection connection, DbAdapter adapter, DbLoaderDelegate delegate, ObjectNameGenerator strategy) {
        this.adapter = adapter;
        this.connection = connection;
        this.delegate = delegate;

        setNameGenerator(strategy);
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

    /**
     * @since 3.0
     */
    public void setCreatingMeaningfulPK(boolean creatingMeaningfulPK) {
        this.creatingMeaningfulPK = creatingMeaningfulPK;
    }

    /**
     * Returns true if the generator should map all primary key columns as
     * ObjAttributes.
     *
     * @since 3.0
     */
    public boolean isCreatingMeaningfulPK() {
        return creatingMeaningfulPK;
    }

    /**
     * Returns database connection used by this DbLoader.
     * 
     * @since 3.0
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Returns DbAdapter associated with this DbLoader.
     * 
     * @since 1.1
     */
    public DbAdapter getAdapter() {
        return adapter;
    }

    /**
     * Retrieves catalogs for the database associated with this DbLoader.
     * 
     * @return List with the catalog names, empty Array if none found.
     */
    public List<String> getCatalogs() throws SQLException {
        return getStrings(getMetaData().getCatalogs());
    }

    /**
     * Retrieves the schemas for the database.
     * 
     * @return List with the schema names, empty Array if none found.
     */
    public List<String> getSchemas() throws SQLException {
        return getStrings(getMetaData().getSchemas());
    }

    private static List<String> getStrings(ResultSet rs) throws SQLException {
        List<String> strings = new ArrayList<String>();
        try {
            while (rs.next()) {
                strings.add(rs.getString(1));
            }
        } finally {
            rs.close();
        }
        return strings;
    }

    /**
     * Returns all the table types for the given database. Types may be such as
     * Typical types are "TABLE",
     *                  "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     *                  "LOCAL TEMPORARY", "ALIAS", "SYNONYM"., etc.
     * 
     * @return List of Strings, empty array if nothing found.
     */
    public List<String> getTableTypes() throws SQLException {
        List<String> types = new ArrayList<String>();
        ResultSet rs = getMetaData().getTableTypes();

        try {
            while (rs.next()) {
                types.add(rs.getString("TABLE_TYPE").trim());
            }
        } finally {
            rs.close();
        }
        return types;
    }

    /**
     * Returns all tables for given combination of the criteria. Tables returned
     * as DbEntities without any attributes or relationships.
     * 
     *
     * @param config
     *
     * @param types
     *            The types of table names to retrieve, null returns all types.
     * @return
     * @since 3.2
     */
    public Map<DbEntity, PatternFilter> getTables(DbLoaderConfiguration config, String[] types)
            throws SQLException {

        Map<DbEntity, PatternFilter> tables = new HashMap<DbEntity, PatternFilter>();
        FiltersConfig filters = config.getFiltersConfig();
        for (CatalogFilter o : filters.catalogs) {
            for (SchemaFilter schema : o.schemas) {
                tables.putAll(getDbEntities(filters, o.name, schema.name, types));
            }
        }

        return tables;
    }

    private Map<DbEntity, PatternFilter> getDbEntities(FiltersConfig filters, String catalog, String schema, String[] types) throws SQLException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Read tables: catalog=" + catalog + ", schema=" + schema + ", types="
                    + Arrays.toString(types));
        }

        ResultSet rs = getMetaData().getTables(catalog, schema, WILDCARD, types);

        Map<DbEntity, PatternFilter> tables = new HashMap<DbEntity, PatternFilter>();
        try {
            while (rs.next()) {
                // Oracle 9i and newer has a nifty recycle bin feature... but we don't
                // want dropped tables to be included here; in fact they may even result
                // in errors on reverse engineering as their names have special chars like
                // "/", etc. So skip them all together

                String name = rs.getString("TABLE_NAME");
                if (name == null) {
                    continue;
                }

                DbEntity table = new DetectedDbEntity(name);

                String c = rs.getString("TABLE_CAT");
                table.setCatalog(c);

                String s = rs.getString("TABLE_SCHEM");
                table.setSchema(s);

                PatternFilter filter = filters.tableFilter(c, s).isIncludeTable(table.getName());
                if (filter != null) {
                    tables.put(table, filter);
                }
            }
        } finally {
            rs.close();
        }
        return tables;
    }

    /**
     * Loads dbEntities for the specified tables.
     * @param map
     *            DataMap to be populated with DbEntities.
     * @param tables
     * @param config
     */
    public List<DbEntity> loadDbEntities(DataMap map, Map<DbEntity, PatternFilter> tables, DbLoaderConfiguration config) throws SQLException {
        /** List of db entities to process. */
        List<DbEntity> dbEntityList = new ArrayList<DbEntity>();
        for (Entry<DbEntity, PatternFilter> entry : tables.entrySet()) {
            DbEntity dbEntity = entry.getKey();
            PatternFilter tableFilter = entry.getValue();

            // Check if there already is a DbEntity under such name
            // if so, consult the delegate what to do
            DbEntity oldEnt = map.getDbEntity(dbEntity.getName());
            if (oldEnt != null) {
                if (delegate == null) {
                    break; // no delegate, don't know what to do, cancel import
                    // TODO continue?
                }

                try {
                    if (delegate.overwriteDbEntity(oldEnt)) {
                        LOGGER.debug("Overwrite: " + oldEnt.getName());
                        map.removeDbEntity(oldEnt.getName(), true);
                        delegate.dbEntityRemoved(oldEnt);
                    } else {
                        LOGGER.debug("Keep old: " + oldEnt.getName());

                        // cay-479 - need to track entities that were not loaded for
                        // relationships exported to entities that were
                        skippedEntities.add(oldEnt);
                        continue;
                    }
                } catch (CayenneException ex) {
                    LOGGER.debug("Load canceled.");

                    return null; // cancel immediately
                }
            }



            map.addDbEntity(dbEntity);

            // notify delegate
            if (delegate != null) {
                delegate.dbEntityAdded(dbEntity);
            }
            loadDbAttributes(dbEntity, tableFilter);

            // delegate might have thrown this entity out... so check if it is still
            // around before continuing processing
            if (map.getDbEntity(dbEntity.getName()) == dbEntity) {
                dbEntityList.add(dbEntity);
            }
        }

        // get primary keys for each table and store it in dbEntity
        getPrimaryKeysForEachTableAndStoreItInDbEntity(map, tables);

        // cay-479 - iterate skipped DbEntities to populate exported keys
        for (DbEntity skippedEntity : skippedEntities) {
            loadDbRelationships(map, skippedEntity, config);
        }

        return dbEntityList;

    }

    private void getPrimaryKeysForEachTableAndStoreItInDbEntity(DataMap map, Map<DbEntity, PatternFilter> tables)
            throws SQLException {

        for (DbEntity dbEntity : map.getDbEntities()) {
            if (tables.get(dbEntity) == null) { // TODO is it ok? equals is not overridden
                continue;
            }

            ResultSet rs = getMetaData().getPrimaryKeys(dbEntity.getCatalog(), dbEntity.getSchema(), dbEntity.getName());
            try {
                while (rs.next()) {
                    String columnName = rs.getString(4); // COLUMN_NAME
                    DbAttribute attribute = dbEntity.getAttribute(columnName);

                    if (attribute != null) {
                        attribute.setPrimaryKey(true);
                    } else {
                        // why an attribute might be null is not quiet clear
                        // but there is a bug report 731406 indicating that it is possible
                        // so just print the warning, and ignore
                        LOGGER.warn("Can't locate attribute for primary key: " + columnName);
                    }

                    String pkName = rs.getString(6); // PK_NAME
                    if (pkName != null && dbEntity instanceof DetectedDbEntity) {
                        ((DetectedDbEntity) dbEntity).setPrimaryKeyName(pkName);
                    }
                }
            } finally {
                rs.close();
            }
        }
    }

    private void loadDbAttributes(DbEntity dbEntity, PatternFilter columnFilter) throws SQLException {
        ResultSet rs = getMetaData().getColumns(dbEntity.getCatalog(), dbEntity.getSchema(), dbEntity.getName(), "%");

        try {
            while (rs.next()) {
                // for a reason not quiet apparent to me, Oracle sometimes
                // returns duplicate record sets for the same table, messing up table
                // names. E.g. for the system table "WK$_ATTR_MAPPING" columns are
                // returned twice - as "WK$_ATTR_MAPPING" and "WK$$_ATTR_MAPPING"... Go figure

                String tableName = rs.getString("TABLE_NAME");
                if (!dbEntity.getName().equals(tableName)) {
                    LOGGER.info("Incorrectly returned columns for '" + tableName + ", skipping.");
                    continue;
                }

                DbAttribute attr = loadDbAttribute(rs);
                attr.setEntity(dbEntity);

                if (!columnFilter.isInclude(attr.getName())) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Importing: attribute '" + attr.getEntity().getName() + "." + attr.getName()
                                + "' is skipped (Path: " + dbEntity.getCatalog() + "/" + dbEntity.getSchema() + "/"
                                + dbEntity.getName() + "; Filter: " + columnFilter + ")");
                    }
                    continue;
                }

                dbEntity.addAttribute(attr);
            }
        } finally {
            rs.close();
        }
    }

    private DbAttribute loadDbAttribute(ResultSet rs) throws SQLException {
        // gets attribute's (column's) information
        int columnType = rs.getInt("DATA_TYPE");

        // ignore precision of non-decimal columns
        int decimalDigits = -1;
        if (TypesMapping.isDecimal(columnType)) {
            decimalDigits = rs.getInt("DECIMAL_DIGITS");
            if (rs.wasNull()) {
                decimalDigits = -1;
            }
        }

        // create attribute delegating this task to adapter
        DbAttribute attr = adapter.buildAttribute(
                rs.getString("COLUMN_NAME"),
                rs.getString("TYPE_NAME"),
                columnType,
                rs.getInt("COLUMN_SIZE"),
                decimalDigits,
                rs.getBoolean("NULLABLE"));

        if (adapter.supportsGeneratedKeys()) {

            // TODO: this actually throws on some drivers... need to
            // ensure that 'supportsGeneratedKeys' check is enough
            // to prevent an exception here.
            String autoIncrement = rs.getString("IS_AUTOINCREMENT");
            if ("YES".equals(autoIncrement)) {
                attr.setGenerated(true);
            }
        }
        return attr;
    }

    /**
     * Creates an ObjEntity for each DbEntity in the map. ObjEntities are
     * created empty without
     */
    protected void loadObjEntities(DataMap map, DbLoaderConfiguration config, Collection<DbEntity> entities) {
        if (entities.isEmpty()) {
            return;
        }

        Collection<ObjEntity> loadedEntities = new ArrayList<ObjEntity>(entities.size());

        // doLoad empty ObjEntities for all the tables
        for (DbEntity dbEntity : entities) {

            // check if there are existing entities
        	
			// TODO: performance. This is an O(n^2) search and it shows on
			// YourKit profiles. Pre-cache mapped entities perhaps (?)
			Collection<ObjEntity> existing = map.getMappedEntities(dbEntity);
			if (!existing.isEmpty()) {
				loadedEntities.addAll(existing);
				continue;
			}

            String objEntityName = DefaultUniqueNameGenerator.generate(NameCheckers.objEntity, map,
                    nameGenerator.createObjEntityName(dbEntity));

            ObjEntity objEntity = new ObjEntity(objEntityName);
            objEntity.setDbEntity(dbEntity);
            objEntity.setClassName(config.getGenericClassName() != null ? config.getGenericClassName()
                    : map.getNameWithDefaultPackage(objEntity.getName()));

            map.addObjEntity(objEntity);
            loadedEntities.add(objEntity);
        }

        // update ObjEntity attributes and relationships
        createEntityMerger(map).synchronizeWithDbEntities(loadedEntities);
    }

    /**
     * @since 4.0
     */
    protected EntityMergeSupport createEntityMerger(DataMap map) {
        return new EntityMergeSupport(map, nameGenerator, !creatingMeaningfulPK);
    }

    /** Loads database relationships into a DataMap. */
    protected void loadDbRelationships(DataMap map, DbLoaderConfiguration config, Iterable<DbEntity> entities) throws SQLException {
        for (DbEntity pkEntity : entities) {
            loadDbRelationships(map, pkEntity, config);
        }
    }

    protected void loadDbRelationships(DataMap map, DbEntity entity, DbLoaderConfiguration config) throws SQLException {
        // Get all the foreign keys referencing this table
        ResultSet rs;

        try {
            rs = getMetaData().getExportedKeys(entity.getCatalog(), entity.getSchema(), entity.getName());
        } catch (SQLException cay182Ex) {
            // Sybase-specific - the line above blows on VIEWS, see CAY-182.
            LOGGER.info("Error getting relationships for '" + entity.getName() + "', ignoring.");
            return;
        }

        try {
            // these will be initialized every time a new target entity is found
            // in the result set (which should be ordered by table name among other things)
            DbRelationship forwardRelationship = null;
            DbRelationshipDetected reverseRelationship = null;
            DbEntity fkEntity = null;
            ExportedKey key = null;

            while (rs.next()) {
                key = ExportedKey.extractData(rs);

                short keySeq = rs.getShort("KEY_SEQ");
                if (keySeq == 1) {

                    if (forwardRelationship != null) {
                        postProcessMasterDbRelationship(forwardRelationship, key);
                        forwardRelationship = null;
                    }

                    // start new entity
                    String fkEntityName = key.getFKTableName();

                    fkEntity = map.getDbEntity(fkEntityName);
                    PatternFilter filter = config.getFiltersConfig()
                            .tableFilter(entity.getCatalog(), entity.getSchema())
                                .isIncludeTable(fkEntity.getName());
                    if (filter == null) {
                        continue;
                    }

                    if (fkEntity == null) {
                        LOGGER.info("FK warning: no entity found for name '" + fkEntityName + "'");
                    } else if (skippedEntities.contains(entity) && skippedEntities.contains(fkEntity)) {
                        // cay-479 - don't doLoad relationships between two skipped entities.
                        continue;
                    } else {
                        // init relationship
                        forwardRelationship = new DbRelationship(generateName(entity, key, true));
                        forwardRelationship.setSourceEntity(entity);
                        forwardRelationship.setTargetEntity(fkEntity);
                        entity.addRelationship(forwardRelationship);

                        reverseRelationship = new DbRelationshipDetected(generateName(fkEntity, key, false));
                        reverseRelationship.setFkName(key.getFKName());
                        reverseRelationship.setToMany(false);
                        reverseRelationship.setSourceEntity(fkEntity);
                        reverseRelationship.setTargetEntity(entity);
                        fkEntity.addRelationship(reverseRelationship);
                    }
                }

                if (fkEntity != null) {
                    // Create and append joins
                    String pkName = key.getPKColumnName();
                    String fkName = key.getFKColumnName();

                    // skip invalid joins...
                    DbAttribute pkAtt = entity.getAttribute(pkName);
                    if (pkAtt == null) {
                        LOGGER.info("no attribute for declared primary key: " + pkName);
                        continue;
                    }

                    DbAttribute fkAtt = fkEntity.getAttribute(fkName);
                    if (fkAtt == null) {
                        LOGGER.info("no attribute for declared foreign key: " + fkName);
                        continue;
                    }

                    if (forwardRelationship != null) {
                        forwardRelationship.addJoin(new DbJoin(forwardRelationship, pkName, fkName));
                    }
                    if (reverseRelationship != null) {
                        reverseRelationship.addJoin(new DbJoin(reverseRelationship, fkName, pkName));
                    }

                }
            }

            if (forwardRelationship != null) {
                postProcessMasterDbRelationship(forwardRelationship, key);
                forwardRelationship = null;
            }

        } finally {
            rs.close();
        }
    }

    private String generateName(DbEntity entity, ExportedKey key, boolean toMany) {
        String forwardPreferredName = nameGenerator.createDbRelationshipName(key, toMany);
        return DefaultUniqueNameGenerator
                .generate(NameCheckers.dbRelationship, entity, forwardPreferredName);
    }

    /**
     * Detects correct relationship multiplicity and "to dep pk" flag. Only
     * called on relationships from PK to FK, not the reverse ones.
     */
    protected void postProcessMasterDbRelationship(DbRelationship relationship, ExportedKey key) {
        boolean toPK = true;
        List<DbJoin> joins = relationship.getJoins();

        for (DbJoin join : joins) {
            if (!join.getTarget().isPrimaryKey()) {
                toPK = false;
                break;
            }

        }

        relationship.setToDependentPK(toPK);
        relationship.setToMany(!(toPK && relationship.getTargetEntity().getPrimaryKeys().size() == joins.size()));

        // if this is really to-one we need to rename the relationship
        if (!relationship.isToMany()) {
            Entity source = relationship.getSourceEntity();
            source.removeRelationship(relationship.getName());
            relationship.setName(DefaultUniqueNameGenerator.generate(NameCheckers.dbRelationship, source,
                    nameGenerator.createDbRelationshipName(key, false)));

            source.addRelationship(relationship);
        }
    }

    /**
     * Flattens many-to-many relationships in the generated model.
     */
    private void flattenManyToManyRelationships(DataMap map) {
        Collection<ObjEntity> entitiesForDelete = new LinkedList<ObjEntity>();

        for (ObjEntity curEntity : map.getObjEntities()) {
            ManyToManyCandidateEntity entity = ManyToManyCandidateEntity.build(curEntity);

            if (entity != null) {
                entity.optimizeRelationships(getNameGenerator());
                entitiesForDelete.add(curEntity);
            }
        }

        // remove needed entities
        for (ObjEntity curDeleteEntity : entitiesForDelete) {
            map.removeObjEntity(curDeleteEntity.getName(), true);
        }
    }

    private void fireObjEntitiesAddedEvents(DataMap map) {
        for (ObjEntity curEntity : map.getObjEntities()) {
            // notify delegate
            if (delegate != null) {
                delegate.objEntityAdded(curEntity);
            }
        }
    }

    /**
     * By default we want to load Tables and Views for mo types
     *
     * @see DbLoader#getTableTypes()
     * @since 4.0
     */
    public String[] getDefaultTableTypes() {
        List<String> list = new ArrayList<String>(2);

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
     * Performs database reverse engineering and generates DataMap that contains
     * default mapping of the tables and views. By default will include regular
     * tables and views.
     * 
     * @since 1.0.7
     * @deprecated since 4.0 use
     */
    @Deprecated
    public DataMap loadDataMapFromDB(String schemaPattern, String tablePattern, DataMap dataMap) throws SQLException {

        DbLoaderConfiguration configuration = new DbLoaderConfiguration();
        configuration.setFiltersConfig(FiltersConfig.create(null, schemaPattern,
                        TableFilter.include(tablePattern), PatternFilter.INCLUDE_NOTHING));

        load(dataMap, configuration);
        return dataMap;
    }

    /**
     * Performs database reverse engineering and generates DataMap object that
     * contains default mapping of the tables and views. Allows to limit types
     * of tables to read.
     * 
     * @deprecated since 4.0 use
     */
    @Deprecated
    public DataMap loadDataMapFromDB(String schemaPattern, String tablePattern, String[] tableTypes, DataMap dataMap)
            throws SQLException {
        dataMap.clear();

        DbLoaderConfiguration config = new DbLoaderConfiguration();
        config.setFiltersConfig(FiltersConfig.create(null, schemaPattern,
                TableFilter.include(tablePattern), PatternFilter.INCLUDE_NOTHING));
        config.setTableTypes(tableTypes);
        
        load(dataMap, config);
        return dataMap;
    }

    /**
     * Performs database reverse engineering based on the specified config 
     * and fills the specified
     * DataMap object with DB and object mapping info.
     *
     * @since 4.0
     */
	public void load(DataMap dataMap, DbLoaderConfiguration config) throws SQLException {

		String[] tableTypes = config.getTableTypes() == null ? this.getDefaultTableTypes() : config.getTableTypes();
		List<DbEntity> entities = loadDbEntities(dataMap, getTables(config, tableTypes), config);

		if (entities != null) {
			loadDbRelationships(dataMap, config, entities);
			loadObjEntities(dataMap, config, entities);

			flattenManyToManyRelationships(dataMap);
			fireObjEntitiesAddedEvents(dataMap);
		}

		loadProcedures(dataMap, config);
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
     * @since 1.1
     * @deprecated since 4.0 use loadProcedures(DataMap, String, String, String) that supports "catalog" pattern.
     */
    @Deprecated
    public void loadProceduresFromDB(String schemaPattern, String namePattern, DataMap dataMap) throws SQLException {
        DbLoaderConfiguration configuration = new DbLoaderConfiguration();
        configuration.setFiltersConfig(FiltersConfig.create(null, schemaPattern,
                        TableFilter.include(null), new PatternFilter().include(namePattern)));

        loadProcedures(dataMap, configuration);
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
    public Map<String, Procedure> loadProcedures(DataMap dataMap, DbLoaderConfiguration config)
            throws SQLException {

        Map<String, Procedure> procedures = loadProcedures(config);
        if (procedures.isEmpty()) {
            return procedures;
        }

        loadProceduresColumns(procedures);

        for (Procedure procedure : procedures.values()) {
            dataMap.addProcedure(procedure);
        }

        return procedures;
    }

    private void loadProceduresColumns(Map<String, Procedure> procedures) throws SQLException {
        ResultSet columnsRS = getMetaData().getProcedureColumns(null, null, null, null); // TODO catalog, schema
        try {
            while (columnsRS.next()) {

                String schema = columnsRS.getString("PROCEDURE_SCHEM");
                String name = columnsRS.getString("PROCEDURE_NAME");
                String key = (schema == null ? "" : schema + '.') + name ;
                Procedure procedure = procedures.get(key);
                if (procedure == null) {
                    continue;
                }

                String columnName = columnsRS.getString("COLUMN_NAME");

                // skip ResultSet columns, as they are not described in Cayenne procedures yet...
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
                        continue;
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
                procedure.addCallParameter(column);
            }
        } finally {
            columnsRS.close();
        }
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

    private Map<String, Procedure> loadProcedures(DbLoaderConfiguration config) throws SQLException {
        Map<String, Procedure> procedures = new HashMap<String, Procedure>();

        FiltersConfig filters = config.getFiltersConfig();
        for (CatalogFilter catalogFilter : filters.catalogs) {
            for (SchemaFilter schema : catalogFilter.schemas) {
                if (filters.proceduresFilter(catalogFilter.name, schema.name) == null) {
                    continue;
                }

                procedures.putAll(loadProcedures(filters, catalogFilter.name, schema.name));
            }
        }

        return procedures;
    }

    private Map<String, Procedure> loadProcedures(FiltersConfig filters, String catalog, String schema) throws SQLException {
        Map<String, Procedure> procedures = new HashMap<String, Procedure>();
        // get procedures
        ResultSet rs = getMetaData().getProcedures(catalog, schema, WILDCARD);
        try {
            while (rs.next()) {

                String name = rs.getString("PROCEDURE_NAME");
                Procedure procedure = new Procedure(name);
                procedure.setCatalog(rs.getString("PROCEDURE_CAT"));
                procedure.setSchema(rs.getString("PROCEDURE_SCHEM"));

                if (filters.proceduresFilter(procedure.getCatalog(), procedure.getSchema()).isInclude(name)) {
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
        } finally {
            rs.close();
        }
        return procedures;
    }

    /**
     * Sets new naming strategy for reverse engineering
     * 
     * @since 3.0
     */
    public void setNameGenerator(ObjectNameGenerator strategy) {
        if (strategy == null) {
            throw new NullPointerException("Null strategy not allowed");
        }

        this.nameGenerator = strategy;
    }

    /**
     * @return naming strategy for reverse engineering
     * @since 3.0
     */
    public ObjectNameGenerator getNameGenerator() {
        return nameGenerator;
    }
}
