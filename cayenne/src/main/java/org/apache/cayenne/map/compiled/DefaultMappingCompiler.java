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
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.access.AttributeFault;
import org.apache.cayenne.access.ToManyListFault;
import org.apache.cayenne.access.ToManyMapFault;
import org.apache.cayenne.access.ToManySetFault;
import org.apache.cayenne.access.ToOneFault;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.AndExp;
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.exp.EqualExp;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.NamedProcedureQuery;
import org.apache.cayenne.map.NamedQuery;
import org.apache.cayenne.map.NamedSQLQuery;
import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.Converter;
import org.apache.cayenne.reflect.ConverterFactory;
import org.apache.cayenne.reflect.EmbeddedFieldAccessor;
import org.apache.cayenne.reflect.FieldAccessor;
import org.apache.cayenne.reflect.IdMapKeyAccessor;
import org.apache.cayenne.reflect.PersistentAccessor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategy;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The default {@link MappingCompiler}. Builds the compiled model in dependency order: the schema (see
 * {@link SchemaCompiler}), then entities super before sub, then their relationships and attributes, and finally links
 * the late-bound facts that depend on the whole graph. Anything the authored model allows still compiles, as it does
 * for the schema.
 * <p>
 * The persistent classes are loaded and the property accessors created, so a mapping error fails the startup rather
 * than the first use. A class missing from the classpath is tolerated with a warning, and its entity fails on the
 * first attempt to create or access its objects. Tooling that needs the tables alone compiles a {@link CompiledSchema}
 * with the {@link SchemaCompiler} instead.
 *
 * @since 6.0
 */
