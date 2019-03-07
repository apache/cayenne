/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.query;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.cay_2521.Issue;
import org.apache.cayenne.testdo.cay_2521.Location;
import org.apache.cayenne.testdo.cay_2521.Team;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseServerRuntime(CayenneProjects.CAY_2521)
public class CAY_2521IT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tTeam;
    private TableHelper tIssue;
    private TableHelper tLocation;

    @Before
    public void before() {
        this.tTeam = new TableHelper(dbHelper, "TEAM1").setColumns("home_location_id", "id")
                .setColumnTypes(Types.INTEGER, Types.INTEGER);
        this.tIssue = new TableHelper(dbHelper, "ISSUE").setColumns("home_team_id", "id", "location_id")
                .setColumnTypes(Types.INTEGER, Types.INTEGER, Types.INTEGER);
        this.tLocation = new TableHelper(dbHelper, "LOCATION").setColumns("id", "name", "team_id")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.INTEGER);
    }

    @After
    public void after() throws SQLException {
        this.tLocation.update().set("team_id", null, Types.INTEGER).execute();
        this.tTeam.update().set("home_location_id", null, Types.INTEGER).execute();

        this.tIssue.deleteAll();
        this.tLocation.deleteAll();
        this.tTeam.deleteAll();
    }

    private void createDataSet() throws SQLException {
        tLocation.insert(71, "Test", null);
        tTeam.insert(null, 8);
        tIssue.insert(8, 100, 71);

        tLocation.update().set("team_id", 8).execute();
        tTeam.update().set("home_location_id", 71).execute();
    }

    @Test
    public void testCay_2521() throws SQLException {
        createDataSet();

        List<Issue> result = ObjectSelect.query(Issue.class)
                .where(ExpressionFactory.exp("homeTeam = 8"))
                .prefetch(Issue.HOME_TEAM.disjoint())
                .prefetch(Issue.HOME_TEAM.dot(Team.HOME_LOCATION).disjoint())
                .select(context);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getHomeTeam().getHomeLocation());
    }

    @Test
    public void testCay_2521_ObjId() throws SQLException {
        createDataSet();

        Team team = Cayenne.objectForPK(context, Team.class, 8);
        List<Issue> result = ObjectSelect.query(Issue.class)
                .where(ExpressionFactory.exp("homeTeam = $id" , (Object) team.getObjectId()))
                .prefetch(Issue.HOME_TEAM.disjoint())
                .prefetch(Issue.HOME_TEAM.dot(Team.HOME_LOCATION).disjoint())
                .select(context);
        assertEquals(1, result.size());
        assertNotNull(result.get(0).getHomeTeam().getHomeLocation());
    }

    @Test
    public void testColumnQuery() throws SQLException {
        createDataSet();

        List<Location> result = ObjectSelect
                .columnQuery(Issue.class, Issue.HOME_TEAM.dot(Team.HOME_LOCATION))
                .select(context);
        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getName());
    }

    @Test
    public void testWithJoin() throws SQLException {
        createDataSet();

        List<Location> locations = ObjectSelect.query(Location.class)
                .prefetch(Location.HOME_TEAM.disjoint())
                .select(context);
        assertEquals(1, locations.size());
        assertEquals("Test", locations.get(0).getName());
        assertNotNull(locations.get(0).getHomeTeam());
    }

    @Test
    public void testWithoutJoin() throws SQLException {
        createDataSet();

        List<Team> teams = ObjectSelect.query(Team.class)
                .prefetch(Team.HOME_LOCATION.disjoint())
                .select(context);
        assertEquals(1, teams.size());
        assertNotNull(teams.get(0).getHomeLocation());
    }
}
