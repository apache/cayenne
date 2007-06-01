/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.map;

import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class ObjRelationshipTst extends CayenneTestCase {
    protected ObjRelationship relationship;

    protected DbEntity artistDBEntity = getDbEntity("ARTIST");
    protected DbEntity artistExhibitDBEntity = getDbEntity("ARTIST_EXHIBIT");
    protected DbEntity exhibitDBEntity = getDbEntity("EXHIBIT");
    protected DbEntity paintingDbEntity = getDbEntity("PAINTING");
    protected DbEntity galleryDBEntity = getDbEntity("GALLERY");

    public void setUp() throws Exception {
        relationship = new ObjRelationship();
    }

    public void testSetDbRelationshipPath() throws Exception {
        relationship.dbRelationshipsRefreshNeeded = false;

        relationship.setDbRelationshipPath("dummy.path");
        assertTrue(relationship.dbRelationshipsRefreshNeeded);

        assertEquals("dummy.path", relationship.getDbRelationshipPath());
        assertTrue(relationship.dbRelationshipsRefreshNeeded);
    }

    public void testRefreshFromPath() throws Exception {
        relationship.setDbRelationshipPath("dummy.path");

        // attempt to resolve must fail - relationship is outside of context,
        // plus the path is random
        try {
            relationship.refreshFromPath(false);
            fail("refresh without source entity should have failed.");
        }
        catch (CayenneRuntimeException ex) {
            // expected
        }

        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("Test");
        map.addObjEntity(entity);

        relationship.setSourceEntity(entity);
        // attempt to resolve must fail - relationship is outside of context,
        // plus the path is random
        try {
            relationship.refreshFromPath(false);
            fail("refresh over a dummy path should have failed.");
        }
        catch (ExpressionException ex) {
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

        relationship.refreshFromPath(false);
        assertFalse(relationship.dbRelationshipsRefreshNeeded);

        List resolvedPath = relationship.getDbRelationships();
        assertEquals(2, resolvedPath.size());
        assertSame(dummyR, resolvedPath.get(0));
        assertSame(pathR, resolvedPath.get(1));
    }

    public void testCalculateToMany() throws Exception {
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

        relationship.setSourceEntity(entity);

        // test how toMany changes dependending on the underlying DbRelationships
        // add DbRelationships directly to avoid events to test "calculateToMany"
        relationship.dbRelationshipsRefreshNeeded = false;
        relationship.dbRelationships.add(dummyR);
        assertFalse(relationship.isToMany());

        dummyR.setToMany(true);
        relationship.calculateToManyValue();
        assertTrue(relationship.isToMany());

        dummyR.setToMany(false);
        relationship.calculateToManyValue();
        assertFalse(relationship.isToMany());

        // test chain
        relationship.dbRelationships.add(pathR);
        assertFalse(relationship.isToMany());

        pathR.setToMany(true);
        relationship.calculateToManyValue();
        assertTrue(relationship.isToMany());
    }

    public void testCalculateToManyFromPath() throws Exception {
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
        relationship.setTargetEntityName("targ");

        try {
            relationship.getTargetEntity();
            fail("Without a container, getTargetEntity() must fail.");
        }
        catch (CayenneRuntimeException ex) {
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

    public void testGetReverseRel1() throws Exception {
        ObjEntity artistObjEnt = getObjEntity("Artist");
        ObjEntity paintingObjEnt = getObjEntity("Painting");

        // start with "to many"
        ObjRelationship r1 =
            (ObjRelationship) artistObjEnt.getRelationship("paintingArray");
        ObjRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(paintingObjEnt.getRelationship("toArtist"), r2);
    }

    public void testGetReverseRel2() throws Exception {
        ObjEntity artistEnt = getObjEntity("Artist");
        ObjEntity paintingEnt = getObjEntity("Painting");

        // start with "to one"
        ObjRelationship r1 = (ObjRelationship) paintingEnt.getRelationship("toArtist");
        ObjRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(artistEnt.getRelationship("paintingArray"), r2);
    }

    public void testSingleDbRelationship() {
        DbRelationship r1 = new DbRelationship();
        relationship.addDbRelationship(r1);
        assertEquals(1, relationship.getDbRelationships().size());
        assertEquals(r1, relationship.getDbRelationships().get(0));
        assertFalse(relationship.isFlattened());
        assertFalse(relationship.isReadOnly());
        assertEquals(r1.isToMany(), relationship.isToMany());
        relationship.removeDbRelationship(r1);
        assertEquals(0, relationship.getDbRelationships().size());
    }

    public void testFlattenedRelationship() throws Exception {
        DbRelationship r1 = new DbRelationship();
        DbRelationship r2 = new DbRelationship();

        r1.setSourceEntity(artistDBEntity);
        r1.setTargetEntity(artistExhibitDBEntity);
        r1.setToMany(true);

        r2.setSourceEntity(artistExhibitDBEntity);
        r2.setTargetEntity(exhibitDBEntity);
        r2.setToMany(false);

        relationship.addDbRelationship(r1);
        relationship.addDbRelationship(r2);
        assertTrue(relationship.isToMany());
        assertEquals(2, relationship.getDbRelationships().size());
        assertEquals(r1, relationship.getDbRelationships().get(0));
        assertEquals(r2, relationship.getDbRelationships().get(1));

        assertTrue(relationship.isFlattened());

        relationship.removeDbRelationship(r1);
        assertFalse(relationship.isToMany()); //only remaining rel is r2... a toOne
        assertEquals(1, relationship.getDbRelationships().size());
        assertEquals(r2, relationship.getDbRelationships().get(0));
        assertFalse(relationship.isFlattened());
        assertFalse(relationship.isReadOnly());

    }

    public void testReadOnlyMoreThan3DbRelsRelationship() {
        //Readonly is a flattened relationship that isn't over a single many->many link table
        DbRelationship r1 = new DbRelationship();
        DbRelationship r2 = new DbRelationship();
        DbRelationship r3 = new DbRelationship();

        r1.setSourceEntity(artistDBEntity);
        r1.setTargetEntity(artistExhibitDBEntity);
        r1.setToMany(true);
        r2.setSourceEntity(artistExhibitDBEntity);
        r2.setTargetEntity(exhibitDBEntity);
        r2.setToMany(false);
        r3.setSourceEntity(exhibitDBEntity);
        r3.setTargetEntity(galleryDBEntity);
        r3.setToMany(false);

        relationship.addDbRelationship(r1);
        relationship.addDbRelationship(r2);
        relationship.addDbRelationship(r3);

        assertTrue(relationship.isFlattened());
        assertTrue(relationship.isReadOnly());
        assertTrue(relationship.isToMany());

    }

    //Test for a read-only flattened relationship that is readonly because it's dbrel sequence is "incorrect" (or rather, unsupported)
    public void testIncorrectSequenceReadOnlyRelationship() {
        DbRelationship r1 = new DbRelationship();
        DbRelationship r2 = new DbRelationship();

        r1.setSourceEntity(artistDBEntity);
        r1.setTargetEntity(paintingDbEntity);
        r1.setToMany(true);
        r2.setSourceEntity(paintingDbEntity);
        r2.setTargetEntity(galleryDBEntity);
        r2.setToMany(false);

        relationship.addDbRelationship(r1);
        relationship.addDbRelationship(r2);

        assertTrue(relationship.isFlattened());
        assertTrue(relationship.isReadOnly());
        assertTrue(relationship.isToMany());
    }

    //Test a relationship loaded from the test datamap that we know should be flattened
    public void testKnownFlattenedRelationship() {
        ObjEntity artistEnt = getObjEntity("Artist");
        ObjRelationship theRel =
            (ObjRelationship) artistEnt.getRelationship("groupArray");
        assertNotNull(theRel);
        assertTrue(theRel.isFlattened());
        assertFalse(theRel.isReadOnly());
    }

    public void testBadDeleteRuleValue() {
        try {
            relationship.setDeleteRule(999);
            fail("Should have failed with IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            //Good... it should throw an exception
        }
    }

    public void testOkDeleteRuleValue() {
        try {
            relationship.setDeleteRule(DeleteRule.CASCADE);
            relationship.setDeleteRule(DeleteRule.DENY);
            relationship.setDeleteRule(DeleteRule.NULLIFY);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
            fail("Should not have thrown an exception :" + e.getMessage());
        }
    }

    public void testWatchesDbRelChanges() {
        DbRelationship r1 = new DbRelationship();
        r1.setToMany(true);
        relationship.addDbRelationship(r1);
        assertTrue(relationship.isToMany());

        //rel should be watching r1 (events) to see when that changes
        r1.setToMany(false);
        assertFalse(relationship.isToMany());
    }
}
