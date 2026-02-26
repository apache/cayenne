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

package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.reader.DefaultRowReaderFactory;
import org.apache.cayenne.access.translator.batch.DefaultBatchTranslatorFactory;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslatorFactory;
import org.apache.cayenne.ashwood.AshwoodEntitySorter;
import org.apache.cayenne.cache.MapQueryCache;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.testdo.extended_type.StringET1ExtendedType;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Default implementation of the AccessStack that has a single DataNode per DataMap.
 */
public class SchemaBuilder {

	private static final Logger logger = LoggerFactory.getLogger(SchemaBuilder.class);

	public static final String SKIP_SCHEMA_KEY = "cayenneTestSkipSchemaCreation";

	private static final String[] MAPS_REQUIRING_SCHEMA_SETUP = { "testmap.map.xml", "compound.map.xml",
			"misc-types.map.xml", "things.map.xml", "numeric-types.map.xml", "binary-pk.map.xml", "no-pk.map.xml",
			"lob.map.xml", "date-time.map.xml", "enum.map.xml", "extended-type.map.xml", "generated.map.xml",
			"mixed-persistence-strategy.map.xml", "people.map.xml", "primitive.map.xml", "inheritance.map.xml",
			"locking.map.xml", "soft-delete.map.xml", "empty.map.xml", "relationships.map.xml",
			"relationships-activity.map.xml", "relationships-delete-rules.map.xml",
			"relationships-collection-to-many.map.xml", "relationships-child-master.map.xml",
			"relationships-clob.map.xml", "relationships-flattened.map.xml", "relationships-many-to-many-join.map.xml", "relationships-set-to-many.map.xml",
			"relationships-to-many-fk.map.xml", "relationships-to-one-fk.map.xml", "return-types.map.xml",
			"uuid.map.xml", "multi-tier.map.xml", "reflexive.map.xml", "delete-rules.map.xml",
            "lifecycle-callbacks-order.map.xml", "lifecycles.map.xml", "map-to-many.map.xml", "toone.map.xml", "meaningful-pk.map.xml",
			"table-primitives.map.xml", "generic.map.xml", "map-db1.map.xml", "map-db2.map.xml", "embeddable.map.xml",
			"qualified.map.xml", "quoted-identifiers.map.xml", "inheritance-single-table1.map.xml",
			"inheritance-vertical.map.xml", "oneway-rels.map.xml", "unsupported-distinct-types.map.xml",
			"array-type.map.xml", "cay-2032.map.xml", "weighted-sort.map.xml", "hybrid-data-object.map.xml",
			"java8.map.xml", "inheritance-with-enum.map.xml", "lazy-attributes.map.xml", "cay2666/datamap.map.xml", "cay2641/datamapLazy.map.xml",
			"annotation/datamapAnnotation.map.xml", "many-to-many-joinTable-objEntity.map.xml" };

	// hardcoded dependent entities that should be excluded
	// if LOBs are not supported
	private static final String[] EXTRA_EXCLUDED_FOR_NO_LOB = new String[] { "CLOB_DETAIL" };

	private ServerCaseDataSourceFactory dataSourceFactory;
	private UnitDbAdapter unitDbAdapter;
	private DbAdapter dbAdapter;
	private DataDomain domain;
	private JdbcEventLogger jdbcEventLogger;

	@Inject
	DataMapLoader loader;

	public SchemaBuilder(@Inject ServerCaseDataSourceFactory dataSourceFactory, @Inject UnitDbAdapter unitDbAdapter,
			@Inject DbAdapter dbAdapter, @Inject JdbcEventLogger jdbcEventLogger) {
		this.dataSourceFactory = dataSourceFactory;
		this.unitDbAdapter = unitDbAdapter;
		this.dbAdapter = dbAdapter;
		this.jdbcEventLogger = jdbcEventLogger;
	}

	/**
	 * Completely rebuilds test schema.
	 */
	// TODO - this method changes the internal state of the object ... refactor
	public void rebuildSchema() {

		// generate schema combining all DataMaps that require schema support.
		// Schema generation is done like that instead of per DataMap on demand
		// to avoid conflicts when dropping and generating PK objects.

		DataMap[] maps = new DataMap[MAPS_REQUIRING_SCHEMA_SETUP.length];

		for (int i = 0; i < maps.length; i++) {
			URL mapURL = getClass().getClassLoader().getResource(MAPS_REQUIRING_SCHEMA_SETUP[i]);
			maps[i] = loader.load(new URLResource(mapURL));
		}

		this.domain = new DataDomain("temp");
		domain.setEventManager(new DefaultEventManager(2));
		domain.setEntitySorter(new AshwoodEntitySorter());
		domain.setQueryCache(new MapQueryCache(50));

		try {
			for (DataMap map : maps) {
				initNode(map);
			}

			if ("true".equalsIgnoreCase(System.getProperty(SKIP_SCHEMA_KEY))) {
				logger.info("skipping schema generation... ");
			} else {
				dropSchema();
				dropPKSupport();
				createSchema();
				createPKSupport();
			}
		} catch (Exception e) {
			throw new RuntimeException("Error rebuilding schema", e);
		}
	}

