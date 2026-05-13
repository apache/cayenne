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

package org.apache.cayenne.map;

import java.util.Collection;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.apache.cayenne.util.Util;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

public class DbEntityIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void serializability() throws Exception {
        DbEntity entity = new DbEntity("entity");

        DbAttribute pk = new DbAttribute("pk");
        pk.setPrimaryKey(true);
        entity.addAttribute(pk);

        DbAttribute generated = new DbAttribute("generated");
        generated.setGenerated(true);
        entity.addAttribute(generated);

        DbEntity d2 = Util.cloneViaSerialization(entity);

        assertNotNull(d2.getPrimaryKeys());
        assertEquals(entity.getPrimaryKeys().size(), d2.getPrimaryKeys().size());

        DbAttribute pk2 = d2.getAttribute(pk.getName());
        assertNotNull(pk2);
        assertTrue(d2.getPrimaryKeys().contains(pk2));

        assertNotNull(d2.getGeneratedAttributes());
        assertEquals(entity.getGeneratedAttributes().size(), d2.getGeneratedAttributes().size());

        DbAttribute generated2 = d2.getAttribute(generated.getName());
        assertNotNull(generated2);
        assertTrue(d2.getGeneratedAttributes().contains(generated2));
    }

    @Test
    public void constructor1() {
        DbEntity ent = new DbEntity();
        assertNull(ent.getName());
    }

    @Test
    public void constructor2() {
        DbEntity ent = new DbEntity("abc");
        assertEquals("abc", ent.getName());
    }

    @Test
    public void catalog() {
        String tstName = "tst_name";
        DbEntity ent = new DbEntity("abc");
        ent.setCatalog(tstName);
        assertEquals(tstName, ent.getCatalog());
    }

    @Test
    public void schema() {
        String tstName = "tst_name";
        DbEntity ent = new DbEntity("abc");
        ent.setSchema(tstName);
        assertEquals(tstName, ent.getSchema());
    }

    @Test
    public void fullyQualifiedName() {

        DbEntity e1 = new DbEntity("e1");
        assertEquals("e1", e1.getFullyQualifiedName());

        DbEntity e2 = new DbEntity("e2");
        e2.setSchema("s2");
        assertEquals("e2", e2.getName());
        assertEquals("s2.e2", e2.getFullyQualifiedName());

        DbEntity e3 = new DbEntity("e3");
        e3.setSchema("s3");
        e3.setCatalog("c3");
        assertEquals("e3", e3.getName());
        assertEquals("c3.s3.e3", e3.getFullyQualifiedName());

        DbEntity e4 = new DbEntity("e4");
        e4.setCatalog("c4");
        assertEquals("e4", e4.getName());
        assertEquals("c4.e4", e4.getFullyQualifiedName());
    }

    @Test
    public void getPrimaryKey() {
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

    @Test
    public void addPKAttribute() {
        DbEntity ent = new DbEntity("abc");

        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);

        assertTrue(ent.getPrimaryKeys().isEmpty());
        ent.addAttribute(a1);
        assertTrue(ent.getPrimaryKeys().isEmpty());
    }

    @Test
    public void changeAttributeToPK() {
        DbEntity ent = new DbEntity("abc");

        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);
        ent.addAttribute(a1);

        assertFalse(ent.getPrimaryKeys().contains(a1));
        a1.setPrimaryKey(true);
        assertTrue(ent.getPrimaryKeys().contains(a1));
    }

    @Test
    public void changePKAttribute() {
        DbEntity ent = new DbEntity("abc");

        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(true);
        ent.addAttribute(a1);

        assertTrue(ent.getPrimaryKeys().contains(a1));
        a1.setPrimaryKey(false);
        assertFalse(ent.getPrimaryKeys().contains(a1));
    }

    @Test
    public void removeAttribute() {
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
        rel.setTargetEntityName(otherEntity);
        rel.addJoin(new DbJoin(rel, "a1", "a11"));

        DbRelationship rel1 = new DbRelationship("relto");
        otherEntity.addRelationship(rel1);
        rel1.setTargetEntityName(ent);
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

    @Test
    public void translateToRelatedEntityIndependentPath() {
        DbEntity artistE = env.runtime().getDataDomain().getEntityResolver().getDbEntity("ARTIST");

        Expression e1 = ExpressionFactory.exp("db:paintingArray");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals(ExpressionFactory.exp("db:toArtist.paintingArray"), translated, "failure: " + translated);
    }

    @Test
    public void translateToRelatedEntityTrimmedPath() {
        DbEntity artistE = env.runtime().getDataDomain().getEntityResolver().getDbEntity("ARTIST");

        Expression e1 = ExpressionFactory.exp("db:artistExhibitArray.toExhibit");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals(ExpressionFactory.exp("db:toArtist.artistExhibitArray.toExhibit"),
                translated, "failure: " + translated);
    }

    @Test
    public void translateToRelatedEntitySplitHalfWay() {
        DbEntity artistE = env.runtime().getDataDomain().getEntityResolver().getDbEntity("ARTIST");

        Expression e1 = ExpressionFactory.exp("db:paintingArray.toPaintingInfo.TEXT_REVIEW");
        Expression translated = artistE.translateToRelatedEntity(e1, "paintingArray.toGallery");
        assertEquals(
                ExpressionFactory.exp("db:paintingArray.toArtist.paintingArray.toPaintingInfo.TEXT_REVIEW"), translated,
                "failure: " + translated);
    }

    @Test
    public void translateToRelatedEntityMatchingPath() {
        DbEntity artistE = env.runtime().getDataDomain().getEntityResolver().getDbEntity("ARTIST");

        Expression e1 = ExpressionFactory.exp("db:artistExhibitArray.toExhibit");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray.toExhibit");

        assertEquals(
                ExpressionFactory.exp("db:artistExhibitArray.toArtist.artistExhibitArray.toExhibit"), translated,
                "failure: " + translated);
    }

    @Test
    public void translateToRelatedEntityToOne() {
        DbEntity paintingE = env.runtime().getDataDomain().getEntityResolver().getDbEntity("PAINTING");

        Expression e1 = ExpressionFactory.exp("db:toArtist.ARTIST_NAME = 'aa'");
        Expression translated = paintingE.translateToRelatedEntity(e1, "toArtist");

        assertEquals(ExpressionFactory.exp("db:paintingArray.toArtist.ARTIST_NAME = 'aa'"), translated,
                "failure: " + translated);
    }
}
