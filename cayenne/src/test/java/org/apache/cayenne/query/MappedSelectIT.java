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
package org.apache.cayenne.query;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the deprecated {@link MappedSelect}. Its functionality is tested via {@link MappedQuery} in
 * {@link MappedQueryIT}.
 */
@SuppressWarnings("removal")
public class MappedSelectIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @BeforeEach
    public void before() throws Exception {
        TableHelper tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
        for (int i = 1; i <= 5; i++) {
            tArtist.insert(i, "artist" + i);
        }
    }

    @Test
    public void query_Objects() {
        List<Artist> artists = MappedSelect.query("ParameterizedQueryWithLocalCache", Artist.class)
                .param("name", "artist4")
                .select(env.context());
        assertEquals(1, artists.size());
        assertEquals("artist4", artists.getFirst().getArtistName());
    }

    @Test
    public void query_Untyped() {
        List<DataRow> rows = MappedSelect.query("ArtistRowsQuery", DataRow.class)
                .param("name", "artist3")
                .select(env.context());
        assertEquals(1, rows.size());
        assertEquals(3L, ((Number) rows.getFirst().get("ARTIST_ID")).longValue());
    }

    @Test
    public void fluentChain_KeepsType() {
        // the chained assignment of the pre-6.0 code must still compile
        MappedSelect<Artist> query = MappedSelect.query("ParameterizedQueryWithLocalCache", Artist.class)
                .param("name", "artist2")
                .limit(1)
                .forceNoCache();
        assertEquals("artist2", query.selectOne(env.context()).getArtistName());
    }
}
