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

package org.apache.cayenne.jcache;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.cache.NestedQueryCache;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.jcache.unit.JCacheCase;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CayenneJCacheModuleIT extends JCacheCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    ObjectContext context;

    @Inject
    ServerRuntime runtime;

    private TableHelper tArtist;

    @Before
    public void setUpTableHelper() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
        tArtist.deleteAll();
    }


    @Test
    public void testCachedQueries() throws Exception {
        // make sure that we have JCacheQueryCache
        assertEquals(JCacheQueryCache.class, ((NestedQueryCache)runtime.getDataDomain().getQueryCache()).getDelegate().getClass());

        ObjectSelect<Artist> g1 = ObjectSelect.query(Artist.class).localCache("g1");
        ObjectSelect<Artist> g2 = ObjectSelect.query(Artist.class).localCache("g2");

        tArtist.insert(1, "artist1");
        tArtist.insert(2, "artist2");
        assertEquals(2, g1.select(context).size());

        // we are still cached, must not see the new changes
        tArtist.insert(3, "artist3");
        tArtist.insert(4, "artist4");
        assertEquals(2, g1.select(context).size());

        // different cache group - must see the changes
        assertEquals(4, g2.select(context).size());

        // refresh the cache, so that "g1" could see the changes
        runtime.getDataDomain().getQueryCache().removeGroup("g1");
        assertEquals(4, g1.select(context).size());
        assertEquals(4, g2.select(context).size());
    }
}
