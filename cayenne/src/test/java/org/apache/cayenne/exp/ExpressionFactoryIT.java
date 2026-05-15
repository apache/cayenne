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

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Award;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.apache.cayenne.exp.ExpressionFactory.exp;
import static org.apache.cayenne.exp.ExpressionFactory.greaterExp;
import static org.apache.cayenne.exp.FunctionExpressionFactory.lengthExp;
import static org.apache.cayenne.exp.FunctionExpressionFactory.substringExp;
import static org.apache.cayenne.exp.FunctionExpressionFactory.trimExp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExpressionFactoryIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    // CAY-416
    @Test
    public void collectionMatch() {
        Artist artist = env.context().newObject(Artist.class);
        artist.setArtistName("artist");
        Painting p1 = env.context().newObject(Painting.class),
                p2 = env.context().newObject(Painting.class),
                p3 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        p3.setPaintingTitle("p3");
        artist.addToPaintingArray(p1);
        artist.addToPaintingArray(p2);

        env.context().commitChanges();

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
    public void in() {
        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("a1");
        Painting p1 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");
        Painting p2 = env.context().newObject(Painting.class);
        p2.setPaintingTitle("p2");
        a1.addToPaintingArray(p1);
        a1.addToPaintingArray(p2);

        Expression in = ExpressionFactory.inExp("paintingArray", p1);
        assertTrue(in.match(a1));
    }

    @Test
    public void escapeCharacter() {
        if (!env.testDbAdapter().supportsEscapeInLike()) {
            return;
        }

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("A_1");
        Artist a2 = env.context().newObject(Artist.class);
        a2.setArtistName("A_2");
        env.context().commitChanges();

        Expression ex1 = ExpressionFactory.likeIgnoreCaseDbExp("ARTIST_NAME", "A*_1", '*');
        List<Artist> artists = ObjectSelect.query(Artist.class, ex1).select(env.context());
        assertEquals(1, artists.size());

        Expression ex2 = ExpressionFactory.likeExp("artistName", "A*_2", '*');
        artists = ObjectSelect.query(Artist.class, ex2).select(env.context());
        assertEquals(1, artists.size());
    }

    @Test
    public void contains_Escape() {

        if (!env.testDbAdapter().supportsEscapeInLike()) {
            return;
        }

        Artist a1 = env.context().newObject(Artist.class);
        a1.setArtistName("MA_1X");
        Artist a2 = env.context().newObject(Artist.class);
        a2.setArtistName("CA%2Y");
        env.context().commitChanges();

        Expression ex1 = ExpressionFactory.containsExp(Artist.ARTIST_NAME.getName(), "A_1");
        List<Artist> artists = ObjectSelect.query(Artist.class, ex1).select(env.context());
        assertEquals(1, artists.size());

        Expression ex2 = ExpressionFactory.containsExp(Artist.ARTIST_NAME.getName(), "A%2");
        artists = ObjectSelect.query(Artist.class, ex2).select(env.context());
        assertEquals(1, artists.size());
    }

    @Test
    public void splitExpressions() {
        Artist artist1 = env.context().newObject(Artist.class),
                artist2 = env.context().newObject(Artist.class);
        artist1.setArtistName("a1");
        artist2.setArtistName("a2");

        Painting p1 = env.context().newObject(Painting.class),
                p2 = env.context().newObject(Painting.class),
                p3 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        p3.setPaintingTitle("p3");

        Gallery g1 = env.context().newObject(Gallery.class),
                g2 = env.context().newObject(Gallery.class);
        g1.setGalleryName("g1");
        g2.setGalleryName("g2");

        artist1.addToPaintingArray(p1);
        artist1.addToPaintingArray(p2);
        artist2.addToPaintingArray(p3);

        g1.addToPaintingArray(p1);
        g1.addToPaintingArray(p3);
        g2.addToPaintingArray(p2);

        env.context().commitChanges();

        List<Artist> objArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("|paintingArray.toGallery.galleryName", "g1", "g2"))
                .select(env.context());
        List<Artist> dbArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("db:|paintingArray.toGallery.GALLERY_NAME", "g1", "g2"))
                .select(env.context());
        assertFalse(objArtists.isEmpty());
        assertEquals(objArtists, dbArtists);
    }

    @Test
    public void splitExpressions_EndsWithRelationship() {
        Artist artist1 = env.context().newObject(Artist.class),
                artist2 = env.context().newObject(Artist.class);
        artist1.setArtistName("a1");
        artist2.setArtistName("a2");

        Painting p1 = env.context().newObject(Painting.class),
                p2 = env.context().newObject(Painting.class),
                p3 = env.context().newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        p3.setPaintingTitle("p3");

        artist1.addToPaintingArray(p1);
        artist1.addToPaintingArray(p2);
        artist2.addToPaintingArray(p3);

        env.context().commitChanges();

        List<Artist> objArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("|paintingArray", p1, p2)).select(env.context());
        List<Artist> dbArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("db:|paintingArray", p1, p2)).select(env.context());
        assertFalse(objArtists.isEmpty());
        assertEquals(objArtists, dbArtists);
    }

    @Test
    public void splitExpressions_EndsWithRelationship_DifferentObjDbPath() {
        Artist artist1 = env.context().newObject(Artist.class),
                artist2 = env.context().newObject(Artist.class);
        artist1.setArtistName("a1");
        artist2.setArtistName("a2");

        Award aw1 = env.context().newObject(Award.class),
                aw2 = env.context().newObject(Award.class),
                aw3 = env.context().newObject(Award.class);
        aw1.setName("aw1");
        aw2.setName("aw2");
        aw3.setName("aw3");

        artist1.addToAwardArray(aw1);
        artist1.addToAwardArray(aw2);
        artist2.addToAwardArray(aw3);

        env.context().commitChanges();

        List<Artist> objArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("|awardArray", aw1, aw2)).select(env.context());
        List<Artist> dbArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("db:|artistAwardArray", aw1, aw2)).select(env.context());
        assertFalse(objArtists.isEmpty());
        assertEquals(objArtists, dbArtists);
    }

    @Test
    public void splitExpressions_EndsWithRelationship_Flattened() {
        Artist a1 = env.context().newObject(Artist.class),
                a2 = env.context().newObject(Artist.class);
        a1.setArtistName("a1");
        a2.setArtistName("a2");

        ArtGroup ag1 = env.context().newObject(ArtGroup.class),
                ag2 = env.context().newObject(ArtGroup.class);
        ag1.setName("ag1");
        ag2.setName("ag2");

        a1.addToGroupArray(ag1);
        a2.addToGroupArray(ag1);
        a2.addToGroupArray(ag2);

        env.context().commitChanges();

        List<Artist> objArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("|groupArray", ag1, ag2))
                .select(env.context());
        List<Artist> dbArtists = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchAllExp("db:|artistGroupArray.toGroup", ag1, ag2))
                .select(env.context());
        assertFalse(objArtists.isEmpty());
        assertEquals(objArtists, dbArtists);
    }

    @Test
    public void differentExpressionAPI() {
        List<Artist> res;

        // First version via expression string
        Expression exp1 = exp("length(substring(artistName, 1, 3)) > length(trim(artistName))");
        res = ObjectSelect.query(Artist.class, exp1).select(env.context());
        assertEquals(0, res.size());

        // Second version via FunctionExpressionFactory API
        Expression exp2 = greaterExp(lengthExp(substringExp(Artist.ARTIST_NAME.getExpression(), 1, 3)),
                                     lengthExp(trimExp(Artist.ARTIST_NAME.getExpression())));
        res = ObjectSelect.query(Artist.class, exp2).select(env.context());
        assertEquals(0, res.size());

        // Third version via Property API
        Expression exp3 = Artist.ARTIST_NAME.substring(1, 3).length().gt(Artist.ARTIST_NAME.trim().length());
        res = ObjectSelect.query(Artist.class, exp3).select(env.context());
        assertEquals(0, res.size());

        // Check that all expressions are equal
        assertEquals(exp1, exp2);
        assertEquals(exp3, exp3);
    }
}
