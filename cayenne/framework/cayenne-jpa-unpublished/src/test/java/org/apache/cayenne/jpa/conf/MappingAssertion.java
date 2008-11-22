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

package org.apache.cayenne.jpa.conf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorType;
import javax.persistence.FetchType;
import javax.persistence.GenerationType;
import javax.persistence.InheritanceType;
import javax.persistence.TemporalType;

import junit.framework.Assert;

import org.apache.cayenne.jpa.map.JpaAttributeOverride;
import org.apache.cayenne.jpa.map.JpaAttributes;
import org.apache.cayenne.jpa.map.JpaBasic;
import org.apache.cayenne.jpa.map.JpaColumn;
import org.apache.cayenne.jpa.map.JpaColumnResult;
import org.apache.cayenne.jpa.map.JpaEmbeddable;
import org.apache.cayenne.jpa.map.JpaEmbeddedId;
import org.apache.cayenne.jpa.map.JpaEntity;
import org.apache.cayenne.jpa.map.JpaEntityListener;
import org.apache.cayenne.jpa.map.JpaEntityListeners;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.map.JpaEntityResult;
import org.apache.cayenne.jpa.map.JpaFieldResult;
import org.apache.cayenne.jpa.map.JpaId;
import org.apache.cayenne.jpa.map.JpaJoinColumn;
import org.apache.cayenne.jpa.map.JpaMappedSuperclass;
import org.apache.cayenne.jpa.map.JpaNamedNativeQuery;
import org.apache.cayenne.jpa.map.JpaNamedQuery;
import org.apache.cayenne.jpa.map.JpaOneToMany;
import org.apache.cayenne.jpa.map.JpaOneToOne;
import org.apache.cayenne.jpa.map.JpaPrimaryKeyJoinColumn;
import org.apache.cayenne.jpa.map.JpaQueryHint;
import org.apache.cayenne.jpa.map.JpaSecondaryTable;
import org.apache.cayenne.jpa.map.JpaSequenceGenerator;
import org.apache.cayenne.jpa.map.JpaSqlResultSetMapping;
import org.apache.cayenne.jpa.map.JpaTable;
import org.apache.cayenne.jpa.map.JpaTableGenerator;
import org.apache.cayenne.jpa.map.JpaTransient;
import org.apache.cayenne.jpa.map.JpaUniqueConstraint;
import org.apache.cayenne.jpa.map.JpaVersion;

/**
 * A helper test class that checks loaded entity map for processed annotations.
 * 
 */
public class MappingAssertion extends Assert {

    public void testEntityMap(JpaEntityMap entityMap) throws Exception {

        assertNotNull(entityMap.getEntities());
        assertEquals(5, entityMap.getEntities().size());
        Iterator<JpaEntity> entityIt = entityMap.getEntities().iterator();
        assertEntity1(entityIt.next());
        assertEntity2(entityIt.next());
        assertEntity3(entityIt.next());
        assertEntity4(entityIt.next());
        assertEntity5(entityIt.next());

        assertNotNull(entityMap.getEmbeddables());
        assertEquals(2, entityMap.getEmbeddables().size());
        Iterator<JpaEmbeddable> embedIt = entityMap.getEmbeddables().iterator();
        assertEmbeddable1(embedIt.next());

        assertNotNull(entityMap.getMappedSuperclasses());
        assertEquals(3, entityMap.getMappedSuperclasses().size());
        Iterator<JpaMappedSuperclass> mappedSuperclassIt = entityMap
                .getMappedSuperclasses()
                .iterator();
        assertMappedSuperclass1(mappedSuperclassIt.next());
        assertMappedSuperclass2(mappedSuperclassIt.next());
        assertMappedSuperclass3(mappedSuperclassIt.next());

        assertEquals(2, entityMap.getNamedQueries().size());
        Iterator<JpaNamedQuery> namedQueryIt = entityMap.getNamedQueries().iterator();
        assertNamedQuery1(namedQueryIt.next());
        assertNamedQuery2(namedQueryIt.next());

        assertEquals(2, entityMap.getNamedNativeQueries().size());
        Iterator<JpaNamedNativeQuery> namedNativeQueryIt = entityMap
                .getNamedNativeQueries()
                .iterator();
        assertNativeNamedQuery3(namedNativeQueryIt.next());
        assertNativeNamedQuery4(namedNativeQueryIt.next());

        assertEquals(2, entityMap.getSqlResultSetMappings().size());
        assertSqlResultSetMapping(entityMap.getSqlResultSetMappings().iterator().next());

        assertEquals(2, entityMap.getSequenceGenerators().size());
        assertSequenceGenerator1(entityMap.getSequenceGenerators().iterator().next());

        assertEquals(2, entityMap.getTableGenerators().size());
        assertTableGenerator(entityMap.getTableGenerators().iterator().next());
    }

