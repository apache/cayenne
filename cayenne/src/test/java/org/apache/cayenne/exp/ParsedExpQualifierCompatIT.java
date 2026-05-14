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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParsedExpQualifierCompatIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @BeforeEach
    public void setUp() throws Exception {
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");

        tPainting = env.table("PAINTING", "PAINTING_ID",
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

    private <T> List<T> execute(Class<T> root, Expression qualifier, String prefetch) {
        ObjectSelect<T> query = ObjectSelect.query(root, qualifier);
        if (prefetch != null) {
            query.prefetch(prefetch, PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
        }
        return query.select(env.context());
    }

    @Test
    public void or() throws Exception {

        createTwentyFiveArtists();

        Expression parsed = ExpressionFactory.exp("artistName='artist1' or artistName='artist3'");
        assertEquals(2, execute(Artist.class, parsed).size());

        parsed = ExpressionFactory.exp("artistName='artist1' or artistName='artist3' or artistName='artist5'");
        assertEquals(3, execute(Artist.class, parsed).size());
    }

    @Test
    public void and() throws Exception {
        createTwentyFiveArtists();

        Expression parsed = ExpressionFactory.exp("artistName='artist1' and artistName='artist1'");
        assertEquals(1, execute(Artist.class, parsed).size());

        parsed = ExpressionFactory.exp("artistName='artist1' and artistName='artist3'");
        assertEquals(0, execute(Artist.class, parsed).size());
    }

    @Test
    public void not() throws Exception {

        createTwentyFiveArtists();

        Expression parsed1 = ExpressionFactory.exp("not artistName='artist3'");
        assertEquals(25 - 1, execute(Artist.class, parsed1).size());

        Expression parsed2 = ExpressionFactory.exp("not artistName='artist3'");
        assertEquals(25 - 1, execute(Artist.class, parsed2).size());
    }

    @Test
    public void equal() throws Exception {

        createTwentyFiveArtists();

        Expression parsed1 = ExpressionFactory.exp("artistName='artist3'");
        assertEquals(1, execute(Artist.class, parsed1).size());

        // test with prefetch... this type of expressions should work with prefetches
        assertEquals(1, execute(Artist.class, parsed1, "paintingArray").size());

        Expression parsed2 = ExpressionFactory.exp("artistName=='artist3'");
        assertEquals(1, execute(Artist.class, parsed2).size());
    }

    @Test
    public void notEqual() throws Exception {

        createTwentyFiveArtists();

        Expression parsed1 = ExpressionFactory.exp("artistName!='artist3'");
        assertEquals(25 - 1, execute(Artist.class, parsed1).size());

        Expression parsed2 = ExpressionFactory.exp("artistName<>'artist3'");
        assertEquals(25 - 1, execute(Artist.class, parsed2).size());
    }

    @Test
    public void lessThan() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = ExpressionFactory.exp("estimatedPrice < 2000.0");
        assertEquals(1, execute(Painting.class, parsed1).size());
    }

    @Test
    public void lessThanEqualTo() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = ExpressionFactory.exp("estimatedPrice <= 2000.0");
        assertEquals(2, execute(Painting.class, parsed1).size());
    }

    @Test
    public void greaterThan() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = ExpressionFactory.exp("estimatedPrice > 2000");
        assertEquals(25 - 2, execute(Painting.class, parsed1).size());
    }

    @Test
    public void greaterThanEqualTo() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = ExpressionFactory.exp("estimatedPrice >= 2000");
        assertEquals(25 - 1, execute(Painting.class, parsed1).size());
    }

    @Test
    public void like() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = ExpressionFactory.exp("artistName like 'artist%2'");
        assertEquals(3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void likeIgnoreCase() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = ExpressionFactory.exp("artistName likeIgnoreCase 'artist%2'");
        assertEquals(3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void notLike() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = ExpressionFactory.exp("artistName not like 'artist%2'");
        assertEquals(25 - 3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void notLikeIgnoreCase() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = ExpressionFactory.exp("artistName not likeIgnoreCase 'artist%2'");
        assertEquals(25 - 3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void in() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = ExpressionFactory.exp("artistName in ('artist1', 'artist3', 'artist19')");
        assertEquals(3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void notIn() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = ExpressionFactory.exp("artistName not in ('artist1', 'artist3', 'artist19')");
        assertEquals(25 - 3, execute(Artist.class, parsed1).size());
    }

    @Test
    public void between() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = ExpressionFactory.exp("estimatedPrice between 2000.0 and 4000.0");
        assertEquals(3, execute(Painting.class, parsed1).size());
    }

    @Test
    public void notBetween() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = ExpressionFactory.exp("estimatedPrice not between 2000.0 and 4000.0");
        assertEquals(25 - 3, execute(Painting.class, parsed1).size());
    }

    @Test
    public void parameter() throws Exception {
        createTwentyFiveArtists();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("artistName", "artist5");
        Expression parsed1 = ExpressionFactory.exp("artistName=$artistName");
        parsed1 = parsed1.params(parameters);
        assertEquals(1, execute(Artist.class, parsed1).size());
    }

    @Test
    public void dbExpression() throws Exception {
        createTwentyFiveArtists();
        Expression parsed1 = ExpressionFactory.exp("db:ARTIST_NAME='artist3'");
        assertEquals(1, execute(Artist.class, parsed1).size());
    }

    @Test
    public void floatExpression() throws Exception {
        createTwentyFiveArtistsAndPaintings();
        Expression parsed1 = ExpressionFactory.exp("estimatedPrice < 2001.01");
        assertEquals(2, execute(Painting.class, parsed1).size());
    }

    @Test
    public void nullExpression() throws Exception {
        createTwentyFiveArtists();

        Expression parsed1 = ExpressionFactory.exp("artistName!=null");
        assertEquals(25, execute(Artist.class, parsed1).size());

        Expression parsed2 = ExpressionFactory.exp("artistName = null");
        assertEquals(0, execute(Artist.class, parsed2).size());
    }

    @Test
    public void trueExpression() throws Exception {

        createTwentyFiveArtistsAndPaintings();

        Expression parsed1 = ExpressionFactory.exp("true");
        assertEquals(25, execute(Painting.class, parsed1).size());

        Expression parsed2 = ExpressionFactory.exp("(estimatedPrice < 2001.01) and true");
        assertEquals(2, execute(Painting.class, parsed2).size());

        Expression parsed3 = ExpressionFactory.exp("(estimatedPrice < 2001.01) or true");
        assertEquals(25, execute(Painting.class, parsed3).size());
    }

    @Test
    public void falseExpression() throws Exception {
        createTwentyFiveArtistsAndPaintings();

        Expression parsed1 = ExpressionFactory.exp("false");
        assertEquals(0, execute(Painting.class, parsed1).size());

        Expression parsed2 = ExpressionFactory.exp("(estimatedPrice < 2001.01) and false");
        assertEquals(0, execute(Painting.class, parsed2).size());

        Expression parsed3 = ExpressionFactory.exp("(estimatedPrice < 2001.01) or false");

        assertEquals(2, execute(Painting.class, parsed3).size());
    }
}
