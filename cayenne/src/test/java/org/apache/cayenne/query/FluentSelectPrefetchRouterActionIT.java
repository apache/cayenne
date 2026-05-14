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

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class FluentSelectPrefetchRouterActionIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void paintings1() {
        ObjEntity paintingEntity = env.entityResolver().getObjEntity(Painting.class);

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("abc"))
                .prefetch(Artist.PAINTING_ARRAY.disjoint());

        FluentSelectPrefetchRouterAction action = new FluentSelectPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(query, router, env.entityResolver());
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);

        assertEquals(paintingEntity.getName(), prefetch.entityName);
        assertEquals(ExpressionFactory.exp("db:toArtist.ARTIST_NAME = 'abc'"), prefetch.getWhere());
    }

    @Test
    public void prefetchPaintings2() {
        ObjEntity paintingEntity = env.entityResolver().getObjEntity(Painting.class);

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("abc"))
                .or(Artist.ARTIST_NAME.eq("xyz"))
                .prefetch(Artist.PAINTING_ARRAY.disjoint());

        FluentSelectPrefetchRouterAction action = new FluentSelectPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(query, router, env.entityResolver());
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);
        assertEquals(paintingEntity.getName(), prefetch.entityName);
        assertEquals(ExpressionFactory.exp("db:toArtist.ARTIST_NAME = 'abc' or db:toArtist.ARTIST_NAME = 'xyz'"),
                prefetch.getWhere());
    }

    @Test
    public void galleries() {
        ObjEntity galleryEntity = env.entityResolver().getObjEntity(Gallery.class);

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("abc"))
                .prefetch(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).disjoint());
        FluentSelectPrefetchRouterAction action = new FluentSelectPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(query, router, env.entityResolver());
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);

        assertEquals(galleryEntity.getName(), prefetch.entityName);
        assertEquals(ExpressionFactory.exp("db:paintingArray.toArtist.ARTIST_NAME = 'abc'"), prefetch.getWhere());
    }
}
