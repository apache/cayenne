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

package org.apache.cayenne.map;

import java.util.Collection;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.Util;

public class DbEntityTest extends CayenneCase {

    public void testSerializability() throws Exception {
        DbEntity entity = new DbEntity("entity");

        DbAttribute pk = new DbAttribute("pk");
        pk.setPrimaryKey(true);
        entity.addAttribute(pk);

        DbAttribute generated = new DbAttribute("generated");
        generated.setGenerated(true);
        entity.addAttribute(generated);

        DbEntity d2 = (DbEntity) Util.cloneViaSerialization(entity);

        assertNotNull(d2.getPrimaryKeys());
        assertEquals(entity.getPrimaryKeys().size(), d2.getPrimaryKeys().size());

        DbAttribute pk2 = (DbAttribute) d2.getAttribute(pk.getName());
        assertNotNull(pk2);
        assertTrue(d2.getPrimaryKeys().contains(pk2));

        assertNotNull(d2.getGeneratedAttributes());
        assertEquals(entity.getGeneratedAttributes().size(), d2
                .getGeneratedAttributes()
                .size());

        DbAttribute generated2 = (DbAttribute) d2.getAttribute(generated.getName());
        assertNotNull(generated2);
        assertTrue(d2.getGeneratedAttributes().contains(generated2));
    }

    public void testSerializabilityWithHessian() throws Exception {
        DbEntity entity = new DbEntity("entity");

        DbAttribute pk = new DbAttribute("pk");
        pk.setPrimaryKey(true);
        entity.addAttribute(pk);

        DbAttribute generated = new DbAttribute("generated");
        generated.setGenerated(true);
        entity.addAttribute(generated);

        DbEntity d2 = (DbEntity) HessianUtil.cloneViaClientServerSerialization(
                entity,
                new EntityResolver());

        assertNotNull(d2.getPrimaryKeys());
        assertEquals(entity.getPrimaryKeys().size(), d2.getPrimaryKeys().size());

        DbAttribute pk2 = (DbAttribute) d2.getAttribute(pk.getName());
        assertNotNull(pk2);
        assertTrue(d2.getPrimaryKeys().contains(pk2));

        assertNotNull(d2.getGeneratedAttributes());
        assertEquals(entity.getGeneratedAttributes().size(), d2
                .getGeneratedAttributes()
                .size());

        DbAttribute generated2 = (DbAttribute) d2.getAttribute(generated.getName());
        assertNotNull(generated2);
        assertTrue(d2.getGeneratedAttributes().contains(generated2));
    }

    public void testConstructor1() {
        DbEntity ent = new DbEntity();
        assertNull(ent.getName());
    }

    public void testConstructor2() {
        DbEntity ent = new DbEntity("abc");
        assertEquals("abc", ent.getName());
    }

    public void testCatalog() {
        String tstName = "tst_name";
        DbEntity ent = new DbEntity("abc");
        ent.setCatalog(tstName);
        assertEquals(tstName, ent.getCatalog());
    }

    public void testSchema() {
        String tstName = "tst_name";
        DbEntity ent = new DbEntity("abc");
        ent.setSchema(tstName);
        assertEquals(tstName, ent.getSchema());
    }

    public void testFullyQualifiedName() {
        DbEntity ent = new DbEntity("abc");

        String tstName = "tst_name";
        String schemaName = "tst_schema_name";
        ent.setName(tstName);

        assertEquals(tstName, ent.getName());
        assertEquals(tstName, ent.getFullyQualifiedName());

        ent.setSchema(schemaName);

        assertEquals(tstName, ent.getName());
        assertEquals(schemaName + "." + tstName, ent.getFullyQualifiedName());
    }

    public void testGetPrimaryKey() {
        DbEntity ent = new DbEntity("abc");

        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);
        ent.addAttribute(a1);

        DbAttribute a2 = new DbAttribute();
        a2.setName("a2");
        a2.setPrimaryKey(true);
        ent.addAttribute(a2);

