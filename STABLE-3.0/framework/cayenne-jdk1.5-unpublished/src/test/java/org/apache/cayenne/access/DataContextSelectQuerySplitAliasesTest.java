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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextSelectQuerySplitAliasesTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testAliasPathSplits_SinglePath() {
        ObjectContext context = createDataContext();
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'AA')"));
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (2, 'BB')"));

        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) "
                        + "VALUES (1, 1, 'X')"));
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE)"
                        + " VALUES (2, 2, 'Y')"));

        SelectQuery query = new SelectQuery(Artist.class);
        query.andQualifier(ExpressionFactory.matchExp("p.paintingTitle", "X"));

        query.aliasPathSplits("paintingArray", "p");

        List<Artist> artists = context.performQuery(query);
        assertEquals(1, artists.size());
        assertEquals("AA", artists.get(0).getArtistName());
    }

    public void testAliasPathSplits_SplitJoin() {
        ObjectContext context = createDataContext();
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'AA')"));
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (2, 'BB')"));

        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) "
                        + "VALUES (1, 1, 'X')"));
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE)"
                        + " VALUES (2, 2, 'Y')"));
        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE)"
                        + " VALUES (3, 2, 'X')"));

        SelectQuery query = new SelectQuery(Artist.class);
        query.andQualifier(ExpressionFactory.matchExp("p1.paintingTitle", "X"));
        query.andQualifier(ExpressionFactory.matchExp("p2.paintingTitle", "Y"));

        query.aliasPathSplits("paintingArray", "p1", "p2");

        List<Artist> artists = context.performQuery(query);
        assertEquals(1, artists.size());
        assertEquals("BB", artists.get(0).getArtistName());
    }
}
