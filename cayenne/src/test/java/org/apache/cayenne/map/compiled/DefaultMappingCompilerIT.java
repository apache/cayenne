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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compiles every test DataMap and checks the compiled model against the authored model it was compiled from, fact by
 * fact.
 */
public class DefaultMappingCompilerIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject("cayenne-ALL.xml").withoutAutoClean();

    private EntityResolver authored() {
        return new EntityResolver(env.project().dataMaps());
    }

    private CompiledMapping compile() {
        MappingCompiler compiler = env.runtime().getInjector().getInstance(MappingCompiler.class);
        return compiler.compile(env.project().dataMaps());
    }

    @Test
    public void entities() {
        EntityResolver resolver = authored();
        CompiledMapping mapping = compile();

        for (ObjEntity entity : resolver.getObjEntities()) {
            String n = entity.getName();
            CompiledEntity compiled = mapping.entity(n);
            assertNotNull(compiled, n);
            if (!entity.isGeneric()) {
                assertSame(compiled, mapping.entity(compiled.objectClass()), n);
            }
            assertEquals(entity.getJavaClassName(), compiled.objectClass().getName(), n);
            assertEquals(entity.getDataMap().getName(), compiled.map().name(), n);
            assertEquals(entity.getDbEntity() != null ? entity.getDbEntity().getName() : null,
                    compiled.table() != null ? compiled.table().name() : null, n);
            assertEquals(entity.getSuperEntity() != null ? entity.getSuperEntity().getName() : null,
                    compiled.superEntity() != null ? compiled.superEntity().name() : null, n);
            assertEquals(entity.isAbstract(), compiled.isAbstract(), n);
            assertEquals(entity.isReadOnly(), compiled.isReadOnly(), n);
            assertEquals(entity.isGeneric(), compiled.isGeneric(), n);
            assertEquals(entity.getLockType(), compiled.lockType(), n);
            assertEquals(String.valueOf(entity.getDeclaredQualifier()), String.valueOf(compiled.declaredQualifier()), n);

            EntityInheritanceTree tree = resolver.getInheritanceTree(n);
            assertEquals(tree.getChildrenCount() > 0, compiled.hasSubEntities(), n);
            assertEquals(tree.getChildren().stream().map(t -> t.getEntity().getName()).collect(Collectors.toSet()),
                    compiled.subEntities().stream().map(CompiledEntity::name).collect(Collectors.toSet()), n);
            assertEquals(tree.allSubEntities().stream().map(ObjEntity::getName).collect(Collectors.toSet()),
                    compiled.allSubEntities().stream().map(CompiledEntity::name).collect(Collectors.toSet()), n);
            assertEquals(String.valueOf(tree.qualifierForEntityAndSubclasses()), String.valueOf(compiled.qualifier()), n);
            assertEquals(String.valueOf(tree.getDbQualifier()), String.valueOf(compiled.tableQualifier()), n);

            // attributes, with the embedded attributes expanded into their parts
            assertEquals(attributeNames(entity.getAttributes()), names(compiled.attributes()), n);
            assertEquals(attributeNames(entity.getDeclaredAttributes()), names(compiled.declaredAttributes()), n);
            assertEquals(attributeNames(tree.allAttributes()), names(compiled.allAttributes()), n);
            Set<String> idAttributes = entity.getAttributes().stream()
                    .filter(a -> !(a instanceof EmbeddedAttribute) && a.isPrimaryKey())
                    .map(ObjAttribute::getName).collect(Collectors.toSet());
            assertEquals(idAttributes, names(compiled.idAttributes()), n);
            assertEquals(entity.getPrimaryKeys().stream().map(ObjAttribute::getName).collect(Collectors.toSet()),
                    names(compiled.primaryKeyAttributes()), n);

            // relationships
            assertEquals(relationshipNames(entity.getRelationships()), names(compiled.relationships()), n);
            assertEquals(relationshipNames(entity.getDeclaredRelationships()), names(compiled.declaredRelationships()), n);
            assertEquals(relationshipNames(tree.allRelationships()), names(compiled.allRelationships()), n);
            Set<String> mapRelationships = entity.getRelationships().stream()
                    .filter(r -> r.getReverseRelationship() != null
                            && "java.util.Map".equals(r.getReverseRelationship().getCollectionType()))
                    .map(ObjRelationship::getName).collect(Collectors.toSet());
            assertEquals(mapRelationships, names(compiled.mapRelationships()), n);

            Set<String> rootTables = new HashSet<>();
            for (ObjEntity e : tree.allSubEntities()) {
                if (e.getDbEntity() != null) {
                    rootTables.add(e.getDbEntity().getName());
                }
            }
            assertEquals(rootTables,
                    compiled.rootTables().stream().map(CompiledTable::name).collect(Collectors.toSet()), n);

            for (ObjAttribute attribute : entity.getAttributes()) {
                if (attribute instanceof EmbeddedAttribute embedded) {
                    for (ObjAttribute part : embedded.getAttributes()) {
                        compareAttribute(n, part, compiled.attribute(part.getName()), true);
                    }
                } else {
                    compareAttribute(n, attribute, compiled.attribute(attribute.getName()), false);
                }
            }
            for (ObjRelationship relationship : entity.getRelationships()) {
                compareRelationship(n, relationship, compiled.relationship(relationship.getName()));
            }
        }
    }

    private static void compareAttribute(String entity, ObjAttribute attribute, CompiledAttribute compiled,
                                         boolean embedded) {
        String n = entity + "." + attribute.getName();
        assertNotNull(compiled, n);
        assertEquals(attribute.getDbAttributePath(), compiled.dbPath(), n);
        assertEquals(attribute.isPrimaryKey(), compiled.isPrimaryKey(), n);
        assertEquals(attribute.isFlattened(), compiled.isFlattened(), n);
        assertEquals(attribute.isInherited(), compiled.isInherited(), n);
        assertEquals(attribute.isUsedForLocking(), compiled.isUsedForLocking(), n);
        assertEquals(attribute.isLazy(), compiled.isLazy(), n);
        assertEquals(attribute.isMandatory(), compiled.isMandatory(), n);
        assertEquals(attribute.getMaxLength(), compiled.maxLength(), n);
        assertEquals(attribute.getType(), compiled.valueClassName(), n);
        assertSame(attribute.getJavaClass(), compiled.valueClass(), n);
        assertEquals(attribute.getDbAttribute() != null ? attribute.getDbAttribute().getName() : null,
                compiled.column() != null ? compiled.column().name() : null, n);
        assertEquals(embedded, compiled.embedded() != null, n);
        assertTrue(!compiled.isSynthetic(), n);
    }

    private static void compareRelationship(String entity, ObjRelationship relationship,
                                            CompiledRelationship compiled) {
        String n = entity + "." + relationship.getName();
        assertNotNull(compiled, n);
        assertEquals(relationship.isToMany(), compiled.isToMany(), n);
        assertEquals(relationship.isToMany(), compiled instanceof CompiledToManyRelationship, n);
        assertEquals(relationship.getSourceEntity().getName(), compiled.source().name(), n);
        assertEquals(relationship.getTargetEntityName(), compiled.target().name(), n);
        assertEquals(relationship.getDbRelationshipPath(), compiled.dbPath(), n);
        // the authored reverse path needs authored reverse DbRelationships, which the compiler synthesizes
        boolean authoredReverses = relationship.getDbRelationships().stream()
                .allMatch(r -> r.getReverseRelationship() != null);
        if (authoredReverses) {
            assertEquals(relationship.getReverseDbRelationshipPath(), compiled.reverseDbPath(), n);
        } else {
            assertNotNull(compiled.reverseDbPath(), n);
        }
        assertEquals(relationship.isFlattened(), compiled.isFlattened(), n);
        // the FK side of a DbRelationship pair is inferred when the flag is set on both sides or neither
        DbRelationship first = relationship.getDbRelationships().getFirst();
        if (relationship.isFlattened() || first.getReverseRelationship() == null
                || first.isForeignKey() != first.getReverseRelationship().isForeignKey()) {
            assertEquals(relationship.isForeignKey(), compiled.isForeignKey(), n);
        }
        assertEquals(relationship.isToPrimaryKey(), compiled.isToPrimaryKey(), n);
        assertEquals(relationship.getDeleteRule(), compiled.deleteRule(), n);
        assertEquals(relationship.isUsedForLocking(), compiled.isUsedForLocking(), n);
        ObjRelationship reverse = relationship.getReverseRelationship();
        assertEquals(reverse != null ? reverse.getName() : null,
                compiled.reverse() != null ? compiled.reverse().name() : null, n);
        if (compiled instanceof CompiledToManyRelationship toMany) {
            assertEquals("java.util.Map".equals(relationship.getCollectionType()), toMany.isMap(), n);
            assertEquals(relationship.getMapKey(), toMany.mapKey(), n);
        }
    }

    @Test
    public void tables() {
        EntityResolver resolver = authored();
        CompiledMapping mapping = compile();

        for (DbEntity table : resolver.getDbEntities()) {
            String n = table.getName();
            CompiledTable compiled = mapping.schema().table(n);
            assertNotNull(compiled, n);
            assertEquals(table.getDataMap().getName(), compiled.map().name(), n);
            assertEquals(table.getFullyQualifiedName(), compiled.fullyQualifiedName(), n);
            assertEquals(table.getTableAliasBase(), compiled.tableAliasBase(), n);
            assertEquals(table.getAttributes().stream().map(DbAttribute::getName).toList(),
                    compiled.columns().stream().map(CompiledColumn::name).toList(), n);
            assertEquals(table.getPrimaryKeys().stream().map(DbAttribute::getName).toList(),
                    compiled.primaryKey().stream().map(CompiledColumn::name).toList(), n);
            assertEquals(table.getGeneratedAttributes().stream().map(DbAttribute::getName).collect(Collectors.toSet()),
                    compiled.generatedColumns().stream().map(CompiledColumn::name).collect(Collectors.toSet()), n);
            assertEquals(table.getPrimaryKeyGenerator() != null, compiled.keySequence() != null, n);
            assertEquals(String.valueOf(table.getQualifier()), String.valueOf(compiled.qualifier()), n);
            assertEquals(table.mappedObjEntities().stream().map(ObjEntity::getName).collect(Collectors.toSet()),
                    compiled.entities().stream().map(CompiledEntity::name).collect(Collectors.toSet()), n);

            for (DbAttribute column : table.getAttributes()) {
                CompiledColumn compiledColumn = compiled.column(column.getName());
                assertSame(compiled, compiledColumn.table());
                assertEquals(column.getType(), compiledColumn.jdbcType());
                assertEquals(column.isPrimaryKey(), compiledColumn.isPrimaryKey());
                assertEquals(column.isMandatory(), compiledColumn.isMandatory());
                assertEquals(column.getJavaClassName(), compiledColumn.valueClassName());
            }

            // every authored relationship is compiled, and the extra compiled ones are the synthesized reverses
            Set<String> authoredNames = table.getRelationships().stream().map(DbRelationship::getName)
                    .collect(Collectors.toSet());
            for (CompiledJoin join : compiled.joins()) {
                if (!authoredNames.contains(join.name())) {
                    assertTrue(join.name().startsWith("runtimeRelationship"), join.toString());
                    assertNotNull(resolver.getDbEntity(join.target().name())
                            .getRelationship(join.reverse().name()), join.toString());
                }
            }
            for (DbRelationship relationship : table.getRelationships()) {
                if (relationship.getReverseRelationship() == null) {
                    // the authored reverse is missing: the compiled one is synthesized
                    CompiledJoin join = compiled.join(relationship.getName());
                    assertNotNull(join, n + "." + relationship.getName());
                    assertNotNull(join.reverse(), n + "." + relationship.getName());
                    continue;
                }
                String rn = n + "." + relationship.getName();
                CompiledJoin compiledRelationship = compiled.join(relationship.getName());
                assertNotNull(compiledRelationship, rn);
                assertSame(compiled, compiledRelationship.source(), rn);
                assertEquals(relationship.getTargetEntityName(), compiledRelationship.target().name(), rn);
                assertEquals(relationship.isToMany(), compiledRelationship.isToMany(), rn);
                // the FK side is inferred when the flag is set on both sides or neither
                if (relationship.isForeignKey() != relationship.getReverseRelationship().isForeignKey()) {
                    assertEquals(relationship.isForeignKey(), compiledRelationship.isForeignKey(), rn);
                    assertEquals(relationship.isIdentifying(), compiledRelationship.isIdentifying(), rn);
                    assertEquals(relationship.isReflexive(), compiledRelationship.isReflexive(), rn);
                }
                assertEquals(relationship.getReverseRelationship().getName(), compiledRelationship.reverse().name(), rn);
                assertSame(compiledRelationship, compiledRelationship.reverse().reverse(), rn);
                assertSame(compiledRelationship.foreignKey(), compiledRelationship.reverse().foreignKey(), rn);
                // a join over a non-existent column is dropped, and the facts derived from it differ
                boolean validJoins = relationship.getJoins().stream()
                        .allMatch(j -> j.getSource() != null && j.getTarget() != null);
                if (validJoins) {
                    assertEquals(relationship.isToPrimaryKey(), compiledRelationship.isToPrimaryKey(), rn);
                }
                List<String> joins = new ArrayList<>();
                for (DbJoin join : relationship.getJoins()) {
                    if (join.getSource() != null && join.getTarget() != null) {
                        joins.add(join.getSourceName() + "=" + join.getTargetName());
                    }
                }
                List<String> compiledJoins = new ArrayList<>();
                for (CompiledColumnPair pair : compiledRelationship.columnPairs()) {
                    compiledJoins.add(pair.source().name() + "=" + pair.target().name());
                }
                assertEquals(joins, compiledJoins, rn);
            }
        }
    }

    @Test
    public void dependencyIndex() {
        CompiledMapping mapping = compile();

        // a referenced table has a lower index than the referencing one, unless the two are in a dependency cycle,
        // in which case they share the index; a table referencing nothing is at 0
        for (CompiledTable table : mapping.schema().tables()) {
            boolean referencing = false;
            for (CompiledForeignKey foreignKey : table.foreignKeys()) {
                // only a reference to a primary key orders the tables
                if (foreignKey.columnPairs().stream().noneMatch(p -> p.target().isPrimaryKey())) {
                    continue;
                }
                referencing = true;
                CompiledTable target = foreignKey.target();
                assertTrue(target.dependencyIndex() <= table.dependencyIndex(),
                        target.name() + " must come before " + table.name());
            }
            if (!referencing) {
                assertEquals(0, table.dependencyIndex(), table.name());
            }
        }
    }

    @Test
    public void dependencyIndex_deterministic() {
        Map<String, Integer> first = indexes(compile());
        Map<String, Integer> second = indexes(compile());
        assertEquals(first, second);
    }

    private static Map<String, Integer> indexes(CompiledMapping mapping) {
        return mapping.schema().tables().stream()
                .collect(Collectors.toMap(CompiledTable::name, CompiledTable::dependencyIndex));
    }

    @Test
    public void schema() {
        CompiledSchema schema = SchemaCompiler.compile(env.project().dataMaps());
        CompiledMapping mapping = compile();
        assertEquals(mapping.schema().tables().size(), schema.tables().size());
        assertEquals(mapping.schema().procedures().size(), schema.procedures().size());
        assertFalse(schema.procedures().isEmpty());
        for (CompiledTable table : mapping.schema().tables()) {
            CompiledTable schemaTable = schema.table(table.name());
            assertNotNull(schemaTable, table.name());
            assertEquals(table.dependencyIndex(), schemaTable.dependencyIndex(), table.name());
            assertEquals(table.foreignKeys().size(), schemaTable.foreignKeys().size(), table.name());
            assertTrue(schemaTable.entities().isEmpty(), table.name());
        }
    }

    private static Set<String> attributeNames(Collection<ObjAttribute> attributes) {
        Set<String> names = new HashSet<>();
        for (ObjAttribute attribute : attributes) {
            if (attribute instanceof EmbeddedAttribute embedded) {
                for (ObjAttribute part : embedded.getAttributes()) {
                    names.add(part.getName());
                }
            } else {
                names.add(attribute.getName());
            }
        }
        return names;
    }

    private static Set<String> relationshipNames(Collection<ObjRelationship> relationships) {
        return relationships.stream().map(ObjRelationship::getName).collect(Collectors.toSet());
    }

    private static Set<String> names(Collection<?> compiled) {
        Set<String> names = new HashSet<>();
        for (Object o : compiled) {
            names.add(switch (o) {
                case CompiledAttribute a -> a.name();
                case CompiledRelationship r -> r.name();
                default -> throw new IllegalArgumentException(o.toString());
            });
        }
        return names;
    }
}
