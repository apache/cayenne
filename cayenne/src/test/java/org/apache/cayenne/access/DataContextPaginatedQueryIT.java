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

import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextPaginatedQueryIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
    }

    protected void createArtistsDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tArtist.insert(33003, "artist3");
        tArtist.insert(33004, "artist4");
        tArtist.insert(33005, "artist5");
        tArtist.insert(33006, "artist6");
        tArtist.insert(33007, "artist7");
        tArtist.insert(33008, "artist8");
        tArtist.insert(33009, "artist9");
        tArtist.insert(33010, "artist10");
    }

    @Test
    public void testLocalCache() throws Exception {

        createArtistsDataSet();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .orderBy(Artist.ARTIST_NAME.asc())
                .cacheStrategy(QueryCacheStrategy.LOCAL_CACHE)
                .pageSize(5);

        List<Artist> results1 = query.select(context);
        assertNotNull(results1);

        List<Artist> results2 = query.select(context);
        assertNotNull(results2);
        assertSame(results1, results2);

        results1.get(1);
        List<Artist> results3 = query.select(context);
        assertNotNull(results3);
        assertSame(results1, results3);

        results1.get(7);
        List<Artist> results4 = query.select(context);
        assertNotNull(results4);
        assertSame(results1, results4);
    }
}
