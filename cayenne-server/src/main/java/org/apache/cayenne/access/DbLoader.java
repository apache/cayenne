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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.cayenne.access.loader.DbAttributesPerSchemaLoader;
import org.apache.cayenne.access.loader.DbLoaderConfiguration;
import org.apache.cayenne.access.loader.DbTableLoader;
import org.apache.cayenne.access.loader.DefaultDbLoaderDelegate;
import org.apache.cayenne.access.loader.ManyToManyCandidateEntity;
import org.apache.cayenne.access.loader.filters.CatalogFilter;
import org.apache.cayenne.access.loader.filters.FiltersConfig;
import org.apache.cayenne.access.loader.filters.PatternFilter;
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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.ExportedKey;
import org.apache.cayenne.map.naming.LegacyNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.map.naming.ObjectNameGenerator;
import org.apache.cayenne.util.EntityMergeSupport;
import org.apache.cayenne.util.EqualsBuilder;
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
		this.delegate = delegate == null ? new DefaultDbLoaderDelegate() : delegate;

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
		try (ResultSet rs = getMetaData().getCatalogs()) {
			return getStrings(rs);
		}
	}

	/**
	 * Retrieves the schemas for the database.
	 *
	 * @return List with the schema names, empty Array if none found.
	 */
	public List<String> getSchemas() throws SQLException {

		try (ResultSet rs = getMetaData().getSchemas()) {
			return getStrings(rs);
		}
	}

	private static List<String> getStrings(ResultSet rs) throws SQLException {
		List<String> strings = new ArrayList<String>();

		while (rs.next()) {
			strings.add(rs.getString(1));
		}

		return strings;
	}

	/**
	 * Returns all the table types for the given database. Types may be such as
	 * Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
	 * "LOCAL TEMPORARY", "ALIAS", "SYNONYM"., etc.
	 *
	 * @return List of Strings, empty array if nothing found.
	 */
	public List<String> getTableTypes() throws SQLException {
		List<String> types = new ArrayList<String>();

		try (ResultSet rs = getMetaData().getTableTypes();) {
			while (rs.next()) {
				types.add(rs.getString("TABLE_TYPE").trim());
			}
		}

		return types;
	}

	/**
	 * Creates an ObjEntity for each DbEntity in the map.
	 */
	public Collection<ObjEntity> loadObjEntities(DataMap map, DbLoaderConfiguration config,
			Collection<DbEntity> entities) {
		Collection<ObjEntity> loadedEntities = DbLoader.loadObjEntities(map, config, entities, nameGenerator);

		createEntityMerger(map).synchronizeWithDbEntities(loadedEntities);

		return loadedEntities;
	}

	public static Collection<ObjEntity> loadObjEntities(DataMap map, DbLoaderConfiguration config,
			Collection<DbEntity> entities, ObjectNameGenerator nameGenerator) {
		if (entities.isEmpty()) {
			return Collections.emptyList();
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
			objEntity.setClassName(config.getGenericClassName() != null ? config.getGenericClassName() : map
					.getNameWithDefaultPackage(objEntity.getName()));

			map.addObjEntity(objEntity);
			loadedEntities.add(objEntity);
		}

		return loadedEntities;
	}

	/**
	 * @since 4.0
	 */
	protected EntityMergeSupport createEntityMerger(DataMap map) {
		return new EntityMergeSupport(map, nameGenerator, !creatingMeaningfulPK);
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

			if (!new EqualsBuilder().append(pkEntity.getCatalog(), key.pkCatalog)
					.append(pkEntity.getSchema(), key.pkSchema).append(fkEntity.getCatalog(), key.fkCatalog)
					.append(fkEntity.getSchema(), key.fkSchema).isEquals()) {

				LOGGER.info("Skip relation: '" + key + "' because it related to objects from other catalog/schema");
				LOGGER.info("     relation primary key: '" + key.pkCatalog + "." + key.pkSchema + "'");
				LOGGER.info("       primary key entity: '" + pkEntity.getCatalog() + "." + pkEntity.getSchema() + "'");
				LOGGER.info("     relation foreign key: '" + key.fkCatalog + "." + key.fkSchema + "'");
				LOGGER.info("       foreign key entity: '" + fkEntity.getCatalog() + "." + fkEntity.getSchema() + "'");
				continue;
			}

			// forwardRelationship is a reference from table with primary key
			DbRelationship forwardRelationship = new DbRelationship(generateName(pkEntity, key, true));
			forwardRelationship.setSourceEntity(pkEntity);
			forwardRelationship.setTargetEntityName(fkEntity);

			// forwardRelationship is a reference from table with foreign key,
			// it is what exactly we load from db
			DbRelationshipDetected reverseRelationship = new DbRelationshipDetected(generateName(fkEntity, key, false));
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
			forwardRelationship.setName(generateName(pkEntity, key, !isOneToOne));

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
			DbRelationship forwardRelationship, DbRelationshipDetected reverseRelationship) {
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
				LOGGER.info(
						"Error getting relationships for '" + catalog + "." + schema + "', ignoring. "
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
		// if (LOGGER.isDebugEnabled()) {
		LOGGER.info("Skip relation: '" + key + "' because table '" + tableName + "' not found");
		// }
	}

	private String generateName(DbEntity entity, ExportedKey key, boolean toMany) {
		String forwardPreferredName = nameGenerator.createDbRelationshipName(key, toMany);
		return DefaultUniqueNameGenerator.generate(NameCheckers.dbRelationship, entity, forwardPreferredName);
	}

	/**
	 * Flattens many-to-many relationships in the generated model.
	 */
	public static void flattenManyToManyRelationships(DataMap map, Collection<ObjEntity> loadedObjEntities,
			ObjectNameGenerator objectNameGenerator) {
		if (loadedObjEntities.isEmpty()) {
			return;
		}
		Collection<ObjEntity> entitiesForDelete = new LinkedList<ObjEntity>();

		for (ObjEntity curEntity : loadedObjEntities) {
			ManyToManyCandidateEntity entity = ManyToManyCandidateEntity.build(curEntity);

			if (entity != null) {
				entity.optimizeRelationships(objectNameGenerator);
				entitiesForDelete.add(curEntity);
			}
		}

		// remove needed entities
		for (ObjEntity curDeleteEntity : entitiesForDelete) {
			map.removeObjEntity(curDeleteEntity.getName(), true);
		}
		loadedObjEntities.removeAll(entitiesForDelete);
	}

	private void fireObjEntitiesAddedEvents(Collection<ObjEntity> loadedObjEntities) {
		for (ObjEntity curEntity : loadedObjEntities) {
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
	 *             {@link #load(org.apache.cayenne.map.DataMap, DbLoaderConfiguration)}
	 *             method that supports catalogs.
	 */
	@Deprecated
	public DataMap loadDataMapFromDB(String schemaPattern, String tablePattern, DataMap dataMap) throws SQLException {

		DbLoaderConfiguration configuration = new DbLoaderConfiguration();
		configuration.setFiltersConfig(FiltersConfig.create(null, schemaPattern, TableFilter.include(tablePattern),
				PatternFilter.INCLUDE_NOTHING));

		load(dataMap, configuration);
		return dataMap;
	}

	/**
	 * Performs database reverse engineering and generates DataMap object that
	 * contains default mapping of the tables and views. Allows to limit types
	 * of tables to read.
	 *
	 * @deprecated since 4.0 use
	 *             {@link #load(org.apache.cayenne.map.DataMap, DbLoaderConfiguration)}
	 *             method that supports catalogs.
	 */
	@Deprecated
	public DataMap loadDataMapFromDB(String schemaPattern, String tablePattern, String[] tableTypes, DataMap dataMap)
			throws SQLException {
		dataMap.clear();

		DbLoaderConfiguration config = new DbLoaderConfiguration();
		config.setFiltersConfig(FiltersConfig.create(null, schemaPattern, TableFilter.include(tablePattern),
				PatternFilter.INCLUDE_NOTHING));
		config.setTableTypes(tableTypes);

		load(dataMap, config);
		return dataMap;
	}

	/**
	 * Performs database reverse engineering based on the specified config and
	 * fills the specified DataMap object with DB and object mapping info.
	 *
	 * @since 4.0
	 */
	public void load(DataMap dataMap, DbLoaderConfiguration config) throws SQLException {
		LOGGER.info("Schema loading...");

		String[] types = config.getTableTypes();
		if (types == null || types.length == 0) {
			types = getDefaultTableTypes();
		}

		for (CatalogFilter catalog : config.getFiltersConfig().catalogs) {
			for (SchemaFilter schema : catalog.schemas) {

				List<DbEntity> entities = createTableLoader(catalog.name, schema.name, schema.tables).loadDbEntities(
						dataMap, config, types);

				if (entities != null) {
					loadDbRelationships(config, catalog.name, schema.name, entities);

					prepareObjLayer(dataMap, config, entities);
				}
			}
		}
	}

	protected DbTableLoader createTableLoader(String catalog, String schema, TableFilter filter) throws SQLException {
		return new DbTableLoader(catalog, schema, getMetaData(), delegate, new DbAttributesPerSchemaLoader(catalog,
				schema, getMetaData(), adapter, filter));
	}

	public void prepareObjLayer(DataMap dataMap, DbLoaderConfiguration config, Collection<DbEntity> entities) {
		Collection<ObjEntity> loadedObjEntities = loadObjEntities(dataMap, config, entities);
		flattenManyToManyRelationships(dataMap, loadedObjEntities, getNameGenerator());
		fireObjEntitiesAddedEvents(loadedObjEntities);
	}

	/**
	 * Performs database reverse engineering to match the specified catalog,
	 * schema, table name and table type patterns and fills the specified
	 * DataMap object with DB and object mapping info.
	 *
	 * @since 4.0
	 */
	public DataMap load(DbLoaderConfiguration config) throws SQLException {

		DataMap dataMap = new DataMap();
		load(dataMap, config);
		loadProcedures(dataMap, config);

		return dataMap;
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
	 * @deprecated since 4.0 use loadProcedures(DataMap, String, String, String)
	 *             that supports "catalog" pattern.
	 */
	@Deprecated
	public void loadProceduresFromDB(String schemaPattern, String namePattern, DataMap dataMap) throws SQLException {
		DbLoaderConfiguration configuration = new DbLoaderConfiguration();
		configuration.setFiltersConfig(FiltersConfig.create(null, schemaPattern, TableFilter.everything(),
				new PatternFilter().include(namePattern)));

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
	public Map<String, Procedure> loadProcedures(DataMap dataMap, DbLoaderConfiguration config) throws SQLException {

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

		for (CatalogFilter catalog : config.getFiltersConfig().catalogs) {
			for (SchemaFilter schema : catalog.schemas) {
				loadProceduresColumns(procedures, catalog.name, schema.name);
			}
		}
	}

	private void loadProceduresColumns(Map<String, Procedure> procedures, String catalog, String schema)
			throws SQLException {

		try (ResultSet columnsRS = getMetaData().getProcedureColumns(catalog, schema, null, null);) {
			while (columnsRS.next()) {

				String s = columnsRS.getString("PROCEDURE_SCHEM");
				String name = columnsRS.getString("PROCEDURE_NAME");
				String key = (s == null ? "" : s + '.') + name;
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
		Map<String, Procedure> procedures = new HashMap<>();

		FiltersConfig filters = config.getFiltersConfig();
		for (CatalogFilter catalog : filters.catalogs) {
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

				if (!filters.proceduresFilter(procedure.getCatalog(), procedure.getSchema()).isInclude(
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

	/**
	 * Sets new naming strategy for reverse engineering
	 *
	 * @since 3.0
	 */
	public void setNameGenerator(ObjectNameGenerator strategy) {
		if (strategy == null) {
			LOGGER.warn("Attempt to set null into NameGenerator. LegacyNameGenerator will be used.");
			this.nameGenerator = new LegacyNameGenerator();
		} else {
			this.nameGenerator = strategy;
		}
	}

	/**
	 * @return naming strategy for reverse engineering
	 * @since 3.0
	 */
	public ObjectNameGenerator getNameGenerator() {
		return nameGenerator;
	}
}