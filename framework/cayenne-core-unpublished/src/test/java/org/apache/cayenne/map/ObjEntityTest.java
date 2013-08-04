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
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.Util;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ObjEntityTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

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
        assertEquals("overridden.path", a1.getDbAttributePath());
        assertEquals("int", a1.getType());

        ObjAttribute a2 = subEntity.getAttribute("a2");
        assertNotNull(a2);
        assertSame(subEntity, a2.getEntity());
        assertNull(a2.getDbAttributePath());

        ObjAttribute a3 = subEntity.getAttribute("a3");
        assertNotNull(a3);
        assertSame(subEntity, a3.getEntity());
    }

    public void testGetPrimaryKeys() {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        Collection<ObjAttribute> pks = artistE.getPrimaryKeys();
        assertEquals(1, pks.size());

        ObjAttribute pk = pks.iterator().next();
        assertEquals("java.lang.Long", pk.getType());
        assertEquals("ARTIST_ID", pk.getDbAttributePath());
        assertEquals("artistId", pk.getName());
        assertNull(pk.getEntity());
        assertFalse(artistE.getAttributes().contains(pk));

        ObjEntity clientArtistE = artistE.getClientEntity();
        Collection<ObjAttribute> clientpks = clientArtistE.getPrimaryKeys();
        assertEquals(1, clientpks.size());
        ObjAttribute clientPk = clientpks.iterator().next();
        assertEquals("java.lang.Long", clientPk.getType());
        assertEquals("ARTIST_ID", clientPk.getDbAttributePath());
        assertEquals("artistId", clientPk.getName());
        assertNull(clientPk.getEntity());
        assertFalse(clientArtistE.getAttributes().contains(pk));

        ObjEntity meaningfulPKE = runtime.getDataDomain().getEntityResolver().getObjEntity("MeaningfulPKTest1");
        Collection<ObjAttribute> mpks = meaningfulPKE.getPrimaryKeys();
        assertEquals(1, mpks.size());

        ObjAttribute mpk = mpks.iterator().next();
        assertTrue(meaningfulPKE.getAttributes().contains(mpk));

        ObjEntity clientMeaningfulPKE = meaningfulPKE.getClientEntity();
        Collection<ObjAttribute> clientmpks = clientMeaningfulPKE.getPrimaryKeys();
        assertEquals(1, clientmpks.size());

        ObjAttribute clientmpk = clientmpks.iterator().next();
        assertEquals("java.lang.Integer", clientmpk.getType());
        assertTrue(clientMeaningfulPKE.getAttributes().contains(clientmpk));
    }

    public void testAttributes() {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        ObjAttribute attr = artistE.getAttribute("artistName");

        assertEquals(attr.getMaxLength(), attr.getDbAttribute().getMaxLength());
        assertEquals(attr.isMandatory(), attr.getDbAttribute().isMandatory());
    }

    public void testLastPathComponent() {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");

        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put("a", "paintingArray.toGallery");

        PathComponent<ObjAttribute, ObjRelationship> lastAttribute = artistE.lastPathComponent(
                Expression.fromString("paintingArray.paintingTitle"), aliases);
        assertTrue(lastAttribute.getAttribute() != null);
        assertEquals("paintingTitle", lastAttribute.getAttribute().getName());

        PathComponent<ObjAttribute, ObjRelationship> lastRelationship = artistE.lastPathComponent(
                Expression.fromString("paintingArray.toGallery"), aliases);
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

        e1.setClassName(CayenneDataObject.class.getName());
        assertTrue(e1.isGeneric());
    }

    public void testServerOnly() {
        ObjEntity e1 = new ObjEntity("e1");

        assertFalse(e1.isServerOnly());
        e1.setServerOnly(true);
        assertTrue(e1.isServerOnly());
    }

    public void testClientAllowed() {
        ObjEntity e1 = new ObjEntity("e1");

        assertFalse("No parent DataMap should have automatically disabled client.", e1.isClientAllowed());

        DataMap map = new DataMap("m1");
        e1.setDataMap(map);

        assertFalse(e1.isClientAllowed());

        map.setClientSupported(true);
        assertTrue(e1.isClientAllowed());

        e1.setServerOnly(true);
        assertFalse(e1.isClientAllowed());
    }

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

    public void testGetClientEntity() {

        DataMap map = new DataMap();
        map.setClientSupported(true);

        final ObjEntity target = new ObjEntity("te1");
        map.addObjEntity(target);

        ObjEntity e1 = new ObjEntity("entity");
        e1.setClassName("x.y.z");
        e1.setClientClassName("a.b.c");
        e1.addAttribute(new ObjAttribute("A1"));
        e1.addAttribute(new ObjAttribute("A2"));
        map.addObjEntity(e1);

        DbEntity dbentity = new DbEntity("dbe");
        map.addDbEntity(dbentity);
        e1.setDbEntity(dbentity);

        ObjRelationship r1 = new ObjRelationship("r1") {

            @Override
            public ObjEntity getTargetEntity() {
                return target;
            }
        };

        e1.addRelationship(r1);

        ObjEntity e2 = e1.getClientEntity();
        assertNotNull(e2);
        assertEquals(e1.getName(), e2.getName());
        assertEquals(e1.getClientClassName(), e2.getClassName());
        assertEquals(e1.getAttributes().size(), e2.getAttributes().size());
        assertEquals(e1.getRelationships().size(), e2.getRelationships().size());
    }

    public void testSerializability() throws Exception {
        ObjEntity entity = new ObjEntity("entity");

        ObjEntity d1 = Util.cloneViaSerialization(entity);
        assertEquals(entity.getName(), d1.getName());
    }

    public void testDbEntityName() {
        ObjEntity entity = new ObjEntity("entity");
        assertNull(entity.getDbEntityName());

        entity.setDbEntityName("dbe");
        assertEquals("dbe", entity.getDbEntityName());

        entity.setDbEntityName(null);
        assertNull(entity.getDbEntityName());
    }

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

    public void testClassName() {
        ObjEntity entity = new ObjEntity("entity");
        String tstName = "tst_name";
        entity.setClassName(tstName);
        assertEquals(tstName, entity.getClassName());
    }

    public void testSuperClassName() {
        ObjEntity entity = new ObjEntity("entity");
        String tstName = "super_tst_name";
        entity.setSuperClassName(tstName);
        assertEquals(tstName, entity.getSuperClassName());
    }

    public void testAttributeForDbAttribute() throws Exception {
        ObjEntity ae = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        DbEntity dae = ae.getDbEntity();

        assertNull(ae.getAttributeForDbAttribute(dae.getAttribute("ARTIST_ID")));
        assertNotNull(ae.getAttributeForDbAttribute(dae.getAttribute("ARTIST_NAME")));
    }

    public void testRelationshipForDbRelationship() throws Exception {
        ObjEntity ae = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        DbEntity dae = ae.getDbEntity();

        assertNull(ae.getRelationshipForDbRelationship(new DbRelationship()));
        assertNotNull(ae.getRelationshipForDbRelationship(dae.getRelationship("paintingArray")));
    }

    public void testReadOnly() throws Exception {
        ObjEntity entity = new ObjEntity("entity");
        assertFalse(entity.isReadOnly());
        entity.setReadOnly(true);
        assertTrue(entity.isReadOnly());
    }

    public void testTranslateToRelatedEntityIndependentPath() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Expression e1 = Expression.fromString("paintingArray");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, Expression.fromString("db:toArtist.paintingArray"), translated);
    }

    public void testTranslateToRelatedEntityTrimmedPath() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Expression e1 = Expression.fromString("artistExhibitArray.toExhibit");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, Expression.fromString("db:toArtist.artistExhibitArray.toExhibit"),
                translated);
    }

    public void testTranslateToRelatedEntitySplitHalfWay() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Expression e1 = Expression.fromString("paintingArray.toPaintingInfo.textReview");
        Expression translated = artistE.translateToRelatedEntity(e1, "paintingArray.toGallery");
        assertEquals("failure: " + translated,
                Expression.fromString("db:paintingArray.toArtist.paintingArray.toPaintingInfo.TEXT_REVIEW"), translated);
    }

    public void testTranslateToRelatedEntityMatchingPath() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);
        Expression e1 = Expression.fromString("artistExhibitArray.toExhibit");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray.toExhibit");
        assertEquals("failure: " + translated,
                Expression.fromString("db:artistExhibitArray.toArtist.artistExhibitArray.toExhibit"), translated);
    }

    public void testTranslateToRelatedEntityMultiplePaths() throws Exception {
        ObjEntity artistE = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Expression e1 = Expression.fromString("paintingArray = $p and artistExhibitArray.toExhibit.closingDate = $d");
        Expression translated = artistE.translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, Expression.fromString("db:toArtist.paintingArray = $p "
                + "and db:toArtist.artistExhibitArray.toExhibit.CLOSING_DATE = $d"), translated);
    }

    public void testTranslateNullArg() {
        ObjEntity entity = context.getEntityResolver().getObjEntity("Artist");

        Expression exp = ExpressionFactory.noMatchExp("dateOfBirth", null);
        Expression translated = entity.translateToDbPath(exp);

        assertFalse(translated.match(new Artist()));
    }
}