    protected void assertEmbeddable1(JpaEmbeddable embeddable1) {
        assertEquals("org.apache.cayenne.jpa.entity.MockEmbed1", embeddable1
                .getClassName());

        JpaAttributes attributes = embeddable1.getAttributes();
        assertNotNull(attributes);
        assertEquals(2, attributes.size());
        assertEquals(1, attributes.getBasicAttributes().size());
        JpaBasic a1 = attributes.getBasicAttribute("ea1");

        assertEquals("ea1", a1.getName());
        assertTrue(a1.isOptional());
        assertSame(FetchType.EAGER, a1.getFetch());

        assertTrue(a1.isLob());
        assertNotNull(a1.getColumn());
        assertEquals("column9", a1.getColumn().getName());

        assertEquals(1, attributes.getTransientAttributes().size());
        JpaTransient a2 = attributes.getTransientAttribute("ea2");
        assertEquals("ea2", a2.getName());
    }

    protected void assertEntity1(JpaEntity entity1) {
        assertNotNull(entity1);
        assertEquals("MockEntity1", entity1.getName());
        assertEquals("org.apache.cayenne.jpa.entity.MockEntity1", entity1.getClassName());

        assertTable(entity1.getTable());

        assertEquals(2, entity1.getSecondaryTables().size());
        Iterator<JpaSecondaryTable> secondaryTableIt = entity1
                .getSecondaryTables()
                .iterator();
        assertSecondaryTable1(secondaryTableIt.next());

        JpaSecondaryTable secondaryTable2 = secondaryTableIt.next();
        assertEquals("secondary2", secondaryTable2.getName());

        assertNotNull(entity1.getIdClass());
        assertEquals("org.apache.cayenne.jpa.entity.MockIdClass", entity1
                .getIdClass()
                .getClassName());

        assertSequenceGenerator1(entity1.getSequenceGenerator());

        assertNotNull(entity1.getTableGenerator());
        assertTableGenerator(entity1.getTableGenerator());

        assertEquals(2, entity1.getNamedQueries().size());
        Iterator<JpaNamedQuery> namedQueryIt = entity1.getNamedQueries().iterator();
        assertNamedQuery1(namedQueryIt.next());
        assertNamedQuery2(namedQueryIt.next());

        assertEquals(2, entity1.getNamedNativeQueries().size());
        Iterator<JpaNamedNativeQuery> namedNativeQueryIt = entity1
                .getNamedNativeQueries()
                .iterator();
        assertNativeNamedQuery3(namedNativeQueryIt.next());
        assertNativeNamedQuery4(namedNativeQueryIt.next());

        assertSqlResultSetMapping(entity1.getSqlResultSetMappings().iterator().next());

        assertTrue(entity1.isExcludeDefaultListeners());
        assertTrue(entity1.isExcludeSuperclassListeners());

        assertEntityListeners(entity1.getEntityListeners());

        assertNotNull(entity1.getPrePersist());
        assertEquals("eprePersist", entity1.getPrePersist().getMethodName());
        assertNotNull(entity1.getPostPersist());
        assertEquals("epostPersist", entity1.getPostPersist().getMethodName());
        assertNotNull(entity1.getPreUpdate());
        assertEquals("epreUpdate", entity1.getPreUpdate().getMethodName());
        assertNotNull(entity1.getPostUpdate());
        assertEquals("epostUpdate", entity1.getPostUpdate().getMethodName());
        assertNotNull(entity1.getPreRemove());
        assertEquals("epreRemove", entity1.getPreRemove().getMethodName());
        assertNotNull(entity1.getPostRemove());
        assertEquals("epostRemove", entity1.getPostRemove().getMethodName());
        assertNotNull(entity1.getPostLoad());
        assertEquals("epostLoad", entity1.getPostLoad().getMethodName());

        assertEquals(1, entity1.getAttributes().getIds().size());
        assertId(entity1.getAttributes().getIds().iterator().next());

        assertNull(entity1.getInheritance());
    }

