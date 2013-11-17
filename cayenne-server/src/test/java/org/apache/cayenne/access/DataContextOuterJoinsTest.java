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
import java.sql.Types;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextOuterJoinsTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper artistHelper;
    protected TableHelper paintingHelper;
    protected TableHelper artgroupHelper;
    protected TableHelper artistGroupHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {

        artistHelper = new TableHelper(dbHelper, "ARTIST", "ARTIST_ID", "ARTIST_NAME");
        paintingHelper = new TableHelper(
                dbHelper,
                "PAINTING",
                "PAINTING_ID",
                "ARTIST_ID",
                "PAINTING_TITLE").setColumnTypes(
                Types.INTEGER,
                Types.BIGINT,
                Types.VARCHAR);

        artgroupHelper = new TableHelper(dbHelper, "ARTGROUP", "GROUP_ID", "NAME");
        artistGroupHelper = new TableHelper(
                dbHelper,
                "ARTIST_GROUP",
                "GROUP_ID",
                "ARTIST_ID");

        artistGroupHelper.deleteAll();
        dbHelper.update("ARTGROUP").set("PARENT_GROUP_ID", null, Types.INTEGER).execute();
        artgroupHelper.deleteAll();
        paintingHelper.deleteAll();
        artistHelper.deleteAll();
    }

    public void testSelectWithOuterJoinFlattened() throws Exception {

        artistHelper.insert(33001, "AA1");
        artistHelper.insert(33002, "AA2");
        artistHelper.insert(33003, "BB1");
        artistHelper.insert(33004, "BB2");

        artgroupHelper.insert(1, "G1");

        artistGroupHelper.insert(1, 33001);
        artistGroupHelper.insert(1, 33002);
        artistGroupHelper.insert(1, 33004);

        SelectQuery missingToManyQuery = new SelectQuery(Artist.class);
        missingToManyQuery.andQualifier(ExpressionFactory.matchExp(
                Artist.GROUP_ARRAY_PROPERTY + Entity.OUTER_JOIN_INDICATOR,
                null));
        missingToManyQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);

        List<Artist> artists = context.performQuery(missingToManyQuery);
        assertEquals(1, artists.size());
        assertEquals("BB1", artists.get(0).getArtistName());
    }

    public void testSelectWithOuterJoin() throws Exception {

        artistHelper.insert(33001, "AA1");
        artistHelper.insert(33002, "AA2");
        artistHelper.insert(33003, "BB1");
        artistHelper.insert(33004, "BB2");

        paintingHelper.insert(33001, 33001, "P1");
        paintingHelper.insert(33002, 33002, "P2");

        SelectQuery missingToManyQuery = new SelectQuery(Artist.class);
        missingToManyQuery.andQualifier(ExpressionFactory.matchExp(
                Artist.PAINTING_ARRAY_PROPERTY + Entity.OUTER_JOIN_INDICATOR,
                null));
        missingToManyQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);

        List<Artist> artists = context.performQuery(missingToManyQuery);
        assertEquals(2, artists.size());
        assertEquals("BB1", artists.get(0).getArtistName());

        SelectQuery mixedConditionQuery = new SelectQuery(Artist.class);
        mixedConditionQuery.andQualifier(ExpressionFactory.matchExp(
                Artist.PAINTING_ARRAY_PROPERTY + Entity.OUTER_JOIN_INDICATOR,
                null));
        mixedConditionQuery.orQualifier(ExpressionFactory.matchExp(
                Artist.ARTIST_NAME_PROPERTY,
                "AA1"));
        mixedConditionQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);

        artists = context.performQuery(mixedConditionQuery);
        assertEquals(3, artists.size());
        assertEquals("AA1", artists.get(0).getArtistName());
        assertEquals("BB1", artists.get(1).getArtistName());
        assertEquals("BB2", artists.get(2).getArtistName());
    }

    public void testSelectWithOuterJoinFromString() throws Exception {

        artistHelper.insert(33001, "AA1");
        artistHelper.insert(33002, "AA2");
        artistHelper.insert(33003, "BB1");
        artistHelper.insert(33004, "BB2");

        paintingHelper.insert(33001, 33001, "P1");
        paintingHelper.insert(33002, 33002, "P2");

        SelectQuery missingToManyQuery = new SelectQuery(Artist.class);
        missingToManyQuery.andQualifier(Expression.fromString("paintingArray+ = null"));
        missingToManyQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);

        List<Artist> artists = context.performQuery(missingToManyQuery);
        assertEquals(2, artists.size());
        assertEquals("BB1", artists.get(0).getArtistName());

        SelectQuery mixedConditionQuery = new SelectQuery(Artist.class);
        mixedConditionQuery.andQualifier(ExpressionFactory.matchExp(
                Artist.PAINTING_ARRAY_PROPERTY + Entity.OUTER_JOIN_INDICATOR,
                null));
        mixedConditionQuery.orQualifier(ExpressionFactory.matchExp(
                Artist.ARTIST_NAME_PROPERTY,
                "AA1"));
        mixedConditionQuery.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);

        artists = context.performQuery(mixedConditionQuery);
        assertEquals(3, artists.size());
        assertEquals("AA1", artists.get(0).getArtistName());
        assertEquals("BB1", artists.get(1).getArtistName());
        assertEquals("BB2", artists.get(2).getArtistName());
    }

    public void testSelectWithOuterOrdering() throws Exception {

        artistHelper.insert(33001, "AA1");
        artistHelper.insert(33002, "AA2");

        paintingHelper.insert(33001, 33001, "P1");
        paintingHelper.insert(33002, 33002, "P2");
        paintingHelper.insert(33003, null, "P3");

        SelectQuery query = new SelectQuery(Painting.class);

        query.addOrdering("toArtist+.artistName", SortOrder.DESCENDING);

        List<Artist> paintings = context.performQuery(query);
        assertEquals(3, paintings.size());
    }
}
