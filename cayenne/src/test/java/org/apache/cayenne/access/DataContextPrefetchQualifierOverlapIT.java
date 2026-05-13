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

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataContextPrefetchQualifierOverlapIT  {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

        private DataContext context;


    private void createTwoArtistsThreePaintingsDataSet() throws Exception {
        TableHelper tArtist = env.table("ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        TableHelper tPainting = env.table("PAINTING");
        tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID");

        tArtist.insert(1, "A1");
        tArtist.insert(2, "A2");

        tPainting.insert(1, "ABC", 1);
        tPainting.insert(2, "ABD", 1);
        tPainting.insert(3, "ACC", 1);
    }

    @BeforeEach
    public void setUp() {
        context = env.dataContext();
    }

    @Test
    public void toManyDisjointOverlappingQualifierWithInnerJoin() throws Exception {
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
    public void toManyJointOverlappingQualifierWithInnerJoin() throws Exception {
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
    public void toManyJointOverlappingQualifierWithOuterJoin() throws Exception {
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
