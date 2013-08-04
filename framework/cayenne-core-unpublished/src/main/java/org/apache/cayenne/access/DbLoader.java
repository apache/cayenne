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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneException;
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
import org.apache.cayenne.map.naming.BasicNamingStrategy;
import org.apache.cayenne.map.naming.ExportedKey;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.util.EntityMergeSupport;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class that does reverse engineering of the database. It can create
 * DataMaps using database meta data obtained via JDBC driver.
 */
public class DbLoader {

    private static final Log logger = LogFactory.getLog(DbLoader.class);

    // TODO: remove this hardcoded stuff once delegate starts to support
    // procedure
    // loading...
    private static final Collection<String> EXCLUDED_PROCEDURES = Arrays.asList("auto_pk_for_table",
            "auto_pk_for_table;1" /*
                                   * the last name is some Mac OS X Sybase
                                   * artifact
                                   */
    );

    public static final String WILDCARD = "%";

    /** List of db entities to process. */
    private List<DbEntity> dbEntityList = new ArrayList<DbEntity>();

    /**
     * CAY-479 - need to track which entities are skipped in the loader so that
     * relationships to non-skipped entities can be loaded
     */
    private Set<DbEntity> skippedEntities = new HashSet<DbEntity>();

    /** Creates a unique name for loaded relationship on the given entity. */
    private static String uniqueRelName(Entity entity, String preferredName) {
        int currentSuffix = 1;
        String relName = preferredName;

        while (entity.getRelationship(relName) != null || entity.getAttribute(relName) != null) {
            relName = preferredName + currentSuffix;
            currentSuffix++;
        }
        return relName;
    }

    protected Connection connection;
    protected DbAdapter adapter;
    protected DatabaseMetaData metaData;
    protected DbLoaderDelegate delegate;
    protected String genericClassName;
    protected boolean creatingMeaningfulPK;

    /**
     * Strategy for choosing names for entities, attributes and relationships
     */
    protected NamingStrategy namingStrategy;

    /**
     * Creates new DbLoader.
     */
    public DbLoader(Connection connection, DbAdapter adapter, DbLoaderDelegate delegate) {
        this(connection, adapter, delegate, new BasicNamingStrategy());
    }

    /**
     * Creates new DbLoader with specified naming strategy.
     * 
     * @since 3.0
     */
    public DbLoader(Connection connection, DbAdapter adapter, DbLoaderDelegate delegate, NamingStrategy strategy) {
        this.adapter = adapter;
        this.connection = connection;
        this.delegate = delegate;

        setNamingStrategy(strategy);
    }

    /**
     * Returns DatabaseMetaData object associated with this DbLoader.
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        if (null == metaData)
            metaData = connection.getMetaData();
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
     * Returns a name of a generic class that should be used for all
     * ObjEntities. The most common generic class is
     * {@link org.apache.cayenne.CayenneDataObject}. If generic class name is
     * null (which is the default), DbLoader will assign each entity a unique
     * class name derived from the table name.
     * 
     * @since 1.2
     */
    public String getGenericClassName() {
        return genericClassName;
    }

