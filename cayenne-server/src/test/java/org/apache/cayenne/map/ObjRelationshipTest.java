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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.testdo.inheritance.vertical.Iv2Sub1;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ObjRelationshipTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    private DbEntity artistDBEntity;
    private DbEntity artistExhibitDBEntity;
    private DbEntity exhibitDBEntity;
    private DbEntity paintingDbEntity;
    private DbEntity galleryDBEntity;

    @Override
    protected void setUpAfterInjection() throws Exception {
        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        artistDBEntity = resolver.getDbEntity("ARTIST");
        artistExhibitDBEntity = resolver.getDbEntity("ARTIST_EXHIBIT");
        exhibitDBEntity = resolver.getDbEntity("EXHIBIT");
        paintingDbEntity = resolver.getDbEntity("PAINTING");
        galleryDBEntity = resolver.getDbEntity("GALLERY");
    }

    public void testEncodeAsXML() {
        StringWriter buffer = new StringWriter();
        PrintWriter out = new PrintWriter(buffer);
        XMLEncoder encoder = new XMLEncoder(out);

        DataMap map = new DataMap("M");
        ObjEntity source = new ObjEntity("S");
        ObjEntity target = new ObjEntity("T");
        map.addObjEntity(source);
        map.addObjEntity(target);

        ObjRelationship r = new ObjRelationship("X");
        r.setSourceEntity(source);
        r.setTargetEntityName("T");
        r.setCollectionType("java.util.Map");
        r.setMapKey("bla");

        r.encodeAsXML(encoder);
        out.close();

        String lineBreak = System.getProperty("line.separator");

        assertEquals("<obj-relationship name=\"X\" source=\"S\" target=\"T\" "
                + "collection-type=\"java.util.Map\" map-key=\"bla\"/>" + lineBreak, buffer.getBuffer().toString());
    }

    public void testCollectionType() {
        ObjRelationship r = new ObjRelationship("X");
        assertNull(r.getCollectionType());
        r.setCollectionType("java.util.Map");
        assertEquals("java.util.Map", r.getCollectionType());
    }

    public void testSerializability() throws Exception {
        ObjEntity artistObjEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");

        // start with "to many"
        ObjRelationship r1 = artistObjEnt.getRelationship("paintingArray");

        ObjRelationship r2 = Util.cloneViaSerialization(r1);
        assertEquals(r1.getName(), r2.getName());
        assertEquals(r1.getDbRelationshipPath(), r2.getDbRelationshipPath());
    }

    public void testGetClientRelationship() {
        final ObjEntity target = new ObjEntity("te1");
        ObjRelationship r1 = new ObjRelationship("r1") {

            @Override
            public ObjEntity getTargetEntity() {
                return target;
            }
        };

        r1.setDeleteRule(DeleteRule.DENY);
        r1.setTargetEntityName("te1");

        ObjRelationship r2 = r1.getClientRelationship();
        assertNotNull(r2);
        assertEquals(r1.getName(), r2.getName());
        assertEquals(r1.getTargetEntityName(), r2.getTargetEntityName());
        assertEquals(r1.getDeleteRule(), r2.getDeleteRule());
    }

    public void testGetReverseDbRelationshipPath() {
        ObjEntity artistObjEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        ObjEntity paintingObjEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Painting");

        // start with "to many"
        ObjRelationship r1 = artistObjEnt.getRelationship("paintingArray");

        assertEquals("toArtist", r1.getReverseDbRelationshipPath());

        ObjRelationship r2 = paintingObjEnt.getRelationship("toArtist");

        assertEquals("paintingArray", r2.getReverseDbRelationshipPath());
    }

    public void testSetDbRelationshipPath() {
        ObjEntity artistObjEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");

        ObjRelationship r = new ObjRelationship("r");
        r.setSourceEntity(artistObjEnt);
        r.setDbRelationshipPath("paintingArray");
        assertEquals(r.getDbRelationshipPath(), "paintingArray");
    }

    public void testRefreshFromPath() {
        ObjRelationship relationship = new ObjRelationship();

        // attempt to resolve must fail - relationship is outside of context,
        // plus the path is random
        try {
            relationship.setDbRelationshipPath("dummy.path");
            fail("set random path should have failed.");
        } catch (CayenneRuntimeException ex) {
            // expected
        }

        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("Test");
        map.addObjEntity(entity);

        relationship.setSourceEntity(entity);
        // attempt to resolve must fail - relationship is outside of context,
        // plus the path is random
        try {
            relationship.refreshFromPath("dummy.path", false);
            fail("refresh over a dummy path should have failed.");
        } catch (ExpressionException ex) {
            // expected
        }

        // finally assemble ObjEntity to make the path valid
        DbEntity dbEntity1 = new DbEntity("TEST1");
        DbEntity dbEntity2 = new DbEntity("TEST2");
        DbEntity dbEntity3 = new DbEntity("TEST3");
        map.addDbEntity(dbEntity1);
        map.addDbEntity(dbEntity2);
        map.addDbEntity(dbEntity3);
        entity.setDbEntityName("TEST1");
        DbRelationship dummyR = new DbRelationship("dummy");
        dummyR.setTargetEntityName("TEST2");
        dummyR.setSourceEntity(dbEntity1);
        DbRelationship pathR = new DbRelationship("path");
        pathR.setTargetEntityName("TEST3");
        pathR.setSourceEntity(dbEntity2);
        dbEntity1.addRelationship(dummyR);
        dbEntity2.addRelationship(pathR);

        relationship.refreshFromPath("dummy.path", false);

        List<DbRelationship> resolvedPath = relationship.getDbRelationships();
        assertEquals(2, resolvedPath.size());
        assertSame(dummyR, resolvedPath.get(0));
        assertSame(pathR, resolvedPath.get(1));
    }

    public void testCalculateToMany() {
        // assemble fixture....
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("Test");
        map.addObjEntity(entity);

        DbEntity dbEntity1 = new DbEntity("TEST1");
        DbEntity dbEntity2 = new DbEntity("TEST2");
        DbEntity dbEntity3 = new DbEntity("TEST3");
        map.addDbEntity(dbEntity1);
        map.addDbEntity(dbEntity2);
        map.addDbEntity(dbEntity3);
        entity.setDbEntityName("TEST1");
        DbRelationship dummyR = new DbRelationship("dummy");
        dummyR.setTargetEntityName("TEST2");
        dummyR.setSourceEntity(dbEntity1);
        DbRelationship pathR = new DbRelationship("path");
        pathR.setTargetEntityName("TEST3");
        pathR.setSourceEntity(dbEntity2);
        dbEntity1.addRelationship(dummyR);
        dbEntity2.addRelationship(pathR);

        ObjRelationship relationship = new ObjRelationship();
        relationship.setSourceEntity(entity);

        // test how toMany changes dependending on the underlying
        // DbRelationships
        // add DbRelationships directly to avoid events to test
        // "calculateToMany"
        relationship.dbRelationships.add(dummyR);
        assertFalse(relationship.isToMany());

        dummyR.setToMany(true);
        relationship.recalculateToManyValue();
        assertTrue(relationship.isToMany());

        dummyR.setToMany(false);
        relationship.recalculateToManyValue();
        assertFalse(relationship.isToMany());

        // test chain
        relationship.dbRelationships.add(pathR);
        assertFalse(relationship.isToMany());

        pathR.setToMany(true);
        relationship.recalculateToManyValue();
        assertTrue(relationship.isToMany());
    }

    public void testCalculateToManyFromPath() {
        // assemble fixture....
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("Test");
        map.addObjEntity(entity);

        DbEntity dbEntity1 = new DbEntity("TEST1");
        DbEntity dbEntity2 = new DbEntity("TEST2");
        DbEntity dbEntity3 = new DbEntity("TEST3");
        map.addDbEntity(dbEntity1);
        map.addDbEntity(dbEntity2);
        map.addDbEntity(dbEntity3);
        entity.setDbEntityName("TEST1");
        DbRelationship dummyR = new DbRelationship("dummy");
        dummyR.setTargetEntityName("TEST2");
        dummyR.setSourceEntity(dbEntity1);
        DbRelationship pathR = new DbRelationship("path");
        pathR.setTargetEntityName("TEST3");
        pathR.setSourceEntity(dbEntity2);
        dbEntity1.addRelationship(dummyR);
        dbEntity2.addRelationship(pathR);

        ObjRelationship relationship = new ObjRelationship();
        relationship.setSourceEntity(entity);

        // test how toMany changes when the path is set as a string

        relationship.setDbRelationshipPath("dummy");
        assertFalse(relationship.isToMany());

        dummyR.setToMany(true);
        relationship.setDbRelationshipPath(null);
        relationship.setDbRelationshipPath("dummy");
        assertTrue(relationship.isToMany());

        dummyR.setToMany(false);
        relationship.setDbRelationshipPath(null);
        relationship.setDbRelationshipPath("dummy");
        assertFalse(relationship.isToMany());

        // test chain
        relationship.setDbRelationshipPath(null);
        relationship.setDbRelationshipPath("dummy.path");
        assertFalse(relationship.isToMany());

        pathR.setToMany(true);
        relationship.setDbRelationshipPath(null);
        relationship.setDbRelationshipPath("dummy.path");
        assertTrue(relationship.isToMany());
    }

    public void testTargetEntity() throws Exception {
        ObjRelationship relationship = new ObjRelationship("some_rel");
        relationship.setTargetEntityName("targ");

        try {
            relationship.getTargetEntity();
            fail("Without a container, getTargetEntity() must fail.");
        } catch (CayenneRuntimeException ex) {
            // expected
        }

        // assemble container
        DataMap map = new DataMap();
        ObjEntity src = new ObjEntity("src");
        map.addObjEntity(src);

        src.addRelationship(relationship);
        assertNull(relationship.getTargetEntity());

        ObjEntity target = new ObjEntity("targ");
        map.addObjEntity(target);

        assertSame(target, relationship.getTargetEntity());
    }

    public void testGetReverseRel1() {

        ObjEntity artistObjEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        ObjEntity paintingObjEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Painting");

        // start with "to many"
        ObjRelationship r1 = artistObjEnt.getRelationship("paintingArray");
        ObjRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(paintingObjEnt.getRelationship("toArtist"), r2);
    }

    public void testGetReverseRel2() {
        ObjEntity artistEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        ObjEntity paintingEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Painting");

        // start with "to one"
        ObjRelationship r1 = paintingEnt.getRelationship("toArtist");
        ObjRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(artistEnt.getRelationship("paintingArray"), r2);
    }

    public void testSingleDbRelationship() {
        ObjRelationship relationship = new ObjRelationship();
        DbRelationship r1 = new DbRelationship("X");
        relationship.addDbRelationship(r1);
        assertEquals(1, relationship.getDbRelationships().size());
        assertEquals(r1, relationship.getDbRelationships().get(0));
        assertFalse(relationship.isFlattened());
        assertFalse(relationship.isReadOnly());
        assertEquals(r1.isToMany(), relationship.isToMany());
        relationship.removeDbRelationship(r1);
        assertEquals(0, relationship.getDbRelationships().size());
    }

    public void testFlattenedRelationship() {
        DbRelationship r1 = new DbRelationship("X");
        DbRelationship r2 = new DbRelationship("Y");

        r1.setSourceEntity(artistDBEntity);
        r1.setTargetEntity(artistExhibitDBEntity);
        r1.setToMany(true);

        r2.setSourceEntity(artistExhibitDBEntity);
        r2.setTargetEntity(exhibitDBEntity);
        r2.setToMany(false);

        ObjRelationship relationship = new ObjRelationship();
        relationship.addDbRelationship(r1);
        relationship.addDbRelationship(r2);
        assertTrue(relationship.isToMany());
        assertEquals(2, relationship.getDbRelationships().size());
        assertEquals(r1, relationship.getDbRelationships().get(0));
        assertEquals(r2, relationship.getDbRelationships().get(1));

        assertTrue(relationship.isFlattened());

        relationship.removeDbRelationship(r1);
        assertFalse(relationship.isToMany()); // only remaining rel is r2... a
                                              // toOne
        assertEquals(1, relationship.getDbRelationships().size());
        assertEquals(r2, relationship.getDbRelationships().get(0));
        assertFalse(relationship.isFlattened());
        assertFalse(relationship.isReadOnly());

    }

    public void testReadOnly_Flattened1_1__N_1() {

        // check common vertical inheritance relationships

        DataMapLoader loader = runtime.getInjector().getInstance(DataMapLoader.class);
        URL url = getClass().getClassLoader().getResource("inheritance-vertical.map.xml");
        DataMap dataMap = loader.load(new URLResource(url));
        EntityResolver resolver = new EntityResolver(Collections.singleton(dataMap));

        ObjEntity iv2Sub1 = resolver.getObjEntity(Iv2Sub1.class);
        ObjRelationship x = iv2Sub1.getRelationship(Iv2Sub1.X_PROPERTY);
        assertTrue(x.isFlattened());
        assertFalse(x.isReadOnly());
    }

    public void testReadOnlyMoreThan3DbRelsRelationship() {
        // Readonly is a flattened relationship that isn't over a single
        // many->many link
        // table
        DbRelationship r1 = new DbRelationship("X");
        DbRelationship r2 = new DbRelationship("Y");
        DbRelationship r3 = new DbRelationship("Z");

        r1.setSourceEntity(artistDBEntity);
        r1.setTargetEntity(artistExhibitDBEntity);
        r1.setToMany(true);
        r2.setSourceEntity(artistExhibitDBEntity);
        r2.setTargetEntity(exhibitDBEntity);
        r2.setToMany(false);
        r3.setSourceEntity(exhibitDBEntity);
        r3.setTargetEntity(galleryDBEntity);
        r3.setToMany(false);

        ObjRelationship relationship = new ObjRelationship();
        relationship.addDbRelationship(r1);
        relationship.addDbRelationship(r2);
        relationship.addDbRelationship(r3);

        assertTrue(relationship.isFlattened());
        assertTrue(relationship.isReadOnly());
        assertTrue(relationship.isToMany());

    }

    // Test for a read-only flattened relationship that is readonly because it's
    // dbrel
    // sequence is "incorrect" (or rather, unsupported)
    public void testIncorrectSequenceReadOnlyRelationship() {
        DbRelationship r1 = new DbRelationship("X");
        DbRelationship r2 = new DbRelationship("Y");

        r1.setSourceEntity(artistDBEntity);
        r1.setTargetEntity(paintingDbEntity);
        r1.setToMany(true);
        r2.setSourceEntity(paintingDbEntity);
        r2.setTargetEntity(galleryDBEntity);
        r2.setToMany(false);

        ObjRelationship relationship = new ObjRelationship();
        relationship.addDbRelationship(r1);
        relationship.addDbRelationship(r2);

        assertTrue(relationship.isFlattened());
        assertTrue(relationship.isReadOnly());
        assertTrue(relationship.isToMany());
    }

    // Test a relationship loaded from the test datamap that we know should be
    // flattened
    public void testKnownFlattenedRelationship() {
        ObjEntity artistEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Artist");
        ObjRelationship theRel = artistEnt.getRelationship("groupArray");
        assertNotNull(theRel);
        assertTrue(theRel.isFlattened());
        assertFalse(theRel.isReadOnly());
    }

    public void testBadDeleteRuleValue() {
        ObjRelationship relationship = new ObjRelationship();

        try {
            relationship.setDeleteRule(999);
            fail("Should have failed with IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Good... it should throw an exception
        }
    }

    public void testOkDeleteRuleValue() {
        ObjRelationship relationship = new ObjRelationship();
        try {
            relationship.setDeleteRule(DeleteRule.CASCADE);
            relationship.setDeleteRule(DeleteRule.DENY);
            relationship.setDeleteRule(DeleteRule.NULLIFY);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail("Should not have thrown an exception :" + e.getMessage());
        }
    }
}
