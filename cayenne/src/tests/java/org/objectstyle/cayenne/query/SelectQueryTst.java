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
package org.objectstyle.cayenne.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;

public class SelectQueryTst extends SelectQueryBase {
    private static final int _artistCount = 20;

    public void testFetchLimit() throws Exception {
        query.setRoot(Artist.class);
        query.setFetchLimit(7);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(7, objects.size());
    }

    public void testSelectAllObjectsRootEntityName() throws Exception {
        query.setRoot(Artist.class);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectAllObjectsRootClass() throws Exception {
        query.setRoot(Artist.class);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectAllObjectsRootObjEntity() throws Exception {
        query.setRoot(this.getDomain().getEntityResolver().lookupObjEntity(Artist.class));
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectLikeExactMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "artist1");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(1, objects.size());
    }

    public void testSelectNotLikeSingleWildcardMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.notLikeExp("artistName", "artist11%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(_artistCount - 1, objects.size());
    }

    public void testSelectNotLikeIgnoreCaseSingleWildcardMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual =
            ExpressionFactory.notLikeIgnoreCaseExp("artistName", "aRtIsT11%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(_artistCount - 1, objects.size());
    }

    public void testSelectLikeCaseSensitive() throws Exception {
        if (!getAccessStackAdapter().supportsCaseSensitiveLike()) {
            return;
        }

        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "aRtIsT%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(0, objects.size());
    }

    public void testSelectLikeSingleWildcardMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "artist11%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(1, objects.size());
    }

    public void testSelectLikeMultipleWildcardMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "artist1%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(11, objects.size());
    }

    /** Test how "like ignore case" works when using uppercase parameter. */
    public void testSelectLikeIgnoreCaseObjects1() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeIgnoreCaseExp("artistName", "ARTIST%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    /** Test how "like ignore case" works when using lowercase parameter. */
    public void testSelectLikeIgnoreCaseObjects2() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeIgnoreCaseExp("artistName", "artist%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectIn() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = Expression.fromString("artistName in ('artist1', 'artist2')");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(2, objects.size());
    }

    public void testSelectParameterizedIn() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = Expression.fromString("artistName in $list");
        query.setQualifier(qual);
        query =
            query.queryWithParameters(
                Collections.singletonMap("list", new Object[] { "artist1", "artist2" }));
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(2, objects.size());
    }

    public void testSelectCustAttributes() throws Exception {
        query.setRoot(Artist.class);
        query.addCustomDbAttribute("ARTIST_NAME");

        List results = createDataContext().performQuery(query);

        // check query results
        assertEquals(_artistCount, results.size());

        Map row = (Map) results.get(0);
        assertNotNull(row.get("ARTIST_NAME"));
        assertEquals(1, row.size());
    }

    protected void populateTables() throws java.lang.Exception {
        String insertArtist =
            "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";
        Connection conn = getConnection();

        try {
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(insertArtist);
            long dateBase = System.currentTimeMillis();

            for (int i = 1; i <= _artistCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "artist" + i);
                stmt.setDate(3, new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();
        }
        finally {
            conn.close();
        }
    }
}