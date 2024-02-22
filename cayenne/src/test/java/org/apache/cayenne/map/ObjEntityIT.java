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

import org.apache.cayenne.GenericPersistentObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.util.Util;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjEntityIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private CayenneRuntime runtime;

    @Test
    public void testGetAttributeWithOverrides() {

        DataMap map = new DataMap("dm");

        ObjEntity superEntity = new ObjEntity("super");
        superEntity.addAttribute(new ObjAttribute("a1", "int", superEntity));
        superEntity.addAttribute(new ObjAttribute("a2", "int", superEntity));

        map.addObjEntity(superEntity);

        ObjEntity subEntity = new ObjEntity("sub");
        subEntity.setSuperEntityName(superEntity.getName());
        subEntity.addAttributeOverride("a1", "overridden.path");
        subEntity.addAttribute(new ObjAttribute("a3", "int", subEntity));

        map.addObjEntity(subEntity);

        ObjAttribute a1 = subEntity.getAttribute("a1");
        assertNotNull(a1);
        assertSame(subEntity, a1.getEntity());
        assertEquals("overridden.path", a1.getDbAttributePath().value());
        assertEquals("int", a1.getType());

        ObjAttribute a2 = subEntity.getAttribute("a2");
        assertNotNull(a2);
        assertSame(subEntity, a2.getEntity());
        assertNull(a2.getDbAttributePath());

        ObjAttribute a3 = subEntity.getAttribute("a3");
        assertNotNull(a3);
        assertSame(subEntity, a3.getEntity());
    }

    @Test
    public void testGetPrimaryKeys() {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        Collection<ObjAttribute> pks = artistE.getPrimaryKeys();
        assertEquals(1, pks.size());

        ObjAttribute pk = pks.iterator().next();
        assertEquals("java.lang.Long", pk.getType());
        assertEquals("ARTIST_ID", pk.getDbAttributePath().value());
        assertEquals("artistId", pk.getName());
        assertNull(pk.getEntity());
        assertFalse(artistE.getAttributes().contains(pk));

        ObjEntity meaningfulPKE = runtime.getDataDomain().getEntityResolver().getObjEntity("MeaningfulGeneratedColumnTestEntity");
        Collection<ObjAttribute> mpks = meaningfulPKE.getPrimaryKeys();
        assertEquals(1, mpks.size());

        ObjAttribute mpk = mpks.iterator().next();
        assertTrue(meaningfulPKE.getAttributes().contains(mpk));
    }

    @Test
    public void testAttributes() {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        ObjAttribute attr = artistE.getAttribute("artistName");

        assertEquals(attr.getMaxLength(), attr.getDbAttribute().getMaxLength());
        assertEquals(attr.isMandatory(), attr.getDbAttribute().isMandatory());
    }

    @Test
    public void testLastPathComponent() {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");

        Map<String, String> aliases = new HashMap<>();
        aliases.put("a", "paintingArray.toGallery");

        PathComponent<ObjAttribute, ObjRelationship> lastAttribute = artistE.lastPathComponent(
                ExpressionFactory.exp("paintingArray.paintingTitle"), aliases);
        assertTrue(lastAttribute.getAttribute() != null);
        assertEquals("paintingTitle", lastAttribute.getAttribute().getName());

        PathComponent<ObjAttribute, ObjRelationship> lastRelationship = artistE.lastPathComponent(
                ExpressionFactory.exp("paintingArray.toGallery"), aliases);
        assertTrue(lastRelationship.getRelationship() != null);
        assertEquals("toGallery", lastRelationship.getRelationship().getName());

        PathComponent<ObjAttribute, ObjRelationship> lastLeftJoinRelationship = artistE.lastPathComponent(
                new ASTObjPath("paintingArray+.toGallery+"), aliases);
        assertTrue(lastLeftJoinRelationship.getRelationship() != null);
        assertEquals("toGallery", lastLeftJoinRelationship.getRelationship().getName());

        PathComponent<ObjAttribute, ObjRelationship> lastAliasedRelationship = artistE.lastPathComponent(
                new ASTObjPath("a"), aliases);
        assertTrue(lastAliasedRelationship.getRelationship() != null);
        assertEquals("toGallery", lastAliasedRelationship.getRelationship().getName());
    }

    @Test
    public void testGeneric() {
        ObjEntity e1 = new ObjEntity("e1");
        assertTrue(e1.isGeneric());

        e1.setClassName("SomeClass");
        assertFalse(e1.isGeneric());

        DataMap m = new DataMap("X");
        m.setDefaultSuperclass("SomeClass");
        m.addObjEntity(e1);

        assertTrue(e1.isGeneric());

        e1.setClassName("SomeOtherClass");
        assertFalse(e1.isGeneric());

        e1.setClassName(GenericPersistentObject.class.getName());
        assertTrue(e1.isGeneric());
    }

    @Test
    public void testGetPrimaryKeyNames() {
        ObjEntity entity = new ObjEntity("entity");
        DbEntity dbentity = new DbEntity("dbe");

        // need a container
        DataMap dataMap = new DataMap();
        dataMap.addObjEntity(entity);
        dataMap.addDbEntity(dbentity);
        entity.setDbEntity(dbentity);

        // Test correctness with no mapped PK.
        assertEquals(0, entity.getPrimaryKeyNames().size());

        // Add a single column PK to the DB entity.
        DbAttribute pk = new DbAttribute();
        pk.setName("id");
        pk.setPrimaryKey(true);
        dbentity.addAttribute(pk);

        // Test correctness with a single column PK.
        assertEquals(1, entity.getPrimaryKeyNames().size());
        assertTrue(entity.getPrimaryKeyNames().contains(pk.getName()));

        // Add a multi-column PK to the DB entity.
        DbAttribute pk2 = new DbAttribute();
        pk2.setName("id2");
        pk2.setPrimaryKey(true);
        dbentity.addAttribute(pk2);

        // Test correctness with a multi-column PK.
        assertEquals(2, entity.getPrimaryKeyNames().size());
        assertTrue(entity.getPrimaryKeyNames().contains(pk.getName()));
        assertTrue(entity.getPrimaryKeyNames().contains(pk2.getName()));
    }

    @Test
    public void testSerializability() throws Exception {
        ObjEntity entity = new ObjEntity("entity");

        ObjEntity d1 = Util.cloneViaSerialization(entity);
        assertEquals(entity.getName(), d1.getName());
    }

    @Test
    public void testDbEntityName() {
        ObjEntity entity = new ObjEntity("entity");
        assertNull(entity.getDbEntityName());

        entity.setDbEntityName("dbe");
        assertEquals("dbe", entity.getDbEntityName());

        entity.setDbEntityName(null);
        assertNull(entity.getDbEntityName());
    }

    @Test
    public void testDbEntity() {
        ObjEntity entity = new ObjEntity("entity");
        DbEntity dbentity = new DbEntity("dbe");

        // need a container
        DataMap dataMap = new DataMap();
        dataMap.addObjEntity(entity);
        dataMap.addDbEntity(dbentity);

        assertNull(entity.getDbEntity());

        entity.setDbEntity(dbentity);
        assertSame(dbentity, entity.getDbEntity());

        entity.setDbEntity(null);
        assertNull(entity.getDbEntity());

        entity.setDbEntityName("dbe");
        assertSame(dbentity, entity.getDbEntity());
    }

    @Test
    public void testDbEntityNoContainer() {
        ObjEntity entity = new ObjEntity("entity");
        entity.setDbEntityName("dbe");

        try {
            entity.getDbEntity();
            fail("Without a container ObjENtity shouldn't resolve DbEntity");
        } catch (CayenneRuntimeException ex) {
            // expected
        }
    }

    @Test
    public void testClassName() {
        ObjEntity entity = new ObjEntity("entity");
        String tstName = "tst_name";
        entity.setClassName(tstName);
        assertEquals(tstName, entity.getClassName());
    }

    @Test
    public void testSuperClassName() {
        ObjEntity entity = new ObjEntity("entity");
        String tstName = "super_tst_name";
        entity.setSuperClassName(tstName);
        assertEquals(tstName, entity.getSuperClassName());
    }

    @Test
    public void testAttributeForDbAttribute() throws Exception {
        ObjEntity ae = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        DbEntity dae = ae.getDbEntity();

        assertNull(ae.getAttributeForDbAttribute(dae.getAttribute("ARTIST_ID")));
        assertNotNull(ae.getAttributeForDbAttribute(dae.getAttribute("ARTIST_NAME")));
    }

    @Test
    public void testRelationshipForDbRelationship() throws Exception {
        ObjEntity ae = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        DbEntity dae = ae.getDbEntity();

        assertNull(ae.getRelationshipForDbRelationship(new DbRelationship()));
        assertNotNull(ae.getRelationshipForDbRelationship(dae.getRelationship("paintingArray")));
    }

    @Test
    public void testReadOnly() throws Exception {
        ObjEntity entity = new ObjEntity("entity");
        assertFalse(entity.isReadOnly());
        entity.setReadOnly(true);
        assertTrue(entity.isReadOnly());
    }

    @Test
    public void testTranslateToRelatedEntityIndependentPath() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Expression e1 = ExpressionFactory.exp("paintingArray");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, ExpressionFactory.exp("db:toArtist.paintingArray"), translated);
    }

    @Test
    public void testTranslateToRelatedEntityTrimmedPath() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Expression e1 = ExpressionFactory.exp("artistExhibitArray.toExhibit");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, ExpressionFactory.exp("db:toArtist.artistExhibitArray.toExhibit"),
                translated);
    }

    @Test
    public void testTranslateToRelatedEntitySplitHalfWay() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Expression e1 = ExpressionFactory.exp("paintingArray.toPaintingInfo.textReview");
        Expression translated = artistE.translateToRelatedEntity(e1, "paintingArray.toGallery");
        assertEquals("failure: " + translated,
                ExpressionFactory.exp("db:paintingArray.toArtist.paintingArray.toPaintingInfo.TEXT_REVIEW"), translated);
    }

    @Test
    public void testTranslateToRelatedEntityMatchingPath() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);
        Expression e1 = ExpressionFactory.exp("artistExhibitArray.toExhibit");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray.toExhibit");
        assertEquals("failure: " + translated,
                ExpressionFactory.exp("db:artistExhibitArray.toArtist.artistExhibitArray.toExhibit"), translated);
    }

    @Test
    public void testTranslateToRelatedEntityMultiplePaths() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Expression e1 = ExpressionFactory.exp("paintingArray = $p and artistExhibitArray.toExhibit.closingDate = $d");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, ExpressionFactory.exp("db:toArtist.paintingArray = $p "
                + "and db:toArtist.artistExhibitArray.toExhibit.CLOSING_DATE = $d"), translated);
    }

    @Test
    public void testTranslateToRelatedEntityOuterJoin_Flattened() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Expression e1 = ExpressionFactory.exp("groupArray+.name");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, ExpressionFactory.exp("db:toArtist.artistGroupArray+.toGroup+.NAME"), translated);
    }

    @Test
    public void testTranslateNullArg() {
        ObjEntity entity = context.getEntityResolver().getObjEntity("Artist");

        Expression exp = ExpressionFactory.noMatchExp("dateOfBirth", null);
        Expression translated = entity.translateToDbPath(exp);

        assertFalse(translated.match(new Artist()));
    }
}