    protected void assertEntity2(JpaEntity entity2) {
        assertNotNull(entity2);
        assertEquals("org.apache.cayenne.jpa.entity.MockEntity2", entity2.getClassName());
        assertEquals("MockEntity2", entity2.getName());

        assertEquals(2, entity2.getPrimaryKeyJoinColumns().size());
        Iterator<JpaPrimaryKeyJoinColumn> pkJoinColumnIt = entity2
                .getPrimaryKeyJoinColumns()
                .iterator();

        JpaPrimaryKeyJoinColumn pk1 = pkJoinColumnIt.next();
        assertEquals("pk_column1", pk1.getName());
        assertEquals("count(1)", pk1.getColumnDefinition());
        assertEquals("super_column1", pk1.getReferencedColumnName());

        assertNotNull(entity2.getInheritance());
        assertEquals(InheritanceType.JOINED, entity2.getInheritance().getStrategy());

        assertEquals(2, entity2.getAttributeOverrides().size());
        List<JpaAttributeOverride> overrides = new ArrayList<JpaAttributeOverride>(
                entity2.getAttributeOverrides());

        assertEquals("attribute1", overrides.get(0).getName());
        JpaColumn c1 = overrides.get(0).getColumn();
        assertEquals("ao_column1", c1.getName());
        assertEquals("count(1)", c1.getColumnDefinition());
        assertEquals("ao_table1", c1.getTable());
        assertEquals(3, c1.getLength());
        assertEquals(4, c1.getPrecision());
        assertEquals(5, c1.getScale());
        assertTrue(c1.isInsertable());
        assertTrue(c1.isNullable());
        assertTrue(c1.isUnique());
        assertTrue(c1.isUpdatable());

        assertEquals("attribute2", overrides.get(1).getName());
    }

    protected void assertEntity3(JpaEntity entity3) {
        assertNotNull(entity3);
        assertEquals("org.apache.cayenne.jpa.entity.MockEntity3", entity3.getClassName());
        assertEquals("MockEntity3", entity3.getName());

        assertNotNull(entity3.getInheritance());
        assertEquals(InheritanceType.SINGLE_TABLE, entity3.getInheritance().getStrategy());
        assertEquals("DV", entity3.getDiscriminatorValue());
        assertNotNull(entity3.getDiscriminatorColumn());
        assertEquals("column1", entity3.getDiscriminatorColumn().getName());
        assertEquals(DiscriminatorType.CHAR, entity3
                .getDiscriminatorColumn()
                .getDiscriminatorType());
        assertEquals("count(1)", entity3.getDiscriminatorColumn().getColumnDefinition());
        assertEquals(5, entity3.getDiscriminatorColumn().getLength());
    }

    protected void assertEntity4(JpaEntity entity4) {
        assertNotNull(entity4);
        assertEquals("org.apache.cayenne.jpa.entity.MockEntity4", entity4.getClassName());
        assertEquals("MockEntity4", entity4.getName());
        assertEmbeddedId(entity4.getAttributes().getEmbeddedId());
    }

    protected void assertEntity5(JpaEntity entity5) {

        assertEquals("MockEntity5", entity5.getName());
        assertNotNull(entity5.getAttributes());
        assertEquals(16, entity5.getAttributes().size());
        assertAttributes(entity5.getAttributes());
    }

    protected void assertSequenceGenerator1(JpaSequenceGenerator generator) {
        assertNotNull(generator);
        assertEquals("sg-name", generator.getName());
        assertEquals("seq-name", generator.getSequenceName());
        assertEquals(10, generator.getAllocationSize());
        assertEquals(5, generator.getInitialValue());
    }

