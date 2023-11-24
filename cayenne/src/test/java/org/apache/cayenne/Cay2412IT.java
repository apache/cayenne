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

package org.apache.cayenne;

import java.sql.Types;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.1
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class Cay2412IT extends RuntimeCase {

    @Inject
    DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Before
    public void prepareData() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
        tArtist.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.DATE);
        tArtist.insert(1, "artist1", new java.sql.Date(System.currentTimeMillis()));

        TableHelper tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
        tGallery.insert(1, "tate modern");

        TableHelper tPaintings = new TableHelper(dbHelper, "PAINTING");
        tPaintings.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "GALLERY_ID", "ESTIMATED_PRICE");
        for (int i = 1; i <= 3; i++) {
            tPaintings.insert(i, "painting" + i, 1, 1, 22 - i);
        }
    }

    @Ignore("selectFirst() call corrupts object state in context, and because of cache it's also returned for unrelated query")
    @Test
    public void testJoinPrefetch() {
        Artist artist0 = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .localCache("test")
                .selectOne(context);
        assertEquals(3, artist0.getPaintingArray().size());

        Artist artist1 = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .selectFirst(context);
        assertEquals(1, artist1.getPaintingArray().size()); // <-- wrong assertion, but expected

        Artist artist2 = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .localCache("test")
                .selectOne(context);
        assertEquals(3, artist2.getPaintingArray().size()); // <-- assertion failure here, got 1 instead of 3
    }

    @Test
    public void testDisjointByIdPrefetch() {
        Artist artist0 = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjointById())
                .localCache("test")
                .selectOne(context);
        assertEquals(3, artist0.getPaintingArray().size());

        Artist artist1 = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjointById())
                .selectFirst(context);
        assertEquals(3, artist1.getPaintingArray().size());

        Artist artist2 = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTING_ARRAY.disjointById())
                .localCache("test")
                .selectOne(context);
        assertEquals(3, artist2.getPaintingArray().size());
    }

}
