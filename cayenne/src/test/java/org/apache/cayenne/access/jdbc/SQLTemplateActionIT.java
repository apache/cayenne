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

package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.StatementContexts;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.apache.cayenne.unit.CayenneProjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Connection;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class SQLTemplateActionIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataNode node;
    private TestDbAdapter testDbAdapter;
    private ObjectContext context;
    private TableHelper tArtist;

    @BeforeEach
    public void setUp() throws Exception {
        node = env.dataNode();
        testDbAdapter = env.testDbAdapter();
        context = env.context();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");
    }

    protected void createFourArtists() throws Exception {

        Date date = new Date(System.currentTimeMillis());

        tArtist.insert(11, "artist2", date);
        tArtist.insert(101, "artist3", date);
        tArtist.insert(201, "artist4", date);
        tArtist.insert(3001, "artist5", date);
    }

    @Test
    public void properties() {
        SQLTemplate template = new SQLTemplate("AAAAA");

        SQLTemplateAction action = new SQLTemplateAction(template, node);
        assertSame(template, action.getQuery());
        assertSame(node, action.dataNode);
    }

    @Test
    public void executeSelect() throws Exception {
        createFourArtists();

        String templateString = "SELECT * FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(templateString);
        template.setColumnNamesCapitalization(CapsStrategy.UPPER);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("id", 201L);
        template.setParams(bindings);

        // must ensure the right SQLTemplateAction is created

        SQLAction plan = node.getAdapter().getAction(template, node);
        assertTrue(plan instanceof SQLTemplateAction);

        StatementContext statementContext = StatementContexts.newRun(node, template);

        try (Connection c = CayenneTestsEnv.COMMON_SCHEMA.dataSource().getConnection();) {
            plan.performAction(c, statementContext);
        }

        List<DataRow> rows = StatementContexts.rows(statementContext);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        // In the absence of ObjEntity most DB's return a Long here, except for
        // Oracle
        // that has no BIGINT type and
        // returns BigDecimal, so do a Number comparison
        Number id = (Number) row.get("ARTIST_ID");
        assertNotNull(id);
        assertEquals(((Number) bindings.get("id")).longValue(), id.longValue());
        assertEquals("artist4", row.get("ARTIST_NAME"));
        assertTrue(row.containsKey("DATE_OF_BIRTH"));
    }

    @Test
    public void selectObjects() throws Exception {
        createFourArtists();

        String templateString = "SELECT * FROM ARTIST";
        SQLTemplate sqlTemplate = new SQLTemplate(Artist.class, templateString);

        if (testDbAdapter.isLowerCaseNames()) {
            sqlTemplate.setColumnNamesCapitalization(CapsStrategy.UPPER);
        }

        @SuppressWarnings("unchecked")
        List<Artist> artists = (List<Artist>) context.select(sqlTemplate);

        assertEquals(4, artists.size());
        for (Artist artist : artists) {
            assertTrue(artist.getArtistName().startsWith("artist"));
        }
    }

    @Test
    public void selectUtilDate() throws Exception {
        createFourArtists();

        String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.util.Date' 'DOB') "
                + "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(templateString);
        template.setColumnNamesCapitalization(CapsStrategy.UPPER);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("id", 101);
        template.setParams(bindings);

        SQLAction plan = node.getAdapter().getAction(template, node);

        StatementContext statementContext = StatementContexts.newRun(node, template);

        try (Connection c = CayenneTestsEnv.COMMON_SCHEMA.dataSource().getConnection();) {
            plan.performAction(c, statementContext);
        }

        List<DataRow> rows = StatementContexts.rows(statementContext);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        assertNotNull(row.get("DOB"));
        assertEquals(java.util.Date.class, row.get("DOB").getClass());
    }

    @Test
    public void selectSQLDate() throws Exception {
        createFourArtists();

        String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.sql.Date' 'DOB') "
                + "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(templateString);
        template.setColumnNamesCapitalization(CapsStrategy.UPPER);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("id", 101);
        template.setParams(bindings);

        SQLAction plan = node.getAdapter().getAction(template, node);

        StatementContext statementContext = StatementContexts.newRun(node, template);

        try (Connection c = CayenneTestsEnv.COMMON_SCHEMA.dataSource().getConnection();) {
            plan.performAction(c, statementContext);
        }

        List<DataRow> rows = StatementContexts.rows(statementContext);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        assertNotNull(row.get("DOB"));
        assertEquals(java.sql.Date.class, row.get("DOB").getClass());
    }

    @Test
    public void selectSQLTimestamp() throws Exception {
        createFourArtists();

        String templateString = "SELECT #result('DATE_OF_BIRTH' 'java.sql.Timestamp' 'DOB') "
                + "FROM ARTIST WHERE ARTIST_ID = #bind($id)";
        SQLTemplate template = new SQLTemplate(templateString);
        template.setColumnNamesCapitalization(CapsStrategy.UPPER);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("id", 201);
        template.setParams(bindings);

        SQLAction plan = node.getAdapter().getAction(template, node);

        StatementContext statementContext = StatementContexts.newRun(node, template);

        try (Connection c = CayenneTestsEnv.COMMON_SCHEMA.dataSource().getConnection();) {
            plan.performAction(c, statementContext);
        }

        List<DataRow> rows = StatementContexts.rows(statementContext);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        assertNotNull(row.get("DOB"));
        // Sybase returns a Timestamp subclass... so can't test equality
        assertTrue(java.sql.Timestamp.class.isAssignableFrom(row.get("DOB").getClass()));
    }

    @Test
    public void executeUpdate() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate template = new SQLTemplate(templateString);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("id", 1L);
        bindings.put("name", "a1");
        bindings.put("dob", new Date(System.currentTimeMillis()));
        template.setParams(bindings);

        SQLAction action = node.getAdapter().getAction(template, node);

        try (Connection c = CayenneTestsEnv.COMMON_SCHEMA.dataSource().getConnection();) {
            StatementContext statementContext = StatementContexts.newRun(node, template);
            action.performAction(c, statementContext);

            int[] batches = StatementContexts.counts(statementContext);
            assertNotNull(batches);
            assertEquals(1, batches.length);
            assertEquals(1, batches[0]);
        }
        assertEquals(1, tArtist.getRowCount());
        assertEquals(1L, tArtist.getLong("ARTIST_ID"));
        assertEquals("a1", tArtist.getString("ARTIST_NAME").trim());
    }

    @Test
    public void executeUpdateNoParameters() throws Exception {
        createFourArtists();

        SQLTemplate template = new SQLTemplate("delete from ARTIST where ARTIST_NAME like 'a%'");

        SQLAction action = node.getAdapter().getAction(template, node);

        try (Connection c = CayenneTestsEnv.COMMON_SCHEMA.dataSource().getConnection();) {
            StatementContext statementContext = StatementContexts.newRun(node, template);
            action.performAction(c, statementContext);

            int[] batches = StatementContexts.counts(statementContext);
            assertNotNull(batches);
            assertEquals(1, batches.length);
            assertEquals(4, batches[0]);
        }
    }

    @Test
    public void executeUpdateWithDollarSigns() throws Exception {
        String templateString = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) "
                + "VALUES (#bind($id), #bind($name), #bind($dob 'DATE'))";
        SQLTemplate template = new SQLTemplate(templateString);

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("id", 33L);
        bindings.put("name", "a$$$$$");
        bindings.put("dob", new Date(System.currentTimeMillis()));
        template.setParams(bindings);

        SQLAction genericAction = node.getAdapter().getAction(template, node);
        assertTrue(genericAction instanceof SQLTemplateAction);
        SQLTemplateAction action = (SQLTemplateAction) genericAction;

        assertSame(node, action.dataNode);
        assertSame(template, action.getQuery());

        try (Connection c = CayenneTestsEnv.COMMON_SCHEMA.dataSource().getConnection();) {
            StatementContext statementContext = StatementContexts.newRun(node, template);
            action.performAction(c, statementContext);

            int[] counts = StatementContexts.counts(statementContext);
            assertNotNull(counts);
            assertEquals(1, counts.length);
            assertEquals(1, counts[0]);
        }

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .orderBy("db:ARTIST_ID", SortOrder.ASCENDING);
        StatementContext statementContext = StatementContexts.run(node, query);

        List<DataRow> data = StatementContexts.rows(statementContext);
        assertEquals(1, data.size());
        DataRow row = data.get(0);
        assertEquals(bindings.get("id"), row.get("ARTIST_ID"));
        assertEquals(bindings.get("name"), row.get("ARTIST_NAME"));
    }

    @Test
    public void extractTemplateString() {
        SQLTemplate template = new SQLTemplate(Artist.class, "A\nBC");
        SQLTemplateAction action = new SQLTemplateAction(template, node);

        assertEquals("A BC", action.extractTemplateString());
    }

}
