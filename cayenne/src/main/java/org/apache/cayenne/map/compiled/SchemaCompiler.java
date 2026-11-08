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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ashwood.graph.Digraph;
import org.apache.cayenne.ashwood.graph.IndegreeTopologicalSort;
import org.apache.cayenne.ashwood.graph.MapDigraph;
import org.apache.cayenne.ashwood.graph.StrongConnection;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbKeyGenerator;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Compiles the database side of DataMaps into a {@link CompiledSchema}: tables, columns, joins, foreign keys, key
 * sequences and procedures, with the dependency order of the tables. Anything the authored model allows compiles: a
 * missing reverse DB relationship is synthesized, a foreign key flag inconsistent with the multiplicity is logged and
 * the side holding the FK is inferred. The authored model is not modified, except that a DataMap with no namespace
 * gets one over the compiled set, as its cross-references can't be resolved otherwise.
 * <p>
 * The schema needs neither the persistent classes nor a DI container, so the tools that generate or compare a DB
 * schema compile with it. The {@link DefaultMappingCompiler} compiles the same schema and layers the entities on it.
 *
 * @since 6.0
 */
public final class SchemaCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaCompiler.class);
    private static final AtomicLong RUNTIME_RELATIONSHIP_COUNTER = new AtomicLong();

    private SchemaCompiler() {
    }

    public static CompiledSchema compile(Collection<DataMap> maps) {
        return new Build(maps).run();
    }

    /**
     * The state of one schema compile. The mapping compiler runs it first and reads the compiled tables and joins
     * back by their authored counterparts.
     */
    static final class Build {

        private final List<DataMap> authoredMaps;

        private final Map<DataMap, CompiledDataMap> maps;
        private final Map<DbEntity, CompiledTable> tables;
        private final Map<DbAttribute, CompiledColumn> columns;
        private final Map<DbRelationship, CompiledJoin> joins;
        private final List<CompiledProcedure> procedures;

        // the reverses synthesized for the DB relationships that have none, in either direction, and by the table
        // they would belong to. They are not added to the authored model, which the compiler does not modify
        private final Map<DbRelationship, DbRelationship> syntheticReverses;
        private final Set<DbRelationship> synthetic;
        private final Map<DbEntity, List<DbRelationship>> syntheticReversesByTable;

        Build(Collection<DataMap> maps) {
            this.authoredMaps = new ArrayList<>(maps);

            // the authored objects are keyed by identity, as the compiler does not rely on their equality
            this.maps = new IdentityHashMap<>();
            this.tables = new IdentityHashMap<>();
            this.columns = new IdentityHashMap<>();
            this.joins = new IdentityHashMap<>();
            this.procedures = new ArrayList<>();

            this.syntheticReverses = new IdentityHashMap<>();
            this.synthetic = Collections.newSetFromMap(new IdentityHashMap<>());
            this.syntheticReversesByTable = new IdentityHashMap<>();
        }

        CompiledSchema run() {
            if (authoredMaps.isEmpty()) {
                return CompiledSchema.empty();
            }

            // the authored model resolves its cross-references through the namespace of each map. A map loaded on
            // its own has none, and gets one over the whole set; a map that has one, e.g. a map of a project, keeps it
            EntityResolver namespace = null;
            for (DataMap map : authoredMaps) {
                if (map.getNamespace() == null) {
                    if (namespace == null) {
                        namespace = new EntityResolver(authoredMaps);
                    }
                    map.setNamespace(namespace);
                }
            }
            synthesizeReverseDbRelationships();

            for (DataMap map : authoredMaps) {
                maps.put(map, new CompiledDataMap(map.getName(), map.isQuotingSQLIdentifiers()));
            }
            for (DataMap map : authoredMaps) {
                for (DbEntity table : map.getDbEntities()) {
                    compileTable(maps.get(map), table);
                }
            }
            compileDbRelationships();
            for (DataMap map : authoredMaps) {
                for (Procedure procedure : map.getProcedures()) {
                    procedures.add(compileProcedure(maps.get(map), procedure));
                }
            }
            return schema();
        }

        // ---- lookups for the mapping compiler

        CompiledDataMap map(DataMap map) {
            return maps.get(map);
        }

        CompiledTable table(DbEntity table) {
            return tables.get(table);
        }

        CompiledJoin join(DbRelationship relationship) {
            return joins.get(relationship);
        }

        Collection<CompiledTable> tables() {
            return tables.values();
        }

        // ---- DB side

        /**
         * Creates a reverse for every DB relationship that has none, so that every compiled relationship can be
         * traversed both ways. The reverses are kept by the compiler, not added to the authored model.
         */
        private void synthesizeReverseDbRelationships() {
            for (DataMap map : authoredMaps) {
                for (DbEntity entity : map.getDbEntities()) {
                    for (DbRelationship relationship : entity.getRelationships()) {
                        if (relationship.getTargetEntity() == null || relationship.getJoins().isEmpty()
                                || relationship.getReverseRelationship() != null) {
                            continue;
                        }
                        DbRelationship reverse = relationship.createReverseRelationship();
                        DbEntity targetEntity = reverse.getSourceEntity();
                        reverse.setName(uniqueRelationshipName(targetEntity));
                        reverse.setRuntime(true);
                        syntheticReverses.put(relationship, reverse);
                        syntheticReverses.put(reverse, relationship);
                        synthetic.add(reverse);
                        syntheticReversesByTable.computeIfAbsent(targetEntity, k -> new ArrayList<>()).add(reverse);
                        LOGGER.info("synthesized complementary runtime DbRelationship '{}' from {} to {}",
                                reverse.getName(), targetEntity.getName(), reverse.getTargetEntityName());
                    }
                }
            }
        }

        private String uniqueRelationshipName(DbEntity entity) {
            List<DbRelationship> synthesized = syntheticReversesByTable.getOrDefault(entity, List.of());
            String name;
            do {
                name = "runtimeRelationship" + RUNTIME_RELATIONSHIP_COUNTER.getAndIncrement();
            } while (entity.getRelationship(name) != null || hasName(synthesized, name));
            return name;
        }

        private static boolean hasName(List<DbRelationship> relationships, String name) {
            for (DbRelationship relationship : relationships) {
                if (name.equals(relationship.getName())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the reverse of a DB relationship: the authored one, or the one synthesized by the compiler.
         */
        private DbRelationship reverseOf(DbRelationship relationship) {
            DbRelationship reverse = relationship.getReverseRelationship();
            return reverse != null ? reverse : syntheticReverses.get(relationship);
        }

        /**
         * Returns the authored relationships of a table followed by the synthesized reverses that belong to it.
         */
        private List<DbRelationship> relationshipsOf(DbEntity table) {
            List<DbRelationship> synthesized = syntheticReversesByTable.get(table);
            if (synthesized == null) {
                return new ArrayList<>(table.getRelationships());
            }
            List<DbRelationship> all = new ArrayList<>(table.getRelationships());
            all.addAll(synthesized);
            return all;
        }

        private void compileTable(CompiledDataMap map, DbEntity table) {
            List<CompiledColumn> tableColumns = new ArrayList<>(table.getAttributes().size());
            Map<String, CompiledColumn> columnsByName = new LinkedHashMap<>();
            for (DbAttribute attribute : table.getAttributes()) {
                CompiledColumn column = new CompiledColumn(attribute.getName(), attribute.getType(),
                        attribute.getMaxLength(), attribute.getScale(), attribute.getAttributePrecision(),
                        attribute.isMandatory(), attribute.isPrimaryKey(), attribute.isGenerated(),
                        TypesMapping.getJavaBySqlType(attribute));
                columns.put(attribute, column);
                tableColumns.add(column);
                columnsByName.put(column.name(), column);
            }
            List<CompiledColumn> pk = new ArrayList<>(table.getPrimaryKeys().size());
            for (DbAttribute attribute : table.getPrimaryKeys()) {
                pk.add(columns.get(attribute));
            }
            List<CompiledColumn> generated = new ArrayList<>(table.getGeneratedAttributes().size());
            for (DbAttribute attribute : table.getGeneratedAttributes()) {
                generated.add(columns.get(attribute));
            }
            DbKeyGenerator generator = table.getPrimaryKeyGenerator();
            CompiledKeySequence keySequence = generator != null
                    ? new CompiledKeySequence(generator.getGeneratorType(), generator.getGeneratorName(),
                    generator.getKeyCacheSize())
                    : null;

            CompiledTable compiled = new CompiledTable(map, table.getName(), table.getCatalog(),
                    table.getSchema(), table.getTableAliasBase(), table.getQualifier(), tableColumns,
                    columnsByName, pk, generated, keySequence);
            tables.put(table, compiled);
        }

        private void compileDbRelationships() {
            for (DbEntity table : tables.keySet()) {
                for (DbRelationship relationship : relationshipsOf(table)) {
                    if (!joins.containsKey(relationship)) {
                        compileDbRelationshipPair(relationship);
                    }
                }
            }
            // index each table's relationships in the authored order
            for (Map.Entry<DbEntity, CompiledTable> e : tables.entrySet()) {
                Map<String, CompiledJoin> byName = new LinkedHashMap<>();
                List<CompiledForeignKey> foreignKeys = new ArrayList<>();
                List<CompiledJoin> reflexive = new ArrayList<>();
                for (DbRelationship relationship : relationshipsOf(e.getKey())) {
                    CompiledJoin compiled = joins.get(relationship);
                    if (compiled == null) {
                        continue;
                    }
                    byName.put(compiled.name(), compiled);
                    if (compiled.isForeignKey()) {
                        foreignKeys.add(compiled.foreignKey());
                        if (compiled.isReflexive()) {
                            reflexive.add(compiled);
                        }
                    }
                }
                CompiledTable compiledTable = e.getValue();
                compiledTable.joins = byName;
                compiledTable.foreignKeys = foreignKeys;
                compiledTable.reflexiveJoins = reflexive;
            }
        }

        private void compileDbRelationshipPair(DbRelationship relationship) {
            DbEntity source = relationship.getSourceEntity();
            DbEntity target = relationship.getTargetEntity();
            if (target == null) {
                LOGGER.warn("DbRelationship '{}.{}' has no target entity '{}', skipping", source.getName(),
                        relationship.getName(), relationship.getTargetEntityName());
                return;
            }
            if (!tables.containsKey(target)) {
                // a partial compile, e.g. of one map of a project: the relationship can't be traversed
                LOGGER.warn("DbRelationship '{}.{}' targets '{}' outside of the compiled DataMaps, skipping",
                        source.getName(), relationship.getName(), target.getName());
                return;
            }
            if (relationship.getJoins().isEmpty()) {
                LOGGER.warn("DbRelationship '{}.{}' has no joins, skipping", source.getName(),
                        relationship.getName());
                return;
            }
            DbRelationship reverse = reverseOf(relationship);
            if (reverse == null) {
                throw new CayenneRuntimeException("No reverse DbRelationship for '%s.%s'", source.getName(),
                        relationship.getName());
            }

            DbRelationship fkSide = foreignKeySide(relationship, reverse);
            DbRelationship otherSide = fkSide == relationship ? reverse : relationship;
            // the authored flags decide whether the FK side is declared. A synthesized reverse carries a derived
            // flag, so with one, the declaration is the authored side's flag alone
            boolean declared;
            if (relationship == reverse) {
                declared = relationship.isForeignKey();
            } else if (synthetic.contains(reverse)) {
                declared = relationship.isForeignKey();
            } else if (synthetic.contains(relationship)) {
                declared = reverse.isForeignKey();
            } else {
                declared = relationship.isForeignKey() != reverse.isForeignKey();
            }
            CompiledForeignKey foreignKey = compileForeignKey(fkSide, declared);

            CompiledJoin forward = new CompiledJoin(fkSide.getName(), foreignKey, true,
                    fkSide.isToMany());
            if (fkSide.isToMany()) {
                LOGGER.warn("DbRelationship '{}.{}' holds the foreign key, but is mapped as to-many",
                        fkSide.getSourceEntityName(), fkSide.getName());
            }
            joins.put(fkSide, forward);

            if (otherSide == fkSide) {
                // a symmetric reflexive relationship is its own reverse
                forward.reverse = forward;
                return;
            }

            boolean reverseToMany = otherSide.isToMany();
            if (reverseToMany == foreignKey.sourceKey()) {
                LOGGER.warn("DbRelationship '{}.{}' is mapped as {}, but its foreign key columns {} the source "
                                + "primary key", otherSide.getSourceEntityName(), otherSide.getName(),
                        reverseToMany ? "to-many" : "to-one", foreignKey.sourceKey() ? "are" : "are not");
            }
            CompiledJoin backward = new CompiledJoin(otherSide.getName(), foreignKey, false,
                    reverseToMany);
            forward.reverse = backward;
            backward.reverse = forward;
            joins.put(otherSide, backward);
        }

        /**
         * Picks the side of a relationship pair that holds the foreign key. The "foreignKey" flag decides when it is
         * set on exactly one side. Otherwise the to-one side holds it, and for a one-to-one the side whose join
         * columns are not all primary key columns, or failing that the first side in name order.
         */
        private DbRelationship foreignKeySide(DbRelationship relationship, DbRelationship reverse) {
            if (relationship == reverse) {
                return relationship;
            }
            if (relationship.isForeignKey() != reverse.isForeignKey()) {
                return relationship.isForeignKey() ? relationship : reverse;
            }
            DbRelationship fkSide;
            if (relationship.isToMany() != reverse.isToMany()) {
                fkSide = relationship.isToMany() ? reverse : relationship;
            } else {
                boolean sourcePk = allPrimaryKeys(relationship.getSourceAttributes());
                boolean reverseSourcePk = allPrimaryKeys(reverse.getSourceAttributes());
                if (sourcePk != reverseSourcePk) {
                    fkSide = sourcePk ? reverse : relationship;
                } else {
                    String key1 = relationship.getSourceEntityName() + "." + relationship.getName();
                    String key2 = reverse.getSourceEntityName() + "." + reverse.getName();
                    fkSide = key1.compareTo(key2) <= 0 ? relationship : reverse;
                }
            }
            LOGGER.warn("DbRelationship '{}.{}' and its reverse '{}.{}' are {} marked as the foreign key, "
                            + "assuming '{}.{}' holds it", relationship.getSourceEntityName(),
                    relationship.getName(), reverse.getSourceEntityName(), reverse.getName(),
                    relationship.isForeignKey() ? "both" : "neither", fkSide.getSourceEntityName(),
                    fkSide.getName());
            return fkSide;
        }

        private static boolean allPrimaryKeys(Collection<DbAttribute> attributes) {
            for (DbAttribute attribute : attributes) {
                if (attribute == null || !attribute.isPrimaryKey()) {
                    return false;
                }
            }
            return true;
        }

        private CompiledForeignKey compileForeignKey(DbRelationship fkSide, boolean declared) {
            CompiledTable source = tables.get(fkSide.getSourceEntity());
            CompiledTable target = tables.get(fkSide.getTargetEntity());
            List<CompiledColumnPair> pairs = new ArrayList<>(fkSide.getJoins().size());
            for (DbJoin join : fkSide.getJoins()) {
                CompiledColumn sourceColumn = columns.get(join.getSource());
                CompiledColumn targetColumn = columns.get(join.getTarget());
                if (sourceColumn == null || targetColumn == null) {
                    // tolerated as long as the relationship is never used, as it was in 5.0
                    LOGGER.warn("DbRelationship '{}.{}' has an invalid join '{} = {}', skipping the join",
                            fkSide.getSourceEntityName(), fkSide.getName(), join.getSourceName(),
                            join.getTargetName());
                    continue;
                }
                pairs.add(new CompiledColumnPair(sourceColumn, targetColumn));
            }

            boolean toPrimaryKey = coversPrimaryKey(target, pairs, false);
            boolean sourceKey = coversPrimaryKey(source, pairs, true);
            boolean identifying = !pairs.isEmpty();
            boolean reflexive = false;
            for (CompiledColumnPair pair : pairs) {
                identifying &= pair.source().isPrimaryKey();
                reflexive |= source == target && pair.target().isPrimaryKey();
            }
            return new CompiledForeignKey(source, target, pairs, toPrimaryKey, identifying, sourceKey,
                    reflexive, declared);
        }

        /**
         * Returns true if the source (or target) columns of the pairs are exactly the primary key of the table.
         */
        private static boolean coversPrimaryKey(CompiledTable table, List<CompiledColumnPair> pairs,
                                                boolean sourceSide) {
            List<CompiledColumn> pk = table.primaryKey();
            if (pk.isEmpty() || pk.size() != pairs.size()) {
                return false;
            }
            Set<CompiledColumn> covered = Collections.newSetFromMap(new IdentityHashMap<>());
            for (CompiledColumnPair pair : pairs) {
                CompiledColumn column = sourceSide ? pair.source() : pair.target();
                if (!column.isPrimaryKey() || !covered.add(column)) {
                    return false;
                }
            }
            return true;
        }

        // ---- procedures

        private static CompiledProcedure compileProcedure(CompiledDataMap map, Procedure procedure) {
            List<CompiledProcedureParameter> parameters = new ArrayList<>(procedure.getCallParameters().size());
            for (ProcedureParameter parameter : procedure.getCallParameters()) {
                parameters.add(new CompiledProcedureParameter(parameter.getName(), parameter.getType(),
                        direction(parameter), parameter.getMaxLength(), parameter.getPrecision()));
            }
            return new CompiledProcedure(map, procedure.getName(), procedure.getCatalog(), procedure.getSchema(),
                    procedure.isReturningValue(), parameters);
        }

        private static int direction(ProcedureParameter parameter) {
            return switch (parameter.getDirection()) {
                case ProcedureParameter.IN_PARAMETER -> CompiledProcedureParameter.IN_PARAMETER;
                case ProcedureParameter.OUT_PARAMETER -> CompiledProcedureParameter.OUT_PARAMETER;
                case ProcedureParameter.IN_OUT_PARAMETER -> CompiledProcedureParameter.IN_OUT_PARAMETER;
                default -> throw new CayenneRuntimeException("Unknown direction of procedure parameter '%s': %d",
                        parameter.getName(), parameter.getDirection());
            };
        }

        // ---- schema

        private CompiledSchema schema() {
            Map<String, CompiledDataMap> mapsByName = new LinkedHashMap<>();
            Map<String, CompiledTable> tablesByName = new LinkedHashMap<>();
            Map<String, CompiledProcedure> proceduresByName = new LinkedHashMap<>();

            // in the order of the maps, and of the declarations within a map
            for (DataMap map : authoredMaps) {
                CompiledDataMap compiled = maps.get(map);
                mapsByName.put(compiled.name(), compiled);
                for (DbEntity authored : map.getDbEntities()) {
                    CompiledTable table = tables.get(authored);
                    warnDuplicate(tablesByName.put(table.name(), table), "DbEntity", table.name());
                }
            }
            for (CompiledProcedure procedure : procedures) {
                warnDuplicate(proceduresByName.put(procedure.name(), procedure), "Procedure", procedure.name());
            }

            assignDependencyIndexes(tablesByName.values());
            return new CompiledSchema(mapsByName, tablesByName, proceduresByName);
        }

        private static void warnDuplicate(Object previous, String kind, String name) {
            if (previous != null) {
                LOGGER.warn("Found duplicated {} name '{}' in the DataMaps", kind, name);
            }
        }

        /**
         * Assigns each table its {@link CompiledTable#dependencyIndex() dependency index}: 0 for a table
         * referencing no other table by a foreign key to its primary key, otherwise one more than the deepest table
         * it references. The tables of a dependency cycle form one strongly connected component and share the index.
         */
        private void assignDependencyIndexes(Collection<CompiledTable> allTables) {
            // an arc from a referenced table to the table holding the FK, for FKs to primary keys
            Digraph<CompiledTable, List<CompiledColumn>> referentialDigraph = new MapDigraph<>();
            Map<CompiledTable, Set<CompiledTable>> referencedTables = new IdentityHashMap<>();
            for (CompiledTable table : allTables) {
                referentialDigraph.addVertex(table);
            }
            for (CompiledTable destination : allTables) {
                for (CompiledForeignKey foreignKey : destination.foreignKeys()) {
                    CompiledTable origin = foreignKey.target();
                    for (CompiledColumnPair pair : foreignKey.columnPairs()) {
                        if (pair.target().isPrimaryKey()) {
                            List<CompiledColumn> fks = referentialDigraph.getArc(origin, destination);
                            if (fks == null) {
                                fks = new ArrayList<>();
                                referentialDigraph.putArc(origin, destination, fks);
                            }
                            fks.add(pair.target());
                            referencedTables.computeIfAbsent(destination,
                                    k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(origin);
                        }
                    }
                }
            }

            // the strongly connected components in topological order
            StrongConnection<CompiledTable, List<CompiledColumn>> contractor =
                    new StrongConnection<>(referentialDigraph);
            Digraph<Collection<CompiledTable>, Collection<List<CompiledColumn>>> contracted =
                    new MapDigraph<>();
            contractor.contract(contracted);
            IndegreeTopologicalSort<Collection<CompiledTable>> sorter = new IndegreeTopologicalSort<>(contracted);
            List<Collection<CompiledTable>> components = new ArrayList<>();
            Map<CompiledTable, Integer> componentOf = new IdentityHashMap<>();
            while (sorter.hasNext()) {
                Collection<CompiledTable> component = sorter.next();
                for (CompiledTable table : component) {
                    componentOf.put(table, components.size());
                }
                components.add(component);
            }

            // the level of a component is one more than the deepest component it references
            int[] levels = new int[components.size()];
            for (int i = 0; i < components.size(); i++) {
                int level = 0;
                for (CompiledTable table : components.get(i)) {
                    for (CompiledTable referenced : referencedTables.getOrDefault(table, Set.of())) {
                        int j = componentOf.get(referenced);
                        if (j != i) {
                            level = Math.max(level, levels[j] + 1);
                        }
                    }
                }
                levels[i] = level;
                for (CompiledTable table : components.get(i)) {
                    table.dependencyIndex = level;
                }
            }
        }
    }
}
