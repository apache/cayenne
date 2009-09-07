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
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextPrefetchQualifierOverlapTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testToManyDisjointOverlappingQualifierWithInnerJoin() {
        QueryChain data = new QueryChain();
        data.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'A1')"));
        data.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (2, 'A2')"));
        data
                .addQuery(new SQLTemplate(
                        Artist.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (1, 1, 'ABC')"));
        data
                .addQuery(new SQLTemplate(
                        Artist.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (2, 1, 'ABD')"));
        data
                .addQuery(new SQLTemplate(
                        Artist.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (3, 1, 'ACC')"));

        createDataContext().performGenericQuery(data);

        SelectQuery query = new SelectQuery(Artist.class);
        query.andQualifier(ExpressionFactory
                .likeExp("paintingArray.paintingTitle", "AB%"));
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);

        List<Artist> result = createDataContext().performQuery(query);
        assertEquals(1, result.size());

        Artist a = result.get(0);
        assertEquals(3, a.getPaintingArray().size());
    }

    public void testToManyJointOverlappingQualifierWithInnerJoin() {
        QueryChain data = new QueryChain();
        data.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'A1')"));
        data.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (2, 'A2')"));
        data
                .addQuery(new SQLTemplate(
                        Artist.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (1, 1, 'ABC')"));
        data
                .addQuery(new SQLTemplate(
                        Artist.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (2, 1, 'ABD')"));
        data
                .addQuery(new SQLTemplate(
                        Artist.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (3, 1, 'ACC')"));

        createDataContext().performGenericQuery(data);

        SelectQuery query = new SelectQuery(Artist.class);
        query.andQualifier(ExpressionFactory
                .likeExp("paintingArray.paintingTitle", "AB%"));
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        List<Artist> result = createDataContext().performQuery(query);
        assertEquals(1, result.size());

        Artist a = result.get(0);
        assertEquals(3, a.getPaintingArray().size());
    }

    public void testToManyJointOverlappingQualifierWithOuterJoin() {
        QueryChain data = new QueryChain();
        data.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'A1')"));
        data.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (2, 'A2')"));
        data
                .addQuery(new SQLTemplate(
                        Artist.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (1, 1, 'ABC')"));
        data
                .addQuery(new SQLTemplate(
                        Artist.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (2, 1, 'ABD')"));
        data
                .addQuery(new SQLTemplate(
                        Artist.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (3, 1, 'ACC')"));

        createDataContext().performGenericQuery(data);

        SelectQuery query = new SelectQuery(Artist.class);
        query.andQualifier(ExpressionFactory.likeExp(
                "paintingArray+.paintingTitle",
                "AB%"));
        query.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        query.orQualifier(ExpressionFactory.likeExp("artistName", "A%"));
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);

        List<Artist> result = createDataContext().performQuery(query);
        assertEquals(2, result.size());

        Artist a = result.get(0);
        assertEquals(3, a.getPaintingArray().size());

        Artist a1 = result.get(1);
        assertEquals(0, a1.getPaintingArray().size());
    }
}
