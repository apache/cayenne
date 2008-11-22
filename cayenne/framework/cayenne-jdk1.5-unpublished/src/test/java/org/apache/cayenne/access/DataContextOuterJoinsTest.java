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

import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextOuterJoinsTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }
    
    public void testSelectWithOuterJoinFlattened() throws Exception {
        createTestData("testSelectWithOuterJoinFlattened");

        SelectQuery missingToManyQuery = new SelectQuery(Artist.class);
        missingToManyQuery.andQualifier(ExpressionFactory.matchExp(
                Artist.GROUP_ARRAY_PROPERTY + Entity.OUTER_JOIN_INDICATOR,
                null));
        missingToManyQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        List<Artist> artists = createDataContext().performQuery(missingToManyQuery);
        assertEquals(1, artists.size());
        assertEquals("BB1", artists.get(0).getArtistName());
    }

    public void testSelectWithOuterJoin() throws Exception {

        createTestData("testSelectWithOuterJoin");

        SelectQuery missingToManyQuery = new SelectQuery(Artist.class);
        missingToManyQuery.andQualifier(ExpressionFactory.matchExp(
                Artist.PAINTING_ARRAY_PROPERTY + Entity.OUTER_JOIN_INDICATOR,
                null));
        missingToManyQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        List<Artist> artists = createDataContext().performQuery(missingToManyQuery);
        assertEquals(2, artists.size());
        assertEquals("BB1", artists.get(0).getArtistName());

        SelectQuery mixedConditionQuery = new SelectQuery(Artist.class);
        mixedConditionQuery.andQualifier(ExpressionFactory.matchExp(
                Artist.PAINTING_ARRAY_PROPERTY + Entity.OUTER_JOIN_INDICATOR,
                null));
        mixedConditionQuery.orQualifier(ExpressionFactory.matchExp(
                Artist.ARTIST_NAME_PROPERTY,
                "AA1"));
        mixedConditionQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        artists = createDataContext().performQuery(mixedConditionQuery);
        assertEquals(3, artists.size());
        assertEquals("AA1", artists.get(0).getArtistName());
        assertEquals("BB1", artists.get(1).getArtistName());
        assertEquals("BB2", artists.get(2).getArtistName());
    }

    public void testSelectWithOuterJoinFromString() throws Exception {

        createTestData("testSelectWithOuterJoin");

        SelectQuery missingToManyQuery = new SelectQuery(Artist.class);
        missingToManyQuery.andQualifier(Expression.fromString("paintingArray+ = null"));
        missingToManyQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        List<Artist> artists = createDataContext().performQuery(missingToManyQuery);
        assertEquals(2, artists.size());
        assertEquals("BB1", artists.get(0).getArtistName());

        SelectQuery mixedConditionQuery = new SelectQuery(Artist.class);
        mixedConditionQuery.andQualifier(ExpressionFactory.matchExp(
                Artist.PAINTING_ARRAY_PROPERTY + Entity.OUTER_JOIN_INDICATOR,
                null));
        mixedConditionQuery.orQualifier(ExpressionFactory.matchExp(
                Artist.ARTIST_NAME_PROPERTY,
                "AA1"));
        mixedConditionQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        artists = createDataContext().performQuery(mixedConditionQuery);
        assertEquals(3, artists.size());
        assertEquals("AA1", artists.get(0).getArtistName());
        assertEquals("BB1", artists.get(1).getArtistName());
        assertEquals("BB2", artists.get(2).getArtistName());
    }

    public void testSelectWithOuterOrdering() throws Exception {

        createTestData("testSelectWithOuterOrdering");

        SelectQuery query = new SelectQuery(Painting.class);

        query.addOrdering("toArtist+.artistName", Ordering.DESC);

        List<Artist> paintings = createDataContext().performQuery(query);
        assertEquals(3, paintings.size());
    }
}
