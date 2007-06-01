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

package org.objectstyle.cayenne.access;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextPerformQueryAPITst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testObjectQueryStringBoolean() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists");
        getAccessStack().createTestData(DataContextTestBase.class, "testPaintings");

        List paintings = createDataContext().performQuery("ObjectQuery", true);
        assertNotNull(paintings);
        assertEquals(25, paintings.size());
    }

    public void testObjectQueryStringMapBoolean() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists");
        getAccessStack().createTestData(DataContextTestBase.class, "testPaintings");

        // fetch artist
        DataContext context = createDataContext();
        Artist a = (Artist) context.registeredObject(new ObjectId(
                Artist.class,
                Artist.ARTIST_ID_PK_COLUMN,
                33018));
        Map parameters = Collections.singletonMap("artist", a);

        List paintings = createDataContext()
                .performQuery("ObjectQuery", parameters, true);
        assertNotNull(paintings);
        assertEquals(1, paintings.size());
    }

    public void testProcedureQueryStringMapBoolean() throws Exception {
        // Don't run this on MySQL
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        getAccessStack().createTestData(DataContextTestBase.class, "testArtists");
        getAccessStack().createTestData(DataContextTestBase.class, "testPaintings");

        // fetch artist
        Map parameters = Collections.singletonMap("aName", "artist2");
        DataContext context = createDataContext();
        List artists;

        // Sybase blows whenever a transaction wraps a SP, so turn of transactions
        boolean transactionsFlag = context
                .getParentDataDomain()
                .isUsingExternalTransactions();

        context.getParentDataDomain().setUsingExternalTransactions(true);
        try {
            artists = context.performQuery("ProcedureQuery", parameters, true);
        }
        finally {
            context.getParentDataDomain().setUsingExternalTransactions(transactionsFlag);
        }

        assertNotNull(artists);
        assertEquals(1, artists.size());

        Artist artist = (Artist) artists.get(0);
        assertEquals(new Integer(33002), artist.getObjectId().getValueForAttribute(
                Artist.ARTIST_ID_PK_COLUMN));
    }

    public void testNonSelectingQueryString() throws Exception {
        DataContext context = createDataContext();

        int[] counts = context.performNonSelectingQuery("NonSelectingQuery");

        assertNotNull(counts);
        assertEquals(1, counts.length);
        assertEquals(1, counts[0]);

        Painting p = (Painting) context.registeredObject(new ObjectId(
                Painting.class,
                Painting.PAINTING_ID_PK_COLUMN,
                512));
        assertEquals("No Painting Like This", p.getPaintingTitle());
    }

    public void testNonSelectingQueryStringMap() throws Exception {
        DataContext context = createDataContext();

        Map parameters = new HashMap();
        parameters.put("id", new Integer(300));
        parameters.put("title", "Go Figure");
        parameters.put("price", new BigDecimal("22.01"));

        int[] counts = context.performNonSelectingQuery(
                "ParameterizedNonSelectingQuery",
                parameters);

        assertNotNull(counts);
        assertEquals(1, counts.length);
        assertEquals(1, counts[0]);

        Painting p = (Painting) context.registeredObject(new ObjectId(
                Painting.class,
                Painting.PAINTING_ID_PK_COLUMN,
                300));
        assertEquals("Go Figure", p.getPaintingTitle());
    }
}