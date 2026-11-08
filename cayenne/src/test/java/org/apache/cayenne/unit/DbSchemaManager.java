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

package org.apache.cayenne.unit;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.project.DataSourceDescriptor;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.compiled.CompiledDataMap;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.CayenneRuntime;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Maps a full physical DB namespace (schema or database) that can be shared between many test DataMaps. Holds its
 * DataSource.
 */
public class DbSchemaManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbSchemaManager.class);

    // hardcoded dependent entities that should be excluded if LOBs are not supported
    private static final Set<String> EXTRA_EXCLUDED_FOR_NO_LOB = Set.of("CLOB_DETAIL");
    private static final Set<String> EXTRA_EXCLUDED_FOR_NO_NATIVE_JSON = Set.of("JSON_OTHER");

    private final DataSourceDescriptor dataSourceDescriptor;
    private final DataSource dataSource;
    private final TestDbAdapter testDbAdapter;
    private final DataDomain domain;
    private final TestProject project;
    private final List<DataMap> dataMapsInSchemaSetupOrder;

    public DbSchemaManager(String project, DataSourceDescriptor dataSourceDescriptor, DataSource dataSource) {

        this.dataSourceDescriptor = dataSourceDescriptor;
        this.dataSource = dataSource;
        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig(project)
                .addModule(TestProject.module())
                .defaultDataNode(dataSource)
                .build();
        this.domain = runtime.getDataDomain();
        this.project = TestProject.of(runtime);

        this.testDbAdapter = TestDbAdapter.of(domain.getDefaultNode().getAdapter());

        for (DataMap map : this.project.dataMaps()) {
            // tweak mapping with a delegate
            for (Procedure proc : map.getProcedures()) {
                testDbAdapter.tweakProcedure(proc);
            }

            filterDataMap(map);
        }

        // "filterDataMap" above mutated the maps, so the compiled mapping is stale
        recompile();

        this.dataMapsInSchemaSetupOrder = sortDataMapsInSchemaSetupOrder();
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public DataSourceDescriptor dataSourceDescriptor() {
        return dataSourceDescriptor;
    }

    /**
     * Rebuilds the test schema, combining all DataMaps that require schema support. Schema generation is done like that
     * instead of on-demand per-DataMap  to avoid conflicts when dropping and generating PK objects.
     */
    public void rebuildSchema() {

        try {
            dropSchema();
            dropPKSupport();
            createSchema();
            createPKSupport();
        } catch (Exception e) {
            throw new RuntimeException("Error rebuilding schema", e);
        }
    }

    private static final Set<Integer> BINARY_TYPES = Set.of(Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY);

    // a table can't exist on a DB without binary PK support if its own PK is binary, or if one of its FKs references
    // a binary PK (the FK column has the PK type)
    private static boolean hasBinaryKey(DbEntity entity) {
        for (DbAttribute pk : entity.getPrimaryKeys()) {
            if (BINARY_TYPES.contains(pk.getType())) {
                return true;
            }
        }
        for (DbRelationship relationship : entity.getRelationships()) {
            if (!relationship.isForeignKey()) {
                continue;
            }
            for (DbJoin join : relationship.getJoins()) {
                DbAttribute target = join.getTarget();
                if (target != null && BINARY_TYPES.contains(target.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    // the same check over a compiled table
    private static boolean hasBinaryKey(CompiledTable table) {
        for (CompiledColumn pk : table.primaryKey()) {
            if (BINARY_TYPES.contains(pk.jdbcType())) {
                return true;
            }
        }
        for (CompiledJoin join : table.joins()) {
            if (!join.isForeignKey()) {
                continue;
            }
            for (CompiledColumn target : join.targetColumns()) {
                if (BINARY_TYPES.contains(target.jdbcType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void filterDataMap(DataMap map) {
        boolean supportsBinaryPK = testDbAdapter.supportsBinaryPK();

        if (supportsBinaryPK) {
            return;
        }

        List<DbEntity> entitiesToRemove = new ArrayList<>();

        for (DbEntity ent : map.getDbEntities()) {
            if (hasBinaryKey(ent)) {
                entitiesToRemove.add(ent);
            }
        }

        for (DbEntity e : entitiesToRemove) {
            map.removeDbEntity(e.getName(), true);
        }
    }

    /**
     * Drops all test tables.
     */
    private void dropSchema() throws Exception {
        ListIterator<DataMap> it = dataMapsInSchemaSetupOrder.listIterator(dataMapsInSchemaSetupOrder.size());
        while (it.hasPrevious()) {
            DataMap map = it.previous();
            dropSchema(domain.lookupDataNode(map.getName()), map);
        }
    }

    /**
     * Creates all test tables in the database.
     */
    private void createSchema() throws Exception {
        for (DataMap map : dataMapsInSchemaSetupOrder) {
            createSchema(domain.lookupDataNode(map.getName()), map);
        }
    }

    public void dropPKSupport() throws Exception {
        for (DataMap map : dataMapsInSchemaSetupOrder) {
            dropPKSupport(domain.lookupDataNode(map.getName()), map);
        }
    }

    /**
     * Creates primary key support for all node DbEntities. Will use its
     * facilities provided by DbAdapter to generate any necessary database
     * objects and data for primary key support.
     */
    public void createPKSupport() throws Exception {
        for (DataMap map : dataMapsInSchemaSetupOrder) {
            createPKSupport(domain.lookupDataNode(map.getName()), map);
        }
    }

    // TODO: this is only needed for "map-db1" and "map-db2". Looks wasteful
    private List<DataMap> sortDataMapsInSchemaSetupOrder() {
        List<DataMap> maps = new ArrayList<>(project.dataMaps());
        Map<DataMap, List<DataMap>> dependencies = new IdentityHashMap<>();

        for (DataMap map : maps) {
            dependencies.put(map, dataMapDependencies(map, maps));
        }

        List<DataMap> sorted = new ArrayList<>(maps.size());
        Set<DataMap> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<DataMap> visiting = Collections.newSetFromMap(new IdentityHashMap<>());

        for (DataMap map : maps) {
            sortDataMap(map, dependencies, visited, visiting, sorted);
        }

        return sorted;
    }

    private List<DataMap> dataMapDependencies(DataMap map, List<DataMap> maps) {
        List<DataMap> dependencies = new ArrayList<>();

        for (DbEntity entity : map.getDbEntities()) {
            for (DbRelationship relationship : entity.getRelationships()) {
                DataMap targetMap = dataMapDependency(map, relationship);
                if (targetMap != null && maps.contains(targetMap) && !dependencies.contains(targetMap)) {
                    dependencies.add(targetMap);
                }
            }
        }

        return dependencies;
    }

    private DataMap dataMapDependency(DataMap sourceMap, DbRelationship relationship) {
        if (relationship.isRuntime() || !relationship.isForeignKey()) {
            return null;
        }

        boolean hasUnresolvedJoin = relationship.getJoins().stream()
                .anyMatch(join -> join.getSource() == null || join.getTarget() == null);
        if (hasUnresolvedJoin) {
            return null;
        }

        DbEntity targetEntity = relationship.getTargetEntity();
        DataMap targetMap = targetEntity != null ? targetEntity.getDataMap() : null;
        if (targetMap == null || targetMap == sourceMap) {
            return null;
        }

        if (domain.lookupDataNode(sourceMap.getName()) != domain.lookupDataNode(targetMap.getName())) {
            return null;
        }

        return targetMap;
    }

    private void sortDataMap(
            DataMap map,
            Map<DataMap, List<DataMap>> dependencies,
            Set<DataMap> visited,
            Set<DataMap> visiting,
            List<DataMap> sorted) {

        if (visited.contains(map)) {
            return;
        }

        if (!visiting.add(map)) {
            throw new IllegalStateException("Cycle in DataMap dependencies involving " + map.getName());
        }

        for (DataMap dependency : dependencies.getOrDefault(map, Collections.emptyList())) {
            sortDataMap(dependency, dependencies, visited, visiting, sorted);
        }

        visiting.remove(map);
        visited.add(map);
        sorted.add(map);
    }

    public List<CompiledTable> dbEntitiesInInsertOrder(String mapName) {
        return sortedDbEntities(mapName, false);
    }

    public List<CompiledTable> dbEntitiesInDeleteOrder(String mapName) {
        return sortedDbEntities(mapName, true);
    }

    private List<CompiledTable> sortedDbEntities(String mapName, boolean deleteOrder) {

        // intentionally taking "mapName", not a "map", as we need to resolve the corresponding map in our private
        // namespace defined by "domain"

        CompiledMapping mapping = domain.getMapping();
        CompiledDataMap localMap = compiledDataMap(dataMap(mapName));
        List<CompiledTable> localTables = new ArrayList<>();
        for (CompiledTable table : mapping.schema().tables()) {
            if (table.map() == localMap) {
                localTables.add(table);
            }
        }
        Set<CompiledTable> excluded = Set.copyOf(excludeEntities(localTables));

        // the dependency index is computed across all the maps of the domain and is the insert order; the name
        // makes the order deterministic within a dependency level
        List<CompiledTable> tables = new ArrayList<>(localTables.size());
        for (CompiledTable table : localTables) {
            if (!excluded.contains(table)) {
                tables.add(table);
            }
        }
        tables.sort(Comparator.comparingInt(CompiledTable::dependencyIndex).thenComparing(CompiledTable::name));

        if (deleteOrder) {
            Collections.reverse(tables);
        }
        return tables;
    }

    private List<CompiledTable> excludeEntities(Collection<CompiledTable> tables) {
        // exclude various unsupported tests...

        boolean excludeLOB = !testDbAdapter.supportsLobs();
        boolean excludeNativeJson = !testDbAdapter.supportsJsonType();
        boolean excludeBinaryPK = !testDbAdapter.supportsBinaryPK();
        if (!excludeLOB && !excludeNativeJson && !excludeBinaryPK) {
            return Collections.emptyList();
        }

        List<CompiledTable> excludedTables = new ArrayList<>();
        for (CompiledTable table : tables) {

            // check for LOB attributes
            if (excludeLOB) {
                if (EXTRA_EXCLUDED_FOR_NO_LOB.contains(table.name())) {
                    excludedTables.add(table);
                    continue;
                }
                Set<Integer> lobTypes = Set.of(Types.BLOB, Types.CLOB, Types.NCLOB);
                boolean hasLob = table.columns().stream()
                        .map(CompiledColumn::jdbcType)
                        .anyMatch(lobTypes::contains);
                if (hasLob) {
                    excludedTables.add(table);
                    continue;
                }
            }

            // check for native json type
            if (excludeNativeJson) {
                if (EXTRA_EXCLUDED_FOR_NO_NATIVE_JSON.contains(table.name())) {
                    excludedTables.add(table);
                    continue;
                }
            }

            if (excludeBinaryPK && hasBinaryKey(table)) {
                excludedTables.add(table);
            }
        }
        return excludedTables;
    }

    private void dropSchema(DataNode node, DataMap map) throws Exception {

        List<CompiledTable> list = dbEntitiesInInsertOrder(map.getName());

        try (Connection conn = dataSource.getConnection()) {

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

            testDbAdapter.willDropTables(conn, domain.getMapping(), compiledDataMap(map), allTables);

            // drop all tables in the map
            try (Statement stmt = conn.createStatement()) {

                ListIterator<CompiledTable> it = list.listIterator(list.size());
                while (it.hasPrevious()) {
                    CompiledTable table = it.previous();
                    if (!allTables.contains(table.name().toUpperCase())) {
                        continue;
                    }

                    for (String dropSql : node.getAdapter().dropTableStatements(table)) {
                        try {
                            LOGGER.info(dropSql);
                            stmt.execute(dropSql);
                        } catch (SQLException sqe) {
                            LOGGER.warn("Can't drop table {}, ignoring...", table.name(), sqe);
                        }
                    }
                }
            }
        }
    }

    private void dropPKSupport(DataNode node, DataMap map) throws Exception {
        List<CompiledTable> filteredTables = dbEntitiesInInsertOrder(map.getName());
        node.getPkGenerator().dropAutoPk(node, filteredTables);
    }

    private void createPKSupport(DataNode node, DataMap map) throws Exception {
        List<CompiledTable> filteredTables = dbEntitiesInInsertOrder(map.getName());
        node.getPkGenerator().createAutoPk(node, filteredTables);
    }

    private void createSchema(DataNode node, DataMap map) throws Exception {

        try (Connection conn = dataSource.getConnection()) {
            // the adapter may tweak the authored map for the target DB, so the DDL is generated from a fresh compile
            testDbAdapter.willCreateTables(conn, map);
            recompile();

            try (Statement stmt = conn.createStatement()) {

                for (String query : tableCreateQueries(node, map)) {
                    LOGGER.info(query);
                    stmt.execute(query);
                }
            }
            testDbAdapter.createdTables(conn, map);
        }
    }

    /**
     * Returns iterator of preprocessed table create queries.
     */
    private Collection<String> tableCreateQueries(DataNode node, DataMap map) {
        DbAdapter adapter = node.getAdapter();

        List<CompiledTable> orderedTables = dbEntitiesInInsertOrder(map.getName());
        List<String> queries = new ArrayList<>();

        // table definitions
        for (CompiledTable table : orderedTables) {
            queries.add(adapter.createTable(table));
        }

        // FK constraints
        for (CompiledTable table : orderedTables) {
            if (!testDbAdapter.supportsFKConstraints(table)) {
                continue;
            }

            queries.addAll(createConstraintsQueries(adapter, table));
        }

        return queries;
    }

    private List<String> createConstraintsQueries(DbAdapter adapter, CompiledTable table) {
        List<String> queries = new ArrayList<>();

        for (CompiledJoin join : table.joins()) {

            // only the FK side of a constraint declared as such in the authored model, and only within a map: the
            // reverse directions, the complementary relationships added by the compiler and the keys inferred by
            // the compiler for pairs flagged on both sides or neither are skipped
            if (!join.isDeclaredForeignKey()) {
                continue;
            }

            if (join.source().map() != join.target().map()) {
                continue;
            }

            // the compiler drops the joins referencing unknown columns, so a constraint may be left without columns
            if (join.columnPairs().isEmpty()) {
                continue;
            }

            if (adapter.supportsUniqueConstraints()) {
                // a one-to-one FK that is not a part of the source PK needs a unique index on the FK columns
                if (!join.reverse().isToMany() && !join.isIdentifying()) {
                    String unique = adapter.createUniqueConstraint(join.source(), join.sourceColumns());
                    if (unique != null) {
                        queries.add(unique);
                    }
                }
            }

            String fk = adapter.createFkConstraint(join);
            if (fk != null) {
                queries.add(fk);
            }
        }

        return queries;
    }

    /**
     * Returns the compiled map of the current domain mapping for an authored map.
     */
    private CompiledDataMap compiledDataMap(DataMap map) {
        CompiledMapping mapping = domain.getMapping();
        CompiledDataMap compiled = mapping.schema().map(map.getName());
        if (compiled == null) {
            throw new IllegalStateException("DataMap is not compiled: " + map.getName());
        }
        return compiled;
    }

    private void recompile() {
        domain.recompile(project.dataMaps());
    }

    private DataMap dataMap(String name) {
        DataMap dataMap = project.project().getDataMap(name);
        if (dataMap != null) {
            return dataMap;
        }

        for (DataMap candidate : project.dataMaps()) {
            if (candidate.getName().endsWith('/' + name)) {
                return candidate;
            }
        }

        throw new IllegalArgumentException("Unknown DataMap: " + name);
    }
}
