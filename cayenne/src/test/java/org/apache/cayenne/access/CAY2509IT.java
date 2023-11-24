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

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.2
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CAY2509IT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPainting;

    @Before
    public void before() {
        this.tArtist = new TableHelper(dbHelper, "ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH")
                .setColumnTypes(Types.BIGINT, Types.CHAR, Types.DATE);
        this.tPainting = new TableHelper(dbHelper, "PAINTING").setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE")
                .setColumnTypes(Types.INTEGER, Types.BIGINT, Types.VARCHAR);
    }

    @Test
    public void testSelectionProblem() throws SQLException {
        tArtist.insert(1, "A1", null);
        tPainting.insert(1, 1, "P1");
        ObjectContext context1 = runtime.newContext();
        List<Artist> artists1 = ObjectSelect.query(Artist.class)
                .select(context1);
        assertEquals("P1", artists1.get(0).getPaintingArray().get(0).getPaintingTitle());

        tPainting.update()
                .set("PAINTING_TITLE", "P2")
                .where("PAINTING_ID", 1)
                .execute();

        ObjectContext context2 = runtime.newContext();
        List<Artist> artists2 = ObjectSelect.query(Artist.class)
                .select(context2);
        assertEquals("P2", artists2.get(0).getPaintingArray().get(0).getPaintingTitle());
    }

    @Test
    public void testChangesInTwoContexts() throws SQLException {
        tArtist.insert(1, "A1", null);
        ObjectContext context1 = runtime.newContext();
        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("A1"))
                .selectFirst(context1);
        assertEquals("A1", artist.getArtistName());
        artist.setArtistName("A2");
        assertEquals("A2", artist.getArtistName());

        ObjectContext context2 = runtime.newContext();
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .select(context2);
        assertEquals(1, artists.size());
        assertEquals("A1", artists.get(0).getArtistName());
    }

    @Test
    public void testChangesInChildContext() throws SQLException {
        tArtist.insert(1, "A1", null);
        ObjectContext parentContext1 = runtime.newContext();
        ObjectContext context1 = runtime.newContext(parentContext1);
        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("A1"))
                .selectFirst(context1);
        assertEquals("A1", artist.getArtistName());
        artist.setArtistName("A2");
        context1.commitChangesToParent();

        ObjectContext parentContext2 = runtime.newContext();
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .select(parentContext2);
        assertEquals(1, artists.size());
        assertEquals("A1", artists.get(0).getArtistName());
    }

}