        Collection<DbAttribute> pk = ent.getPrimaryKeys();
        assertNotNull(pk);
        assertEquals(1, pk.size());
        assertSame(a2, pk.iterator().next());
    }

    public void testAddPKAttribute() {
        DbEntity ent = new DbEntity("abc");

        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);

        assertTrue(ent.getPrimaryKeys().isEmpty());
        ent.addAttribute(a1);
        assertTrue(ent.getPrimaryKeys().isEmpty());
    }

    public void testChangeAttributeToPK() {
        DbEntity ent = new DbEntity("abc");

        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);
        ent.addAttribute(a1);

        assertFalse(ent.getPrimaryKeys().contains(a1));
        a1.setPrimaryKey(true);
        assertTrue(ent.getPrimaryKeys().contains(a1));
    }

    public void testChangePKAttribute() {
        DbEntity ent = new DbEntity("abc");

        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(true);
        ent.addAttribute(a1);

        assertTrue(ent.getPrimaryKeys().contains(a1));
        a1.setPrimaryKey(false);
        assertFalse(ent.getPrimaryKeys().contains(a1));
    }

    public void testRemoveAttribute() {
        DbEntity ent = new DbEntity("abc");

        DataMap map = new DataMap("map");
        ent.setName("ent");
        map.addDbEntity(ent);

        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);
        ent.addAttribute(a1);

        DbEntity otherEntity = new DbEntity("22ent1");
        assertNotNull(otherEntity.getName());
        map.addDbEntity(otherEntity);
        DbAttribute a11 = new DbAttribute();
        a11.setName("a11");
        a11.setPrimaryKey(false);
        otherEntity.addAttribute(a11);

        DbRelationship rel = new DbRelationship("relfrom");
        ent.addRelationship(rel);
        rel.setTargetEntity(otherEntity);
        rel.addJoin(new DbJoin(rel, "a1", "a11"));

        DbRelationship rel1 = new DbRelationship("relto");
        otherEntity.addRelationship(rel1);
        rel1.setTargetEntity(ent);
        rel1.addJoin(new DbJoin(rel1, "a11", "a1"));

        // check that the test case is working
        assertSame(a1, ent.getAttribute(a1.getName()));
        assertSame(rel, ent.getRelationship(rel.getName()));

        // test removal
        ent.removeAttribute(a1.getName());

        assertNull(ent.getAttribute(a1.getName()));
        assertEquals(0, rel1.getJoins().size());
        assertEquals(0, rel.getJoins().size());
    }

    public void testTranslateToRelatedEntityIndependentPath() {
        DbEntity artistE = getDomain().getEntityResolver().getDbEntity("ARTIST");

        Expression e1 = Expression.fromString("db:paintingArray");
        Expression translated = artistE
                .translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, Expression
                .fromString("db:toArtist.paintingArray"), translated);
    }

    public void testTranslateToRelatedEntityTrimmedPath() {
        DbEntity artistE = getDomain().getEntityResolver().getDbEntity("ARTIST");

        Expression e1 = Expression.fromString("db:artistExhibitArray.toExhibit");
        Expression translated = artistE
                .translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals(
                "failure: " + translated,
                Expression.fromString("db:toExhibit"),
                translated);
    }

    public void testTranslateToRelatedEntitySplitHalfWay() {
        DbEntity artistE = getDomain().getEntityResolver().getDbEntity("ARTIST");

        Expression e1 = Expression
                .fromString("db:paintingArray.toPaintingInfo.TEXT_REVIEW");
        Expression translated = artistE.translateToRelatedEntity(
                e1,
                "paintingArray.toGallery");
        assertEquals("failure: " + translated, Expression
                .fromString("db:paintingArray.toPaintingInfo.TEXT_REVIEW"), translated);
    }

    public void testTranslateToRelatedEntityMatchingPath() {
        DbEntity artistE = getDomain().getEntityResolver().getDbEntity("ARTIST");

        Expression e1 = Expression.fromString("db:artistExhibitArray.toExhibit");
        Expression translated = artistE.translateToRelatedEntity(
                e1,
                "artistExhibitArray.toExhibit");

        assertEquals("failure: " + translated, Expression
                .fromString("db:artistExhibitArray.toExhibit"), translated);
    }
}
