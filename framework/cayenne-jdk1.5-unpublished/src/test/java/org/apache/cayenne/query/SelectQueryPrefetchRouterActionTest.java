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

package org.apache.cayenne.query;

import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.CayenneCase;

public class SelectQueryPrefetchRouterActionTest extends CayenneCase {

    public void testPaintings1() {
        ObjEntity paintingEntity = getDomain().getEntityResolver().lookupObjEntity(
                Painting.class);
        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.matchExp(
                "artistName",
                "abc"));
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);

        SelectQueryPrefetchRouterAction action = new SelectQueryPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(q, router, getDomain().getEntityResolver());
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);

        assertSame(paintingEntity, prefetch.getRoot());
        assertEquals(Expression.fromString("db:toArtist.ARTIST_NAME = 'abc'"), prefetch
                .getQualifier());
    }

    public void testPrefetchPaintings2() {
        ObjEntity paintingEntity = getDomain().getEntityResolver().lookupObjEntity(
                Painting.class);

        SelectQuery q = new SelectQuery(Artist.class, Expression
                .fromString("artistName = 'abc' or artistName = 'xyz'"));
        q.addPrefetch(Artist.PAINTING_ARRAY_PROPERTY);

        SelectQueryPrefetchRouterAction action = new SelectQueryPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(q, router, getDomain().getEntityResolver());
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);
        assertSame(paintingEntity, prefetch.getRoot());
        assertEquals(
                Expression
                        .fromString("db:toArtist.ARTIST_NAME = 'abc' or db:toArtist.ARTIST_NAME = 'xyz'"),
                prefetch.getQualifier());
    }

    public void testGalleries() {
        ObjEntity galleryEntity = getDomain().getEntityResolver().lookupObjEntity(
                Gallery.class);
        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.matchExp(
                "artistName",
                "abc"));
        q.addPrefetch("paintingArray.toGallery");

        SelectQueryPrefetchRouterAction action = new SelectQueryPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(q, router, getDomain().getEntityResolver());
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);

        assertSame(galleryEntity, prefetch.getRoot());
        assertEquals(Expression
                .fromString("db:paintingArray.toArtist.ARTIST_NAME = 'abc'"), prefetch
                .getQualifier());
    }
}
