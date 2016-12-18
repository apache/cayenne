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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SelectQueryPrefetchRouterActionIT extends ServerCase {

    @Inject
    private EntityResolver resolver;

    @Test
    public void testPaintings1() {
        ObjEntity paintingEntity = resolver.getObjEntity(Painting.class);
        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.matchExp("artistName", "abc"));
        q.addPrefetch(Artist.PAINTING_ARRAY.disjoint());

        SelectQueryPrefetchRouterAction action = new SelectQueryPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(q, router, resolver);
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);

        assertSame(paintingEntity, prefetch.getRoot());
        assertEquals(ExpressionFactory.exp("db:toArtist.ARTIST_NAME = 'abc'"), prefetch.getQualifier());
    }

    @Test
    public void testPrefetchPaintings2() {
        ObjEntity paintingEntity = resolver.getObjEntity(Painting.class);

        SelectQuery<Artist> q = new SelectQuery<>(Artist.class, ExpressionFactory.exp("artistName = 'abc' or artistName = 'xyz'"));
        q.addPrefetch(Artist.PAINTING_ARRAY.disjoint());

        SelectQueryPrefetchRouterAction action = new SelectQueryPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(q, router, resolver);
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);
        assertSame(paintingEntity, prefetch.getRoot());
        assertEquals(ExpressionFactory.exp("db:toArtist.ARTIST_NAME = 'abc' or db:toArtist.ARTIST_NAME = 'xyz'"),
                prefetch.getQualifier());
    }

    @Test
    public void testGalleries() {
        ObjEntity galleryEntity = resolver.getObjEntity(Gallery.class);
        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.matchExp("artistName", "abc"));
        q.addPrefetch("paintingArray.toGallery");

        SelectQueryPrefetchRouterAction action = new SelectQueryPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(q, router, resolver);
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);

        assertSame(galleryEntity, prefetch.getRoot());
        assertEquals(ExpressionFactory.exp("db:paintingArray.toArtist.ARTIST_NAME = 'abc'"), prefetch.getQualifier());
    }
}
