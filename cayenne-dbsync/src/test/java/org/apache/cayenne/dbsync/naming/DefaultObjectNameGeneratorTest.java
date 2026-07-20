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
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultObjectNameGeneratorTest {

    private final DefaultObjectNameGenerator generator = new DefaultObjectNameGenerator();

    private DbRelationship makeRelationship(
            String srcEntity,
            String srcKey,
            String targetEntity,
            String targetKey,
            boolean toMany) {

        DbRelationship relationship = new DbRelationship();
        relationship.addJoin(new DbJoin(relationship, srcKey, targetKey));
        relationship.setToMany(toMany);
        relationship.setSourceEntity(new DbEntity(srcEntity));
        relationship.setTargetEntityName(targetEntity);

        return relationship;
    }

    private String dbRelationshipName(DbRelationship relationship) {
        return generator.dbRelationshipName(relationship.getJoins(), relationship.isToMany());
    }

    @Test
    public void objRelationshipName_MirrorsDbRelationshipName() {

        // the DbRelationship name is the source of truth for the ObjRelationship name
        DbRelationship r1 = makeRelationship("painting", "artist_id", "artist", "artist_id", false);
        r1.setName("creator");
        assertEquals("creator", generator.objRelationshipName(r1));

        // a to-many DbRelationship name is already plural and is mirrored as-is
        DbRelationship r2 = makeRelationship("artist", "artist_id", "painting", "artist_id", true);
        r2.setName("works");
        assertEquals("works", generator.objRelationshipName(r2));

        // underscored db names are converted to Java style
        DbRelationship r3 = makeRelationship("TEAM", "TEAM_ID", "GAME", "HOME_TEAM_ID", true);
        r3.setName("HOME_GAMES");
        assertEquals("homeGames", generator.objRelationshipName(r3));

        // even the NameBuilder placeholder is mirrored, not regenerated
        DbRelationship r4 = makeRelationship("painting", "artist_id", "artist", "artist_id", false);
        r4.setName("untitledRel2");
        assertEquals("untitledRel2", generator.objRelationshipName(r4));
    }

    @Test
    public void objRelationshipName_FlattenedChain() {

        // flattened many-to-many: a to-many leg to the join table, then a to-one leg to the target;
        // the last leg's singular name is mirrored and pluralized
        DbRelationship r1 = makeRelationship("movie", "movie_id", "person_movie", "movie_id", true);
        r1.setName("personMovies");

        DbRelationship r2 = makeRelationship("person_movie", "actor_id", "person", "person_id", false);
        r2.setName("actor");
        assertEquals("actors", generator.objRelationshipName(r1, r2));

        DbRelationship r3 = makeRelationship("person_movie", "director_id", "person", "person_id", false);
        r3.setName("director");
        assertEquals("directors", generator.objRelationshipName(r1, r3));
    }

    @Test
    public void dbRelationshipName_LowerCase_Underscores() {

        DbRelationship r1 = makeRelationship("painting", "artist_id", "artist", "artist_id", false);
        assertEquals("artist", dbRelationshipName(r1));

        DbRelationship r2 = makeRelationship("artist", "artist_id", "painting", "artist_id", true);
        assertEquals("paintings", dbRelationshipName(r2));

        DbRelationship r3 = makeRelationship("person", "mother_id", "person", "person_id", false);
        assertEquals("mother", dbRelationshipName(r3));

        DbRelationship r4 = makeRelationship("person", "person_id", "person", "mother_id", true);
        assertEquals("people", dbRelationshipName(r4));

        DbRelationship r5 = makeRelationship("person", "shipping_address_id", "address", "id", false);
        assertEquals("shippingAddress", dbRelationshipName(r5));

        DbRelationship r6 = makeRelationship("person", "id", "address", "person_id", true);
        assertEquals("addresses", dbRelationshipName(r6));
    }

    @Test
    public void dbRelationshipName_UpperCase_Underscores() {

        DbRelationship r1 = makeRelationship("PAINTING", "ARTIST_ID", "ARTIST", "ARTIST_ID", false);
        assertEquals("artist", dbRelationshipName(r1));

        DbRelationship r2 = makeRelationship("ARTIST", "ARTIST_ID", "PAINTING", "ARTIST_ID", true);
        assertEquals("paintings", dbRelationshipName(r2));

        DbRelationship r3 = makeRelationship("PERSON", "MOTHER_ID", "PERSON", "PERSON_ID", false);
        assertEquals("mother", dbRelationshipName(r3));

        DbRelationship r4 = makeRelationship("PERSON", "PERSON_ID", "PERSON", "MOTHER_ID", true);
        assertEquals("people", dbRelationshipName(r4));

        DbRelationship r5 = makeRelationship("PERSON", "SHIPPING_ADDRESS_ID", "ADDRESS", "ID", false);
        assertEquals("shippingAddress", dbRelationshipName(r5));

        DbRelationship r6 = makeRelationship("PERSON", "ID", "ADDRESS", "PERSON_ID", true);
        assertEquals("addresses", dbRelationshipName(r6));
    }

    @Test
    public void dbRelationshipName_ToMany_RoleQualifiedFk() {

        // two FKs from GAME to TEAM: the reverse collections take the role from the FK column
        DbRelationship r1 = makeRelationship("TEAM", "TEAM_ID", "GAME", "HOME_TEAM_ID", true);
        assertEquals("homeGames", dbRelationshipName(r1));

        DbRelationship r2 = makeRelationship("TEAM", "TEAM_ID", "GAME", "AWAY_TEAM_ID", true);
        assertEquals("awayGames", dbRelationshipName(r2));

        DbRelationship r3 = makeRelationship("team", "team_id", "game", "home_team_id", true);
        assertEquals("homeGames", dbRelationshipName(r3));

        // FK without an ID suffix still carries the role
        DbRelationship r4 = makeRelationship("TEAM", "TEAM_ID", "GAME", "HOME_TEAM", true);
        assertEquals("homeGames", dbRelationshipName(r4));

        // multi-word qualifier and entity name
        DbRelationship r5 = makeRelationship("ARTIST_GROUP", "ID", "EXHIBIT", "PRIMARY_ARTIST_GROUP_ID", true);
        assertEquals("primaryExhibits", dbRelationshipName(r5));

        // FK role unrelated to the source entity name gets no qualifier
        DbRelationship r6 = makeRelationship("PERSON", "PERSON_ID", "PERSON", "MOTHER_ID", true);
        assertEquals("people", dbRelationshipName(r6));

        // FK ending with the entity name without a "_" boundary gets no qualifier
        DbRelationship r7 = makeRelationship("TEAM", "TEAM_ID", "GAME", "STEAM_ID", true);
        assertEquals("games", dbRelationshipName(r7));
    }

    @Test
    public void dbRelationshipName_ToOne_FkWithoutIdSuffix() {

        // an FK without an ID suffix still names the relationship after the column, not the target table
        DbRelationship r1 = makeRelationship("employee", "birth_country", "country", "id", false);
        assertEquals("birthCountry", dbRelationshipName(r1));

        DbRelationship r2 = makeRelationship("PERSON", "SHIPPING_ADDRESS", "ADDRESS", "ID", false);
        assertEquals("shippingAddress", dbRelationshipName(r2));

        // an FK named after the bare target concept keeps the column name
        DbRelationship r3 = makeRelationship("employee", "country", "acme_country", "id", false);
        assertEquals("country", dbRelationshipName(r3));
    }

    @Test
    public void dbRelationshipName_ToMany_RoleQualifiedFk_FkDropsTablePrefix() {

        // FK columns matching a "_"-token suffix of the entity name still carry the role...
        DbRelationship r1 = makeRelationship("acme_team", "id", "acme_game", "home_team_id", true);
        assertEquals("homeAcmeGames", dbRelationshipName(r1));

        DbRelationship r2 = makeRelationship("acme_team", "id", "acme_game", "visiting_team_id", true);
        assertEquals("visitingAcmeGames", dbRelationshipName(r2));

        // ... and a plain suffix reference gets no qualifier
        DbRelationship r3 = makeRelationship("acme_team", "id", "acme_award", "team_id", true);
        assertEquals("acmeAwards", dbRelationshipName(r3));

        DbRelationship r4 = makeRelationship("acme_game_type", "id", "acme_game", "type_id", true);
        assertEquals("acmeGames", dbRelationshipName(r4));
    }

    @Test
    public void dbRelationshipName_MultiJoin() {

        // a compound FK's columns describe PK components, so the to-one side is named after the target entity
        DbRelationship r1 = makeRelationship("shipment", "order_id", "order_line", "order_id", false);
        r1.addJoin(new DbJoin(r1, "line_num", "line_num"));
        assertEquals("orderLine", dbRelationshipName(r1));

        // ... and the to-many side gets an unqualified plural
        DbRelationship r2 = makeRelationship("order_line", "order_id", "shipment", "order_id", true);
        r2.addJoin(new DbJoin(r2, "line_num", "line_num"));
        assertEquals("shipments", dbRelationshipName(r2));

        // no role qualifier even when the first FK column alone would suggest one
        DbRelationship r3 = makeRelationship("team", "team_id", "game", "home_team_id", true);
        r3.addJoin(new DbJoin(r3, "season_id", "season_id"));
        assertEquals("games", dbRelationshipName(r3));
    }

    @Test
    public void objEntityName() {
        assertEquals("Artist", generator.objEntityName(new DbEntity("ARTIST")));
        assertEquals("ArtistWork", generator.objEntityName(new DbEntity("ARTIST_WORK")));
    }

    @Test
    public void objAttributeName() {
        assertEquals("name", generator.objAttributeName(new DbAttribute("NAME")));
        assertEquals("artistName", generator.objAttributeName(new DbAttribute("ARTIST_NAME")));
    }
}
