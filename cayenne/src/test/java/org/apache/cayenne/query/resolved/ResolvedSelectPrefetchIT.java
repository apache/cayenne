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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolution of disjoint prefetches into child selects.
 */
public class ResolvedSelectPrefetchIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void noPrefetches() {
        ResolvedSelect select = ObjectSelect.query(Artist.class).resolve(env.mapping());
        assertTrue(select.prefetches().isEmpty());
        assertNull(select.prefetchPath());
    }

    @Test
    public void paintings() {
        CompiledEntity paintingEntity = env.mapping().entity(Painting.class);
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("abc"))
                .prefetch(Artist.PAINTINGS.disjoint());

        ResolvedSelect select = query.resolve(env.mapping());
        assertEquals(1, select.prefetches().size());

        ResolvedSelect prefetch = select.prefetches().getFirst();
        assertEquals(paintingEntity, prefetch.entity());
        assertEquals(Artist.PAINTINGS.getPath(), prefetch.prefetchPath());
        assertEquals(ExpressionFactory.exp("db:artist.ARTIST_NAME = 'abc'"), prefetch.where());
        assertTrue(prefetch.prefetches().isEmpty());
        // a to-many prefetch selects the FK to attach the rows to the parents
        assertEquals(1, prefetch.resultPaths().size());
        assertEquals(query, prefetch.source());
    }

    @Test
    public void paintingsOrQualifier() {
        CompiledEntity paintingEntity = env.mapping().entity(Painting.class);
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("abc"))
                .or(Artist.ARTIST_NAME.eq("xyz"))
                .prefetch(Artist.PAINTINGS.disjoint());

        ResolvedSelect select = query.resolve(env.mapping());
        assertEquals(1, select.prefetches().size());

        ResolvedSelect prefetch = select.prefetches().getFirst();
        assertEquals(paintingEntity, prefetch.entity());
        assertEquals(ExpressionFactory.exp("db:artist.ARTIST_NAME = 'abc' or db:artist.ARTIST_NAME = 'xyz'"),
                prefetch.where());
    }

    @Test
    public void galleries() {
        CompiledEntity galleryEntity = env.mapping().entity(Gallery.class);
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("abc"))
                .prefetch(Artist.PAINTINGS.dot(Painting.GALLERY).disjoint());

        ResolvedSelect select = query.resolve(env.mapping());
        assertEquals(1, select.prefetches().size());

        ResolvedSelect prefetch = select.prefetches().getFirst();
        assertEquals(galleryEntity, prefetch.entity());
        assertEquals(Artist.PAINTINGS.dot(Painting.GALLERY).getPath(), prefetch.prefetchPath());
        assertEquals(ExpressionFactory.exp("db:paintings.artist.ARTIST_NAME = 'abc'"), prefetch.where());
    }

    @Test
    public void jointPrefetchHasNoChildren() {
        ResolvedSelect select = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTINGS.joint())
                .resolve(env.mapping());
        assertTrue(select.prefetches().isEmpty());
    }

    @Test
    public void paginatedQueryHasNoChildren() {
        ResolvedSelect select = ObjectSelect.query(Artist.class)
                .prefetch(Artist.PAINTINGS.disjoint())
                .pageSize(10)
                .resolve(env.mapping());
        assertTrue(select.prefetches().isEmpty());
    }

    @Test
    public void dataRowQueryHasNoChildren() {
        ResolvedSelect select = ObjectSelect.dataRowQuery(Artist.class)
                .prefetch(Artist.PAINTINGS.disjoint())
                .resolve(env.mapping());
        assertTrue(select.prefetches().isEmpty());
    }
}
