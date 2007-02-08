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

import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * @author Andrei Adamchik
 */
public class IncrementalFaultListPrefetchTst extends DataContextTestBase {

    protected void setUp() throws Exception {
        super.setUp();
        createTestData("testPaintings");
    }

    /**
     * Test that all queries specified in prefetch are executed with a single prefetch
     * path.
     */
    public void testPrefetch1() {

        Expression e = ExpressionFactory.likeExp("artistName", "artist1%");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.setPageSize(4);

        IncrementalFaultList result = (IncrementalFaultList) context.performQuery(q);

        assertEquals(11, result.size());

        // currently queries with prefetch do not resolve their first page
        assertEquals(result.size(), result.getUnfetchedObjects());

        // go through the second page objects and count queries
        getDomain().restartQueryCounter();
        for (int i = 4; i < 8; i++) {
            result.get(i);

            // within the same page only one query should've been executed
            assertEquals(1, getDomain().getQueryCount());
        }
    }

    /**
     * Test that a to-many relationship is initialized.
     */
    public void testPrefetch3() {

        Expression e = ExpressionFactory.likeExp("artistName", "artist1%");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.setPageSize(4);

        IncrementalFaultList result = (IncrementalFaultList) context.performQuery(q);

        assertEquals(11, result.size());

        // currently queries with prefetch do not resolve their first page
        assertEquals(result.size(), result.getUnfetchedObjects());

        // go through the second page objects and check their to many
        for (int i = 4; i < 8; i++) {
            Artist a = (Artist) result.get(i);

            List paintings = a.getPaintingArray();
            assertFalse(((ValueHolder) paintings).isFault());
            assertEquals(1, paintings.size());
        }
    }

    /**
     * Test that a to-one relationship is initialized.
     */
    public void testPrefetch4() {

        SelectQuery q = new SelectQuery("Painting");
        q.setPageSize(4);
        q.addPrefetch("toArtist");

        IncrementalFaultList result = (IncrementalFaultList) context.performQuery(q);

        // get an objects from the second page
        DataObject p1 = (DataObject) result.get(q.getPageSize());

        blockQueries();

        try {

            Object toOnePrefetch = p1.readNestedProperty("toArtist");
            assertNotNull(toOnePrefetch);
            assertTrue(
                    "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
                    toOnePrefetch instanceof DataObject);

            DataObject a1 = (DataObject) toOnePrefetch;
            assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
        }
        finally {
            unblockQueries();
        }
    }

}
