/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatternObjectNameGeneratorTest {

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
    public void objRelationshipName_ToMany_RoleQualifiedFk() {
        PatternObjectNameGenerator generator = new PatternObjectNameGenerator("^AA_");

        // the stripped prefix is transparent to role qualifier matching...
        DbRelationship r1 = makeRelationship("AA_TEAM", "TEAM_ID", "AA_GAME", "HOME_TEAM_ID", true);
        assertEquals("homeGames", generator.objRelationshipName(r1));

        // ... and doesn't itself become a qualifier when the FK column carries it too
        DbRelationship r2 = makeRelationship("AA_TEAM", "TEAM_ID", "AA_GAME", "AA_TEAM_ID", true);
        assertEquals("games", generator.objRelationshipName(r2));
    }

    @Test
    public void dbEntityBaseName_NoMatch() {
        assertEquals("xyzabc", new PatternObjectNameGenerator("^pre").dbEntityBaseName("xyzabc"));
    }

    @Test
    public void dbEntityBaseName() {
        assertEquals("lowercase", new PatternObjectNameGenerator("^pre").dbEntityBaseName("prelowercase"));
        assertEquals("UPPERCASE", new PatternObjectNameGenerator("^pre").dbEntityBaseName("PREUPPERCASE"));
    }

    @Test
    public void stripHead() {
        assertEquals("name", new PatternObjectNameGenerator("^strip_").dbEntityBaseName("strip_name"));
        assertEquals("strip_name", new PatternObjectNameGenerator("^strip_").dbEntityBaseName("strip_strip_name"));
    }

    @Test
    public void stripTail() {
        assertEquals("name", new PatternObjectNameGenerator("_strip$").dbEntityBaseName("name_strip"));
        assertEquals("name_strip", new PatternObjectNameGenerator("_strip$").dbEntityBaseName("name_strip_strip"));
    }

    @Test
    public void stripiddle() {
        assertEquals("start_end", new PatternObjectNameGenerator("_strip").dbEntityBaseName("start_strip_end"));
        assertEquals("start_end", new PatternObjectNameGenerator("_strip").dbEntityBaseName("start_strip_strip_end"));
    }
}