    protected void assertTable(JpaTable table) {
        assertNotNull(table);
        assertEquals("mock_persistent_1", table.getName());
        assertEquals("catalog1", table.getCatalog());
        assertEquals("schema1", table.getSchema());

        assertEquals(2, table.getUniqueConstraints().size());
        Iterator<JpaUniqueConstraint> constraintsIt = table
                .getUniqueConstraints()
                .iterator();
        JpaUniqueConstraint c1 = constraintsIt.next();
        assertEquals(2, c1.getColumnNames().size());
        assertTrue(c1.getColumnNames().contains("column1"));
        assertTrue(c1.getColumnNames().contains("column2"));

        JpaUniqueConstraint c2 = constraintsIt.next();
        assertEquals(1, c2.getColumnNames().size());
        assertTrue(c2.getColumnNames().contains("column3"));
    }

    protected void assertSecondaryTable1(JpaSecondaryTable secondaryTable1) {
        assertEquals("secondary1", secondaryTable1.getName());
        assertEquals("catalog1", secondaryTable1.getCatalog());
        assertEquals("schema1", secondaryTable1.getSchema());

        assertEquals(2, secondaryTable1.getPrimaryKeyJoinColumns().size());
        Iterator<JpaPrimaryKeyJoinColumn> pkJoinColumnIt = secondaryTable1
                .getPrimaryKeyJoinColumns()
                .iterator();

        JpaPrimaryKeyJoinColumn pk1 = pkJoinColumnIt.next();
        assertEquals("secondary_column1", pk1.getName());
        assertEquals("count(1)", pk1.getColumnDefinition());
        assertEquals("column1", pk1.getReferencedColumnName());

        assertEquals(1, secondaryTable1.getUniqueConstraints().size());
        Iterator<JpaUniqueConstraint> constraintsIt = secondaryTable1
                .getUniqueConstraints()
                .iterator();
        JpaUniqueConstraint c1 = constraintsIt.next();
        assertTrue(c1.getColumnNames().contains("column1"));
        assertTrue(c1.getColumnNames().contains("column2"));
    }

    protected void assertTableGenerator(JpaTableGenerator generator) {
        assertEquals("table-generator", generator.getName());
        assertEquals("auto_pk_table", generator.getTable());
        assertEquals("catalog1", generator.getCatalog());
        assertEquals("schema1", generator.getSchema());
        assertEquals("next_id", generator.getPkColumnName());
        assertEquals("x", generator.getValueColumnName());
        assertEquals("y", generator.getPkColumnValue());
        assertEquals(4, generator.getInitialValue());
        assertEquals(20, generator.getAllocationSize());

        assertEquals(1, generator.getUniqueConstraints().size());
        Iterator<JpaUniqueConstraint> constraintsIt = generator
                .getUniqueConstraints()
                .iterator();
        JpaUniqueConstraint c1 = constraintsIt.next();
        assertTrue(c1.getColumnNames().contains("pk1"));
    }

    protected void assertNamedQuery1(JpaNamedQuery namedQuery) {
        assertEquals("query1", namedQuery.getName());
        assertEquals("select x", namedQuery.getQuery());
        assertEquals(2, namedQuery.getHints().size());

        Iterator<JpaQueryHint> hintIt = namedQuery.getHints().iterator();
        JpaQueryHint h1 = hintIt.next();
        assertEquals("hint1", h1.getName());
        assertEquals("value1", h1.getValue());

        JpaQueryHint h2 = hintIt.next();
        assertEquals("hint2", h2.getName());
        assertEquals("value2", h2.getValue());
    }

    protected void assertNamedQuery2(JpaNamedQuery namedQuery) {
        assertEquals("query2", namedQuery.getName());
        assertEquals("select y", namedQuery.getQuery());
        assertEquals(0, namedQuery.getHints().size());
    }

