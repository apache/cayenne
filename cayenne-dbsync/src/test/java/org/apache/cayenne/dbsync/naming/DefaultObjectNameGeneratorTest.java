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

    @Test
    public void objRelationshipName_LowerCase_Underscores() {

        DbRelationship r1 = makeRelationship("painting", "artist_id", "artist", "artist_id", false);
        assertEquals("artist", generator.objRelationshipName(r1));

        DbRelationship r2 = makeRelationship("artist", "artist_id", "painting", "artist_id", true);
        assertEquals("paintings", generator.objRelationshipName(r2));

        DbRelationship r3 = makeRelationship("person", "mother_id", "person", "person_id", false);
        assertEquals("mother", generator.objRelationshipName(r3));

        DbRelationship r4 = makeRelationship("person", "person_id", "person", "mother_id", true);
        assertEquals("people", generator.objRelationshipName(r4));

        DbRelationship r5 = makeRelationship("person", "shipping_address_id", "address", "id", false);
        assertEquals("shippingAddress", generator.objRelationshipName(r5));

        DbRelationship r6 = makeRelationship("person", "id", "address", "person_id", true);
        assertEquals("addresses", generator.objRelationshipName(r6));
    }

    @Test
    public void objRelationshipName_UpperCase_Underscores() {

        DbRelationship r1 = makeRelationship("PAINTING", "ARTIST_ID", "ARTIST", "ARTIST_ID", false);
        assertEquals("artist", generator.objRelationshipName(r1));

        DbRelationship r2 = makeRelationship("ARTIST", "ARTIST_ID", "PAINTING", "ARTIST_ID", true);
        assertEquals("paintings", generator.objRelationshipName(r2));

        DbRelationship r3 = makeRelationship("PERSON", "MOTHER_ID", "PERSON", "PERSON_ID", false);
        assertEquals("mother", generator.objRelationshipName(r3));

        DbRelationship r4 = makeRelationship("PERSON", "PERSON_ID", "PERSON", "MOTHER_ID", true);
        assertEquals("people", generator.objRelationshipName(r4));

        DbRelationship r5 = makeRelationship("PERSON", "SHIPPING_ADDRESS_ID", "ADDRESS", "ID", false);
        assertEquals("shippingAddress", generator.objRelationshipName(r5));

        DbRelationship r6 = makeRelationship("PERSON", "ID", "ADDRESS", "PERSON_ID", true);
        assertEquals("addresses", generator.objRelationshipName(r6));
    }

    @Test
    public void objRelationshipName_ToMany_RoleQualifiedFk() {

        // two FKs from GAME to TEAM: the reverse collections take the role from the FK column
        DbRelationship r1 = makeRelationship("TEAM", "TEAM_ID", "GAME", "HOME_TEAM_ID", true);
        assertEquals("homeGames", generator.objRelationshipName(r1));

        DbRelationship r2 = makeRelationship("TEAM", "TEAM_ID", "GAME", "AWAY_TEAM_ID", true);
        assertEquals("awayGames", generator.objRelationshipName(r2));

        DbRelationship r3 = makeRelationship("team", "team_id", "game", "home_team_id", true);
        assertEquals("homeGames", generator.objRelationshipName(r3));

        // FK without an ID suffix still carries the role
        DbRelationship r4 = makeRelationship("TEAM", "TEAM_ID", "GAME", "HOME_TEAM", true);
        assertEquals("homeGames", generator.objRelationshipName(r4));

        // multi-word qualifier and entity name
        DbRelationship r5 = makeRelationship("ARTIST_GROUP", "ID", "EXHIBIT", "PRIMARY_ARTIST_GROUP_ID", true);
        assertEquals("primaryExhibits", generator.objRelationshipName(r5));

        // FK role unrelated to the source entity name gets no qualifier
        DbRelationship r6 = makeRelationship("PERSON", "PERSON_ID", "PERSON", "MOTHER_ID", true);
        assertEquals("people", generator.objRelationshipName(r6));

        // FK ending with the entity name without a "_" boundary gets no qualifier
        DbRelationship r7 = makeRelationship("TEAM", "TEAM_ID", "GAME", "STEAM_ID", true);
        assertEquals("games", generator.objRelationshipName(r7));
    }

    @Test
    public void objRelationshipName_ToMany_RoleQualifiedFk_FkDropsTablePrefix() {

        // FK columns matching a "_"-token suffix of the entity name still carry the role...
        DbRelationship r1 = makeRelationship("nhl_team", "id", "nhl_game", "home_team_id", true);
        assertEquals("homeNhlGames", generator.objRelationshipName(r1));

        DbRelationship r2 = makeRelationship("nhl_team", "id", "nhl_game", "visiting_team_id", true);
        assertEquals("visitingNhlGames", generator.objRelationshipName(r2));

        // ... and a plain suffix reference gets no qualifier
        DbRelationship r3 = makeRelationship("nhl_team", "id", "nhl_award", "team_id", true);
        assertEquals("nhlAwards", generator.objRelationshipName(r3));

        DbRelationship r4 = makeRelationship("nhl_game_type", "id", "nhl_game", "type_id", true);
        assertEquals("nhlGames", generator.objRelationshipName(r4));
    }

    @Test
    public void dbRelationshipName_ToMany_RoleQualifiedFk() {

        DbRelationship r1 = makeRelationship("TEAM", "TEAM_ID", "GAME", "HOME_TEAM_ID", true);
        assertEquals("homeGames", generator.dbRelationshipName(r1.getJoins(), r1.isToMany()));

        DbRelationship r2 = makeRelationship("TEAM", "TEAM_ID", "GAME", "AWAY_TEAM_ID", true);
        assertEquals("awayGames", generator.dbRelationshipName(r2.getJoins(), r2.isToMany()));
    }

    @Test
    public void dbRelationshipName() {

        DbRelationship r1 = makeRelationship("painting", "artist_id", "artist", "artist_id", false);
        assertEquals("artist", generator.dbRelationshipName(r1.getJoins(), r1.isToMany()));

        DbRelationship r2 = makeRelationship("artist", "artist_id", "painting", "artist_id", true);
        assertEquals("paintings", generator.dbRelationshipName(r2.getJoins(), r2.isToMany()));

        DbRelationship r3 = makeRelationship("person", "shipping_address_id", "address", "id", false);
        assertEquals("shippingAddress", generator.dbRelationshipName(r3.getJoins(), r3.isToMany()));
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
