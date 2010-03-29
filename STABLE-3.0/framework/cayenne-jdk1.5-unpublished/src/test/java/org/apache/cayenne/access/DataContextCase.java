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

package org.apache.cayenne.access;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.art.ROArtist;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public abstract class DataContextCase extends CayenneCase {

    public static final int artistCount = 25;
    public static final int galleryCount = 10;

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        createTestData("testArtists");

        context = createDataContext();
    }

    protected Painting fetchPainting(String name, boolean prefetchArtist) {
        SelectQuery select = new SelectQuery(Painting.class, ExpressionFactory.matchExp(
                "paintingTitle",
                name));
        if (prefetchArtist) {
            select.addPrefetch("toArtist");
        }

        List ats = context.performQuery(select);
        return (ats.size() > 0) ? (Painting) ats.get(0) : null;
    }

    protected Artist fetchArtist(String name, boolean prefetchPaintings) {
        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.matchExp(
                "artistName",
                name));
        if (prefetchPaintings) {
            q.addPrefetch("paintingArray");
        }
        List ats = context.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }

    protected ROArtist fetchROArtist(String name) {
        SelectQuery q = new SelectQuery(ROArtist.class, ExpressionFactory.matchExp(
                "artistName",
                name));
        List ats = context.performQuery(q);
        return (ats.size() > 0) ? (ROArtist) ats.get(0) : null;
    }

    /**
     * Temporary workaround for current inability to store dates in test fixture XML
     * files.
     */
    public void populateExhibits() throws Exception {
        String insertPaint = "INSERT INTO EXHIBIT (EXHIBIT_ID, GALLERY_ID, OPENING_DATE, CLOSING_DATE) VALUES (?, ?, ?, ?)";

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
     * Helper method that takes one of the artists from the standard dataset (always the
     * same one) and creates a new painting for this artist, committing it to the
     * database. Both Painting and Artist will be cached in current DataContext.
     */
    protected Painting insertPaintingInContext(String paintingName) {
        Painting painting = (Painting) context.newObject("Painting");
        painting.setPaintingTitle(paintingName);
        painting.setToArtist(fetchArtist("artist2", false));

        context.commitChanges();

        return painting;
    }
}