    protected void assertNativeNamedQuery3(JpaNamedNativeQuery namedQuery) {
        assertEquals("query3", namedQuery.getName());
        assertEquals("select z", namedQuery.getQuery());
        assertEquals("org.apache.cayenne.jpa.entity.MockResultClass", namedQuery
                .getResultClassName());
        assertEquals("rs-mapping1", namedQuery.getResultSetMapping());
        assertEquals(2, namedQuery.getHints().size());

        Iterator<JpaQueryHint> hintIt = namedQuery.getHints().iterator();
        JpaQueryHint h1 = hintIt.next();
        assertEquals("hint3", h1.getName());
        assertEquals("value3", h1.getValue());

        JpaQueryHint h2 = hintIt.next();
        assertEquals("hint4", h2.getName());
        assertEquals("value4", h2.getValue());
    }

    protected void assertNativeNamedQuery4(JpaNamedNativeQuery namedQuery) {
        assertEquals("query4", namedQuery.getName());
        assertEquals("select a", namedQuery.getQuery());
        assertEquals(0, namedQuery.getHints().size());
    }

    protected void assertSqlResultSetMapping(JpaSqlResultSetMapping mapping) {
        assertNotNull(mapping);
        assertEquals("result-map1", mapping.getName());
        assertEquals(2, mapping.getEntityResults().size());

        Iterator<JpaEntityResult> erIt = mapping.getEntityResults().iterator();
        JpaEntityResult er1 = erIt.next();
        assertEquals("org.apache.cayenne.jpa.entity.MockEntityX", er1
                .getEntityClassName());
        assertEquals("column1", er1.getDiscriminatorColumn());
        assertEquals(2, er1.getFieldResults().size());

        Iterator<JpaFieldResult> frIt1 = er1.getFieldResults().iterator();
        JpaFieldResult fr11 = frIt1.next();
        assertEquals("field1", fr11.getName());
        assertEquals("column1", fr11.getColumn());

        JpaFieldResult fr12 = frIt1.next();
        assertEquals("field2", fr12.getName());
        assertEquals("column2", fr12.getColumn());

        JpaEntityResult er2 = erIt.next();
        assertEquals("org.apache.cayenne.jpa.entity.MockEntityY", er2
                .getEntityClassName());
        assertEquals("column2", er2.getDiscriminatorColumn());
        assertEquals(2, er2.getFieldResults().size());

        Iterator<JpaFieldResult> frIt2 = er2.getFieldResults().iterator();
        JpaFieldResult fr21 = frIt2.next();
        assertEquals("field3", fr21.getName());
        assertEquals("column3", fr21.getColumn());

        JpaFieldResult fr22 = frIt2.next();
        assertEquals("field4", fr22.getName());
        assertEquals("column4", fr22.getColumn());

        assertEquals(2, mapping.getColumnResults().size());
        Iterator<JpaColumnResult> crIt = mapping.getColumnResults().iterator();
        assertEquals("column-result1", crIt.next().getName());
        assertEquals("column-result2", crIt.next().getName());
    }

    protected void assertEntityListeners(JpaEntityListeners listeners) {
        assertNotNull(listeners);
        assertEquals(2, listeners.getEntityListeners().size());
        Iterator<JpaEntityListener> elIt = listeners.getEntityListeners().iterator();
        JpaEntityListener listener1 = elIt.next();

        assertEquals("org.apache.cayenne.jpa.entity.MockEntityListener1", listener1
                .getClassName());

        assertNotNull(listener1.getPrePersist());
        assertEquals("prePersist", listener1.getPrePersist().getMethodName());
        assertNotNull(listener1.getPostPersist());
        assertEquals("postPersist", listener1.getPostPersist().getMethodName());
        assertNotNull(listener1.getPreUpdate());
        assertEquals("preUpdate", listener1.getPreUpdate().getMethodName());
        assertNotNull(listener1.getPostUpdate());
        assertEquals("postUpdate", listener1.getPostUpdate().getMethodName());
        assertNotNull(listener1.getPreRemove());
        assertEquals("preRemove", listener1.getPreRemove().getMethodName());
        assertNotNull(listener1.getPostRemove());
        assertEquals("postRemove", listener1.getPostRemove().getMethodName());
        assertNotNull(listener1.getPostLoad());
        assertEquals("postLoad", listener1.getPostLoad().getMethodName());

        JpaEntityListener listener2 = elIt.next();

        assertEquals("org.apache.cayenne.jpa.entity.MockEntityListener2", listener2
                .getClassName());

        assertNull(listener2.getPrePersist());
        assertNotNull(listener2.getPostPersist());
        assertEquals("postPersist", listener2.getPostPersist().getMethodName());
        assertNull(listener2.getPreUpdate());
        assertNull(listener2.getPostUpdate());
        assertNull(listener2.getPreRemove());
        assertNull(listener2.getPostRemove());
        assertNull(listener2.getPostLoad());
    }

