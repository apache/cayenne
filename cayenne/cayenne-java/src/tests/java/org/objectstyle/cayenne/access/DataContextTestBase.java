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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.art.ROArtist;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextTestBase extends CayenneTestCase {

    public static final int artistCount = 25;
    public static final int galleryCount = 10;

    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        createTestData("testArtists");

        context = createDataContext();
    }

    protected Painting fetchPainting(String name, boolean prefetchArtist) {
        SelectQuery select =
            new SelectQuery(
                Painting.class,
                ExpressionFactory.matchExp("paintingTitle", name));
        if (prefetchArtist) {
            select.addPrefetch("toArtist");
        }

        List ats = context.performQuery(select);
        return (ats.size() > 0) ? (Painting) ats.get(0) : null;
    }

    protected Artist fetchArtist(String name, boolean prefetchPaintings) {
        SelectQuery q =
            new SelectQuery(Artist.class, ExpressionFactory.matchExp("artistName", name));
        if (prefetchPaintings) {
            q.addPrefetch("paintingArray");
        }
        List ats = context.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }

    protected ROArtist fetchROArtist(String name) {
        SelectQuery q =
            new SelectQuery(
                ROArtist.class,
                ExpressionFactory.matchExp("artistName", name));
        List ats = context.performQuery(q);
        return (ats.size() > 0) ? (ROArtist) ats.get(0) : null;
    }

    /**
     * Temporary workaround for current inability to store dates in test 
     * fixture XML files.
     */
    public void populateExhibits() throws Exception {
        String insertPaint =
            "INSERT INTO EXHIBIT (EXHIBIT_ID, GALLERY_ID, OPENING_DATE, CLOSING_DATE) VALUES (?, ?, ?, ?)";

        Connection conn = getConnection();

        try {
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(insertPaint);
            Timestamp now = new Timestamp(System.currentTimeMillis());

            for (int i = 1; i <= 2; i++) {
                stmt.setInt(1, i);
                stmt.setInt(2, 33000 + i);
                stmt.setTimestamp(3, now);
                stmt.setTimestamp(4, now);
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();
        }
        finally {
            conn.close();
        }
    }

    /**
     * Helper method that takes one of the artists from the standard
     * dataset (always the same one) and creates a new painting for this artist,
     * committing it to the database. Both Painting and Artist will be cached in current
     * DataContext.
     */
    protected Painting insertPaintingInContext(String paintingName) {
        Painting painting = (Painting) context.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle(paintingName);
        painting.setToArtist(fetchArtist("artist2", false));

        context.commitChanges();

        return painting;
    }
}
