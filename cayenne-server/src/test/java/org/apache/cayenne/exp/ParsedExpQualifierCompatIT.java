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

package org.apache.cayenne.exp;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ParsedExpQualifierCompatIT extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE");
    }

    protected void createTwentyFiveArtists() throws Exception {
        for (int i = 1; i <= 25; i++) {
            tArtist.insert(i, "artist" + i);
        }
    }

    protected void createTwentyFiveArtistsAndPaintings() throws Exception {
        createTwentyFiveArtists();
        for (int i = 1; i <= 25; i++) {
            tPainting.insert(i, "p_artist" + i, i, i * 1000);
        }
    }

    private <T> List<T> execute(Class<T> root, Expression qualifier) {
        return execute(root, qualifier, null);
    }

    private <T> List<T> execute(Class<T> root, Expression qualifier, String prefecth) {
        SelectQuery query = new SelectQuery(root, qualifier);
        if (prefecth != null) {
            query.addPrefetch(prefecth);
        }
        return context.performQuery(query);
    }

    @Test
    public void testOr() throws Exception {

        createTwentyFiveArtists();

        Expression parsed = Expression
                .fromString("artistName='artist1' or artistName='artist3'");
        assertEquals(2, execute(Artist.class, parsed).size());

        parsed = Expression
                .fromString("artistName='artist1' or artistName='artist3' or artistName='artist5'");
        assertEquals(3, execute(Artist.class, parsed).size());
    }

    @Test
    public void testAnd() throws Exception {
        createTwentyFiveArtists();

        Expression parsed = Expression
                .fromString("artistName='artist1' and artistName='artist1'");
        assertEquals(1, execute(Artist.class, parsed).size());

        parsed = Expression.fromString("artistName='artist1' and artistName='artist3'");
        assertEquals(0, execute(Artist.class, parsed).size());
    }

    @Test
    public void testNot() throws Exception {

        createTwentyFiveArtists();

        Expression parsed1 = Expression.fromString("not artistName='artist3'");
        assertEquals(25 - 1, execute(Artist.class, parsed1).size());

        Expression parsed2 = Expression.fromString("not artistName='artist3'");
        assertEquals(25 - 1, execute(Artist.class, parsed2).size());
    }

    @Test
    public void testEqual() throws Exception {

        createTwentyFiveArtists();

        Expression parsed1 = Expression.fromString("artistName='artist3'");
        assertEquals(1, execute(Artist.class, parsed1).size());

        // test with prefetch... this type of expressions should work with prefetches
        assertEquals(1, execute(Artist.class, parsed1, "paintingArray").size());

        Expression parsed2 = Expression.fromString("artistName=='artist3'");
        assertEquals(1, execute(Artist.class, parsed2).size());
    }

    @Test
    public void testNotEqual() throws Exception {

        createTwentyFiveArtists();

        Expression parsed1 = Expression.fromString("artistName!='artist3'");
        assertEquals(25 - 1, execute(Artist.class, parsed1).size());

        Expression parsed2 = Expression.fromString("artistName<>'artist3'");
        assertEquals(25 - 1, execute(Artist.class, parsed2).size());
    }

    @Test
    public void testLessThan() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = Expression.fromString("estimatedPrice < 2000.0");
        assertEquals(1, execute(Painting.class, parsed1).size());
    }

    @Test
    public void testLessThanEqualTo() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = Expression.fromString("estimatedPrice <= 2000.0");
        assertEquals(2, execute(Painting.class, parsed1).size());
    }

    @Test
    public void testGreaterThan() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = Expression.fromString("estimatedPrice > 2000");
        assertEquals(25 - 2, execute(Painting.class, parsed1).size());
    }

    @Test
    public void testGreaterThanEqualTo() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = Expression.fromString("estimatedPrice >= 2000");
        assertEquals(25 - 1, execute(Painting.class, parsed1).size());
    }

    @Test
    public void testLike() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = Expression.fromString("artistName like 'artist%2'");
        assertEquals(3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void testLikeIgnoreCase() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = Expression
                .fromString("artistName likeIgnoreCase 'artist%2'");
        assertEquals(3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void testNotLike() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = Expression.fromString("artistName not like 'artist%2'");
        assertEquals(25 - 3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void testNotLikeIgnoreCase() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = Expression
                .fromString("artistName not likeIgnoreCase 'artist%2'");
        assertEquals(25 - 3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void testIn() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = Expression
                .fromString("artistName in ('artist1', 'artist3', 'artist19')");
        assertEquals(3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void testNotIn() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = Expression
                .fromString("artistName not in ('artist1', 'artist3', 'artist19')");
        assertEquals(25 - 3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void testBetween() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = Expression
                .fromString("estimatedPrice between 2000.0 and 4000.0");
        assertEquals(3, execute(Painting.class, parsed1).size());
    }

    @Test
    public void testNotBetween() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = Expression
                .fromString("estimatedPrice not between 2000.0 and 4000.0");
        assertEquals(25 - 3, execute(Painting.class, parsed1).size());
    }

    @Test
    public void testParameter() throws Exception {
        createTwentyFiveArtists();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("artistName", "artist5");
        Expression parsed1 = Expression.fromString("artistName=$artistName");
        parsed1 = parsed1.expWithParameters(parameters);
        assertEquals(1, execute(Artist.class, parsed1).size());
    }

    @Test
    public void testDbExpression() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = Expression.fromString("db:ARTIST_NAME='artist3'");
        assertEquals(1, execute(Artist.class, parsed1).size());
    }

    @Test
    public void testFloatExpression() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = Expression.fromString("estimatedPrice < 2001.01");
        assertEquals(2, execute(Painting.class, parsed1).size());
    }

    @Test
    public void testNullExpression() throws Exception {
        createTwentyFiveArtists();

        Expression parsed1 = Expression.fromString("artistName!=null");
        assertEquals(25, execute(Artist.class, parsed1).size());

        Expression parsed2 = Expression.fromString("artistName = null");
        assertEquals(0, execute(Artist.class, parsed2).size());
    }

    @Test
    public void testTrueExpression() throws Exception {

        createTwentyFiveArtistsAndPaintings();

        Expression parsed1 = Expression.fromString("true");
        assertEquals(25, execute(Painting.class, parsed1).size());

        Expression parsed2 = Expression.fromString("(estimatedPrice < 2001.01) and true");
        assertEquals(2, execute(Painting.class, parsed2).size());

        Expression parsed3 = Expression.fromString("(estimatedPrice < 2001.01) or true");
        assertEquals(25, execute(Painting.class, parsed3).size());
    }

    @Test
    public void testFalseExpression() throws Exception {
        createTwentyFiveArtistsAndPaintings();

        Expression parsed1 = Expression.fromString("false");
        assertEquals(0, execute(Painting.class, parsed1).size());

        Expression parsed2 = Expression
                .fromString("(estimatedPrice < 2001.01) and false");
        assertEquals(0, execute(Painting.class, parsed2).size());

        Expression parsed3 = Expression.fromString("(estimatedPrice < 2001.01) or false");

        assertEquals(2, execute(Painting.class, parsed3).size());
    }
}
