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
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CAY2509IT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private TableHelper tArtist;
    private TableHelper tPainting;

    @BeforeEach
    public void before() {
        this.tArtist = env.table("ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH")
                .setColumnTypes(Types.BIGINT, Types.CHAR, Types.DATE);
        this.tPainting = env.table("PAINTING").setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE")
                .setColumnTypes(Types.INTEGER, Types.BIGINT, Types.VARCHAR);
    }

    @Test
    public void selectionProblem() throws SQLException {
        tArtist.insert(1, "A1", null);
        tPainting.insert(1, 1, "P1");
        ObjectContext context1 = env.runtime().newContext();
        List<Artist> artists1 = ObjectSelect.query(Artist.class)
                .select(context1);
        assertEquals("P1", artists1.get(0).getPaintingArray().get(0).getPaintingTitle());

        tPainting.update()
                .set("PAINTING_TITLE", "P2")
                .where("PAINTING_ID", 1)
                .execute();

        ObjectContext context2 = env.runtime().newContext();
        List<Artist> artists2 = ObjectSelect.query(Artist.class)
                .select(context2);
        assertEquals("P2", artists2.get(0).getPaintingArray().get(0).getPaintingTitle());
    }

    @Test
    public void changesInTwoContexts() throws SQLException {
        tArtist.insert(1, "A1", null);
        ObjectContext context1 = env.runtime().newContext();
        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("A1"))
                .selectFirst(context1);
        assertEquals("A1", artist.getArtistName());
        artist.setArtistName("A2");
        assertEquals("A2", artist.getArtistName());

        ObjectContext context2 = env.runtime().newContext();
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .select(context2);
        assertEquals(1, artists.size());
        assertEquals("A1", artists.get(0).getArtistName());
    }

    @Test
    public void changesInChildContext() throws SQLException {
        tArtist.insert(1, "A1", null);
        ObjectContext parentContext1 = env.runtime().newContext();
        ObjectContext context1 = env.runtime().newContext(parentContext1);
        Artist artist = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("A1"))
                .selectFirst(context1);
        assertEquals("A1", artist.getArtistName());
        artist.setArtistName("A2");
        context1.commitChangesToParent();

        ObjectContext parentContext2 = env.runtime().newContext();
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .select(parentContext2);
        assertEquals(1, artists.size());
        assertEquals("A1", artists.get(0).getArtistName());
    }

}
