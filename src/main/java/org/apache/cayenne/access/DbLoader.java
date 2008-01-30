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
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.util.EntityMergeSupport;
import org.apache.cayenne.util.NameConverter;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.objectstyle.ashwood.dbutil.Table;

/**
 * Utility class that does reverse engineering of the database. It can create DataMaps
 * using database meta data obtained via JDBC driver.
 * 
 * @author Michael Shengaout
 * @author Andrus Adamchik
 */
public class DbLoader {

    private static Log logObj = LogFactory.getLog(DbLoader.class);

    // TODO: remove this hardcoded stuff once delegate starts to support procedure
    // loading...
    private static final Collection<String> EXCLUDED_PROCEDURES = Arrays.asList(
            "auto_pk_for_table", "auto_pk_for_table;1" /*
                                                         * the last name is some Mac OS X
                                                         * Sybase artifact
                                                         */
    );

    public static final String WILDCARD = "%";

    /** List of db entities to process. */
    private List<DbEntity> dbEntityList = new ArrayList<DbEntity>();
    
    /**
     * CAY-479 - need to track which entities are skipped in 
     * the loader so that relationships to non-skipped entities can be loaded 
     */
    private Set<DbEntity> skippedEntities = new HashSet<DbEntity>();

    /** Creates default name for loaded relationship */
    private static String defaultRelName(String dstName, boolean toMany) {
        String uglyName = (toMany) ? dstName + "_ARRAY" : "to_" + dstName;
        return NameConverter.underscoredToJava(uglyName, false);
    }

    /** Creates a unique name for loaded relationship on the given entity. */
    private static String uniqueRelName(Entity entity, String dstName, boolean toMany) {
        int currentSuffix = 1;
        String baseRelName = defaultRelName(dstName, toMany);
        String relName = baseRelName;

        while (entity.getRelationship(relName) != null) {
            relName = baseRelName + currentSuffix;
            currentSuffix++;
        }
        return relName;
    }

    protected Connection con;
    protected DbAdapter adapter;
    protected DatabaseMetaData metaData;
    protected DbLoaderDelegate delegate;
    protected String genericClassName;

    /** Creates new DbLoader. */
    public DbLoader(Connection con, DbAdapter adapter, DbLoaderDelegate delegate) {
        this.adapter = adapter;
        this.con = con;
        this.delegate = delegate;
    }

    /**
     * Returns DatabaseMetaData object associated with this DbLoader.
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        if (null == metaData)
            metaData = con.getMetaData();
        return metaData;
    }

    /**
     * Returns database connection used by this DbLoader.
     */
    public Connection getCon() {
        return con;
    }

    /**
     * Returns a name of a generic class that should be used for all ObjEntities. The most
     * common generic class is {@link org.apache.cayenne.CayenneDataObject}. If
     * generic class name is null (which is the default), DbLoader will assign each entity
     * a unique class name derived from the table name.
     * 
     * @since 1.2
     */
    public String getGenericClassName() {
        return genericClassName;
    }

