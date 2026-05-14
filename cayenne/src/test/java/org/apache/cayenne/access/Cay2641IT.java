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
package org.apache.cayenne.access;

import org.apache.cayenne.Fault;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.cay_2641.ArtistLazy;
import org.apache.cayenne.testdo.cay_2641.DatamapLazy;
import org.apache.cayenne.testdo.cay_2641.PaintingLazy;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.shaded.org.hamcrest.MatcherAssert;
import org.testcontainers.shaded.org.hamcrest.Matchers;

import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Cay2641IT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.CAY_2641);

    private DbAdapter adapter;

    @BeforeEach
    public void setup() throws Exception {
        adapter = env.dataNode().getAdapter();

        TableHelper th = env.table("ArtistLazy")
                .setColumns("ID", "NAME", "SURNAME")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.VARCHAR);
        th.insert(1, "artist1", "artist2");

        th = env.table("PaintingLazy")
                .setColumns("ID", "NAME", "ARTIST_ID")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);
        th.insert(1, "painting1", 1);
    }

    @Test
    public void translatorSql() {
        ObjectSelect<ArtistLazy> artists = ObjectSelect.query(ArtistLazy.class);

        DefaultSelectTranslator translator = new DefaultSelectTranslator(artists, adapter, env.context().getEntityResolver());

        String sql = translator.getSql();
        assertFalse(sql.contains("t0.NAME"));

        String pattern = "SELECT t0.SURNAME( c0)?, t0.ID( c1)? FROM ArtistLazy t0";
        MatcherAssert.assertThat(sql, Matchers.matchesPattern(pattern));

        ColumnSelect<String> select = ObjectSelect.columnQuery(ArtistLazy.class, ArtistLazy.NAME);
        translator = new DefaultSelectTranslator(select, adapter, env.context().getEntityResolver());
        sql = translator.getSql();

        assertTrue(sql.contains("t0.NAME"));
    }

    @Test
    public void typeAttributes() {
        List<ArtistLazy> artists = ObjectSelect.query(ArtistLazy.class).select(env.context());

        Object object = artists.getFirst().readPropertyDirectly("name");
        assertInstanceOf(Fault.class, object);

        object = artists.getFirst().readPropertyDirectly("surname");
        assertEquals("artist2", object);
    }

    @Test
    public void typeLazyAttribute() {
        ArtistLazy artist = ObjectSelect.query(ArtistLazy.class).selectFirst(env.context());

        Object object = artist.readPropertyDirectly("name");
        assertInstanceOf(Fault.class, object);

        artist.getName();
        object = artist.readPropertyDirectly("name");
        assertEquals("artist1", object);
    }

    @Test
    public void prefetchLazyTranslatorSql() {
        ObjectSelect<PaintingLazy> paintingLazyObjectSelect = ObjectSelect.query(PaintingLazy.class).prefetch(PaintingLazy.ARTIST.joint());
        DefaultSelectTranslator translator = new DefaultSelectTranslator(paintingLazyObjectSelect, adapter, env.context().getEntityResolver());
        String sql = translator.getSql();
        assertFalse(sql.contains("t0.NAME"));

        String pattern = "SELECT t0.ARTIST_ID( c0)?, t0.ID( c1)?, t1.ID( c2)?, t1.SURNAME( c3)?"
                + " FROM PaintingLazy t0 LEFT JOIN ArtistLazy t1 ON t0.ARTIST_ID = t1.ID";
        MatcherAssert.assertThat(sql, Matchers.matchesPattern(pattern));
    }

    @Test
    public void prefetchLazyTypeAttributes() {
        List<PaintingLazy> paintingLazyList = ObjectSelect.query(PaintingLazy.class)
                .prefetch(PaintingLazy.ARTIST.joint())
                .select(env.context());

        Object object = paintingLazyList.getFirst().readPropertyDirectly("name");
        assertInstanceOf(Fault.class, object);

        object = paintingLazyList.getFirst().getName();
        assertInstanceOf(String.class, object);
        assertEquals("painting1", object);

        ArtistLazy artist = (ArtistLazy) paintingLazyList.getFirst().readPropertyDirectly("artist");
        object = artist.readPropertyDirectly("name");
        assertInstanceOf(Fault.class, object);

        object = artist.readPropertyDirectly("surname");
        assertEquals("artist2", object);

        object = artist.getName();
        assertInstanceOf(String.class, object);
        assertEquals("artist1", object);
    }

    @Test
    public void simpleSelectCustomer() {
        DatamapLazy optimistic = DatamapLazy.getInstance();
        List<ArtistLazy> artistLazies = optimistic.performSimpleSelect(env.context());

        Object object = artistLazies.getFirst().readPropertyDirectly("name");
        assertInstanceOf(Fault.class, object);

        object = artistLazies.getFirst().readPropertyDirectly("surname");
        assertInstanceOf(String.class, object);
        assertEquals("artist2", object);
    }

    @Test
    public void prefetchSelectCustomer() {
        DatamapLazy optimistic = DatamapLazy.getInstance();
        List<PaintingLazy> paintingLazies = optimistic.performPrefetchSelect(env.context());

        Object object = paintingLazies.getFirst().readPropertyDirectly("name");
        assertInstanceOf(Fault.class, object);

        ArtistLazy artist = (ArtistLazy) paintingLazies.getFirst().readPropertyDirectly("artist");
        object = artist.readPropertyDirectly("name");
        assertInstanceOf(Fault.class, object);
    }
}
