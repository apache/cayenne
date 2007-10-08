/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne;

import java.util.List;

import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.MockDataNode;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * Test case for objects with flattened relationships.
 * 
 * @author Andrei Adamchik
 */
// TODO: redefine all test cases in terms of entities in "relationships" map
// and merge this test case with FlattenedRelationshipsTst that inherits
// from RelationshipTestCase.
public class CayenneDataObjectFlattenedRelTst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testReadFlattenedRelationship() throws Exception {
        DataContext context = createDataContext();

        createTestData("testReadFlattenedRelationship");
        Artist a1 = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33001);
        List groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(0, groupList.size());
    }

    public void testReadFlattenedRelationship2() throws Exception {
        DataContext context = createDataContext();

        createTestData("testReadFlattenedRelationship2");

        Artist a1 = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33001);
        List groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(1, groupList.size());
        assertEquals(PersistenceState.COMMITTED, ((ArtGroup) groupList.get(0))
                .getPersistenceState());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());
    }

    public void testAddToFlattenedRelationship() throws Exception {

        createTestData("testAddToFlattenedRelationship");
        DataContext context = createDataContext();

        Artist a1 = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33001);
        assertEquals(0, a1.getGroupArray().size());

        SelectQuery q = new SelectQuery(ArtGroup.class, ExpressionFactory.matchExp(
                "name",
                "g1"));
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        assertFalse(context.hasChanges());
        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);
        assertTrue(context.hasChanges());

        List groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());

        // Ensure that the commit doesn't fail
        a1.getDataContext().commitChanges();

        // and check again
        assertFalse(context.hasChanges());

        // refetch artist with a different context
        context = createDataContext();
        a1 = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33001);
        groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());
    }

    // Test case to show up a bug in committing more than once
    public void testDoubleCommitAddToFlattenedRelationship() throws Exception {
        createTestData("testDoubleCommitAddToFlattenedRelationship");
        DataContext context = createDataContext();

        Artist a1 = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33001);

        SelectQuery q = new SelectQuery(ArtGroup.class, ExpressionFactory.matchExp(
                "name",
                "g1"));
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);

        List groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals("g1", ((ArtGroup) groupList.get(0)).getName());

        // Ensure that the commit doesn't fail
        a1.getDataContext().commitChanges();

        try {
            // The bug caused the second commit to fail (the link record
            // was inserted again)
            a1.getDataContext().commitChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }

    }

    public void testRemoveFromFlattenedRelationship() throws Exception {
        createTestData("testRemoveFromFlattenedRelationship");
        DataContext context = createDataContext();

        Artist a1 = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33001);

        ArtGroup group = (ArtGroup) a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group);

        List groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());

        // Ensure that the commit doesn't fail
        a1.getDataContext().commitChanges();

        // and check again
        groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());
    }

    // Shows up a possible bug in ordering of deletes, when a flattened relationships link
    // record is deleted
    // at the same time (same transaction) as one of the record to which it links.
    public void testRemoveFlattenedRelationshipAndRootRecord() throws Exception {
        DataContext context = createDataContext();
        createTestData("testRemoveFlattenedRelationshipAndRootRecord");
        Artist a1 = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33001);
        DataContext dc = a1.getDataContext();

        ArtGroup group = (ArtGroup) a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group); // Cause the delete of the link record

        dc.deleteObject(a1); // Cause the deletion of the artist

        try {
            dc.commitChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown the exception :" + e.getMessage());
        }
    }

    public void testAddRemoveFlattenedRelationship1() throws Exception {
        DataContext context = createDataContext();
        createTestData("testAddRemoveFlattenedRelationship1");
        Artist a1 = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33001);

        SelectQuery q = new SelectQuery(ArtGroup.class, ExpressionFactory.matchExp(
                "name",
                "g1"));
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);
        group.removeFromArtistArray(a1);

        blockQueries();
        try {
            context.commitChanges();
        }
        finally {
            unblockQueries();
        }
    }

    public void testAddRemoveFlattenedRelationship2() throws Exception {
        createTestData("testAddRemoveFlattenedRelationship2");

        DataContext context = createDataContext();

        Artist a1 = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 33001);

        SelectQuery q = new SelectQuery(ArtGroup.class);
        List results = context.performQuery(q);
        assertEquals(2, results.size());

        ArtGroup g1 = (ArtGroup) results.get(0);
        ArtGroup g2 = (ArtGroup) results.get(1);
        a1.addToGroupArray(g1);
        a1.addToGroupArray(g2);

        // test that there is no delete query issued when a flattened join is first
        // added
        // and then deleted AND there are some other changes (CAY-548)
        a1.removeFromGroupArray(g1);

        MockDataNode engine = MockDataNode.interceptNode(getDomain(), getNode());
        try {
            context.commitChanges();
            assertEquals(1, engine.getRunCount());
        }
        finally {
            engine.stopInterceptNode();
        }
    }
}