    /**
     * Sets a name of a generic class that should be used for all ObjEntities. The most
     * common generic class is {@link org.apache.cayenne.CayenneDataObject}. If
     * generic class name is set to null (which is the default), DbLoader will assign each
     * entity a unique class name derived from the table name.
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
     * Retrieves catalogues for the database associated with this DbLoader.
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
        }
        finally {
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
        }
        finally {
            rs.close();
        }
        return schemas;
    }

    /**
     * Returns all the table types for the given database. Types may be such as "TABLE",
     * "VIEW", "SYSTEM TABLE", etc.
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
        }
        finally {
            rs.close();
        }
        return types;
    }

    /**
     * Returns all table names for given combination of the criteria.
     * 
     * @param catalog The name of the catalog, may be null.
     * @param schemaPattern The pattern for schema name, use "%" for wildcard.
     * @param tableNamePattern The pattern for table names, % for wildcard, if null or ""
     *            defaults to "%".
     * @param types The types of table names to retrieve, null returns all types.
     * @return List of TableInfo objects, empty array if nothing found.
     */
    public List<Table> getTables(
            String catalog,
            String schemaPattern,
            String tableNamePattern,
            String[] types) throws SQLException {

        List<Table> tables = new ArrayList<Table>();

        if (logObj.isDebugEnabled()) {
            logObj.debug("Read tables: catalog="
                    + catalog
                    + ", schema="
                    + schemaPattern
                    + ", tableNames="
                    + tableNamePattern);

            if (types != null && types.length > 0) {
                for (String type : types) {
                    logObj.debug("Read tables: table type=" + type);
                }
            }
        }

        ResultSet rs = getMetaData().getTables(
                catalog,
                schemaPattern,
                tableNamePattern,
                types);

        try {
            while (rs.next()) {
                String cat = rs.getString("TABLE_CAT");
                String schema = rs.getString("TABLE_SCHEM");
                String name = rs.getString("TABLE_NAME");

                // Oracle 9i and newer has a nifty recycle bin feature... but we don't
                // want dropped tables to be included here; in fact they may even result
                // in errors on reverse engineering as their names have special chars like
                // "/", etc. So skip them all together

                // TODO: Andrus, 10/29/2005 - this type of filtering should be delegated
                // to adapter
                if (name == null || name.startsWith("BIN$")) {
                    continue;
                }

                Table info = new Table(cat, schema, name);
                tables.add(info);
            }
        }
        finally {
            rs.close();
        }
        return tables;
    }