public class DefaultMappingCompiler implements MappingCompiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMappingCompiler.class);

    private final ValueComparisonStrategyFactory comparisonStrategyFactory;
    private final AdhocObjectFactory objectFactory;

    public DefaultMappingCompiler(
            @Inject ValueComparisonStrategyFactory comparisonStrategyFactory,
            @Inject AdhocObjectFactory objectFactory) {
        this.comparisonStrategyFactory = comparisonStrategyFactory;
        this.objectFactory = objectFactory;
    }

    @Override
    public CompiledMapping compile(Collection<DataMap> maps) {
        return new Build(maps).run();
    }

    private final class Build {

        private static final CompiledDbPath UNRESOLVED_DB_PATH = new CompiledDbPath(List.of(), null);

        private final List<DataMap> authoredMaps;
        private final SchemaCompiler.Build schemaBuild;

        private final Map<Embeddable, CompiledEmbeddable> embeddables;
        private final Map<ObjEntity, CompiledEntity> entities;
        private final Map<ObjRelationship, CompiledRelationship> objRelationships;
        private final Map<CompiledToManyRelationship, DeferredAccessor> mapKeyAccessors;

        // entities in compile order: super before sub
        private final List<CompiledEntity> entityOrder;

        Build(Collection<DataMap> maps) {
            this.authoredMaps = new ArrayList<>(maps);
            this.schemaBuild = new SchemaCompiler.Build(this.authoredMaps);

            // the authored objects are keyed by identity, as the compiler does not rely on their equality
            this.embeddables = new IdentityHashMap<>();
            this.entities = new LinkedHashMap<>();
            this.objRelationships = new IdentityHashMap<>();
            this.mapKeyAccessors = new IdentityHashMap<>();

            this.entityOrder = new ArrayList<>();
        }

        CompiledMapping run() {
            if (authoredMaps.isEmpty()) {
                return CompiledMapping.empty();
            }

            CompiledSchema schema = schemaBuild.run();

            for (DataMap map : authoredMaps) {
                for (Embeddable embeddable : map.getEmbeddables()) {
                    compileEmbeddable(embeddable);
                }
            }
            for (DataMap map : authoredMaps) {
                for (ObjEntity entity : map.getObjEntities()) {
                    compileEntity(entity);
                }
            }
            for (CompiledEntity entity : entityOrder) {
                compileRelationships(entity);
            }
            for (CompiledEntity entity : entityOrder) {
                compileAttributes(entity);
            }
            link();
            return mapping(schema);
        }

        // ---- embeddables

        /**
         * Returns the values by name, iterating in the order of the names, so that the properties of an entity come
         * in a predictable order regardless of the order they are declared in. The list is sorted in place.
         */
        private static <T> Map<String, T> byName(List<T> values, Function<T, String> name) {
            values.sort(Comparator.comparing(name));
            Map<String, T> result = LinkedHashMap.newLinkedHashMap(values.size());
            for (T value : values) {
                result.put(name.apply(value), value);
            }
            return result;
        }

        private void compileEmbeddable(Embeddable embeddable) {
            Class<?> objectClass = loadClass(embeddable.getClassName());
            List<CompiledEmbeddableAttribute> attributes = new ArrayList<>();
            for (EmbeddableAttribute attribute : embeddable.getAttributes()) {
                attributes.add(new CompiledEmbeddableAttribute(attribute.getName(), attribute.getType(),
                        attribute.getDbAttributeName()));
            }
            embeddables.put(embeddable, new CompiledEmbeddable(embeddable.getClassName(), objectClass,
                    byName(attributes, CompiledEmbeddableAttribute::name)));
        }

        private static int lockType(int authoredLockType) {
            return switch (authoredLockType) {
                case ObjEntity.LOCK_TYPE_NONE -> CompiledEntity.LOCK_TYPE_NONE;
                case ObjEntity.LOCK_TYPE_OPTIMISTIC -> CompiledEntity.LOCK_TYPE_OPTIMISTIC;
                default -> throw new CayenneRuntimeException("Unknown lock type: %d", authoredLockType);
            };
        }

        // ---- object side

        private CompiledEntity compileEntity(ObjEntity entity) {
            CompiledEntity compiled = entities.get(entity);
            if (compiled != null) {
                return compiled;
            }
            CompiledEntity superEntity = null;
            if (entity.getSuperEntityName() != null) {
                ObjEntity authoredSuper = entity.getSuperEntity();
                if (authoredSuper == null) {
                    LOGGER.warn("No super entity mapping for '{}'", entity.getSuperEntityName());
                } else {
                    superEntity = compileEntity(authoredSuper);
                }
            }
            DbEntity dbEntity = entity.getDbEntity();
            CompiledTable table = dbEntity != null ? schemaBuild.table(dbEntity) : null;
            Class<?> objectClass = loadClass(entity.getJavaClassName());

            compiled = new CompiledEntity(schemaBuild.map(entity.getDataMap()), entity.getName(), table, superEntity,
                    objectClass, entity.isAbstract(), entity.isReadOnly(), entity.isGeneric(),
                    lockType(entity.getLockType()), lockType(entity.getDeclaredLockType()),
                    entity.getDeclaredQualifier());
            entities.put(entity, compiled);
            entityOrder.add(compiled);
            return compiled;
        }

        private void compileRelationships(CompiledEntity source) {
            ObjEntity entity = authored(source);
            List<CompiledRelationship> declared = new ArrayList<>();
            for (ObjRelationship relationship : entity.getDeclaredRelationships()) {
                CompiledRelationship compiled = compileRelationship(source, relationship);
                if (compiled != null) {
                    objRelationships.put(relationship, compiled);
                    declared.add(compiled);
                }
            }
            source.declaredRelationships = byName(declared, CompiledRelationship::name);
        }

        private CompiledRelationship compileRelationship(CompiledEntity source, ObjRelationship relationship) {
            ObjEntity authoredTarget = relationship.getTargetEntity();
            CompiledEntity target = authoredTarget != null ? entities.get(authoredTarget) : null;
            if (target == null) {
                // tolerated as long as the relationship is never used, as it was in 5.0; also the case of a partial
                // compile, e.g. of one map of a project
                LOGGER.warn("ObjRelationship '{}.{}' has no target entity '{}' in the compiled DataMaps, skipping",
                        source.name(), relationship.getName(), relationship.getTargetEntityName());
                return null;
            }
            List<CompiledJoin> dbPath = new ArrayList<>(relationship.getDbRelationships().size());
            for (DbRelationship dbRelationship : relationship.getDbRelationships()) {
                CompiledJoin compiled = schemaBuild.join(dbRelationship);
                if (compiled == null) {
                    LOGGER.warn("ObjRelationship '{}.{}' maps to a DbRelationship '{}.{}' that did not compile, "
                                    + "skipping", source.name(), relationship.getName(),
                            dbRelationship.getSourceEntityName(), dbRelationship.getName());
                    return null;
                }
                dbPath.add(compiled);
            }

            String name = relationship.getName();
            DeleteRule deleteRule = relationship.getDeleteRule();
            boolean usedForLocking = relationship.isUsedForLocking();

            if (!relationship.isToMany()) {
                CompiledToOneRelationship compiled = new CompiledToOneRelationship(source, target, name, deleteRule,
                        usedForLocking, dbPath);
                compiled.fault = new ToOneFault(name);
                return compiled;
            }

            // the kind of the collection is compiled into the fault that creates it; only a Map is a fact of the
            // relationship, as its targets are keyed
            String collectionType = relationship.getCollectionType();
            String mapKey = relationship.getMapKey();
            boolean map = "java.util.Map".equals(collectionType);
            CompiledToManyRelationship compiled = new CompiledToManyRelationship(source, target, name, deleteRule,
                    usedForLocking, map, mapKey, dbPath);
            if (collectionType == null || ObjRelationship.DEFAULT_COLLECTION_TYPE.equals(collectionType)) {
                compiled.fault = new ToManyListFault(name);
                return compiled;
            }
            switch (collectionType) {
                case "java.util.Set" -> compiled.fault = new ToManySetFault(name);
                case "java.util.Collection" -> compiled.fault = new ToManyListFault(name);
                case "java.util.Map" -> {
                    DeferredAccessor mapKeyAccessor = new DeferredAccessor();
                    compiled.fault = new ToManyMapFault(name, mapKeyAccessor);
                    compiled.mapKeyAccessor = mapKeyAccessor;
                    mapKeyAccessors.put(compiled, mapKeyAccessor);
                }
                default -> throw new CayenneRuntimeException("Unsupported collection type '%s' of relationship %s.%s",
                        collectionType, relationship.getSourceEntity().getName(), relationship.getName());
            }
            return compiled;
        }

        private void compileAttributes(CompiledEntity entity) {
            ObjEntity authored = authored(entity);
            Set<String> declaredNames = new HashSet<>();
            for (ObjAttribute attribute : authored.getDeclaredAttributes()) {
                declaredNames.add(attribute.getName());
            }

            List<CompiledAttribute> attributes = new ArrayList<>();
            List<CompiledEmbeddedAttribute> embedded = new ArrayList<>();
            for (ObjAttribute attribute : authored.getAttributes()) {
                // "inherited" means a super-entity has the attribute, even if this entity redeclares it
                boolean inherited = attribute.isInherited();
                if (attribute instanceof EmbeddedAttribute embeddedAttribute) {
                    CompiledEmbeddedAttribute compiledEmbedded = compileEmbeddedAttribute(entity, embeddedAttribute,
                            inherited, attributes);
                    embedded.add(compiledEmbedded);
                } else {
                    attributes.add(compileAttribute(entity, attribute, inherited, null, null));
                }
            }
            entity.attributes = byName(attributes, CompiledAttribute::name);
            entity.embeddedAttributes = byName(embedded, CompiledEmbeddedAttribute::name);

            Map<String, CompiledAttribute> declared = new LinkedHashMap<>();
            for (CompiledAttribute attribute : entity.attributes.values()) {
                String declaredName = attribute.embedded() != null ? attribute.embedded().name() : attribute.name();
                if (declaredNames.contains(declaredName)) {
                    declared.put(attribute.name(), attribute);
                }
            }
            entity.declaredAttributes = declared;
        }

        private CompiledEmbeddedAttribute compileEmbeddedAttribute(
                CompiledEntity entity,
                EmbeddedAttribute attribute,
                boolean inherited,
                List<CompiledAttribute> attributes) {

            Embeddable authoredEmbeddable = attribute.getEmbeddable();
            CompiledEmbeddable embeddable = authoredEmbeddable != null ? embeddables.get(authoredEmbeddable) : null;
            if (embeddable == null) {
                throw new CayenneRuntimeException("Embedded attribute '%s.%s' has no embeddable '%s'", entity.name(),
                        attribute.getName(), attribute.getType());
            }
            Accessor embeddedAccessor = new PersistentAccessor(attribute.getName());
            CompiledEmbeddedAttribute compiled = new CompiledEmbeddedAttribute(attribute.getName(), embeddable,
                    embeddedAccessor);

            List<CompiledAttribute> parts = new ArrayList<>();
            for (ObjAttribute part : attribute.getAttributes()) {
                Accessor accessor = null;
                if (embeddable.objectClass() != null) {
                    String fieldName = part.getName().substring(attribute.getName().length() + 1);
                    Accessor fieldAccessor = new FieldAccessor(embeddable.objectClass(), fieldName,
                            loadClass(part.getType()));
                    accessor = new EmbeddedFieldAccessor(embeddedAccessor, fieldAccessor);
                }
                CompiledAttribute compiledPart = compileAttribute(entity, part, inherited, compiled, accessor);
                parts.add(compiledPart);
                attributes.add(compiledPart);
            }
            compiled.attributes = parts;
            return compiled;
        }

        private CompiledAttribute compileAttribute(
                CompiledEntity entity,
                ObjAttribute attribute,
                boolean inherited,
                CompiledEmbeddedAttribute embedded,
                Accessor embeddedAccessor) {

            Class<?> valueClass = loadClass(attribute.getType());
            CompiledDbPath resolvedDbPath = resolvedDbPath(entity, attribute);
            Accessor accessor;
            ValueComparisonStrategy<Object> comparisonStrategy = null;
            if (embedded != null) {
                accessor = embeddedAccessor;
            } else {
                accessor = new PersistentAccessor(attribute.getName());
                comparisonStrategy = valueClass != null ? comparisonStrategyFactory.getStrategy(valueClass) : null;
            }
            boolean primaryKey = resolvedDbPath.column() != null && resolvedDbPath.column().isPrimaryKey();
            CompiledAttribute compiled = new CompiledAttribute(entity, attribute.getName(), attribute.getType(),
                    valueClass, attribute.getDbAttributePath(), resolvedDbPath.joins(), resolvedDbPath.column(),
                    primaryKey, attribute.isUsedForLocking(), inherited, false, embedded, accessor,
                    comparisonStrategy);
            if (attribute.isLazy()) {
                compiled.fault = new AttributeFault(compiled.name(), valueClass);
            }
            return compiled;
        }

        private CompiledDbPath resolvedDbPath(CompiledEntity entity, ObjAttribute attribute) {
            CayennePath dbPath = attribute.getDbAttributePath();
            if (dbPath == null || dbPath.isEmpty() || entity.table() == null) {
                return UNRESOLVED_DB_PATH;
            }
            List<CompiledJoin> joins = new ArrayList<>(dbPath.length());
            CompiledTable table = entity.table();
            for (int i = 0; i < dbPath.length(); i++) {
                String segment = dbPath.segments().get(i).value();
                if (i == dbPath.length() - 1) {
                    CompiledColumn column = table.column(segment);
                    if (column == null) {
                        LOGGER.warn("ObjAttribute '{}.{}' maps to a non-existent column '{}' of '{}'",
                                entity.name(), attribute.getName(), segment, table.name());
                        return UNRESOLVED_DB_PATH;
                    }
                    return new CompiledDbPath(joins, column);
                }
                CompiledJoin join = table.join(segment);
                if (join == null) {
                    LOGGER.warn("ObjAttribute '{}.{}' maps to a non-existent DbRelationship '{}' of '{}'",
                            entity.name(), attribute.getName(), segment, table.name());
                    return UNRESOLVED_DB_PATH;
                }
                joins.add(join);
                table = join.target();
            }
            return UNRESOLVED_DB_PATH;
        }

        /**
         * Creates a synthetic attribute for a column of the entity table that has no property.
         */
        private CompiledAttribute syntheticAttribute(CompiledEntity entity, CompiledColumn column) {
            String type = column.valueClassName();
            Class<?> valueClass = loadClass(type);
            return new CompiledAttribute(entity, Util.underscoredToJava(column.name(), false), type, valueClass,
                    CayennePath.of(column.name()), List.of(), column, column.isPrimaryKey(), false, false, true,
                    null, null, null);
        }

        // ---- link

        private void link() {
            // sub-entities, in compile order, which is super before sub
            Map<CompiledEntity, List<CompiledEntity>> children = new IdentityHashMap<>();
            for (CompiledEntity entity : entityOrder) {
                if (entity.superEntity() != null) {
                    children.computeIfAbsent(entity.superEntity(), k -> new ArrayList<>()).add(entity);
                }
            }
            for (CompiledEntity entity : entityOrder) {
                entity.subEntities = children.getOrDefault(entity, List.of());
            }
            for (CompiledEntity entity : entityOrder) {
                List<CompiledEntity> all = new ArrayList<>();
                appendSubEntities(entity, all);
                entity.allSubEntities = all;
                Map<String, CompiledEntity> byName = new HashMap<>();
                for (int i = 1; i < all.size(); i++) {
                    byName.put(all.get(i).name(), all.get(i));
                }
                entity.subEntitiesByName = byName;
            }

            // relationships, own and inherited, and their reverses
            for (CompiledEntity entity : entityOrder) {
                Map<String, CompiledRelationship> all = new LinkedHashMap<>();
                if (entity.superEntity() != null) {
                    all.putAll(entity.superEntity().relationships);
                }
                all.putAll(entity.declaredRelationships);
                entity.relationships = byName(new ArrayList<>(all.values()), CompiledRelationship::name);
            }
            for (Map.Entry<ObjRelationship, CompiledRelationship> e : objRelationships.entrySet()) {
                ObjRelationship reverse = e.getKey().getReverseRelationship();
                e.getValue().reverse = reverse != null ? objRelationships.get(reverse) : null;
            }
            for (CompiledRelationship relationship : objRelationships.values()) {
                relationship.reversesByTarget = reversesByTarget(relationship);
            }
            for (Map.Entry<CompiledToManyRelationship, DeferredAccessor> e : mapKeyAccessors.entrySet()) {
                e.getValue().delegate = mapKeyAccessor(e.getKey());
            }

            // the facts that depend on the attributes and relationships of the whole hierarchy
            for (CompiledEntity entity : entityOrder) {
                linkEntity(entity);
            }

            // tables
            Map<CompiledTable, List<CompiledEntity>> tableEntities = new IdentityHashMap<>();
            for (CompiledEntity entity : entityOrder) {
                if (entity.table() != null) {
                    tableEntities.computeIfAbsent(entity.table(), k -> new ArrayList<>()).add(entity);
                }
            }
            for (CompiledTable table : schemaBuild.tables()) {
                table.entities = tableEntities.getOrDefault(table, List.of());
            }
        }

        /**
         * Finds, for every sub-entity of the target of a relationship, the relationship going back to the source over
         * the reverse joins, if the sub-entity declares one.
         */
        private static Map<CompiledEntity, CompiledRelationship> reversesByTarget(CompiledRelationship relationship) {
            if (relationship.reverseDbPath == null) {
                return Map.of();
            }
            Map<CompiledEntity, CompiledRelationship> reverses = new IdentityHashMap<>();
            for (CompiledEntity targetEntity : relationship.target().allSubEntities()) {
                for (CompiledRelationship candidate : targetEntity.relationships()) {
                    CompiledEntity candidateTarget = candidate.target();
                    boolean toSource = candidateTarget == relationship.source()
                            || relationship.source().isSubEntityOf(candidateTarget);
                    if (toSource && relationship.reverseDbPath.equals(candidate.dbPath)) {
                        reverses.put(targetEntity, candidate);
                        break;
                    }
                }
            }
            return reverses;
        }

        private static void appendSubEntities(CompiledEntity entity, List<CompiledEntity> all) {
            all.add(entity);
            for (CompiledEntity subEntity : entity.subEntities) {
                appendSubEntities(subEntity, all);
            }
        }

        private Accessor mapKeyAccessor(CompiledToManyRelationship toMany) {
            String mapKey = toMany.mapKey();
            if (mapKey == null) {
                return IdMapKeyAccessor.SHARED_ACCESSOR;
            }
            CompiledEntity target = toMany.target();
            CompiledAttribute attribute = target.attribute(mapKey);
            if (attribute != null) {
                return new Accessor() {
                    @Override
                    public String getName() {
                        return mapKey;
                    }

                    @Override
                    public Object getValue(Object object) throws PropertyException {
                        return attribute.readProperty(object);
                    }

                    @Override
                    public void setValue(Object object, Object newValue) throws PropertyException {
                        attribute.writeProperty(object, null, newValue);
                    }
                };
            }
            CompiledRelationship keyRelationship = target.relationship(mapKey);
            if (keyRelationship != null) {
                return new Accessor() {
                    @Override
                    public String getName() {
                        return mapKey;
                    }

                    @Override
                    public Object getValue(Object object) throws PropertyException {
                        return keyRelationship.readProperty(object);
                    }

                    @Override
                    public void setValue(Object object, Object newValue) throws PropertyException {
                        keyRelationship.writeProperty(object, null, newValue);
                    }
                };
            }
            throw new CayenneRuntimeException("Map key '%s' of relationship '%s.%s' is not a property of '%s'",
                    mapKey, toMany.source().name(), toMany.name(), target.name());
        }

        private void linkEntity(CompiledEntity entity) {
            // id attributes, PK attributes
            List<CompiledAttribute> idAttributes = new ArrayList<>();
            for (CompiledAttribute attribute : entity.attributes()) {
                if (attribute.isPrimaryKey()) {
                    idAttributes.add(attribute);
                }
            }
            entity.idAttributes = idAttributes;

            List<CompiledAttribute> primaryKeyAttributes = new ArrayList<>();
            if (entity.table() != null) {
                for (CompiledColumn pk : entity.table().primaryKey()) {
                    CompiledAttribute attribute = entity.attributeForColumn(pk);
                    primaryKeyAttributes.add(attribute != null ? attribute : syntheticAttribute(entity, pk));
                }
            }
            entity.primaryKeyAttributes = primaryKeyAttributes;

            // relationships whose reverses are maps
            List<CompiledRelationship> mapRelationships = new ArrayList<>();
            for (CompiledRelationship relationship : entity.relationships()) {
                if (relationship.reverse() instanceof CompiledToManyRelationship reverse && reverse.isMap()) {
                    mapRelationships.add(relationship);
                }
            }
            entity.mapRelationships = mapRelationships;

            // all attributes and relationships of the hierarchy below
            List<CompiledAttribute> allAttributes = new ArrayList<>(entity.attributes());
            List<CompiledRelationship> allRelationships = new ArrayList<>(entity.relationships());
            for (int i = 1; i < entity.allSubEntities.size(); i++) {
                CompiledEntity subEntity = entity.allSubEntities.get(i);
                allAttributes.addAll(subEntity.declaredAttributes());
                allRelationships.addAll(subEntity.declaredRelationships());
            }
            entity.allAttributes = allAttributes;
            entity.allRelationships = allRelationships;

            // qualifiers
            entity.qualifier = qualifierForEntityAndSubEntities(entity);
            entity.tableQualifier = entity.declaredQualifier() != null && entity.table() != null
                    ? entity.translateToDbPath(entity.declaredQualifier())
                    : null;
            entity.discriminatorAttributes = discriminatorAttributes(entity);
            entity.discriminatorValues = discriminatorValues(entity);

            // tables
            List<CompiledTable> rootTables = new ArrayList<>();
            for (CompiledEntity e : entity.allSubEntities) {
                if (e.table() != null && !rootTables.contains(e.table())) {
                    rootTables.add(e.table());
                }
            }
            entity.rootTables = rootTables;
            entity.flattenedTables = flattenedTables(entity);
            entity.snapshotLayout = snapshotLayout(entity);
        }

        private Expression qualifierForEntityAndSubEntities(CompiledEntity entity) {
            Expression qualifier = entity.declaredQualifier();
            if (qualifier == null && entity.superEntity() == null) {
                // a root with no qualifier matches all
                return null;
            }
            for (CompiledEntity subEntity : entity.subEntities) {
                Expression subQualifier = qualifierForEntityAndSubEntities(subEntity);
                // if any sub-entity has no qualifier, no filtering is possible
                if (subQualifier == null) {
                    return null;
                }
                qualifier = qualifier == null ? subQualifier : qualifier.orExp(subQualifier);
            }
            return qualifier;
        }

        private List<CompiledAttribute> discriminatorAttributes(CompiledEntity entity) {
            if (entity.qualifier == null) {
                return List.of();
            }
            Map<String, CompiledAttribute> attributes = new LinkedHashMap<>();
            entity.qualifier.traverse(new TraversalHandler() {
                @Override
                public void startNode(Expression node, Expression parentNode) {
                    CompiledAttribute attribute = discriminatorAttribute(entity, node);
                    if (attribute != null) {
                        attributes.put(attribute.name(), attribute);
                    }
                }
            });
            return new ArrayList<>(attributes.values());
        }

        /**
         * Resolves an "obj:" or "db:" path node of an inheritance qualifier to an attribute of the entity,
         * synthesizing one for a DB column that has no attribute. Returns null for any other node.
         */
        private CompiledAttribute discriminatorAttribute(CompiledEntity entity, Expression node) {
            if (node instanceof DbPathExp) {
                if (entity.table() == null) {
                    return null;
                }
                // inheritance qualifiers over related tables are not supported: the column is of the root table
                CompiledColumn column = entity.table().column(node.getOperand(0).toString());
                if (column == null) {
                    return null;
                }
                CompiledAttribute attribute = entity.attributeForColumn(column);
                return attribute != null ? attribute : syntheticAttribute(entity, column);
            }
            if (node instanceof ObjPathExp) {
                return entity.attribute(node.getOperand(0).toString());
            }
            return null;
        }

        /**
         * Compiles the values that the entity's own qualifier pins its attributes to, i.e. the "attribute = value"
         * comparisons in it, possibly joined by "and". Values are converted to the attribute Java type here, once.
         */
        private Map<CompiledAttribute, Object> discriminatorValues(CompiledEntity entity) {
            Expression qualifier = entity.declaredQualifier();
            if (qualifier == null) {
                return Map.of();
            }
            Map<CompiledAttribute, Object> values = new LinkedHashMap<>();
            collectDiscriminatorValues(entity, qualifier, values);
            return values;
        }

        private void collectDiscriminatorValues(CompiledEntity entity, Expression exp,
                                                Map<CompiledAttribute, Object> values) {
            switch (exp) {
                case AndExp and -> {
                    for (int i = 0; i < and.getOperandCount(); i++) {
                        collectDiscriminatorValues(entity, (Expression) and.getOperand(i), values);
                    }
                }
                case EqualExp equal -> {
                    Object left = equal.getOperand(0);
                    Object right = equal.getOperand(1);
                    if (left instanceof Expression path && !(right instanceof Expression)) {
                        putDiscriminatorValue(entity, path, right, values);
                    } else if (right instanceof Expression path && !(left instanceof Expression)) {
                        putDiscriminatorValue(entity, path, left, values);
                    }
                }
                default -> {
                    // other conditions don't pin an attribute to a single value
                }
            }
        }

        private void putDiscriminatorValue(CompiledEntity entity, Expression path, Object value,
                                           Map<CompiledAttribute, Object> values) {
            CompiledAttribute attribute = discriminatorAttribute(entity, path);
            // a synthetic attribute of a bare DB column has no property to write to
            if (attribute == null || attribute.isSynthetic()) {
                return;
            }
            values.put(attribute, convertDiscriminatorValue(value, attribute.valueClass()));
        }

        private static <T> Object convertDiscriminatorValue(Object value, Class<T> javaType) {
            Converter<T> converter = javaType != null ? ConverterFactory.factory.getConverter(javaType) : null;
            return converter != null ? converter.convert(value, javaType) : value;
        }

        private Map<CayennePath, CompiledFlattenedTable> flattenedTables(CompiledEntity entity) {
            Map<CayennePath, CompiledFlattenedTable> additional = new HashMap<>();
            for (CompiledAttribute attribute : entity.attributes()) {
                if (!attribute.isFlattened()) {
                    continue;
                }
                CayennePath path = CayennePath.EMPTY_PATH;
                for (CompiledJoin join : attribute.joins()) {
                    // When deleting an object with flattened attributes the target row of the flattened attribute
                    // is deleted too (see RootRowOpProcessor.visitDelete), EXCEPT if the target table has a to-many
                    // relationship with the source table (as other rows of the source may reference it), OR the
                    // delete rules prevent it (i.e. an ObjRelationship over the same DbRelationship is not CASCADE)
                    boolean noDelete = join.reverse().isToMany()
                            || entity.relationships().stream()
                            .filter(r -> r.joins().equals(List.of(join)))
                            .anyMatch(r -> r.deleteRule() != DeleteRule.CASCADE);
                    path = path.dot(join.name());
                    additional.put(path, new CompiledFlattenedTable(path, join.target(), noDelete));
                }
            }
            for (CompiledRelationship relationship : entity.relationships()) {
                if (relationship.isToMany() || !relationship.isFlattened()) {
                    continue;
                }
                CayennePath path = CayennePath.EMPTY_PATH;
                List<CompiledJoin> joins = relationship.joins();
                for (int i = 0; i < joins.size() - 1; i++) {
                    CompiledJoin join = joins.get(i);
                    path = path.dot(join.name());
                    additional.put(path, new CompiledFlattenedTable(path, join.target(), false));
                }
            }
            return additional;
        }

        private DataRowLayout snapshotLayout(CompiledEntity entity) {
            // a column is listed once however many roles it has: a PK column is often an attribute or an FK as well
            Set<String> names = new LinkedHashSet<>();
            for (CompiledAttribute attribute : entity.attributes()) {
                if (attribute.dbPath() != null) {
                    names.add(attribute.dbPath().value());
                }
            }
            for (CompiledRelationship relationship : entity.relationships()) {
                // a to-one that does not hold the FK on the source has no column in the snapshot
                if (relationship.isForeignKey()) {
                    for (CompiledColumnPair pair : relationship.joins().getFirst().columnPairs()) {
                        names.add(pair.source().name());
                    }
                }
            }
            if (entity.table() != null) {
                for (CompiledColumn pk : entity.table().primaryKey()) {
                    names.add(pk.name());
                }
            }
            for (CompiledFlattenedTable flattened : entity.flattenedTables.values()) {
                for (CompiledColumn pk : flattened.table().primaryKey()) {
                    names.add(flattened.path().dot(pk.name()).value());
                }
            }
            return DataRowLayout.of(names.toArray(new String[0]));
        }

        // ---- mapping

        private CompiledMapping mapping(CompiledSchema schema) {
            Map<String, CompiledEntity> entitiesByName = new LinkedHashMap<>();
            Map<Class<?>, CompiledEntity> entitiesByClass = new HashMap<>();
            Map<String, CompiledEmbeddable> embeddablesByName = new LinkedHashMap<>();
            Map<String, CompiledQuery> queries = new LinkedHashMap<>();

            // in the order of the maps, and of the declarations within a map
            for (DataMap map : authoredMaps) {
                for (ObjEntity authored : map.getObjEntities()) {
                    CompiledEntity entity = entities.get(authored);
                    warnDuplicate(entitiesByName.put(entity.name(), entity), "ObjEntity", entity.name());
                    // entities sharing a class can't be looked up by it; an entity with no bound class is not indexed
                    Class<?> objectClass = entity.objectClass();
                    if (objectClass != null) {
                        entitiesByClass.compute(objectClass,
                                (k, existing) -> existing == null ? entity : CompiledMapping.duplicateClassMarker());
                    }
                }
                for (Embeddable embeddable : map.getEmbeddables()) {
                    embeddablesByName.put(embeddable.getClassName(), embeddables.get(embeddable));
                }
                for (NamedQuery query : map.getNamedQueries()) {
                    CompiledQuery compiled = compileQuery(schemaBuild.map(map), query);
                    warnDuplicate(queries.put(compiled.name(), compiled), "query", compiled.name());
                }
            }

            return new CompiledMapping(
                    schema,
                    entitiesByName,
                    entitiesByClass,
                    embeddablesByName,
                    queries);
        }

        /**
         * Compiles a named query to its settings, with the roots reduced to names. A missing root stays null, to be
         * reported when the query runs.
         */
        private static CompiledQuery compileQuery(CompiledDataMap map, NamedQuery query) {
            return switch (query) {
                case NamedSelectQuery select -> new CompiledSelectQuery(
                        map,
                        select.getName(),
                        select.getCacheStrategy(),
                        select.getCacheGroup(),
                        select.isFetchingDataRows(),
                        select.getPageSize(),
                        select.getStatementFetchSize(),
                        select.getQl());
                case NamedSQLQuery sql -> new CompiledSQLQuery(
                        map,
                        sql.getName(),
                        sql.getCacheStrategy(),
                        sql.getCacheGroup(),
                        sql.isFetchingDataRows(),
                        sql.getPageSize(),
                        sql.getStatementFetchSize(),
                        switch (sql.getRoot()) {
                            case null -> null;
                            case String entityName -> entityName;
                            case ObjEntity entity -> entity.getName();
                            case DataMap ignored -> null;
                            default -> throw new CayenneRuntimeException("Unsupported root of SQL query '%s': %s",
                                    sql.getName(), sql.getRoot());
                        },
                        sql.getRoot() instanceof DataMap rootMap ? rootMap.getName() : null,
                        sql.getSql(),
                        adapterSql(sql),
                        Map.copyOf(sql.getPrefetchesMap()),
                        sql.getColumnNamesCapitalization());
                case NamedProcedureQuery procedure -> new CompiledProcedureQuery(
                        map,
                        procedure.getName(),
                        procedure.getCacheStrategy(),
                        procedure.getCacheGroup(),
                        procedure.isFetchingDataRows(),
                        procedure.getPageSize(),
                        procedure.getStatementFetchSize(),
                        switch (procedure.getRoot()) {
                            case String name -> name;
                            case Procedure authored -> authored.getName();
                            case null, default -> null;
                        },
                        procedure.getResultEntityName(),
                        procedure.getFetchLimit(),
                        procedure.getFetchOffset(),
                        procedure.getColumnNamesCapitalization());
            };
        }

        // the authored map may hold half-edited entries with no adapter or no SQL, which are skipped
        private static Map<String, String> adapterSql(NamedSQLQuery query) {
            Map<String, String> result = new HashMap<>();
            query.getAdapterSql().forEach((adapter, sql) -> {
                if (adapter != null && sql != null) {
                    result.put(adapter, sql);
                }
            });
            return result;
        }

        private static void warnDuplicate(Object previous, String kind, String name) {
            if (previous != null) {
                LOGGER.warn("Found duplicated {} name '{}' in the DataMaps", kind, name);
            }
        }

        // ---- helpers

        private ObjEntity authored(CompiledEntity entity) {
            for (Map.Entry<ObjEntity, CompiledEntity> e : entities.entrySet()) {
                if (e.getValue() == entity) {
                    return e.getKey();
                }
            }
            throw new IllegalStateException("Not a compiled entity: " + entity);
        }

        /**
         * Loads a mapped class, or returns null for no class name, which is a legitimate mapping, or with a warning
         * if the class can't be loaded, as a mapping may refer to a class that is not on the classpath yet, e.g. an
         * entity just added by a tool. The entity compiles, and fails on the first attempt to create or access its
         * objects.
         */
        private Class<?> loadClass(String className) {
            if (className == null) {
                return null;
            }
            try {
                return objectFactory.getJavaClass(className);
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to load mapped class '{}': {}", className, e.getMessage());
                return null;
            }
        }
    }

    /**
     * An accessor whose delegate is bound after the objects referring to it are created.
     */
    private static final class DeferredAccessor implements Accessor {

        Accessor delegate;

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Object getValue(Object object) throws PropertyException {
            return delegate.getValue(object);
        }

        @Override
        public void setValue(Object object, Object newValue) throws PropertyException {
            delegate.setValue(object, newValue);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
