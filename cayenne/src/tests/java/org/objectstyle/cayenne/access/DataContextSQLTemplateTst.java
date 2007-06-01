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

import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextSQLTemplateTst extends CayenneTestCase {
    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }

    public void testFetchDataRows() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists");

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Artist.class, template, true);

        getSQLTemplateBuilder().updateSQLTemplate(query);

        query.setFetchingDataRows(true);

        List rows = context.performQuery(query);
        assertEquals(DataContextTestBase.artistCount, rows.size());
        assertTrue(rows.get(1) instanceof DataRow);

        DataRow row2 = (DataRow) rows.get(1);
        assertEquals(3, row2.size());
        assertEquals(new Integer(33002), row2.get("ARTIST_ID"));
    }

    public void testFetchObjects() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists");

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query =
            getSQLTemplateBuilder().createSQLTemplate(Artist.class, template, true);

        query.setFetchingDataRows(false);

        List objects = context.performQuery(query);
        assertEquals(DataContextTestBase.artistCount, objects.size());
        assertTrue(objects.get(1) instanceof Artist);

        Artist artist2 = (Artist) objects.get(1);
        assertEquals("artist2", artist2.getArtistName());
    }

    public void testFetchLimit() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists");

        int fetchLimit = 3;

        // sanity check
        assertTrue(fetchLimit < DataContextTestBase.artistCount);
        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query =
            getSQLTemplateBuilder().createSQLTemplate(Artist.class, template, true);
        query.setFetchLimit(fetchLimit);

        List objects = context.performQuery(query);
        assertEquals(fetchLimit, objects.size());
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testPageSize() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists");

        int pageSize = 3;

        // sanity check
        assertTrue(pageSize < DataContextTestBase.artistCount);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query =
            getSQLTemplateBuilder().createSQLTemplate(Artist.class, template, true);

        query.setPageSize(pageSize);

        List objects = context.performQuery(query);
        assertEquals(DataContextTestBase.artistCount, objects.size());
        assertTrue(objects.get(0) instanceof Artist);

        assertTrue(objects instanceof IncrementalFaultList);
        IncrementalFaultList pagedList = (IncrementalFaultList) objects;
        assertEquals(
            DataContextTestBase.artistCount - pageSize,
            pagedList.getUnfetchedObjects());

        // check if we can resolve subsequent pages
        Artist artist = (Artist) objects.get(pageSize);

        int expectUnresolved = DataContextTestBase.artistCount - pageSize - pageSize;
        if (expectUnresolved < 0) {
            expectUnresolved = 0;
        }
        assertEquals(expectUnresolved, pagedList.getUnfetchedObjects());
        assertEquals("artist" + (pageSize + 1), artist.getArtistName());
    }

    public void testIteratedQuery() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists");

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query =
            getSQLTemplateBuilder().createSQLTemplate(Artist.class, template, true);

        ResultIterator it = context.performIteratedQuery(query);

        try {
            int i = 0;

            while (it.hasNextRow()) {
                i++;

                Map row = it.nextDataRow();
                assertEquals(3, row.size());
                assertEquals(new Integer(33000 + i), row.get("ARTIST_ID"));
            }

            assertEquals(DataContextTestBase.artistCount, i);
        }
        finally {
            it.close();
        }
    }
}
