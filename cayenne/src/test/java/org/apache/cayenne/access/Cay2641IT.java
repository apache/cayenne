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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.cay_2641.ArtistLazy;
import org.apache.cayenne.testdo.cay_2641.DatamapLazy;
import org.apache.cayenne.testdo.cay_2641.PaintingLazy;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.org.hamcrest.MatcherAssert;
import org.testcontainers.shaded.org.hamcrest.Matchers;

import java.sql.Types;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
@UseCayenneRuntime(CayenneProjects.CAY_2641)
public class Cay2641IT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private DbAdapter adapter;

    @Before
    public void setup() throws Exception {
        TableHelper th = new TableHelper(dbHelper, "ArtistLazy")
                .setColumns("ID", "NAME", "SURNAME")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.VARCHAR);
        th.insert(1, "artist1", "artist2");

        th = new TableHelper(dbHelper, "PaintingLazy")
                .setColumns("ID", "NAME", "ARTIST_ID")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);
        th.insert(1, "painting1", 1);
    }

    @Test
    public void testTranslatorSql() {
        ObjectSelect<ArtistLazy> artists = ObjectSelect.query(ArtistLazy.class);

        DefaultSelectTranslator translator = new DefaultSelectTranslator(artists, adapter, context.getEntityResolver());

        String sql = translator.getSql();
        assertFalse(sql.contains("t0.NAME"));

        String pattern = "SELECT t0.SURNAME( c0)?, t0.ID( c1)? FROM ArtistLazy t0";
        MatcherAssert.assertThat(sql, Matchers.matchesPattern(pattern));

        ColumnSelect<String> select = ObjectSelect.columnQuery(ArtistLazy.class, ArtistLazy.NAME);
        translator = new DefaultSelectTranslator(select, adapter, context.getEntityResolver());
        sql = translator.getSql();

        assertTrue(sql.contains("t0.NAME"));
    }

    @Test
    public void testTypeAttributes() {
        List<ArtistLazy> artists = ObjectSelect.query(ArtistLazy.class).select(context);

        Object object = artists.get(0).readPropertyDirectly("name");
        assertTrue(object instanceof Fault);

        object = artists.get(0).readPropertyDirectly("surname");
        assertEquals("artist2", object);
    }

    @Test
    public void testTypeLazyAttribute() {
        ArtistLazy artist = ObjectSelect.query(ArtistLazy.class).selectFirst(context);

        Object object = artist.readPropertyDirectly("name");
        assertTrue(object instanceof Fault);

        artist.getName();
        object = artist.readPropertyDirectly("name");
        assertEquals("artist1", object);
    }

    @Test
    public void testPrefetchLazyTranslatorSql() {
        ObjectSelect<PaintingLazy> paintingLazyObjectSelect = ObjectSelect.query(PaintingLazy.class).prefetch(PaintingLazy.ARTIST.joint());
        DefaultSelectTranslator translator = new DefaultSelectTranslator(paintingLazyObjectSelect, adapter, context.getEntityResolver());
        String sql = translator.getSql();
        assertFalse(sql.contains("t0.NAME"));

        String pattern = "SELECT t0.ARTIST_ID( c0)?, t0.ID( c1)?, t1.ID( c2)?, t1.SURNAME( c3)?"
                + " FROM PaintingLazy t0 LEFT JOIN ArtistLazy t1 ON t0.ARTIST_ID = t1.ID";
        MatcherAssert.assertThat(sql, Matchers.matchesPattern(pattern));
    }

    @Test
    public void testPrefetchLazyTypeAttributes() {
        List<PaintingLazy> paintingLazyList = ObjectSelect.query(PaintingLazy.class)
                .prefetch(PaintingLazy.ARTIST.joint())
                .select(context);

        Object object = paintingLazyList.get(0).readPropertyDirectly("name");
        assertTrue(object instanceof Fault);

        object = paintingLazyList.get(0).getName();
        assertTrue(object instanceof String);
        assertEquals("painting1", object);

        ArtistLazy artist = (ArtistLazy) paintingLazyList.get(0).readPropertyDirectly("artist");
        object = artist.readPropertyDirectly("name");
        assertTrue(object instanceof Fault);

        object = artist.readPropertyDirectly("surname");
        assertEquals("artist2", object);

        object = artist.getName();
        assertTrue(object instanceof String);
        assertEquals("artist1", object);
    }

    @Test
    public void testsSimpleSelectCustomer() {
        DatamapLazy optimistic = DatamapLazy.getInstance();
        List<ArtistLazy> artistLazies = optimistic.performSimpleSelect(context);

        Object object = artistLazies.get(0).readPropertyDirectly("name");
        assertTrue(object instanceof Fault);

        object = artistLazies.get(0).readPropertyDirectly("surname");
        assertTrue(object instanceof String);
        assertEquals("artist2", object);
    }

    @Test
    public void testsPrefetchSelectCustomer() {
        DatamapLazy optimistic = DatamapLazy.getInstance();
        List<PaintingLazy> paintingLazies = optimistic.performPrefetchSelect(context);

        Object object = paintingLazies.get(0).readPropertyDirectly("name");
        assertTrue(object instanceof Fault);

        ArtistLazy artist = (ArtistLazy) paintingLazies.get(0).readPropertyDirectly("artist");
        object = artist.readPropertyDirectly("name");
        assertTrue(object instanceof Fault);
    }
}