	private void initNode(DataMap map) {

		DataNode node = new DataNode(map.getName());
		node.setJdbcEventLogger(jdbcEventLogger);
		node.setAdapter(dbAdapter);
		node.setDataSource(dataSourceFactory.getSharedDataSource());

		// setup test extended types
		node.getAdapter().getExtendedTypes().registerType(new StringET1ExtendedType());

		// tweak mapping with a delegate
		for (Procedure proc : map.getProcedures()) {
			unitDbAdapter.tweakProcedure(proc);
		}
		filterDataMap(map);

		node.addDataMap(map);

		node.setSchemaUpdateStrategy(new SkipSchemaUpdateStrategy());
		node.setRowReaderFactory(new DefaultRowReaderFactory());
		node.setBatchTranslatorFactory(new DefaultBatchTranslatorFactory());
		node.setSelectTranslatorFactory(new DefaultSelectTranslatorFactory());
		domain.addNode(node);
	}

	/**
	 * Remote binary pk {@link DbEntity} for {@link DbAdapter} not supporting
	 * that and so on.
	 */
	protected void filterDataMap(DataMap map) {
		boolean supportsBinaryPK = unitDbAdapter.supportsBinaryPK();

		if (supportsBinaryPK) {
			return;
		}

		List<DbEntity> entitiesToRemove = new ArrayList<>();

		for (DbEntity ent : map.getDbEntities()) {
			for (DbAttribute attr : ent.getAttributes()) {
				// check for BIN PK or FK to BIN Pk
				if (attr.getType() == Types.BINARY || attr.getType() == Types.VARBINARY
						|| attr.getType() == Types.LONGVARBINARY) {
					if (attr.isPrimaryKey() || attr.isForeignKey()) {
						entitiesToRemove.add(ent);
						break;
					}
				}
			}
		}

		for (DbEntity e : entitiesToRemove) {
			map.removeDbEntity(e.getName(), true);
		}
	}

	/** Drops all test tables. */
	private void dropSchema() throws Exception {
		for (DataNode node : domain.getDataNodes()) {
			dropSchema(node, node.getDataMaps().iterator().next());
		}
	}

	/**
	 * Creates all test tables in the database.
	 */
	private void createSchema() throws Exception {
		for (DataNode node : domain.getDataNodes()) {
			createSchema(node, node.getDataMaps().iterator().next());
		}
	}

	public void dropPKSupport() throws Exception {
		for (DataNode node : domain.getDataNodes()) {
			dropPKSupport(node, node.getDataMaps().iterator().next());
		}
	}

	/**
	 * Creates primary key support for all node DbEntities. Will use its
	 * facilities provided by DbAdapter to generate any necessary database
	 * objects and data for primary key support.
	 */
	public void createPKSupport() throws Exception {
		for (DataNode node : domain.getDataNodes()) {
			createPKSupport(node, node.getDataMaps().iterator().next());
		}
	}

	/**
	 * Helper method that orders DbEntities to satisfy referential constraints
	 * and returns an ordered list.
	 */
	private List<DbEntity> dbEntitiesInInsertOrder(DataMap map) {
		TreeMap<String, DbEntity> dbEntityMap = new TreeMap<>(map.getDbEntityMap());
		List<DbEntity> entities = new ArrayList<>(dbEntityMap.values());

		dbEntitiesFilter(entities);

		domain.getEntitySorter().sortDbEntities(entities, false);
		return entities;
	}

	protected List<DbEntity> dbEntitiesInDeleteOrder(DataMap dataMap) {
		DataMap map = domain.getDataMap(dataMap.getName());
		Map<String, DbEntity> dbEntityMap = new TreeMap<>(map.getDbEntityMap());
		List<DbEntity> entities = new ArrayList<>(dbEntityMap.values());

		dbEntitiesFilter(entities);

		domain.getEntitySorter().sortDbEntities(entities, true);
		return entities;
	}

