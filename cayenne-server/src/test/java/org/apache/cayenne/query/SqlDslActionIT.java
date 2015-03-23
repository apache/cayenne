/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cayenne.query;

import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.table.Table;
import de.jexp.jequel.table.IColumn;
import de.jexp.jequel.expression.types.NUMERIC;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.NewTableHelper;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.MockOperationObserver;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.sqldsl.SqlQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Date;
import java.util.List;

import static de.jexp.jequel.expression.Expressions.param;
import static org.junit.Assert.*;

/**
 * TODO We can mock connection here in order to check sql string,
 * and binding params but avoid real db connection
 *
 *
 * */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SqlDslActionIT extends ServerCase {

    public static final ARTIST ARTIST = new ARTIST();
    public static class ARTIST extends Table<ARTIST> {
        public NUMERIC ARTIST_ID = integer().primaryKey();
        public IColumn<String> ARTIST_NAME = character(254).mandatory();
        public IColumn<Date> DATE_OF_BIRTH = date();

        {
            initFields();
        }
    }

    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;

    @Inject
    private DataNode node;

    @Inject
    private JdbcAdapter adapter;

    @Inject
    private ObjectContext objectContext;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;

    @Before
    public void setUp() throws Exception {
        tArtist = new NewTableHelper(dbHelper, ARTIST);
    }

    protected void createFourArtists() throws Exception {
        Date date = new Date(System.currentTimeMillis());

        tArtist.insert(11, "artist2", date);
        tArtist.insert(101, "artist3", date);
        tArtist.insert(201, "artist4", date);
        tArtist.insert(3001, "artist5", date);
    }

    @Test
    public void testExecuteSelect() throws Exception {
        createFourArtists();

        Sql sql = Sql.Select(ARTIST).where(ARTIST.ARTIST_ID.eq(param(201L))).toSql();
        SqlQuery query = new SqlQuery(sql);

        SQLAction action = adapter.getAction(query, node);

        MockOperationObserver observer = new MockOperationObserver();
        Connection c = dataSourceFactory.getSharedDataSource().getConnection();

        try {
            action.performAction(c, observer);
        }
        finally {
            c.close();
        }

        List<DataRow> rows = observer.rowsForQuery(query);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        // In the absence of ObjEntity most DB's return a Long here, except for Oracle
        // that has no BIGINT type and returns BigDecimal, so do a Number comparison
        Number id = (Number) row.get("ARTIST_ID");
        assertNotNull(id);
        assertEquals(((Number) query.getPositionalParams().get(0)).longValue(), id.longValue());
        assertEquals("artist4", row.get("ARTIST_NAME"));
        assertTrue(row.containsKey("DATE_OF_BIRTH"));
    }

    @Test
    public void testSelectUtilDate() throws Exception {
        createFourArtists();

        Sql sql = Sql.Select(ARTIST.DATE_OF_BIRTH.as("DOB"))
                .from(ARTIST).where(ARTIST.ARTIST_ID.eq(param(101))).toSql();

        SqlQuery<DataRow> query = new SqlQuery<DataRow>(sql);

        SQLAction action = adapter.getAction(query, node);

        MockOperationObserver observer = new MockOperationObserver();
        Connection connection = dataSourceFactory.getSharedDataSource().getConnection();

        try {
            action.performAction(connection, observer);
        } finally {
            connection.close();
        }

        List<DataRow> rows = observer.rowsForQuery(query);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = rows.get(0);

        assertNotNull(row.get("DOB"));
        assertEquals(java.util.Date.class, row.get("DOB").getClass());
    }
}