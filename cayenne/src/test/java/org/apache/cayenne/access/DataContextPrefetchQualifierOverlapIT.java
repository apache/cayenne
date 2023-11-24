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
package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextPrefetchQualifierOverlapIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    private void createTwoArtistsThreePaintingsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        TableHelper tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID");

        tArtist.insert(1, "A1");
        tArtist.insert(2, "A2");

        tPainting.insert(1, "ABC", 1);
        tPainting.insert(2, "ABD", 1);
        tPainting.insert(3, "ACC", 1);
    }

    @Test
    public void testToManyDisjointOverlappingQualifierWithInnerJoin() throws Exception {
        createTwoArtistsThreePaintingsDataSet();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .and(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("AB%"))
                .prefetch(Artist.PAINTING_ARRAY.disjoint());

        List<Artist> result = query.select(context);
        assertEquals(1, result.size());

        Artist a = result.get(0);
        assertEquals(3, a.getPaintingArray().size());
    }

    @Test
    public void testToManyJointOverlappingQualifierWithInnerJoin() throws Exception {
        createTwoArtistsThreePaintingsDataSet();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .and(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("AB%"))
                .prefetch(Artist.PAINTING_ARRAY.joint());

        List<Artist> result = query.select(context);
        assertEquals(1, result.size());

        Artist a = result.get(0);
        assertEquals(3, a.getPaintingArray().size());
    }

    @Test
    public void testToManyJointOverlappingQualifierWithOuterJoin() throws Exception {
        createTwoArtistsThreePaintingsDataSet();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .and(ExpressionFactory.likeExp("paintingArray+.paintingTitle", "AB%"))
                .prefetch(Artist.PAINTING_ARRAY.joint())
                .or(Artist.ARTIST_NAME.like("A%"))
                .orderBy(Artist.ARTIST_NAME.asc());

        List<Artist> result = query.select(context);
        assertEquals(2, result.size());

        Artist a = result.get(0);
        assertEquals(3, a.getPaintingArray().size());

        Artist a1 = result.get(1);
        assertEquals(0, a1.getPaintingArray().size());
    }
}
