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
package org.objectstyle.cayenne.access;

import java.sql.Date;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * Tests joint prefetch handling by Cayenne access stack.
 * 
 * @author Andrei Adamchik
 */
public class JointPrefetchTst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testJointPrefetchDataRows() throws Exception {
        createTestData("testJointPrefetch1");

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering("db:PAINTING_ID", true);
        q.setFetchingDataRows(true);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        DataContext context = createDataContext();

        List rows = context.performQuery(q);

        blockQueries();

        try {
            assertEquals(3, rows.size());

            // row should contain columns from both entities minus those duplicated in a
            // join...
            int rowWidth = getDbEntity("ARTIST").getAttributes().size()
                    + getDbEntity("PAINTING").getAttributes().size()
                    - 1;
            Iterator it = rows.iterator();
            while (it.hasNext()) {
                DataRow row = (DataRow) it.next();
                assertEquals(rowWidth, row.size());

                // assert columns presence
                assertTrue(row + "", row.containsKey("PAINTING_ID"));
                assertTrue(row + "", row.containsKey("ARTIST_ID"));
                assertTrue(row + "", row.containsKey("GALLERY_ID"));
                assertTrue(row + "", row.containsKey("PAINTING_TITLE"));
                assertTrue(row + "", row.containsKey("ESTIMATED_PRICE"));
                assertTrue(row + "", row.containsKey("toArtist.ARTIST_NAME"));
                assertTrue(row + "", row.containsKey("toArtist.DATE_OF_BIRTH"));
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testJointPrefetchSQLTemplate() throws Exception {
        createTestData("testJointPrefetch1");

        // correctly naming columns is the key..
        SQLTemplate q = new SQLTemplate(
                Artist.class,
                "SELECT distinct "
                        + "#result('ESTIMATED_PRICE' 'BigDecimal' '' 'paintingArray.ESTIMATED_PRICE'), "
                        + "#result('PAINTING_TITLE' 'String' '' 'paintingArray.PAINTING_TITLE'), "
                        + "#result('GALLERY_ID' 'int' '' 'paintingArray.GALLERY_ID'), "
                        + "#result('PAINTING_ID' 'int' '' 'paintingArray.PAINTING_ID'), "
                        + "#result('ARTIST_NAME' 'String'), "
                        + "#result('DATE_OF_BIRTH' 'java.util.Date'), "
                        + "#result('t0.ARTIST_ID' 'int' '' 'ARTIST_ID') "
                        + "FROM ARTIST t0, PAINTING t1 "
                        + "WHERE t0.ARTIST_ID = t1.ARTIST_ID");

        PrefetchTreeNode prefetch = q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);
        assertEquals(
                "Default semantics for SQLTemplate is assumed to be joint.",
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS,
                prefetch.getSemantics());
        q.setFetchingDataRows(false);

        DataContext context = createDataContext();

        List objects = context.performQuery(q);

        blockQueries();

        try {

            // without OUTER join we will get fewer objects...
            assertEquals(2, objects.size());

            Iterator it = objects.iterator();
            while (it.hasNext()) {
                Artist a = (Artist) it.next();
                List list = a.getPaintingArray();

                assertNotNull(list);
                assertFalse(((ValueHolder) list).isFault());
                assertTrue(list.size() > 0);

                Iterator children = list.iterator();
                while (children.hasNext()) {
                    Painting p = (Painting) children.next();
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());

                    // make sure properties are not null..
                    assertNotNull(p.getPaintingTitle());
                }
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testJointPrefetchToOne() throws Exception {
        createTestData("testJointPrefetch1");

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Painting.class);
        q.addOrdering("db:PAINTING_ID", true);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        DataContext context = createDataContext();

        List objects = context.performQuery(q);

        blockQueries();

        try {
            assertEquals(3, objects.size());

            Iterator it = objects.iterator();
            while (it.hasNext()) {
                Painting p = (Painting) it.next();
                Artist target = p.getToArtist();
                assertNotNull(target);
                assertEquals(PersistenceState.COMMITTED, target.getPersistenceState());
            }
        }
        finally {
            unblockQueries();
        }
    }

    /**
     * Tests that joined entities can have non-standard type mappings.
     */
    public void testJointPrefetchDataTypes() throws Exception {
        // prepare... can't load from XML, as it doesn't yet support dates..
        SQLTemplate artistSQL = new SQLTemplate(
                Artist.class,
                "insert into ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                        + "values (33001, 'a1', #bind($date 'DATE'))");
        artistSQL.setParameters(Collections.singletonMap("date", new Date(System
                .currentTimeMillis())));
        SQLTemplate paintingSQL = new SQLTemplate(
                Painting.class,
                "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
                        + "VALUES (33001, 'p1', 33001, 1000)");
        DataContext context = createDataContext();

        context.performNonSelectingQuery(artistSQL);
        context.performNonSelectingQuery(paintingSQL);

        // test
        SelectQuery q = new SelectQuery(Painting.class);
        q.addPrefetch(Painting.TO_ARTIST_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        ObjEntity artistE = getObjEntity("Artist");
        ObjAttribute dateOfBirth = (ObjAttribute) artistE.getAttribute("dateOfBirth");
        assertEquals("java.util.Date", dateOfBirth.getType());
        dateOfBirth.setType("java.sql.Date");
        try {
            List objects = context.performQuery(q);

            blockQueries();

            try {
                assertEquals(1, objects.size());

                Iterator it = objects.iterator();
                while (it.hasNext()) {
                    Painting p = (Painting) it.next();
                    Artist a = p.getToArtist();
                    assertNotNull(a);
                    assertNotNull(a.getDateOfBirth());
                    assertTrue(Date.class.isAssignableFrom(a.getDateOfBirth().getClass()));
                }
            }
            finally {
                unblockQueries();
            }
        }
        finally {
            dateOfBirth.setType("java.util.Date");
        }
    }

    public void testJointPrefetchToMany() throws Exception {
        createTestData("testJointPrefetch1");

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Artist.class);
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        DataContext context = createDataContext();

        List objects = context.performQuery(q);

        blockQueries();
        try {
            // without OUTER join we will get fewer objects...
            assertEquals(2, objects.size());

            Iterator it = objects.iterator();
            while (it.hasNext()) {
                Artist a = (Artist) it.next();
                List list = a.getPaintingArray();

                assertNotNull(list);
                assertFalse(((ValueHolder) list).isFault());
                assertTrue(list.size() > 0);

                Iterator children = list.iterator();
                while (children.hasNext()) {
                    Painting p = (Painting) children.next();
                    assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                    // make sure properties are not null..
                    assertNotNull(p.getPaintingTitle());
                }
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testJointPrefetchToManyNonConflictingQualifier() throws Exception {
        createTestData("testJointPrefetch1");

        // query with to-many joint prefetches and qualifier that doesn't match
        // prefetch....
        Expression qualifier = ExpressionFactory.matchExp(
                Artist.ARTIST_NAME_PROPERTY,
                "artist1");
        SelectQuery q = new SelectQuery(Artist.class, qualifier);
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        DataContext context = createDataContext();

        List objects = context.performQuery(q);

        blockQueries();

        try {

            assertEquals(1, objects.size());

            Artist a = (Artist) objects.get(0);
            List list = a.getPaintingArray();

            assertNotNull(list);
            assertFalse(((ValueHolder) list).isFault());
            assertEquals(2, list.size());

            Iterator children = list.iterator();
            while (children.hasNext()) {
                Painting p = (Painting) children.next();
                assertEquals(PersistenceState.COMMITTED, p.getPersistenceState());
                // make sure properties are not null..
                assertNotNull(p.getPaintingTitle());
            }

            // assert no duplicates
            Set s = new HashSet(list);
            assertEquals(s.size(), list.size());
        }
        finally {
            unblockQueries();
        }
    }

    public void testJointPrefetchMultiStep() throws Exception {
        createTestData("testJointPrefetch2");

        // query with to-many joint prefetches
        SelectQuery q = new SelectQuery(Artist.class);
        q
                .addPrefetch(
                        Artist.PAINTING_ARRAY_PROPERTY
                                + "."
                                + Painting.TO_GALLERY_PROPERTY)
                .setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        DataContext context = createDataContext();

        // sanity check...
        DataObject g1 = (DataObject) context.getGraphManager().getNode(
                new ObjectId("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33001));
        assertNull(g1);

        List objects = context.performQuery(q);

        blockQueries();
        try {

            // without OUTER join we will get fewer objects...
            assertEquals(2, objects.size());

            Iterator it = objects.iterator();
            while (it.hasNext()) {
                Artist a = (Artist) it.next();
                ValueHolder list = (ValueHolder) a.getPaintingArray();

                assertNotNull(list);

                // intermediate relationship is not fetched...
                assertTrue(list.isFault());
            }

            // however both galleries must be in memory...
            g1 = (DataObject) context.getGraphManager().getNode(
                    new ObjectId("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33001));
            assertNotNull(g1);
            assertEquals(PersistenceState.COMMITTED, g1.getPersistenceState());
            DataObject g2 = (DataObject) context.getGraphManager().getNode(
                    new ObjectId("Gallery", Gallery.GALLERY_ID_PK_COLUMN, 33002));
            assertNotNull(g2);
            assertEquals(PersistenceState.COMMITTED, g2.getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }
}