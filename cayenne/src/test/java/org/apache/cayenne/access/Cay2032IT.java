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
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.cay_2032.Team;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @since 4.0
 */
@UseCayenneRuntime(CayenneProjects.CAY_2032)
public class Cay2032IT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Before
    public void createTestData() throws Exception {
        // USERS table has field `name` BLOB to trigger suppressDistinct in translator
        TableHelper tUser = new TableHelper(dbHelper, "USERS");
        tUser.setColumns("user_id");
        tUser.insert(1);
        tUser.insert(2);
        tUser.insert(3);

        TableHelper tTeam = new TableHelper(dbHelper, "TEAM");
        tTeam.setColumns("team_id");
        tTeam.insert(1);
        tTeam.insert(2);
        tTeam.insert(3);
        tTeam.insert(4);

        TableHelper tTeamHasUser = new TableHelper(dbHelper, "USER_HAS_TEAM");
        tTeamHasUser.setColumns("team_id", "user_id");
        tTeamHasUser.insert(1, 2);
        tTeamHasUser.insert(2, 1);
        tTeamHasUser.insert(2, 2);
        tTeamHasUser.insert(2, 3);
        tTeamHasUser.insert(3, 1);
        tTeamHasUser.insert(3, 3);
    }

    private void checkResult(List<Team> result) throws Exception {
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals(1, result.get(0).getTeamUsers().size());
        assertEquals(3, result.get(1).getTeamUsers().size());
        assertEquals(2, result.get(2).getTeamUsers().size());
        assertEquals(0, result.get(3).getTeamUsers().size());
    }

    @Test
    public void testPrefetchDisjoint() throws Exception {
        List<Team> result = ObjectSelect.query(Team.class)
                .prefetch(Team.TEAM_USERS.disjoint())
                .orderBy(Team.TEAM_ID_PK_PROPERTY.asc())
                .select(context);

        checkResult(result);
    }

    @Test
    public void testPrefetchDisjointById() throws Exception {
        List<Team> result = ObjectSelect.query(Team.class)
                .prefetch(Team.TEAM_USERS.disjointById())
                .orderBy(Team.TEAM_ID_PK_PROPERTY.asc())
                .select(context);

        checkResult(result);
    }

    @Test
    public void testPrefetchJoint() throws Exception {
        List<Team> result = ObjectSelect.query(Team.class)
                .prefetch(Team.TEAM_USERS.joint())
                .orderBy(Team.TEAM_ID_PK_PROPERTY.asc())
                .select(context);

        checkResult(result);
    }
}
