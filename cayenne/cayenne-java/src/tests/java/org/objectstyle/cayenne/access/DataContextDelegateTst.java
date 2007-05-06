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

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Gallery;
import org.objectstyle.cayenne.query.MockQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * Tests various DataContextDelegate methods invocation and consequences on DataContext
 * behavior.
 * 
 * @author Andrus Adamchik
 */
public class DataContextDelegateTst extends CayenneTestCase {

    protected Gallery gallery;
    protected Artist artist;

    protected void setUp() throws Exception {
        super.setUp();

        DataContext context = createDataContextWithSharedCache();

        // prepare a single gallery record
        gallery = (Gallery) context.createAndRegisterNewObject("Gallery");
        gallery.setGalleryName("version1");

        // prepare a single artist record
        artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName("version1");

        context.commitChanges();
    }

    public void testWillPerformGenericQuery() throws Exception {
        DataContext context = gallery.getDataContext();

        final List queriesPerformed = new ArrayList(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            public Query willPerformGenericQuery(DataContext context, Query query) {
                queriesPerformed.add(query);
                return query;
            }
        };
        context.setDelegate(delegate);

        // test that delegate is consulted before select
        MockQuery query = new MockQuery();
        context.performGenericQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());
        assertTrue("Delegate unexpectedly blocked the query.", query.isRouteCalled());
    }

    public void testWillPerformGenericQueryBlocked() throws Exception {
        DataContext context = gallery.getDataContext();

        final List queriesPerformed = new ArrayList(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            public Query willPerformGenericQuery(DataContext context, Query query) {
                queriesPerformed.add(query);
                return null;
            }
        };

        context.setDelegate(delegate);
        MockQuery query = new MockQuery();
        context.performGenericQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());
        assertFalse("Delegate couldn't block the query.", query.isRouteCalled());
    }

    public void testWillPerformQuery() throws Exception {
        DataContext context = gallery.getDataContext();

        final List queriesPerformed = new ArrayList(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            public Query willPerformQuery(DataContext context, Query query) {
                queriesPerformed.add(query);
                return query;
            }
        };
        context.setDelegate(delegate);

        // test that delegate is consulted before select
        SelectQuery query = new SelectQuery(Gallery.class);
        List results = context.performQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());
        assertNotNull(results);
    }

    public void testWillPerformQueryBlocked() throws Exception {
        DataContext context = gallery.getDataContext();

        final List queriesPerformed = new ArrayList(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            public Query willPerformQuery(DataContext context, Query query) {
                queriesPerformed.add(query);
                return null;
            }
        };

        context.setDelegate(delegate);
        SelectQuery query = new SelectQuery(Gallery.class);
        List results = context.performQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());

        assertNotNull(results);

        // blocked
        assertEquals("Delegate couldn't block the query.", 0, results.size());
    }

    /**
     * @deprecated since 1.2
     */
    public void testWillPerformSelect() throws Exception {
        DataContext context = gallery.getDataContext();

        final List queriesPerformed = new ArrayList(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            public org.objectstyle.cayenne.query.GenericSelectQuery willPerformSelect(
                    DataContext context,
                    org.objectstyle.cayenne.query.GenericSelectQuery query) {
                // save query, and allow its execution
                queriesPerformed.add(query);
                return query;
            }
        };
        context.setDelegate(delegate);

        // test that delegate is consulted before select
        SelectQuery query = new SelectQuery(Gallery.class);
        List results = context.performQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());
        assertNotNull(results);
    }

    /**
     * @deprecated since 1.2
     */
    public void testWillPerformSelectQueryBlocked() throws Exception {
        DataContext context = gallery.getDataContext();

        final List queriesPerformed = new ArrayList(1);
        DataContextDelegate delegate = new MockDataContextDelegate() {

            public org.objectstyle.cayenne.query.GenericSelectQuery willPerformSelect(
                    DataContext context,
                    org.objectstyle.cayenne.query.GenericSelectQuery query) {
                // save query, and block its execution
                queriesPerformed.add(query);
                return null;
            }
        };

        context.setDelegate(delegate);
        SelectQuery query = new SelectQuery(Gallery.class);
        List results = context.performQuery(query);

        assertTrue("Delegate is not notified of a query being run.", queriesPerformed
                .contains(query));
        assertEquals(1, queriesPerformed.size());

        assertNotNull(results);

        // blocked
        assertEquals("Delegate couldn't block the query.", 0, results.size());
    }
}
