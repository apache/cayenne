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
package org.apache.cayenne.docs;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.docs.persistent.CompoundPk;
import org.apache.cayenne.docs.persistent.Painting;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLExec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SelectByIdTest extends BaseTest {

    private void createArtistsWithIds() {
        for (int i = 1; i <= 4; i++) {
            SQLExec.query("INSERT INTO ARTIST (ID, NAME) VALUES (#bind($id), #bind($name))")
                    .params("id", i)
                    .params("name", "artist" + i)
                    .update(context);
        }

        SQLExec.query("INSERT INTO PAINTING (ID, TITLE, ARTIST_ID) VALUES (1, 'P1', 1)")
                .update(context);
    }

    @Test
    public void byId() {
        createArtistsWithIds();

        // tag::byId[]
        Artist artistWithId1 = ObjectSelect.query(Artist.class)
                .byId(1)
                .selectOne(context);

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .byIds(1, 2, 3)
                .select(context);
        // end::byId[]

        assertEquals("artist1", artistWithId1.getName());
        assertEquals(3, artists.size());
    }

    @Test
    public void byIdForms() {
        createArtistsWithIds();
        SQLExec.query("INSERT INTO COMPOUND_PK (KEY1, KEY2, NAME) VALUES ('x', 'y', 'xy')").update(context);
        ObjectId objectId = ObjectId.of("Artist", Artist.ID_PK_COLUMN, 2);

        // tag::byIdForms[]
        CompoundPk object = ObjectSelect.query(CompoundPk.class)
                .byId(Map.of("KEY1", "x", "KEY2", "y"))
                .selectOne(context);

        Artist artist = ObjectSelect.query(Artist.class)
                .byId(objectId)
                .selectOne(context);
        // end::byIdForms[]

        assertEquals("xy", object.getName());
        assertNotNull(artist);
        assertEquals("artist2", artist.getName());
    }

    @Test
    public void idExpressions() {
        createArtistsWithIds();

        // tag::idExpressions[]
        List<Artist> artists = ObjectSelect.query(Artist.class)
                .where(Artist.NAME.eq("artist4"))
                .or(Artist.SELF.idsIn(1, 2, 3))
                .select(context);

        List<Painting> paintings = ObjectSelect.query(Painting.class)
                .where(Painting.ARTIST.eqId(1))
                .select(context);
        // end::idExpressions[]

        assertEquals(4, artists.size());
        assertEquals(1, paintings.size());
    }
}
