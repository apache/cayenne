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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextPrefetchQualifierOverlapTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("PAINTING1");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

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

    public void testToManyDisjointOverlappingQualifierWithInnerJoin() throws Exception {
        createTwoArtistsThreePaintingsDataSet();

        SelectQuery query = new SelectQuery(Artist.class);
        query.andQualifier(ExpressionFactory
                .likeExp("paintingArray.paintingTitle", "AB%"));
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);

        List<Artist> result = context.performQuery(query);
        assertEquals(1, result.size());

        Artist a = result.get(0);
        assertEquals(3, a.getPaintingArray().size());
    }

    public void testToManyJointOverlappingQualifierWithInnerJoin() throws Exception {
        createTwoArtistsThreePaintingsDataSet();

        SelectQuery query = new SelectQuery(Artist.class);
        query.andQualifier(ExpressionFactory
                .likeExp("paintingArray.paintingTitle", "AB%"));
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        List<Artist> result = context.performQuery(query);
        assertEquals(1, result.size());

        Artist a = result.get(0);
        assertEquals(3, a.getPaintingArray().size());
    }

    public void testToManyJointOverlappingQualifierWithOuterJoin() throws Exception {
        createTwoArtistsThreePaintingsDataSet();

        SelectQuery query = new SelectQuery(Artist.class);
        query.andQualifier(ExpressionFactory.likeExp(
                "paintingArray+.paintingTitle",
                "AB%"));
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        query.orQualifier(ExpressionFactory.likeExp("artistName", "A%"));
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING);

        List<Artist> result = context.performQuery(query);
        assertEquals(2, result.size());

        Artist a = result.get(0);
        assertEquals(3, a.getPaintingArray().size());

        Artist a1 = result.get(1);
        assertEquals(0, a1.getPaintingArray().size());
    }
}
