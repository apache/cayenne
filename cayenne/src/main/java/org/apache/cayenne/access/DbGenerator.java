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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledDataMap;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledSchema;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Utility class that generates database schema based on Cayenne mapping. It is
 * a logical counterpart of DbLoader class.
 */
public class DbGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbGenerator.class);

	protected DbAdapter adapter;

	/**
	 * The compiled schema the DDL is generated from, and the map whose tables are generated.
	 *
	 * @since 6.0
	 */
	protected CompiledSchema schema;
	protected CompiledDataMap map;

	// the names of the maps on the same database: a foreign key to a table outside of them is not a constraint
	protected Set<String> databaseMaps;

	protected SQLLogger sqlLogger;

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
	protected List<CompiledTable> tablesInInsertOrder;
	protected List<CompiledTable> tablesRequiringAutoPK;

	protected boolean shouldDropTables;
	protected boolean shouldCreateTables;
	protected boolean shouldDropPKSupport;
	protected boolean shouldCreatePKSupport;
	protected boolean shouldCreateFKConstraints;

	protected ValidationResult failures;

	/**
	 * Creates a generator of the tables of a map within its compiled schema.
	 *
	 * @since 6.0
	 */
	public DbGenerator(DbAdapter adapter, CompiledSchema schema, String mapName, SQLLogger logger) {
		this(adapter, schema, mapName, List.of(), schema.maps().stream().map(CompiledDataMap::name).toList(), logger);
	}

	/**
	 * Creates a generator of the tables of a map within its compiled schema.
	 *
	 * @param adapter        DbAdapter corresponding to the database
	 * @param schema         the compiled schema the map belongs to, which resolves the relationships to the tables
	 *                       of the other maps compiled with it
	 * @param mapName        the name of the map in the schema whose tables are generated
	 * @param excludedTables the names of the tables that should be ignored during schema generation
	 * @param databaseMaps   the names of the maps on the same database as the generated one; a foreign key to a
	 *                       table of another map is not generated as a constraint
	 * @since 6.0
	 */
	public DbGenerator(
			DbAdapter adapter,
			CompiledSchema schema,
			String mapName,
			Collection<String> excludedTables,
			Collection<String> databaseMaps,
			SQLLogger logger) {

		if (adapter == null) {
			throw new IllegalArgumentException("Adapter must not be null.");
		}
		if (schema == null) {
			throw new IllegalArgumentException("Schema must not be null.");
		}

		CompiledDataMap map = schema.map(mapName);
		if (map == null) {
			throw new IllegalArgumentException("DataMap '" + mapName + "' is not in the schema " + schema);
		}

		this.databaseMaps = new HashSet<>(databaseMaps);
		this.schema = schema;
		this.map = map;
		this.adapter = adapter;
		this.sqlLogger = logger;

		prepareDbEntities(excludedTables);
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
		for (CompiledTable table : this.tablesInInsertOrder) {

			String name = table.name();

			// build "DROP TABLE"
			dropTables.put(name, adapter.dropTableStatements(table));

			// build "CREATE TABLE"
			createTables.put(name, adapter.createTable(table));

			// build constraints
			createConstraints.put(name, createConstraintsQueries(table));
		}

		PkGenerator pkGenerator = adapter.createPkGenerator();
		dropPK = pkGenerator.dropAutoPkStatements(adapter, tablesRequiringAutoPK);
		createPK = pkGenerator.createAutoPkStatements(adapter, tablesRequiringAutoPK);
	}

	/**
	 * Returns <code>true</code> if there is nothing to be done by this
	 * generator. If <code>respectConfiguredSettings</code> is <code>true</code>
	 * , checks are done applying currently configured settings, otherwise check
	 * is done, assuming that all possible generated objects.
	 */
	public boolean isEmpty(boolean respectConfiguredSettings) {
		if (tablesInInsertOrder.isEmpty() && tablesRequiringAutoPK.isEmpty()) {
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
			ListIterator<CompiledTable> it = tablesInInsertOrder.listIterator(tablesInInsertOrder.size());
			while (it.hasPrevious()) {
				CompiledTable table = it.previous();
				list.addAll(dropTables.get(table.name()));
			}
		}

		if (shouldCreateTables) {
			for (CompiledTable table : tablesInInsertOrder) {
				list.add(createTables.get(table.name()));
			}
		}

		if (shouldCreateFKConstraints) {
			for (CompiledTable table : tablesInInsertOrder) {
				List<String> fks = createConstraints.get(table.name());
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
					ListIterator<CompiledTable> it = tablesInInsertOrder.listIterator(tablesInInsertOrder.size());
					while (it.hasPrevious()) {
						CompiledTable table = it.previous();
						for (String statement : dropTables.get(table.name())) {
							safeExecute(connection, statement);
						}
					}
				}

				// create tables
				List<String> createdTables = new ArrayList<>();
				if (shouldCreateTables) {
					for (CompiledTable table : tablesInInsertOrder) {

						// only create missing tables

						safeExecute(connection, createTables.get(table.name()));
						createdTables.add(table.name());
					}
				}

				// create FK
				if (shouldCreateTables && shouldCreateFKConstraints) {
					for (CompiledTable table : tablesInInsertOrder) {

						if (createdTables.contains(table.name())) {
							List<String> fks = createConstraints.get(table.name());
							for (String fk : fks) {
								safeExecute(connection, fk);
							}
						}
					}
				}

				// drop PK
				if (shouldDropPKSupport) {
					for (String sql : dropPK) {
						safeExecute(connection, sql);
					}
				}

				// create pk
				if (shouldCreatePKSupport) {
					for (String sql : createPK) {
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
			sqlLogger.logMessage(sql);
			statement.execute(sql);
			return true;
		} catch (SQLException ex) {
			if (this.failures == null) {
				this.failures = new ValidationResult();
			}

			// the failure is recorded and surfaced to the caller via getFailures()
			failures.addFailure(new SimpleValidationFailure(sql, ex.getMessage()));
			return false;
		}
	}

	/**
	 * Creates FK and UNIQUE constraint statements for a given table.
	 * 
	 * @since 3.0
	 */
	public List<String> createConstraintsQueries(CompiledTable table) {
		List<String> list = new ArrayList<>();
		for (CompiledJoin join : table.joins()) {

			// an FK constraint exists only where the source row holds the FK; a master-to-dependent
			// relationship is constrained from the dependent side. An FK side the compiler had to infer is not
			// a constraint the model declares
			if (!join.isDeclaredForeignKey()) {
				continue;
			}

			// skip FK to a different DB
			if (!databaseMaps.contains(join.target().map().name())) {
				continue;
			}

			// create UNIQUE CONSTRAINT on FK if reverse relationship is to-one
			if (getAdapter().supportsUniqueConstraints()) {

				CompiledJoin reverse = join.reverse();
				if (reverse != null && !reverse.isToMany() && !reverse.isToPrimaryKey()) {

					String unique = getAdapter().createUniqueConstraint(join.source(), join.sourceColumns());
					if (unique != null) {
						list.add(unique);
					}
				}
			}

			String fk = getAdapter().createFkConstraint(join);
			if (fk != null) {
				list.add(fk);
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
	 * Collects the tables of the map ordered to satisfy referential constraints. The tables that can't be generated
	 * (no columns, undefined column types) and the excluded tables are filtered out.
	 */
	private void prepareDbEntities(Collection<String> excludedTables) {
		Set<String> excludedNames = new HashSet<>(excludedTables != null ? excludedTables : List.of());

		List<CompiledTable> tables = new ArrayList<>();
		List<CompiledTable> tablesWithAutoPk = new ArrayList<>();

		// the dependency order of the tables is the insert order; the name makes it deterministic within a level
		List<CompiledTable> inDependencyOrder = new ArrayList<>();
		for (CompiledTable table : schema.tables()) {
			if (map.equals(table.map())) {
				inDependencyOrder.add(table);
			}
		}
		inDependencyOrder.sort(Comparator.comparingInt(CompiledTable::dependencyIndex)
				.thenComparing(CompiledTable::name));
		for (CompiledTable nextTable : inDependencyOrder) {

			// do sanity checks...

			// tables with no columns are not included
			if (nextTable.columns().isEmpty()) {
				LOGGER.info("Skipping entity with no attributes: {}", nextTable.name());
				continue;
			}

			// check if this entity is explicitly excluded
			if (excludedNames.contains(nextTable.name())) {
				continue;
			}

			// tables with invalid DbAttributes are not included
			boolean invalidAttributes = false;
			for (CompiledColumn column : nextTable.columns()) {
				if (column.jdbcType() == TypesMapping.NOT_DEFINED) {
					LOGGER.info("Skipping entity, attribute type is undefined: {}.{}", nextTable.name(),
							column.name());
					invalidAttributes = true;
					break;
				}
			}
			if (invalidAttributes) {
				continue;
			}

			tables.add(nextTable);

			// check if an automatic PK generation can be potentially supported in this entity. For now simply check
			// that the key is not propagated: FK columns get their values from the master row
			Iterator<CompiledJoin> joins = nextTable.joins().iterator();

			// create a copy of the original PK list,
			// since the list will be modified locally
			List<CompiledColumn> pkColumns = new ArrayList<>(nextTable.primaryKey());
			while (pkColumns.size() > 0 && joins.hasNext()) {
				CompiledJoin nextJoin = joins.next();
				if (!nextJoin.isDeclaredForeignKey()) {
					continue;
				}

				for (CompiledColumnPair join : nextJoin.columnPairs()) {
					pkColumns.remove(join.source());
				}
			}

			// primary key is needed only if at least one of the primary key
			// attributes
			// is not propagated via relationship
			if (pkColumns.size() > 0) {
				tablesWithAutoPk.add(nextTable);
			}
		}

		this.tablesInInsertOrder = tables;
		this.tablesRequiringAutoPK = tablesWithAutoPk;
	}
}