    /**
     * Loads dbEntities for the specified tables.
     * 
     * @param map DataMap to be populated with DbEntities.
     * @param tables The list of org.objectstyle.ashwood.dbutil.Table objects for which
     *            DbEntities must be created.
     * @return false if loading must be immediately aborted.
     */
    public boolean loadDbEntities(DataMap map, List<? extends Table> tables) throws SQLException {
        this.dbEntityList = new ArrayList<DbEntity>();

        Iterator<? extends Table> iter = tables.iterator();
        while (iter.hasNext()) {
            Table table = iter.next();

            // Check if there already is a DbEntity under such name
            // if so, consult the delegate what to do
            DbEntity oldEnt = map.getDbEntity(table.getName());
            if (oldEnt != null) {
                if (delegate == null) {
                    // no delegate, don't know what to do, cancel import
                    break;
                }

                try {
                    if (delegate.overwriteDbEntity(oldEnt)) {
                        logObj.debug("Overwrite: " + oldEnt.getName());
                        map.removeDbEntity(oldEnt.getName(), true);
                        delegate.dbEntityRemoved(oldEnt);
                    }
                    else {
                        logObj.debug("Keep old: " + oldEnt.getName());
                        
                        // cay-479 - need to track entities that were not loaded for 
                        // relationships exported to entities that were
                        skippedEntities.add(oldEnt);
                        continue;
                    }
                }
                catch (CayenneException ex) {
                    logObj.debug("Load canceled.");

                    // cancel immediately
                    return false;
                }
            }

            DbEntity dbEntity = new DbEntity();
            dbEntity.setName(table.getName());
            dbEntity.setSchema(table.getSchema());
            dbEntity.setCatalog(table.getCatalog());

            // Create DbAttributes from column information --
            ResultSet rs = getMetaData().getColumns(
                    table.getCatalog(),
                    table.getSchema(),
                    table.getName(),
                    "%");

            try {
                while (rs.next()) {
                    // for a reason not quiet apparent to me, Oracle sometimes
                    // returns duplicate record sets for the same table, messing up table
                    // names. E.g. for the system table "WK$_ATTR_MAPPING" columns are
                    // returned twice - as "WK$_ATTR_MAPPING" and "WK$$_ATTR_MAPPING"...
                    // Go figure

                    String tableName = rs.getString("TABLE_NAME");
                    if (!dbEntity.getName().equals(tableName)) {
                        logObj.info("Incorrectly returned columns for '"
                                + tableName
                                + ", skipping.");
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
                    DbAttribute attr = adapter.buildAttribute(
                            columnName,
                            typeName,
                            columnType,
                            columnSize,
                            decimalDigits,
                            allowNulls);
                    attr.setEntity(dbEntity);
                    dbEntity.addAttribute(attr);
                }
            }
            finally {
                rs.close();
            }

            map.addDbEntity(dbEntity);

            // notify delegate
            if (delegate != null) {
                delegate.dbEntityAdded(dbEntity);
            }

            // delegate might have thrown this entity out... so check if it is still
            // around
            // before continuing processing
            if (map.getDbEntity(table.getName()) == dbEntity) {
                this.dbEntityList.add(dbEntity);
            }
        }
        
        // get primary keys for each table and store it in dbEntity
        Iterator<DbEntity> i = map.getDbEntities().iterator();
        while (i.hasNext()) {
            DbEntity dbEntity = i.next();
            String tableName = dbEntity.getName();
            ResultSet rs = metaData.getPrimaryKeys(null, dbEntity.getSchema(), tableName);

            try {
                while (rs.next()) {
                    String keyName = rs.getString(4);
                    DbAttribute attribute = (DbAttribute) dbEntity.getAttribute(keyName);

                    if (attribute != null) {
                        attribute.setPrimaryKey(true);
                    }
                    else {
                        // why an attribute might be null is not quiet clear
                        // but there is a bug report 731406 indicating that it is possible
                        // so just print the warning, and ignore
                        logObj.warn("Can't locate attribute for primary key: " + keyName);
                    }
                }
            }
            finally {
                rs.close();
            }
        }
        
        // cay-479 - iterate skipped DbEntities to populate exported keys
        Iterator<DbEntity> skippedEntityIter = skippedEntities.iterator();
        while (skippedEntityIter.hasNext()) {

            DbEntity skippedEntity = skippedEntityIter.next();
            loadDbRelationships(skippedEntity, map);
        }
            
        return true;
        
    }

    /**
     * Creates an ObjEntity for each DbEntity in the map. ObjEntities are created empty
     * without
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
        }
        else if (!packageName.endsWith(".")) {
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

            String objEntityName = NameConverter.underscoredToJava(
                    dbEntity.getName(),
                    true);
            // this loop will terminate even if no valid name is found
            // to prevent loader from looping forever (though such case is very unlikely)
            String baseName = objEntityName;
            for (int i = 1; i < 1000 && map.getObjEntity(objEntityName) != null; i++) {
                objEntityName = baseName + i;
            }

            ObjEntity objEntity = new ObjEntity(objEntityName);
            objEntity.setDbEntity(dbEntity);

            objEntity.setClassName(getGenericClassName() != null
                    ? getGenericClassName()
                    : packageName + objEntity.getName());
            map.addObjEntity(objEntity);
            loadedEntities.add(objEntity);

            // added entity without attributes or relationships...
            if (delegate != null) {
                delegate.objEntityAdded(objEntity);
            }
        }

        // update ObjEntity attributes and relationships
        new EntityMergeSupport(map).synchronizeWithDbEntities(loadedEntities);
    }

    /** Loads database relationships into a DataMap. */
    public void loadDbRelationships(DataMap map) throws SQLException {
        Iterator<DbEntity> it = dbEntityList.iterator();
        while (it.hasNext()) {
            DbEntity pkEntity = it.next();
            loadDbRelationships(pkEntity, map);
        }
    }

    private void loadDbRelationships(DbEntity pkEntity, DataMap map) throws SQLException {
        DatabaseMetaData md = getMetaData();
        String pkEntName = pkEntity.getName();

        // Get all the foreign keys referencing this table
        ResultSet rs = null;

        try {
            rs = md.getExportedKeys(
                    pkEntity.getCatalog(),
                    pkEntity.getSchema(),
                    pkEntity.getName());
        }
        catch (SQLException cay182Ex) {
            // Sybase-specific - the line above blows on VIEWS, see CAY-182.
            logObj.info("Error getting relationships for '"
                    + pkEntName
                    + "', ignoring.");
            return;
        }

        try {
            if (!rs.next())
                return;

            // these will be initailzed every time a new target entity
            // is found in the result set (which should be ordered by table name among
            // other things)
            DbRelationship forwardRelationship = null;
            DbRelationshipDetected reverseRelationship = null;
            DbEntity fkEntity = null;

            do {
                short keySeq = rs.getShort("KEY_SEQ");
                if (keySeq == 1) {

                    if (forwardRelationship != null) {
                        postprocessMasterDbRelationship(forwardRelationship);
                        forwardRelationship = null;
                    }

                    // start new entity
                    String fkEntityName = rs.getString("FKTABLE_NAME");
                    String fkName = rs.getString("FK_NAME");

                    fkEntity = map.getDbEntity(fkEntityName);

                    if (fkEntity == null) {
                        logObj.info("FK warning: no entity found for name '"
                                + fkEntityName
                                + "'");
                    } else if (skippedEntities.contains(pkEntity) && skippedEntities.contains(fkEntity)) {
                        // cay-479 - don't load relationships between two
                        // skipped entities.
                        continue;
                    }
                    else {

                        // init relationship
                        forwardRelationship = new DbRelationship(DbLoader
                                .uniqueRelName(pkEntity, fkEntityName, true));

                        forwardRelationship.setSourceEntity(pkEntity);
                        forwardRelationship.setTargetEntity(fkEntity);
                        pkEntity.addRelationship(forwardRelationship);

                        reverseRelationship = new DbRelationshipDetected(uniqueRelName(
                                fkEntity,
                                pkEntName,
                                false));
                        reverseRelationship.setFkName(fkName);
                        reverseRelationship.setToMany(false);
                        reverseRelationship.setSourceEntity(fkEntity);
                        reverseRelationship.setTargetEntity(pkEntity);
                        fkEntity.addRelationship(reverseRelationship);
                    }
                }

                if (fkEntity != null) {
                    // Create and append joins
                    String pkName = rs.getString("PKCOLUMN_NAME");
                    String fkName = rs.getString("FKCOLUMN_NAME");

                    // skip invalid joins...
                    DbAttribute pkAtt = (DbAttribute) pkEntity.getAttribute(pkName);
                    if (pkAtt == null) {
                        logObj.info("no attribute for declared primary key: "
                                + pkName);
                        continue;
                    }

                    DbAttribute fkAtt = (DbAttribute) fkEntity.getAttribute(fkName);
                    if (fkAtt == null) {
                        logObj.info("no attribute for declared foreign key: "
                                + fkName);
                        continue;
                    }

                    forwardRelationship.addJoin(new DbJoin(
                            forwardRelationship,
                            pkName,
                            fkName));
                    reverseRelationship.addJoin(new DbJoin(
                            reverseRelationship,
                            fkName,
                            pkName));
                }
            } while (rs.next());

            if (forwardRelationship != null) {
                postprocessMasterDbRelationship(forwardRelationship);
                forwardRelationship = null;
            }

        }
        finally {
            rs.close();
        }
    }
    /**
     * Detects correct relationship multiplicity and "to dep pk" flag. Only called on
     * relationships from PK to FK, not the reverse ones.
     */
    protected void postprocessMasterDbRelationship(DbRelationship relationship) {
        boolean toPK = true;
        List<DbJoin> joins = relationship.getJoins();

        Iterator<DbJoin> joinsIt = joins.iterator();
        while (joinsIt.hasNext()) {
            DbJoin join = joinsIt.next();
            if (!join.getTarget().isPrimaryKey()) {
                toPK = false;
                break;
            }

        }

        boolean toDependentPK = false;
        boolean toMany = true;

        if (toPK) {
            toDependentPK = true;
            if (((DbEntity) relationship.getTargetEntity()).getPrimaryKeys().size() == joins
                    .size()) {
                toMany = false;
            }
        }

        // if this is really to-one we need to rename the relationship
        if (!toMany) {
            Entity source = relationship.getSourceEntity();
            source.removeRelationship(relationship.getName());
            relationship.setName(DbLoader.uniqueRelName(source, relationship
                    .getTargetEntityName(), false));
            source.addRelationship(relationship);
        }

        relationship.setToDependentPK(toDependentPK);
        relationship.setToMany(toMany);
    }

    private String[] getDefaultTableTypes() {
        String viewType = adapter.tableTypeForView();
        String tableType = adapter.tableTypeForTable();

        // use types that are not null
        List<String> list = new ArrayList<String>();
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
     * Performs database reverse engineering and generates DataMap that contains default
     * mapping of the tables and views. By default will include regular tables and views.
     * 
     * @since 1.0.7
     */
    public DataMap loadDataMapFromDB(
            String schemaName,
            String tablePattern,
            DataMap dataMap) throws SQLException {

        String[] types = getDefaultTableTypes();
        if (types.length == 0) {
            throw new SQLException("No supported table types found.");
        }

        return loadDataMapFromDB(schemaName, tablePattern, types, dataMap);
    }

    /**
     * Performs database reverse engineering and generates DataMap object that contains
     * default mapping of the tables and views. Allows to limit types of tables to read.
     */
    public DataMap loadDataMapFromDB(
            String schemaName,
            String tablePattern,
            String[] tableTypes,
            DataMap dataMap) throws SQLException {

        if (tablePattern == null) {
            tablePattern = WILDCARD;
        }

        if (!loadDbEntities(
                dataMap,
                getTables(null, schemaName, tablePattern, tableTypes))) {
            return dataMap;
        }

        loadDbRelationships(dataMap);
        loadObjEntities(dataMap);
        return dataMap;
    }

    /**
     * Loads database stored procedures into the DataMap.
     * <p>
     * <i>As of 1.1 there is no boolean property or delegate method to make procedure
     * loading optional or to implement custom merging logic, so currently this method is
     * NOT CALLED from "loadDataMapFromDB" and should be invoked explicitly by the user.
     * </i>
     * </p>
     * 
     * @since 1.1
     */
    public void loadProceduresFromDB(
            String schemaPattern,
            String namePattern,
            DataMap dataMap) throws SQLException {

        Map<String, Procedure> procedures = null;

        // get procedures
        ResultSet rs = getMetaData().getProcedures(null, schemaPattern, namePattern);
        try {
            while (rs.next()) {
                String name = rs.getString("PROCEDURE_NAME");

                // TODO: this will be moved to Delegate...
                if (EXCLUDED_PROCEDURES.contains(name)) {
                    logObj.info("skipping Cayenne PK procedure: " + name);
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
        }
        finally {
            rs.close();
        }

        // if nothing found, return
        if (procedures == null) {
            return;
        }

        // get columns
        ResultSet columnsRS = getMetaData().getProcedureColumns(
                null,
                schemaPattern,
                namePattern,
                null);
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

                // skip ResultSet columns, as they are not described in Cayenne procedures
                // yet...
                if (type == DatabaseMetaData.procedureColumnResult) {
                    logObj.debug("skipping ResultSet column: " + key + "." + columnName);
                }

                Procedure procedure = procedures.get(key);

                if (procedure == null) {
                    logObj.info("invalid procedure column, no procedure found: "
                            + key
                            + "."
                            + columnName);
                    continue;
                }

                ProcedureParameter column = new ProcedureParameter(columnName);

                if (columnName == null) {
                    if (type == DatabaseMetaData.procedureColumnReturn) {
                        logObj.debug("null column name, assuming result column: " + key);
                        column.setName("_return_value");
                    }
                    else {
                        logObj.info("invalid null column name, skipping column : " + key);
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
        }
        finally {
            columnsRS.close();
        }

        Iterator<Procedure> it = procedures.values().iterator();
        while (it.hasNext()) {
            // overwrite existing procedures...

            Procedure procedure = it.next();
            dataMap.addProcedure(procedure);
        }
    }
}