    protected void assertId(JpaId id) {
        assertEquals("id1", id.getName());
        assertNotNull(id.getColumn());
        assertEquals("id_column", id.getColumn().getName());
        assertNull("Expected null, got: " + id.getColumn().getColumnDefinition(), id
                .getColumn()
                .getColumnDefinition());
        assertEquals("id_table", id.getColumn().getTable());
        assertEquals(3, id.getColumn().getLength());
        assertEquals(4, id.getColumn().getPrecision());
        assertEquals(5, id.getColumn().getScale());
        assertTrue(id.getColumn().isInsertable());
        assertTrue(id.getColumn().isNullable());
        assertTrue(id.getColumn().isUnique());
        assertTrue(id.getColumn().isUpdatable());

        assertNotNull(id.getGeneratedValue());
        assertSame(GenerationType.SEQUENCE, id.getGeneratedValue().getStrategy());
        assertEquals("id-generator", id.getGeneratedValue().getGenerator());

        assertEquals(TemporalType.TIME, id.getTemporal());
    }

    protected void assertEmbeddedId(JpaEmbeddedId id) {
        assertNotNull(id);
        assertEquals("embeddedId", id.getName());

        assertEquals(2, id.getAttributeOverrides().size());
        List<JpaAttributeOverride> overrides = new ArrayList<JpaAttributeOverride>(id
                .getAttributeOverrides());

        assertEquals("attribute1", overrides.get(0).getName());
        assertEquals("ao_column1", overrides.get(0).getColumn().getName());

        assertEquals("attribute2", overrides.get(1).getName());
        assertEquals("ao_column2", overrides.get(1).getColumn().getName());
    }

    protected void assertMappedSuperclass1(JpaMappedSuperclass mappedSuperclass1) {

        assertNotNull(mappedSuperclass1);

        assertEquals(
                "org.apache.cayenne.jpa.entity.MockMappedSuperclass1",
                mappedSuperclass1.getClassName());

        assertNotNull(mappedSuperclass1.getIdClass());
        assertEquals("org.apache.cayenne.jpa.entity.MockIdClass", mappedSuperclass1
                .getIdClass()
                .getClassName());

        assertTrue(mappedSuperclass1.isExcludeDefaultListeners());
        assertTrue(mappedSuperclass1.isExcludeSuperclassListeners());

        assertNotNull(mappedSuperclass1.getEntityListeners());
        JpaEntityListeners listeners = mappedSuperclass1.getEntityListeners();
        assertEquals(1, listeners.getEntityListeners().size());
        Iterator<JpaEntityListener> elIt = listeners.getEntityListeners().iterator();
        assertEquals("org.apache.cayenne.jpa.entity.MockEntityListener1", elIt
                .next()
                .getClassName());

        assertNotNull(mappedSuperclass1.getPrePersist());
        assertEquals("eprePersist", mappedSuperclass1.getPrePersist().getMethodName());
        assertNotNull(mappedSuperclass1.getPostPersist());
        assertEquals("epostPersist", mappedSuperclass1.getPostPersist().getMethodName());
        assertNotNull(mappedSuperclass1.getPreUpdate());
        assertEquals("epreUpdate", mappedSuperclass1.getPreUpdate().getMethodName());
        assertNotNull(mappedSuperclass1.getPostUpdate());
        assertEquals("epostUpdate", mappedSuperclass1.getPostUpdate().getMethodName());
        assertNotNull(mappedSuperclass1.getPreRemove());
        assertEquals("epreRemove", mappedSuperclass1.getPreRemove().getMethodName());
        assertNotNull(mappedSuperclass1.getPostRemove());
        assertEquals("epostRemove", mappedSuperclass1.getPostRemove().getMethodName());
        assertNotNull(mappedSuperclass1.getPostLoad());
        assertEquals("epostLoad", mappedSuperclass1.getPostLoad().getMethodName());
    }

