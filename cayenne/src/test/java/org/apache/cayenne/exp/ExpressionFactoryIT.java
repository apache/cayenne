/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.exp;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Award;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.List;

import static org.apache.cayenne.exp.ExpressionFactory.exp;
import static org.apache.cayenne.exp.ExpressionFactory.greaterExp;
import static org.apache.cayenne.exp.FunctionExpressionFactory.lengthExp;
import static org.apache.cayenne.exp.FunctionExpressionFactory.substringExp;
import static org.apache.cayenne.exp.FunctionExpressionFactory.trimExp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ExpressionFactoryIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    // CAY-416
    @Test
    public void testCollectionMatch() {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("artist");
        Painting p1 = context.newObject(Painting.class),
                p2 = context.newObject(Painting.class),
                p3 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        p3.setPaintingTitle("p3");
        artist.addToPaintingArray(p1);
        artist.addToPaintingArray(p2);

        context.commitChanges();

        assertTrue(ExpressionFactory.matchExp("paintingArray", p1).match(artist));
        assertFalse(ExpressionFactory.matchExp("paintingArray", p3).match(artist));
        assertTrue(ExpressionFactory.noMatchExp("paintingArray", p1).match(artist)); // changed to align with SQL
        assertTrue(ExpressionFactory.noMatchExp("paintingArray", p3).match(artist));

        assertTrue(ExpressionFactory.matchExp("paintingArray.paintingTitle", "p1").match(artist));
        assertFalse(ExpressionFactory.matchExp("paintingArray.paintingTitle", "p3").match(artist));
        assertTrue(ExpressionFactory.noMatchExp("paintingArray.paintingTitle", "p1")
                           .match(artist)); // changed to align with SQL
        assertTrue(ExpressionFactory.noMatchExp("paintingArray.paintingTitle", "p3").match(artist));

        assertTrue(ExpressionFactory.inExp("paintingTitle", "p1").match(p1));
        assertFalse(ExpressionFactory.notInExp("paintingTitle", "p3").match(p3));
    }

    @Test
    public void testIn() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a1");
        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        Painting p2 = context.newObject(Painting.class);
        p2.setPaintingTitle("p2");
        a1.addToPaintingArray(p1);
        a1.addToPaintingArray(p2);

        Expression in = ExpressionFactory.inExp("paintingArray", p1);
        assertTrue(in.match(a1));
    }

    @Test
    public void testEscapeCharacter() {
        if (!accessStackAdapter.supportsEscapeInLike()) {
            return;
        }

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("A_1");
        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("A_2");
        context.commitChanges();

        Expression ex1 = ExpressionFactory.likeIgnoreCaseDbExp("ARTIST_NAME", "A*_1", '*');
        List<Artist> artists = ObjectSelect.query(Artist.class, ex1).select(context);
        assertEquals(1, artists.size());

        Expression ex2 = ExpressionFactory.likeExp("artistName", "A*_2", '*');
        artists = ObjectSelect.query(Artist.class, ex2).select(context);
        assertEquals(1, artists.size());
    }

    @Test
    public void testContains_Escape() {

        if (!accessStackAdapter.supportsEscapeInLike()) {
            return;
        }

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("MA_1X");
        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("CA%2Y");
        context.commitChanges();

        Expression ex1 = ExpressionFactory.containsExp(Artist.ARTIST_NAME.getName(), "A_1");
        List<Artist> artists = ObjectSelect.query(Artist.class, ex1).select(context);
        assertEquals(1, artists.size());

        Expression ex2 = ExpressionFactory.containsExp(Artist.ARTIST_NAME.getName(), "A%2");
        artists = ObjectSelect.query(Artist.class, ex2).select(context);
        assertEquals(1, artists.size());
    }

    @Test
    public void testSplitExpressions() {
        Artist artist1 = context.newObject(Artist.class),
                artist2 = context.newObject(Artist.class);
        artist1.setArtistName("a1");
        artist2.setArtistName("a2");

        Painting p1 = context.newObject(Painting.class),
                p2 = context.newObject(Painting.class),
                p3 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        p3.setPaintingTitle("p3");

        Gallery g1 = context.newObject(Gallery.class),
                g2 = context.newObject(Gallery.class);
        g1.setGalleryName("g1");
        g2.setGalleryName("g2");

        artist1.addToPaintingArray(p1);
        artist1.addToPaintingArray(p2);
        artist2.addToPaintingArray(p3);

        g1.addToPaintingArray(p1);
        g1.addToPaintingArray(p3);
        g2.addToPaintingArray(p2);

        context.commitChanges();

        List<Artist> objArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("|paintingArray.toGallery.galleryName", "g1", "g2"))
                .select(context);
        List<Artist> dbArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("db:|paintingArray.toGallery.GALLERY_NAME", "g1", "g2"))
                .select(context);
        assertFalse(objArtists.isEmpty());
        assertEquals(objArtists, dbArtists);
    }

    @Test
    public void testSplitExpressions_EndsWithRelationship() {
        Artist artist1 = context.newObject(Artist.class),
                artist2 = context.newObject(Artist.class);
        artist1.setArtistName("a1");
        artist2.setArtistName("a2");

        Painting p1 = context.newObject(Painting.class),
                p2 = context.newObject(Painting.class),
                p3 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        p3.setPaintingTitle("p3");

        artist1.addToPaintingArray(p1);
        artist1.addToPaintingArray(p2);
        artist2.addToPaintingArray(p3);

        context.commitChanges();

        List<Artist> objArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("|paintingArray", p1, p2)).select(context);
        List<Artist> dbArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("db:|paintingArray", p1, p2)).select(context);
        assertFalse(objArtists.isEmpty());
        assertEquals(objArtists, dbArtists);
    }

    @Test
    public void testSplitExpressions_EndsWithRelationship_DifferentObjDbPath() {
        Artist artist1 = context.newObject(Artist.class),
                artist2 = context.newObject(Artist.class);
        artist1.setArtistName("a1");
        artist2.setArtistName("a2");

        Award aw1 = context.newObject(Award.class),
                aw2 = context.newObject(Award.class),
                aw3 = context.newObject(Award.class);
        aw1.setName("aw1");
        aw2.setName("aw2");
        aw3.setName("aw3");

        artist1.addToAwardArray(aw1);
        artist1.addToAwardArray(aw2);
        artist2.addToAwardArray(aw3);

        context.commitChanges();

        List<Artist> objArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("|awardArray", aw1, aw2)).select(context);
        List<Artist> dbArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("db:|artistAwardArray", aw1, aw2)).select(context);
        assertFalse(objArtists.isEmpty());
        assertEquals(objArtists, dbArtists);
    }

    @Test
    public void testSplitExpressions_EndsWithRelationship_Flattened() {
        Artist a1 = context.newObject(Artist.class),
                a2 = context.newObject(Artist.class);
        a1.setArtistName("a1");
        a2.setArtistName("a2");

        ArtGroup ag1 = context.newObject(ArtGroup.class),
                ag2 = context.newObject(ArtGroup.class);
        ag1.setName("ag1");
        ag2.setName("ag2");

        a1.addToGroupArray(ag1);
        a2.addToGroupArray(ag1);
        a2.addToGroupArray(ag2);

        context.commitChanges();

        List<Artist> objArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("|groupArray", ag1, ag2))
                .select(context);
        List<Artist> dbArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("db:|artistGroupArray.toGroup", ag1, ag2))
                .select(context);
        assertFalse(objArtists.isEmpty());
        assertEquals(objArtists, dbArtists);
    }

    @Test
    public void testDifferentExpressionAPI() {
        List<Artist> res;

        // First version via expression string
        Expression exp1 = exp("length(substring(artistName, 1, 3)) > length(trim(artistName))");
        res = ObjectSelect.query(Artist.class, exp1).select(context);
        assertEquals(0, res.size());

        // Second version via FunctionExpressionFactory API
        Expression exp2 = greaterExp(lengthExp(substringExp(Artist.ARTIST_NAME.getExpression(), 1, 3)),
                                     lengthExp(trimExp(Artist.ARTIST_NAME.getExpression())));
        res = ObjectSelect.query(Artist.class, exp2).select(context);
        assertEquals(0, res.size());

        // Third version via Property API
        Expression exp3 = Artist.ARTIST_NAME.substring(1, 3).length().gt(Artist.ARTIST_NAME.trim().length());
        res = ObjectSelect.query(Artist.class, exp3).select(context);
        assertEquals(0, res.size());

        // Check that all expressions are equal
        assertEquals(exp1, exp2);
        assertEquals(exp3, exp3);
    }
}