	// This seems actually unused for some time now (from 2014 to 2018), and caused no trouble
	private void dbEntitiesFilter(List<DbEntity> entities) {
		// filter various unsupported tests...

		// LOBs
		boolean excludeLOB = !unitDbAdapter.supportsLobs();
		boolean excludeBinPK = !unitDbAdapter.supportsBinaryPK();
		if (excludeLOB || excludeBinPK) {

			List<DbEntity> filtered = new ArrayList<>();

			for (DbEntity ent : entities) {

				// check for LOB attributes
				if (excludeLOB) {
					if (Arrays.binarySearch(EXTRA_EXCLUDED_FOR_NO_LOB, ent.getName()) >= 0) {
						continue;
					}

					boolean hasLob = false;
					for (final DbAttribute attr : ent.getAttributes()) {
						if (attr.getType() == Types.BLOB || attr.getType() == Types.CLOB) {
							hasLob = true;
							break;
						}
					}

					if (hasLob) {
						continue;
					}
				}

				// check for BIN PK
				if (excludeBinPK) {
					boolean skip = false;
					for (final DbAttribute attr : ent.getAttributes()) {
						// check for BIN PK or FK to BIN Pk
						if (attr.getType() == Types.BINARY || attr.getType() == Types.VARBINARY
								|| attr.getType() == Types.LONGVARBINARY) {

							if (attr.isPrimaryKey() || attr.isForeignKey()) {
								skip = true;
								break;
							}
						}
					}

					if (skip) {
						continue;
					}
				}

				filtered.add(ent);
			}

			entities = filtered;
		}
	}

	private void dropSchema(DataNode node, DataMap map) throws Exception {

		List<DbEntity> list = dbEntitiesInInsertOrder(map);

		try (Connection conn = dataSourceFactory.getSharedDataSource().getConnection()) {

			DatabaseMetaData md = conn.getMetaData();
			List<String> allTables = new ArrayList<>();

			try (ResultSet tables = md.getTables(null, null, "%", null)) {
				while (tables.next()) {
					// 'toUpperCase' is needed since most databases are case insensitive,
					// and some will convert names to lower case (e.g. PostgreSQL)
					String name = tables.getString("TABLE_NAME");
					if (name != null) {
						allTables.add(name.toUpperCase());
					}
				}
			}

			unitDbAdapter.willDropTables(conn, map, allTables);

			// drop all tables in the map
			try (Statement stmt = conn.createStatement()) {

				ListIterator<DbEntity> it = list.listIterator(list.size());
				while (it.hasPrevious()) {
					DbEntity ent = it.previous();
					if (!allTables.contains(ent.getName().toUpperCase())) {
						continue;
					}

					for (String dropSql : node.getAdapter().dropTableStatements(ent)) {
						try {
							logger.info(dropSql);
							stmt.execute(dropSql);
						} catch (SQLException sqe) {
							logger.warn("Can't drop table " + ent.getName() + ", ignoring...", sqe);
						}
					}
				}
			}

			unitDbAdapter.droppedTables(conn, map);
		}
	}

	private void dropPKSupport(DataNode node, DataMap map) throws Exception {
		List<DbEntity> filteredEntities = dbEntitiesInInsertOrder(map);
		node.getAdapter().getPkGenerator().dropAutoPk(node, filteredEntities);
	}

	private void createPKSupport(DataNode node, DataMap map) throws Exception {
		List<DbEntity> filteredEntities = dbEntitiesInInsertOrder(map);
		node.getAdapter().getPkGenerator().createAutoPk(node, filteredEntities);
	}

	private void createSchema(DataNode node, DataMap map) throws Exception {

		try (Connection conn = dataSourceFactory.getSharedDataSource().getConnection()) {
			unitDbAdapter.willCreateTables(conn, map);
			try (Statement stmt = conn.createStatement()) {

				for (String query : tableCreateQueries(node, map)) {
					logger.info(query);
					stmt.execute(query);
				}
			}
			unitDbAdapter.createdTables(conn, map);
		}
	}

	/**
	 * Returns iterator of preprocessed table create queries.
	 */
	private Collection<String> tableCreateQueries(DataNode node, DataMap map) {
		DbAdapter adapter = node.getAdapter();
		DbGenerator gen = new DbGenerator(adapter, map, null, domain, jdbcEventLogger);

		List<DbEntity> orderedEnts = dbEntitiesInInsertOrder(map);
		List<String> queries = new ArrayList<>();

		// table definitions
		for (DbEntity ent : orderedEnts) {
			queries.add(adapter.createTable(ent));
		}

		// FK constraints
		for (DbEntity ent : orderedEnts) {
			if (!unitDbAdapter.supportsFKConstraints(ent)) {
				continue;
			}

			List<String> qs = gen.createConstraintsQueries(ent);
			queries.addAll(qs);
		}

		return queries;
	}

}
