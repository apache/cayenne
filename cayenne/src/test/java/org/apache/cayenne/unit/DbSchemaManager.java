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
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.runtime.CayenneRuntime;
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
    private final List<DataMap> dataMapsInSchemaSetupOrder;

    public DbSchemaManager(String project, DataSourceDescriptor dataSourceDescriptor, DataSource dataSource) {

        this.dataSourceDescriptor = dataSourceDescriptor;
        this.dataSource = dataSource;
        this.domain = CayenneRuntime.builder()
                .addConfig(project)
                .dataSource(dataSource)
                .build()
                .getDataDomain();

        this.testDbAdapter = TestDbAdapter.of(domain.getDefaultNode().getAdapter());

        for (DataMap map : domain.getDataMaps()) {
            // tweak mapping with a delegate
            for (Procedure proc : map.getProcedures()) {
                testDbAdapter.tweakProcedure(proc);
            }

            filterDataMap(map);
        }

        // TODO: suspect
        domain.getEntitySorter().setEntityResolver(domain.getEntityResolver());

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

    private void filterDataMap(DataMap map) {
        boolean supportsBinaryPK = testDbAdapter.supportsBinaryPK();

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

    /**
     * Drops all test tables.
     */
    private void dropSchema() throws Exception {
        ListIterator<DataMap> it = dataMapsInSchemaSetupOrder.listIterator(dataMapsInSchemaSetupOrder.size());
        while (it.hasPrevious()) {
            DataMap map = it.previous();
            dropSchema(domain.lookupDataNode(map), map);
        }
    }

    /**
     * Creates all test tables in the database.
     */
    private void createSchema() throws Exception {
        for (DataMap map : dataMapsInSchemaSetupOrder) {
            createSchema(domain.lookupDataNode(map), map);
        }
    }

    public void dropPKSupport() throws Exception {
        for (DataMap map : dataMapsInSchemaSetupOrder) {
            dropPKSupport(domain.lookupDataNode(map), map);
        }
    }

    /**
     * Creates primary key support for all node DbEntities. Will use its
     * facilities provided by DbAdapter to generate any necessary database
     * objects and data for primary key support.
     */
    public void createPKSupport() throws Exception {
        for (DataMap map : dataMapsInSchemaSetupOrder) {
            createPKSupport(domain.lookupDataNode(map), map);
        }
    }

    // TODO: this is only needed for "map-db1" and "map-db2". Looks wasteful
    private List<DataMap> sortDataMapsInSchemaSetupOrder() {
        List<DataMap> maps = new ArrayList<>(domain.getDataMaps());
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
        if (relationship.isRuntime() || relationship.isToMany() || !relationship.isToPK() || relationship.isToDependentPK()) {
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

        if (domain.lookupDataNode(sourceMap) != domain.lookupDataNode(targetMap)) {
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

    public List<DbEntity> dbEntitiesInInsertOrder(String mapName) {
        return sortedDbEntities(mapName, false);
    }

    public List<DbEntity> dbEntitiesInDeleteOrder(String mapName) {
        return sortedDbEntities(mapName, true);
    }

    private List<DbEntity> sortedDbEntities(String mapName, boolean deleteOrder) {

        // intentionally taking "mapName", not a "map", as we need to resolve the corresponding map in our private
        // namespace defined by "domain"

        DataMap localMap = dataMap(mapName);
        List<DbEntity> entities = new ArrayList<>(localMap.getDbEntities());
        entities.removeAll(excludeEntities(entities));

        // Deterministic tiebreaker for entities AshwoodEntitySorter cannot distinguish
        // (members of a strongly connected component compare as equal).
        // List.sort is stable, so this name order survives sortDbEntities
        // for any pair the comparator returns 0 for.
        // TODO:
        //  1. this should really be fixed in AshwoodEntitySorter that should have a deterministic tiebreaker
        //  2. Also, alpha order of entities only works by incident. It may likely break on different combinations
        //  of circular relationships
        entities.sort(Comparator.comparing(DbEntity::getName));

        domain.getEntitySorter().sortDbEntities(entities, deleteOrder);
        return entities;
    }

    private List<DbEntity> excludeEntities(Collection<DbEntity> entities) {
        // exclude various unsupported tests...

        boolean excludeLOB = !testDbAdapter.supportsLobs();
        boolean excludeNativeJson = !testDbAdapter.supportsJsonType();
        boolean excludeBinaryPK = !testDbAdapter.supportsBinaryPK();
        if (!excludeLOB && !excludeNativeJson && !excludeBinaryPK) {
            return Collections.emptyList();
        }

        List<DbEntity> excludedEntities = new ArrayList<>();
        for (DbEntity entity : entities) {

            // check for LOB attributes
            if (excludeLOB) {
                if (EXTRA_EXCLUDED_FOR_NO_LOB.contains(entity.getName())) {
                    excludedEntities.add(entity);
                    continue;
                }
                Set<Integer> lobTypes = Set.of(Types.BLOB, Types.CLOB, Types.NCLOB);
                boolean hasLob = entity.getAttributes().stream()
                        .map(DbAttribute::getType)
                        .anyMatch(lobTypes::contains);
                if (hasLob) {
                    excludedEntities.add(entity);
                    continue;
                }
            }

            // check for native json type
            if (excludeNativeJson) {
                if (EXTRA_EXCLUDED_FOR_NO_NATIVE_JSON.contains(entity.getName())) {
                    excludedEntities.add(entity);
                    continue;
                }
            }

            // check for BIN PK
            if (excludeBinaryPK) {
                Set<Integer> binaryTypes = Set.of(Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY);
                boolean hasBinaryPK = entity.getAttributes().stream()
                        .filter(attribute -> attribute.isPrimaryKey() || attribute.isForeignKey())
                        .map(DbAttribute::getType)
                        .anyMatch(binaryTypes::contains);
                if (hasBinaryPK) {
                    excludedEntities.add(entity);
                }
            }
        }
        return excludedEntities;
    }

    private void dropSchema(DataNode node, DataMap map) throws Exception {

        List<DbEntity> list = dbEntitiesInInsertOrder(map.getName());

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

            testDbAdapter.willDropTables(conn, map, allTables);

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
                            LOGGER.info(dropSql);
                            stmt.execute(dropSql);
                        } catch (SQLException sqe) {
                            LOGGER.warn("Can't drop table " + ent.getName() + ", ignoring...", sqe);
                        }
                    }
                }
            }
        }
    }

    private void dropPKSupport(DataNode node, DataMap map) throws Exception {
        List<DbEntity> filteredEntities = dbEntitiesInInsertOrder(map.getName());
        node.getAdapter().getPkGenerator().dropAutoPk(node, filteredEntities);
    }

    private void createPKSupport(DataNode node, DataMap map) throws Exception {
        List<DbEntity> filteredEntities = dbEntitiesInInsertOrder(map.getName());
        node.getAdapter().getPkGenerator().createAutoPk(node, filteredEntities);
    }

    private void createSchema(DataNode node, DataMap map) throws Exception {

        try (Connection conn = dataSource.getConnection()) {
            testDbAdapter.willCreateTables(conn, map);
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

        List<DbEntity> orderedEntities = dbEntitiesInInsertOrder(map.getName());
        List<String> queries = new ArrayList<>();

        // table definitions
        for (DbEntity ent : orderedEntities) {
            queries.add(adapter.createTable(ent));
        }

        // FK constraints
        for (DbEntity ent : orderedEntities) {
            if (!testDbAdapter.supportsFKConstraints(ent)) {
                continue;
            }

            queries.addAll(createConstraintsQueries(adapter, ent));
        }

        return queries;
    }

    private List<String> createConstraintsQueries(DbAdapter adapter, DbEntity table) {
        List<String> queries = new ArrayList<>();

        for (DbRelationship rel : table.getRelationships()) {

            if (rel.isRuntime()) {
                continue;
            }

            if (rel.isToMany()) {
                continue;
            }

            DataMap srcMap = rel.getSourceEntity().getDataMap();
            DataMap targetMap = rel.getTargetEntity().getDataMap();

            if (srcMap != null && targetMap != null && srcMap != targetMap) {
                continue;
            }

            if (rel.isToPK() && !rel.isToDependentPK()) {
                boolean hasUnresolvedJoin = rel.getJoins().stream()
                        .anyMatch(join -> join.getSource() == null || join.getTarget() == null);

                if (hasUnresolvedJoin) {
                    continue;
                }

                if (adapter.supportsUniqueConstraints()) {
                    DbRelationship reverse = rel.getReverseRelationship();
                    if (reverse != null && !reverse.isToMany() && !reverse.isToPK()) {
                        String unique = adapter.createUniqueConstraint(rel.getSourceEntity(), rel.getSourceAttributes());
                        if (unique != null) {
                            queries.add(unique);
                        }
                    }
                }

                String fk = adapter.createFkConstraint(rel);
                if (fk != null) {
                    queries.add(fk);
                }
            }
        }

        return queries;
    }

    private DataMap dataMap(String name) {
        DataMap dataMap = domain.getDataMap(name);
        if (dataMap != null) {
            return dataMap;
        }

        for (DataMap candidate : domain.getDataMaps()) {
            if (candidate.getName().endsWith('/' + name)) {
                return candidate;
            }
        }

        throw new IllegalArgumentException("Unknown DataMap: " + name);
    }
}
