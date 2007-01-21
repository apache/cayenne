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

import org.apache.art.Artist;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.Util;

public class ObjEntityTest extends CayenneCase {

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

        assertFalse("No parent DataMap should have automatically disabled client.", e1
                .isClientAllowed());

        DataMap map = new DataMap("m1");
        e1.setDataMap(map);

        assertFalse(e1.isClientAllowed());

        map.setClientSupported(true);
        assertTrue(e1.isClientAllowed());

        e1.setServerOnly(true);
        assertFalse(e1.isClientAllowed());
    }

    public void testGetClientEntity() {
        final ObjEntity target = new ObjEntity("te1");

        ObjEntity e1 = new ObjEntity("entity");
        e1.setClassName("x.y.z");
        e1.setClientClassName("a.b.c");
        e1.addAttribute(new ObjAttribute("A1"));
        e1.addAttribute(new ObjAttribute("A2"));

        ObjRelationship r1 = new ObjRelationship("r1") {

            public Entity getTargetEntity() {
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

        ObjEntity d1 = (ObjEntity) Util.cloneViaSerialization(entity);
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
        }
        catch (CayenneRuntimeException ex) {
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
        ObjEntity ae = getObjEntity("Artist");
        DbEntity dae = ae.getDbEntity();

        assertNull(ae.getAttributeForDbAttribute((DbAttribute) dae
                .getAttribute("ARTIST_ID")));
        assertNotNull(ae.getAttributeForDbAttribute((DbAttribute) dae
                .getAttribute("ARTIST_NAME")));
    }

    public void testRelationshipForDbRelationship() throws Exception {
        ObjEntity ae = getObjEntity("Artist");
        DbEntity dae = ae.getDbEntity();

        assertNull(ae.getRelationshipForDbRelationship(new DbRelationship()));
        assertNotNull(ae.getRelationshipForDbRelationship((DbRelationship) dae
                .getRelationship("paintingArray")));
    }

    public void testReadOnly() throws Exception {
        ObjEntity entity = new ObjEntity("entity");
        assertFalse(entity.isReadOnly());
        entity.setReadOnly(true);
        assertTrue(entity.isReadOnly());
    }

    public void testTranslateToRelatedEntityIndependentPath() throws Exception {
        ObjEntity artistE = getDomain().getEntityResolver().lookupObjEntity(Artist.class);

        Expression e1 = Expression.fromString("paintingArray");
        Expression translated = artistE
                .translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, Expression
                .fromString("db:toArtist.paintingArray"), translated);
    }

    public void testTranslateToRelatedEntityTrimmedPath() throws Exception {
        ObjEntity artistE = getDomain().getEntityResolver().lookupObjEntity(Artist.class);

        Expression e1 = Expression.fromString("artistExhibitArray.toExhibit");
        Expression translated = artistE
                .translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals(
                "failure: " + translated,
                Expression.fromString("db:toExhibit"),
                translated);
    }

    public void testTranslateToRelatedEntitySplitHalfWay() throws Exception {
        ObjEntity artistE = getDomain().getEntityResolver().lookupObjEntity(Artist.class);

        Expression e1 = Expression.fromString("paintingArray.toPaintingInfo.textReview");
        Expression translated = artistE.translateToRelatedEntity(
                e1,
                "paintingArray.toGallery");
        assertEquals("failure: " + translated, Expression
                .fromString("db:paintingArray.toPaintingInfo.TEXT_REVIEW"), translated);
    }

    public void testTranslateToRelatedEntityMatchingPath() throws Exception {
        ObjEntity artistE = getDomain().getEntityResolver().lookupObjEntity(Artist.class);
        Expression e1 = Expression.fromString("artistExhibitArray.toExhibit");
        Expression translated = artistE.translateToRelatedEntity(
                e1,
                "artistExhibitArray.toExhibit");
        assertEquals("failure: " + translated, Expression
                .fromString("db:artistExhibitArray.toExhibit"), translated);
    }

    public void testTranslateToRelatedEntityMultiplePaths() throws Exception {
        ObjEntity artistE = getDomain().getEntityResolver().lookupObjEntity(Artist.class);

        Expression e1 = Expression
                .fromString("paintingArray = $p and artistExhibitArray.toExhibit.closingDate = $d");
        Expression translated = artistE
                .translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals(
                "failure: " + translated,
                Expression
                        .fromString("db:toArtist.paintingArray = $p and db:toExhibit.CLOSING_DATE = $d"),
                translated);
    }
}