    /**
     * Sets a name of a generic class that should be used for all ObjEntities.
     * The most common generic class is
     * {@link org.apache.cayenne.CayenneDataObject}. If generic class name is
     * set to null (which is the default), DbLoader will assign each entity a
     * unique class name derived from the table name.
     * 
     * @since 1.2
     */
    public void setGenericClassName(String genericClassName) {
        this.genericClassName = genericClassName;
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
     * A method that return true if the given table name should be included. The
     * default implementation include all tables.
     */
    public boolean includeTableName(String tableName) {
        return true;
    }

    /**
     * Retrieves catalogs for the database associated with this DbLoader.
     * 
     * @return List with the catalog names, empty Array if none found.
     */
    public List<String> getCatalogs() throws SQLException {
        List<String> catalogs = new ArrayList<String>();
        ResultSet rs = getMetaData().getCatalogs();

        try {
            while (rs.next()) {
                String catalog_name = rs.getString(1);
                catalogs.add(catalog_name);
            }
        } finally {
            rs.close();
        }
        return catalogs;
    }

    /**
     * Retrieves the schemas for the database.
     * 
     * @return List with the schema names, empty Array if none found.
     */
    public List<String> getSchemas() throws SQLException {
        List<String> schemas = new ArrayList<String>();
        ResultSet rs = getMetaData().getSchemas();

        try {
            while (rs.next()) {
                String schema_name = rs.getString(1);
                schemas.add(schema_name);
            }
        } finally {
            rs.close();
        }
        return schemas;
    }

    /**
     * Returns all the table types for the given database. Types may be such as
     * "TABLE", "VIEW", "SYSTEM TABLE", etc.
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
     * @param catalogPattern
     *            The name of the catalog, may be null.
     * @param schemaPattern
     *            The pattern for schema name, use "%" for wildcard.
     * @param tableNamePattern
     *            The pattern for table names, % for wildcard, if null or ""
     *            defaults to "%".
     * @param types
     *            The types of table names to retrieve, null returns all types.
     * @return List of TableInfo objects, empty array if nothing found.
     */
    public List<DbEntity> getTables(String catalogPattern, String schemaPattern, String tableNamePattern, String[] types)
            throws SQLException {

        List<DbEntity> tables = new ArrayList<DbEntity>();

        if (logger.isDebugEnabled()) {
            logger.debug("Read tables: catalog=" + catalogPattern + ", schema=" + schemaPattern + ", tableNames="
                    + tableNamePattern);

            if (types != null && types.length > 0) {
                for (String type : types) {
                    logger.debug("Read tables: table type=" + type);
                }
            }
        }

        ResultSet rs = getMetaData().getTables(catalogPattern, schemaPattern, tableNamePattern, types);

        try {
            while (rs.next()) {
                String catalog = rs.getString("TABLE_CAT");
                String schema = rs.getString("TABLE_SCHEM");
                String name = rs.getString("TABLE_NAME");

                // Oracle 9i and newer has a nifty recycle bin feature... but we
                // don't
                // want dropped tables to be included here; in fact they may
                // even result
                // in errors on reverse engineering as their names have special
                // chars like
                // "/", etc. So skip them all together

                // TODO: Andrus, 10/29/2005 - this type of filtering should be
                // delegated
                // to adapter
                if (name == null || name.startsWith("BIN$")) {
                    continue;
                }

                if (!includeTableName(name)) {
                    continue;
                }

                DbEntity table = new DetectedDbEntity(name);
                table.setCatalog(catalog);
                table.setSchema(schema);
                tables.add(table);
            }
        } finally {
            rs.close();
        }
        return tables;
    }

    /**
     * Loads dbEntities for the specified tables.
     * 
     * @param map
     *            DataMap to be populated with DbEntities.
     * @param tables
     *            The list of org.apache.cayenne.ashwood.dbutil.Table objects
     *            for which DbEntities must be created.
     * @return false if loading must be immediately aborted.
     */
    public boolean loadDbEntities(DataMap map, List<? extends DbEntity> tables) throws SQLException {
        this.dbEntityList = new ArrayList<DbEntity>();
        for (DbEntity dbEntity : tables) {
            // Check if there already is a DbEntity under such name
            // if so, consult the delegate what to do
            DbEntity oldEnt = map.getDbEntity(dbEntity.getName());
            if (oldEnt != null) {
                if (delegate == null) {
                    // no delegate, don't know what to do, cancel import
                    break;
                }

                try {
                    if (delegate.overwriteDbEntity(oldEnt)) {
                        logger.debug("Overwrite: " + oldEnt.getName());
                        map.removeDbEntity(oldEnt.getName(), true);
                        delegate.dbEntityRemoved(oldEnt);
                    } else {
                        logger.debug("Keep old: " + oldEnt.getName());

                        // cay-479 - need to track entities that were not loaded
                        // for
                        // relationships exported to entities that were
                        skippedEntities.add(oldEnt);
                        continue;
                    }
                } catch (CayenneException ex) {
                    logger.debug("Load canceled.");

                    // cancel immediately
                    return false;
                }
            }

            // Create DbAttributes from column information --
            ResultSet rs = getMetaData().getColumns(dbEntity.getCatalog(), dbEntity.getSchema(), dbEntity.getName(),
                    "%");

            try {
                while (rs.next()) {
                    // for a reason not quiet apparent to me, Oracle sometimes
                    // returns duplicate record sets for the same table, messing
                    // up table
                    // names. E.g. for the system table "WK$_ATTR_MAPPING"
                    // columns are
                    // returned twice - as "WK$_ATTR_MAPPING" and
                    // "WK$$_ATTR_MAPPING"...
                    // Go figure

                    String tableName = rs.getString("TABLE_NAME");
                    if (!dbEntity.getName().equals(tableName)) {
                        logger.info("Incorrectly returned columns for '" + tableName + ", skipping.");
                        continue;
                    }

                    // gets attribute's (column's) information
                    String columnName = rs.getString("COLUMN_NAME");

                    boolean allowNulls = rs.getBoolean("NULLABLE");
                    int columnType = rs.getInt("DATA_TYPE");
                    int columnSize = rs.getInt("COLUMN_SIZE");
                    String typeName = rs.getString("TYPE_NAME");

                    // ignore precision of non-decimal columns
                    int decimalDigits = -1;
                    if (TypesMapping.isDecimal(columnType)) {
                        decimalDigits = rs.getInt("DECIMAL_DIGITS");
                        if (rs.wasNull()) {
                            decimalDigits = -1;
                        }
                    }

                    // create attribute delegating this task to adapter
                    DbAttribute attr = adapter.buildAttribute(columnName, typeName, columnType, columnSize,
                            decimalDigits, allowNulls);

                    if (adapter.supportsGeneratedKeys()) {

                        // TODO: this actually throws on some drivers... need to
                        // ensure that 'supportsGeneratedKeys' check is enough
                        // to prevent an exception here.
                        String autoIncrement = rs.getString("IS_AUTOINCREMENT");
                        if ("YES".equals(autoIncrement)) {
                            attr.setGenerated(true);
                        }
                    }

                    attr.setEntity(dbEntity);
                    dbEntity.addAttribute(attr);
                }
            } finally {
                rs.close();
            }

            map.addDbEntity(dbEntity);

            // notify delegate
            if (delegate != null) {
                delegate.dbEntityAdded(dbEntity);
            }

            // delegate might have thrown this entity out... so check if it is
            // still
            // around before continuing processing
            if (map.getDbEntity(dbEntity.getName()) == dbEntity) {
                this.dbEntityList.add(dbEntity);
            }
        }

        // get primary keys for each table and store it in dbEntity
        for (final DbEntity dbEntity : map.getDbEntities()) {
            if (tables.contains(dbEntity)) {
                String tableName = dbEntity.getName();
                ResultSet rs = metaData.getPrimaryKeys(dbEntity.getCatalog(), dbEntity.getSchema(), tableName);
                try {
                    while (rs.next()) {
                        String columnName = rs.getString(4);
                        DbAttribute attribute = dbEntity.getAttribute(columnName);

                        if (attribute != null) {
                            attribute.setPrimaryKey(true);
                        } else {
                            // why an attribute might be null is not quiet clear
                            // but there is a bug report 731406 indicating that
                            // it is
                            // possible
                            // so just print the warning, and ignore
                            logger.warn("Can't locate attribute for primary key: " + columnName);
                        }

                        String pkName = rs.getString(6);
                        if ((pkName != null) && (dbEntity instanceof DetectedDbEntity)) {
                            ((DetectedDbEntity) dbEntity).setPrimaryKeyName(pkName);
                        }
                    }
                } finally {
                    rs.close();
                }
            }
        }

        // cay-479 - iterate skipped DbEntities to populate exported keys
        for (final DbEntity skippedEntity : skippedEntities) {
            loadDbRelationships(skippedEntity, map);
        }

        return true;

    }

    /**
     * Creates an ObjEntity for each DbEntity in the map. ObjEntities are
     * created empty without
     */
    public void loadObjEntities(DataMap map) {

        Iterator<DbEntity> dbEntities = dbEntityList.iterator();
        if (!dbEntities.hasNext()) {
            return;
        }

        List<ObjEntity> loadedEntities = new ArrayList<ObjEntity>(dbEntityList.size());

        String packageName = map.getDefaultPackage();
        if (Util.isEmptyString(packageName)) {
            packageName = "";
        } else if (!packageName.endsWith(".")) {
            packageName = packageName + ".";
        }

        // load empty ObjEntities for all the tables
        while (dbEntities.hasNext()) {
            DbEntity dbEntity = dbEntities.next();

            // check if there are existing entities
            Collection<ObjEntity> existing = map.getMappedEntities(dbEntity);
            if (existing.size() > 0) {
                loadedEntities.addAll(existing);
                continue;
            }

            String objEntityName = namingStrategy.createObjEntityName(dbEntity);
            // this loop will terminate even if no valid name is found
            // to prevent loader from looping forever (though such case is very
            // unlikely)
            String baseName = objEntityName;
            for (int i = 1; i < 1000 && map.getObjEntity(objEntityName) != null; i++) {
                objEntityName = baseName + i;
            }

            ObjEntity objEntity = new ObjEntity(objEntityName);
            objEntity.setDbEntity(dbEntity);

            objEntity.setClassName(getGenericClassName() != null ? getGenericClassName() : packageName
                    + objEntity.getName());
            map.addObjEntity(objEntity);
            loadedEntities.add(objEntity);
        }

        // update ObjEntity attributes and relationships
        EntityMergeSupport objEntityMerger = createEntityMerger(map);
        objEntityMerger.synchronizeWithDbEntities(loadedEntities);
    }

    /**
     * @since 3.2
     */
    protected EntityMergeSupport createEntityMerger(DataMap map) {
        return new EntityMergeSupport(map, namingStrategy, !creatingMeaningfulPK);
    }

    /** Loads database relationships into a DataMap. */
    public void loadDbRelationships(DataMap map) throws SQLException {
        for (final DbEntity pkEntity : dbEntityList) {
            loadDbRelationships(pkEntity, map);
        }
    }

    private void loadDbRelationships(DbEntity pkEntity, DataMap map) throws SQLException {
        DatabaseMetaData md = getMetaData();
        String pkEntName = pkEntity.getName();

        // Get all the foreign keys referencing this table
        ResultSet rs = null;

        try {
            rs = md.getExportedKeys(pkEntity.getCatalog(), pkEntity.getSchema(), pkEntity.getName());
        } catch (SQLException cay182Ex) {
            // Sybase-specific - the line above blows on VIEWS, see CAY-182.
            logger.info("Error getting relationships for '" + pkEntName + "', ignoring.");
            return;
        }

        try {
            if (!rs.next())
                return;

            // these will be initailzed every time a new target entity
            // is found in the result set (which should be ordered by table name
            // among
            // other things)
            DbRelationship forwardRelationship = null;
            DbRelationshipDetected reverseRelationship = null;
            DbEntity fkEntity = null;
            ExportedKey key = null;

            do {
                // extract data from resultset
                key = ExportedKey.extractData(rs);

                short keySeq = rs.getShort("KEY_SEQ");
                if (keySeq == 1) {

                    if (forwardRelationship != null) {
                        postprocessMasterDbRelationship(forwardRelationship, key);
                        forwardRelationship = null;
                    }

                    // start new entity
                    String fkEntityName = key.getFKTableName();
                    String fkName = key.getFKName();

                    if (!includeTableName(fkEntityName)) {
                        continue;
                    }

                    fkEntity = map.getDbEntity(fkEntityName);

                    if (fkEntity == null) {
                        logger.info("FK warning: no entity found for name '" + fkEntityName + "'");
                    } else if (skippedEntities.contains(pkEntity) && skippedEntities.contains(fkEntity)) {
                        // cay-479 - don't load relationships between two
                        // skipped entities.
                        continue;
                    } else {
                        // init relationship
                        String forwardPreferredName = namingStrategy.createDbRelationshipName(key, true);
                        forwardRelationship = new DbRelationship(uniqueRelName(pkEntity, forwardPreferredName));

                        forwardRelationship.setSourceEntity(pkEntity);
                        forwardRelationship.setTargetEntity(fkEntity);
                        pkEntity.addRelationship(forwardRelationship);

                        String reversePreferredName = namingStrategy.createDbRelationshipName(key, false);
                        reverseRelationship = new DbRelationshipDetected(uniqueRelName(fkEntity, reversePreferredName));
                        reverseRelationship.setFkName(fkName);
                        reverseRelationship.setToMany(false);
                        reverseRelationship.setSourceEntity(fkEntity);
                        reverseRelationship.setTargetEntity(pkEntity);
                        fkEntity.addRelationship(reverseRelationship);
                    }
                }

                if (fkEntity != null) {
                    // Create and append joins
                    String pkName = key.getPKColumnName();
                    String fkName = key.getFKColumnName();

                    // skip invalid joins...
                    DbAttribute pkAtt = pkEntity.getAttribute(pkName);
                    if (pkAtt == null) {
                        logger.info("no attribute for declared primary key: " + pkName);
                        continue;
                    }

                    DbAttribute fkAtt = fkEntity.getAttribute(fkName);
                    if (fkAtt == null) {
                        logger.info("no attribute for declared foreign key: " + fkName);
                        continue;
                    }

                    if (forwardRelationship != null) {
                        forwardRelationship.addJoin(new DbJoin(forwardRelationship, pkName, fkName));
                    }
                    if (reverseRelationship != null) {
                        reverseRelationship.addJoin(new DbJoin(reverseRelationship, fkName, pkName));
                    }

                }
            } while (rs.next());

            if (forwardRelationship != null) {
                postprocessMasterDbRelationship(forwardRelationship, key);
                forwardRelationship = null;
            }

        } finally {
            rs.close();
        }
    }

    /**
     * Detects correct relationship multiplicity and "to dep pk" flag. Only
     * called on relationships from PK to FK, not the reverse ones.
     */
    protected void postprocessMasterDbRelationship(DbRelationship relationship, ExportedKey key) {
        boolean toPK = true;
        List<DbJoin> joins = relationship.getJoins();

        for (final DbJoin join : joins) {
            if (!join.getTarget().isPrimaryKey()) {
                toPK = false;
                break;
            }

        }

        boolean toDependentPK = false;
        boolean toMany = true;

        if (toPK) {
            toDependentPK = true;
            if (((DbEntity) relationship.getTargetEntity()).getPrimaryKeys().size() == joins.size()) {
                toMany = false;
            }
        }

        // if this is really to-one we need to rename the relationship
        if (!toMany) {
            Entity source = relationship.getSourceEntity();
            source.removeRelationship(relationship.getName());
            relationship.setName(DbLoader.uniqueRelName(source, namingStrategy.createDbRelationshipName(key, false)));
            source.addRelationship(relationship);
        }

        relationship.setToDependentPK(toDependentPK);
        relationship.setToMany(toMany);
    }

    /**
     * Flattens many-to-many relationships in the generated model.
     */
    private void flattenManyToManyRelationships(DataMap map) {
        List<ObjEntity> entitiesForDelete = new ArrayList<ObjEntity>();

        for (ObjEntity curEntity : map.getObjEntities()) {
            ManyToManyCandidateEntity entity = new ManyToManyCandidateEntity(curEntity);

            if (entity.isRepresentManyToManyTable()) {
                entity.optimizeRelationships();
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
     * @since 3.2
     */
    public String[] getDefaultTableTypes() {
        String viewType = adapter.tableTypeForView();
        String tableType = adapter.tableTypeForTable();

        // use types that are not null
        List<String> list = new ArrayList<String>(2);
        if (viewType != null) {
            list.add(viewType);
        }
        if (tableType != null) {
            list.add(tableType);
        }

        String[] types = new String[list.size()];
        list.toArray(types);
        return types;
    }

    /**
     * Performs database reverse engineering and generates DataMap that contains
     * default mapping of the tables and views. By default will include regular
     * tables and views.
     * 
     * @since 1.0.7
     * @deprecated since 3.2 use
     *             {@link #load(DataMap, String, String, String, String...)}
     *             method that supports catalogs.
     */
    public DataMap loadDataMapFromDB(String schemaPattern, String tablePattern, DataMap dataMap) throws SQLException {

        String[] types = getDefaultTableTypes();
        if (types.length == 0) {
            throw new SQLException("No supported table types found.");
        }

        load(dataMap, null, schemaPattern, tablePattern, types);
        return dataMap;
    }

    /**
     * Performs database reverse engineering and generates DataMap object that
     * contains default mapping of the tables and views. Allows to limit types
     * of tables to read.
     * 
     * @deprecated since 3.2 use
     *             {@link #load(DataMap, String, String, String, String...)}
     *             method that supports catalogs.
     */
    public DataMap loadDataMapFromDB(String schemaPattern, String tablePattern, String[] tableTypes, DataMap dataMap)
            throws SQLException {
        clearDataMap(dataMap);

        load(dataMap, null, schemaPattern, tablePattern, tableTypes);
        return dataMap;
    }

    private void clearDataMap(DataMap dataMap) {
        dataMap.clearDbEntities();
        dataMap.clearEmbeddables();
        dataMap.clearObjEntities();
        dataMap.clearProcedures();
        dataMap.clearQueries();
        dataMap.clearResultSets();
    }

    /**
     * Performs database reverse engineering to match the specified catalog,
     * schema, table name and table type patterns and fills the specified
     * DataMap object with DB and object mapping info.
     * 
     * @since 3.2
     */
    public void load(DataMap dataMap, String catalogPattern, String schemaPattern, String tablePattern,
            String... tableTypes) throws SQLException {

        if (tablePattern == null) {
            tablePattern = WILDCARD;
        }

        List<DbEntity> tables = getTables(catalogPattern, schemaPattern, tablePattern, tableTypes);

        if (loadDbEntities(dataMap, tables)) {
            loadDbRelationships(dataMap);

            loadObjEntities(dataMap);
            flattenManyToManyRelationships(dataMap);
            fireObjEntitiesAddedEvents(dataMap);
        }
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
     * @deprecated since 3.2 use
     *             {@link #loadProcedures(DataMap, String, String, String)} that
     *             supports "catalog" pattern.
     */
    public void loadProceduresFromDB(String schemaPattern, String namePattern, DataMap dataMap) throws SQLException {
        loadProcedures(dataMap, null, schemaPattern, namePattern);
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
     * @since 3.2
     */
    public void loadProcedures(DataMap dataMap, String catalogPattern, String schemaPattern, String namePattern)
            throws SQLException {

        Map<String, Procedure> procedures = null;

        // get procedures
        ResultSet rs = getMetaData().getProcedures(catalogPattern, schemaPattern, namePattern);
        try {
            while (rs.next()) {
                String name = rs.getString("PROCEDURE_NAME");

                // TODO: this will be moved to Delegate...
                if (EXCLUDED_PROCEDURES.contains(name)) {
                    logger.info("skipping Cayenne PK procedure: " + name);
                    continue;
                }

                String catalog = rs.getString("PROCEDURE_CAT");
                String schema = rs.getString("PROCEDURE_SCHEM");

                short type = rs.getShort("PROCEDURE_TYPE");

                Procedure procedure = new Procedure(name);
                procedure.setCatalog(catalog);
                procedure.setSchema(schema);

                switch (type) {
                case DatabaseMetaData.procedureNoResult:
                case DatabaseMetaData.procedureResultUnknown:
                    procedure.setReturningValue(false);
                    break;
                case DatabaseMetaData.procedureReturnsResult:
                    procedure.setReturningValue(true);
                    break;
                }

                if (procedures == null) {
                    procedures = new HashMap<String, Procedure>();
                }

                procedures.put(procedure.getFullyQualifiedName(), procedure);
            }
        } finally {
            rs.close();
        }

        // if nothing found, return
        if (procedures == null) {
            return;
        }

        // get columns
        ResultSet columnsRS = getMetaData().getProcedureColumns(null, schemaPattern, namePattern, null);
        try {
            while (columnsRS.next()) {

                String schema = columnsRS.getString("PROCEDURE_SCHEM");
                String name = columnsRS.getString("PROCEDURE_NAME");

                // TODO: this will be moved to Delegate...
                if (EXCLUDED_PROCEDURES.contains(name)) {
                    continue;
                }

                String columnName = columnsRS.getString("COLUMN_NAME");
                short type = columnsRS.getShort("COLUMN_TYPE");

                String key = (schema != null) ? schema + '.' + name : name;

                // skip ResultSet columns, as they are not described in Cayenne
                // procedures
                // yet...
                if (type == DatabaseMetaData.procedureColumnResult) {
                    logger.debug("skipping ResultSet column: " + key + "." + columnName);
                }

                Procedure procedure = procedures.get(key);

                if (procedure == null) {
                    logger.info("invalid procedure column, no procedure found: " + key + "." + columnName);
                    continue;
                }

                ProcedureParameter column = new ProcedureParameter(columnName);

                if (columnName == null) {
                    if (type == DatabaseMetaData.procedureColumnReturn) {
                        logger.debug("null column name, assuming result column: " + key);
                        column.setName("_return_value");
                    } else {
                        logger.info("invalid null column name, skipping column : " + key);
                        continue;
                    }
                }

                int columnType = columnsRS.getInt("DATA_TYPE");
                int columnSize = columnsRS.getInt("LENGTH");

                // ignore precision of non-decimal columns
                int decimalDigits = -1;
                if (TypesMapping.isDecimal(columnType)) {
                    decimalDigits = columnsRS.getShort("SCALE");
                    if (columnsRS.wasNull()) {
                        decimalDigits = -1;
                    }
                }

                switch (type) {
                case DatabaseMetaData.procedureColumnIn:
                    column.setDirection(ProcedureParameter.IN_PARAMETER);
                    break;
                case DatabaseMetaData.procedureColumnInOut:
                    column.setDirection(ProcedureParameter.IN_OUT_PARAMETER);
                    break;
                case DatabaseMetaData.procedureColumnOut:
                    column.setDirection(ProcedureParameter.OUT_PARAMETER);
                    break;
                case DatabaseMetaData.procedureColumnReturn:
                    procedure.setReturningValue(true);
                    break;
                }

                column.setMaxLength(columnSize);
                column.setPrecision(decimalDigits);
                column.setProcedure(procedure);
                column.setType(columnType);
                procedure.addCallParameter(column);
            }
        } finally {
            columnsRS.close();
        }

        for (final Procedure procedure : procedures.values()) {
            // overwrite existing procedures...
            dataMap.addProcedure(procedure);
        }
    }

    /**
     * Sets new naming strategy for reverse engineering
     * 
     * @since 3.0
     */
    public void setNamingStrategy(NamingStrategy strategy) {
        // null values are not allowed
        if (strategy == null) {
            throw new NullPointerException("Null strategy not allowed");
        }

        this.namingStrategy = strategy;
    }

    /**
     * @return naming strategy for reverse engineering
     * @since 3.0
     */
    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }
}