    protected void assertMappedSuperclass2(JpaMappedSuperclass mappedSuperclass2) {
        assertNotNull(mappedSuperclass2);
        assertEquals(
                "org.apache.cayenne.jpa.entity.MockMappedSuperclass2",
                mappedSuperclass2.getClassName());
    }

    protected void assertMappedSuperclass3(JpaMappedSuperclass mappedSuperclass3) {

        assertNotNull(mappedSuperclass3.getAttributes());
        assertEquals(16, mappedSuperclass3.getAttributes().size());
        assertAttributes(mappedSuperclass3.getAttributes());
    }

    protected void assertAttributes(JpaAttributes attributes) {
        // BASIC
        assertEquals(5, attributes.getBasicAttributes().size());
        JpaBasic a0 = attributes.getBasicAttribute("attribute1");
        assertEquals("attribute1", a0.getName());
        assertTrue(a0.isOptional());
        assertSame(FetchType.EAGER, a0.getFetch());

        // LOB
        assertNotNull(attributes.getBasicAttribute("attribute12"));
        assertTrue(attributes.getBasicAttribute("attribute12").isLob());

        // VERSION
        assertEquals(1, attributes.getVersionAttributes().size());
        JpaVersion a1 = attributes.getVersionAttributes().iterator().next();
        assertEquals("attribute2", a1.getName());

        // ONE-TO_ONE
        assertEquals(1, attributes.getOneToOneRelationships().size());
        JpaOneToOne a2 = attributes.getOneToOneRelationship("attribute3");
        assertEquals("attribute3", a2.getName());
        assertEquals("org.apache.cayenne.jpa.entity.MockTargetEntity1", a2
                .getTargetEntityName());
        assertTrue(a2.isOptional());
        assertSame(FetchType.LAZY, a2.getFetch());
        assertEquals("mb1", a2.getMappedBy());

        assertNotNull(a2.getCascade());
        Collection<CascadeType> cascades = a2.getCascade().getCascades();
        assertEquals(2, cascades.size());
        Iterator<CascadeType> cascades1It = cascades.iterator();
        assertSame(CascadeType.REMOVE, cascades1It.next());
        assertSame(CascadeType.REFRESH, cascades1It.next());

        // ONE-TO-MANY
        assertTrue(attributes.getOneToManyRelationships().size() > 0);
        JpaOneToMany a3 = attributes.getOneToManyRelationships().iterator().next();
        assertEquals("attribute4", a3.getName());
        assertEquals("org.apache.cayenne.jpa.entity.MockTargetEntity2", a3
                .getTargetEntityName());
        assertSame(FetchType.LAZY, a3.getFetch());
        assertEquals("mb2", a3.getMappedBy());
        assertNotNull(a3.getCascade());
        assertEquals(2, a3.getCascade().getCascades().size());
        Iterator<CascadeType> cascades2It = a3.getCascade().getCascades().iterator();
        assertSame(CascadeType.PERSIST, cascades2It.next());
        assertSame(CascadeType.MERGE, cascades2It.next());

        // JOIN COLUMN
        JpaOneToMany a9 = attributes.getOneToManyRelationship("attribute10");
        assertNotNull(a9.getJoinColumns());
        assertEquals(1, a9.getJoinColumns().size());
        JpaJoinColumn joinColumn = a9.getJoinColumns().iterator().next();
        assertEquals("join-column-10", joinColumn.getName());
        assertEquals("x-def", joinColumn.getColumnDefinition());
        assertEquals("x-ref", joinColumn.getReferencedColumnName());
        assertEquals("jt1", joinColumn.getTable());
        assertTrue(joinColumn.isInsertable());
        assertTrue(joinColumn.isNullable());
        assertTrue(joinColumn.isUnique());
        assertTrue(joinColumn.isUpdatable());

    }
}
