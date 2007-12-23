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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.apache.art.CompoundPainting;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * @author Andrus Adamchik
 */
public class DataContextFlattenedAttributesTest extends CayenneCase {

    final int artistCount = 4;
    final int galleryCount = 2;
    final int paintCount = 8;

    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    private void populateTables() throws Exception {
        String insertArtist = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";
        String insertGal = "INSERT INTO GALLERY (GALLERY_ID, GALLERY_NAME) VALUES (?,?)";
        String insertPaint = "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE, GALLERY_ID) VALUES (?, ?, ?, ?, ?)";
        String insertPaintInfo = "INSERT INTO PAINTING_INFO (PAINTING_ID, TEXT_REVIEW) VALUES (?, ?)";

        Connection conn = getConnection();

        try {
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(insertArtist);
            long dateBase = System.currentTimeMillis();
            for (int i = 1; i <= artistCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "artist" + i);
                stmt.setDate(3, new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
                stmt.executeUpdate();
            }
            stmt.close();

            stmt = conn.prepareStatement(insertGal);
            for (int i = 1; i <= galleryCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "gallery" + i);
                stmt.executeUpdate();
            }
            stmt.close();

            stmt = conn.prepareStatement(insertPaint);
            for (int i = 1; i <= paintCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "painting" + i);
                stmt.setInt(3, (i - 1) % artistCount + 1);
                stmt.setBigDecimal(4, new BigDecimal(1000d));
                if (i == 3)
                    stmt.setNull(5, Types.INTEGER);
                else
                    stmt.setInt(5, (i - 1) % galleryCount + 1);
                stmt.executeUpdate();
            }
            stmt.close();

            stmt = conn.prepareStatement(insertPaintInfo);
            for (int i = 1; i <= paintCount / 2; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "painting review" + i);
                stmt.executeUpdate();
            }
            stmt.close();

            conn.commit();
        }
        finally {
            conn.close();
        }
    }

    public void testSelectCompound1() throws Exception {
        populateTables();
        SelectQuery query = new SelectQuery(CompoundPainting.class);
        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue("CompoundPainting expected, got " + objects.get(0).getClass(), objects
                .get(0) instanceof CompoundPainting);

        for (Iterator<?> i = objects.iterator(); i.hasNext();) {
            CompoundPainting painting = (CompoundPainting) i.next();
            Number id = (Number) painting
                    .getObjectId()
                    .getIdSnapshot()
                    .get("PAINTING_ID");
            assertEquals("CompoundPainting.getPaintingTitle(): "
                    + painting.getPaintingTitle(), "painting" + id, painting
                    .getPaintingTitle());
            assertEquals(
                    "CompoundPainting.getTextReview(): " + painting.getTextReview(),
                    "painting review" + id,
                    painting.getTextReview());
            assertEquals(
                    "CompoundPainting.getArtistName(): " + painting.getArtistName(),
                    painting.getToArtist().getArtistName(),
                    painting.getArtistName());
            assertEquals(
                    "CompoundPainting.getArtistName(): " + painting.getGalleryName(),
                    painting.getToGallery().getGalleryName(),
                    painting.getGalleryName());
        }
    }

    public void testSelectCompound2() throws Exception {
        populateTables();
        SelectQuery query = new SelectQuery(CompoundPainting.class, ExpressionFactory
                .matchExp("artistName", "artist2"));
        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(1, objects.size());
        assertTrue("CompoundPainting expected, got " + objects.get(0).getClass(), objects
                .get(0) instanceof CompoundPainting);

        for (Iterator<?> i = objects.iterator(); i.hasNext();) {
            CompoundPainting painting = (CompoundPainting) i.next();
            Number id = (Number) painting
                    .getObjectId()
                    .getIdSnapshot()
                    .get("PAINTING_ID");
            assertEquals("CompoundPainting.getObjectId(): " + id, id.intValue(), 2);
            assertEquals("CompoundPainting.getPaintingTitle(): "
                    + painting.getPaintingTitle(), "painting" + id, painting
                    .getPaintingTitle());
            assertEquals(
                    "CompoundPainting.getTextReview(): " + painting.getTextReview(),
                    "painting review" + id,
                    painting.getTextReview());
            assertEquals(
                    "CompoundPainting.getArtistName(): " + painting.getArtistName(),
                    "artist2",
                    painting.getArtistName());
            assertEquals(
                    "CompoundPainting.getArtistName(): " + painting.getGalleryName(),
                    painting.getToGallery().getGalleryName(),
                    painting.getGalleryName());
        }
    }
    
    public void testInsert() {
        CompoundPainting o1 = context.newObject(CompoundPainting.class);
        o1.setArtistName("A1");
        o1.setEstimatedPrice(new BigDecimal(1.0d));
        o1.setGalleryName("G1");
        o1.setPaintingTitle("P1");
        o1.setTextReview("T1");
        
        context.commitChanges();
    }
}
