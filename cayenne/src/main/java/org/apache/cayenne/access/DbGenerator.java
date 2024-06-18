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

package org.apache.cayenne.access;

import org.apache.cayenne.ashwood.AshwoodEntitySorter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Utility class that generates database schema based on Cayenne mapping. It is
 * a logical counterpart of DbLoader class.
 */
public class DbGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbGenerator.class);

	protected DbAdapter adapter;
	protected DataMap map;

	// optional DataDomain needed for correct FK generation in cross-db
	// situations
	protected DataDomain domain;

	protected JdbcEventLogger jdbcEventLogger;

	// stores generated SQL statements
	protected Map<String, Collection<String>> dropTables;
	protected Map<String, String> createTables;
	protected Map<String, List<String>> createConstraints;
	protected List<String> createPK;
	protected List<String> dropPK;

	/**
	 * Contains all DbEntities ordered considering their interdependencies.
	 * DerivedDbEntities are filtered out of this list.
	 */
	protected List<DbEntity> dbEntitiesInInsertOrder;
	protected List<DbEntity> dbEntitiesRequiringAutoPK;

	protected boolean shouldDropTables;
	protected boolean shouldCreateTables;
	protected boolean shouldDropPKSupport;
	protected boolean shouldCreatePKSupport;
	protected boolean shouldCreateFKConstraints;

	protected ValidationResult failures;

	/**
	 * @since 3.1
	 */
	public DbGenerator(DbAdapter adapter, DataMap map, JdbcEventLogger logger) {
		this(adapter, map, logger, Collections.emptyList());
	}

	/**
	 * @since 3.1
	 */
	public DbGenerator(DbAdapter adapter, DataMap map, JdbcEventLogger logger, Collection<DbEntity> excludedEntities) {
		this(adapter, map, excludedEntities, null, logger);
	}

	/**
	 * Creates and initializes new DbGenerator instance.
	 * 
	 * @param adapter
	 *            DbAdapter corresponding to the database
	 * @param map
	 *            DataMap whose entities will be used in schema generation
	 * @param excludedEntities
	 *            entities that should be ignored during schema generation
	 * @param domain
	 *            optional DataDomain used to detect cross-database
	 *            relationships.
	 * @since 3.1
	 */
	public DbGenerator(DbAdapter adapter, DataMap map, Collection<DbEntity> excludedEntities, DataDomain domain,
			JdbcEventLogger logger) {
		// sanity check
		if (adapter == null) {
			throw new IllegalArgumentException("Adapter must not be null.");
		}

		if (map == null) {
			throw new IllegalArgumentException("DataMap must not be null.");
		}

		this.domain = domain;
		this.map = map;
		this.adapter = adapter;
		this.jdbcEventLogger = logger;

		prepareDbEntities(excludedEntities);
		resetToDefaults();
		buildStatements();
	}

	protected void resetToDefaults() {
		this.shouldDropTables = false;
		this.shouldDropPKSupport = false;
		this.shouldCreatePKSupport = true;
		this.shouldCreateTables = true;
		this.shouldCreateFKConstraints = true;
	}

	/**
	 * Creates and stores internally a set of statements for database schema
	 * creation, ignoring configured schema creation preferences. Statements are
	 * NOT executed in this method.
	 */
	protected void buildStatements() {
		dropTables = new HashMap<>();
		createTables = new HashMap<>();
		createConstraints = new HashMap<>();

		DbAdapter adapter = getAdapter();
		for (final DbEntity dbe : this.dbEntitiesInInsertOrder) {

			String name = dbe.getName();

			// build "DROP TABLE"
			dropTables.put(name, adapter.dropTableStatements(dbe));

			// build "CREATE TABLE"
			createTables.put(name, adapter.createTable(dbe));

			// build constraints
			createConstraints.put(name, createConstraintsQueries(dbe));
		}

		PkGenerator pkGenerator = adapter.getPkGenerator();
		dropPK = pkGenerator.dropAutoPkStatements(dbEntitiesRequiringAutoPK);
		createPK = pkGenerator.createAutoPkStatements(dbEntitiesRequiringAutoPK);
	}

	/**
	 * Returns <code>true</code> if there is nothing to be done by this
	 * generator. If <code>respectConfiguredSettings</code> is <code>true</code>
	 * , checks are done applying currently configured settings, otherwise check
	 * is done, assuming that all possible generated objects.
	 */
	public boolean isEmpty(boolean respectConfiguredSettings) {
		if (dbEntitiesInInsertOrder.isEmpty() && dbEntitiesRequiringAutoPK.isEmpty()) {
			return true;
		}

		if (!respectConfiguredSettings) {
			return false;
		}

		return !(shouldDropTables || shouldCreateTables || shouldCreateFKConstraints || shouldCreatePKSupport || shouldDropPKSupport);
	}

	/** Returns DbAdapter associated with this DbGenerator. */
	public DbAdapter getAdapter() {
		return adapter;
	}

	/**
	 * Returns a list of all schema statements that should be executed with the
	 * current configuration.
	 */
	public List<String> configuredStatements() {
		List<String> list = new ArrayList<>();

		if (shouldDropTables) {
			ListIterator<DbEntity> it = dbEntitiesInInsertOrder.listIterator(dbEntitiesInInsertOrder.size());
			while (it.hasPrevious()) {
				DbEntity ent = it.previous();
				list.addAll(dropTables.get(ent.getName()));
			}
		}

		if (shouldCreateTables) {
			for (final DbEntity ent : dbEntitiesInInsertOrder) {
				list.add(createTables.get(ent.getName()));
			}
		}

		if (shouldCreateFKConstraints) {
			for (final DbEntity ent : dbEntitiesInInsertOrder) {
				List<String> fks = createConstraints.get(ent.getName());
				list.addAll(fks);
			}
		}

		if (shouldDropPKSupport) {
			list.addAll(dropPK);
		}

		if (shouldCreatePKSupport) {
			list.addAll(createPK);
		}

		return list;
	}

	/**
	 * Executes a set of commands to drop/create database objects. This is the
	 * main worker method of DbGenerator. Command set is built based on
	 * pre-configured generator settings.
	 */
	public void runGenerator(DataSource ds) throws Exception {
		this.failures = null;

		try (Connection connection = ds.getConnection()) {
			// force connection to autocommit, see CAY-2354
			boolean autoCommit = connection.getAutoCommit();
			connection.setAutoCommit(true);
			try {
				// drop tables
				if (shouldDropTables) {
					ListIterator<DbEntity> it = dbEntitiesInInsertOrder.listIterator(dbEntitiesInInsertOrder.size());
					while (it.hasPrevious()) {
						DbEntity ent = it.previous();
						for (String statement : dropTables.get(ent.getName())) {
							safeExecute(connection, statement);
						}
					}
				}

				// create tables
				List<String> createdTables = new ArrayList<>();
				if (shouldCreateTables) {
					for (final DbEntity ent : dbEntitiesInInsertOrder) {

						// only create missing tables

						safeExecute(connection, createTables.get(ent.getName()));
						createdTables.add(ent.getName());
					}
				}

				// create FK
				if (shouldCreateTables && shouldCreateFKConstraints) {
					for (DbEntity ent : dbEntitiesInInsertOrder) {

						if (createdTables.contains(ent.getName())) {
							List<String> fks = createConstraints.get(ent.getName());
							for (String fk : fks) {
								safeExecute(connection, fk);
							}
						}
					}
				}

				// drop PK
				if (shouldDropPKSupport) {
					List<String> dropAutoPKSQL = getAdapter().getPkGenerator().dropAutoPkStatements(
							dbEntitiesRequiringAutoPK);
					for (final String sql : dropAutoPKSQL) {
						safeExecute(connection, sql);
					}
				}

				// create pk
				if (shouldCreatePKSupport) {
					List<String> createAutoPKSQL = getAdapter().getPkGenerator().createAutoPkStatements(
							dbEntitiesRequiringAutoPK);
					for (final String sql : createAutoPKSQL) {
						safeExecute(connection, sql);
					}
				}

				new DbGeneratorPostprocessor().execute(connection, getAdapter());
			} finally {
				// restore connection autocommit state in case it will be recycled in some underlying pool
				connection.setAutoCommit(autoCommit);
			}
		}
	}

	/**
	 * Builds and executes a SQL statement, catching and storing SQL exceptions
	 * resulting from invalid SQL. Only non-recoverable exceptions are rethrown.
	 * 
	 * @since 1.1
	 */
	protected boolean safeExecute(Connection connection, String sql) {

		try (Statement statement = connection.createStatement()) {
			jdbcEventLogger.log(sql);
			statement.execute(sql);
			return true;
		} catch (SQLException ex) {
			if (this.failures == null) {
				this.failures = new ValidationResult();
			}

			failures.addFailure(new SimpleValidationFailure(sql, ex.getMessage()));
			jdbcEventLogger.logQueryError(ex);
			return false;
		}
	}

	/**
	 * Creates FK and UNIQUE constraint statements for a given table.
	 * 
	 * @since 3.0
	 */
	public List<String> createConstraintsQueries(DbEntity table) {
		List<String> list = new ArrayList<>();
		for (final DbRelationship rel : table.getRelationships()) {

			if (rel.isToMany()) {
				continue;
			}

			// skip FK to a different DB
			if (domain != null) {
				DataMap srcMap = rel.getSourceEntity().getDataMap();
				DataMap targetMap = rel.getTargetEntity().getDataMap();

				if (srcMap != null && targetMap != null && srcMap != targetMap) {
					if (domain.lookupDataNode(srcMap) != domain.lookupDataNode(targetMap)) {
						continue;
					}
				}
			}

			// create an FK CONSTRAINT only if the relationship is to PK
			// and if this is not a dependent PK

			// create UNIQUE CONSTRAINT on FK if reverse relationship is to-one

			if (rel.isToPK() && !rel.isToDependentPK()) {

				if (getAdapter().supportsUniqueConstraints()) {

					DbRelationship reverse = rel.getReverseRelationship();
					if (reverse != null && !reverse.isToMany() && !reverse.isToPK()) {

						String unique = getAdapter().createUniqueConstraint(rel.getSourceEntity(),
								rel.getSourceAttributes());
						if (unique != null) {
							list.add(unique);
						}
					}
				}

				String fk = getAdapter().createFkConstraint(rel);
				if (fk != null) {
					list.add(fk);
				}
			}
		}
		return list;
	}

	/**
	 * Returns an object representing a collection of failures that occurred on
	 * the last "runGenerator" invocation, or null if there were no failures.
	 * Failures usually indicate problems with generated DDL (such as
	 * "create...", "drop...", etc.) and usually happen due to the DataMap being
	 * out of sync with the database.
	 * 
	 * @since 1.1
	 */
	public ValidationResult getFailures() {
		return failures;
	}

	/**
	 * Returns whether DbGenerator is configured to create primary key support
	 * for DataMap entities.
	 */
	public boolean shouldCreatePKSupport() {
		return shouldCreatePKSupport;
	}

	/**
	 * Returns whether DbGenerator is configured to create tables for DataMap
	 * entities.
	 */
	public boolean shouldCreateTables() {
		return shouldCreateTables;
	}

	public boolean shouldDropPKSupport() {
		return shouldDropPKSupport;
	}

	public boolean shouldDropTables() {
		return shouldDropTables;
	}

	public boolean shouldCreateFKConstraints() {
		return shouldCreateFKConstraints;
	}

	public void setShouldCreatePKSupport(boolean shouldCreatePKSupport) {
		this.shouldCreatePKSupport = shouldCreatePKSupport;
	}

	public void setShouldCreateTables(boolean shouldCreateTables) {
		this.shouldCreateTables = shouldCreateTables;
	}

	public void setShouldDropPKSupport(boolean shouldDropPKSupport) {
		this.shouldDropPKSupport = shouldDropPKSupport;
	}

	public void setShouldDropTables(boolean shouldDropTables) {
		this.shouldDropTables = shouldDropTables;
	}

	public void setShouldCreateFKConstraints(boolean shouldCreateFKConstraints) {
		this.shouldCreateFKConstraints = shouldCreateFKConstraints;
	}

	/**
	 * Returns a DataDomain used by the DbGenerator to detect cross-database
	 * relationships. By default DataDomain is null.
	 * 
	 * @since 1.2
	 */
	public DataDomain getDomain() {
		return domain;
	}

	/**
	 * Helper method that orders DbEntities to satisfy referential constraints
	 * and returns an ordered list. It also filters out DerivedDbEntities.
	 */
	private void prepareDbEntities(Collection<DbEntity> excludedEntities) {
		if (excludedEntities == null) {
			excludedEntities = Collections.emptyList();
		}

		List<DbEntity> tables = new ArrayList<>();
		List<DbEntity> tablesWithAutoPk = new ArrayList<>();

		for (DbEntity nextEntity : map.getDbEntities()) {

			// do sanity checks...

			// tables with no columns are not included
			if (nextEntity.getAttributes().size() == 0) {
				LOGGER.info("Skipping entity with no attributes: " + nextEntity.getName());
				continue;
			}

			// check if this entity is explicitly excluded
			if (excludedEntities.contains(nextEntity)) {
				continue;
			}

			// tables with invalid DbAttributes are not included
			boolean invalidAttributes = false;
			for (final DbAttribute attr : nextEntity.getAttributes()) {
				if (attr.getType() == TypesMapping.NOT_DEFINED) {
					LOGGER.info("Skipping entity, attribute type is undefined: " + nextEntity.getName() + "."
							+ attr.getName());
					invalidAttributes = true;
					break;
				}
			}
			if (invalidAttributes) {
				continue;
			}

			tables.add(nextEntity);

			// check if an automatic PK generation can be potentially supported
			// in this entity. For now simply check that the key is not
			// propagated
			Iterator<DbRelationship> relationships = nextEntity.getRelationships().iterator();

			// create a copy of the original PK list,
			// since the list will be modified locally
			List<DbAttribute> pkAttributes = new ArrayList<>(nextEntity.getPrimaryKeys());
			while (pkAttributes.size() > 0 && relationships.hasNext()) {
				DbRelationship nextRelationship = relationships.next();
				if (!nextRelationship.isToMasterPK()) {
					continue;
				}

				// supposedly all source attributes of the relationship
				// to master entity must be a part of primary key,
				// so
				for (DbJoin join : nextRelationship.getJoins()) {
					pkAttributes.remove(join.getSource());
				}
			}

			// primary key is needed only if at least one of the primary key
			// attributes
			// is not propagated via relationship
			if (pkAttributes.size() > 0) {
				tablesWithAutoPk.add(nextEntity);
			}
		}

		// sort table list
		if (tables.size() > 1) {
			EntitySorter sorter = new AshwoodEntitySorter();
			sorter.setEntityResolver(new EntityResolver(Collections.singleton(map)));
			sorter.sortDbEntities(tables, false);
		}

		this.dbEntitiesInInsertOrder = tables;
		this.dbEntitiesRequiringAutoPK = tablesWithAutoPk;
	}
}
