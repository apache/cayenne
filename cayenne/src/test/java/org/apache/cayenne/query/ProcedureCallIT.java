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
package org.apache.cayenne.query;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ProcedureResult;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.ExternalTransaction;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ProcedureCallIT extends RuntimeCase {

    public static final String UPDATE_STORED_PROCEDURE = "cayenne_tst_upd_proc";
    public static final String UPDATE_STORED_PROCEDURE_NOPARAM = "cayenne_tst_upd_proc2";
    public static final String SELECT_STORED_PROCEDURE = "cayenne_tst_select_proc";
    public static final String OUT_STORED_PROCEDURE = "cayenne_tst_out_proc";

    @Inject
    private DataContext context;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private JdbcEventLogger jdbcEventLogger;

    @Test
    public void testUpdate() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        runProcedureSelect(ProcedureCall.query(UPDATE_STORED_PROCEDURE).param("paintingPrice", 3000));

        // check that price have doubled
        List<Artist> artists = ObjectSelect.query(Artist.class).prefetch(Artist.PAINTING_ARRAY.disjoint()).select(context);
        assertEquals(1, artists.size());

        Artist a = artists.get(0);
        Painting p = a.getPaintingArray().get(0);
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    @Test
    public void testUpdateNoParam() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        runProcedureSelect(ProcedureCall.query(UPDATE_STORED_PROCEDURE_NOPARAM));

        // check that price have doubled
        List<Artist> artists = ObjectSelect.query(Artist.class).prefetch(Artist.PAINTING_ARRAY.disjoint()).select(context);
        assertEquals(1, artists.size());

        Artist a = artists.get(0);
        Painting p = a.getPaintingArray().get(0);
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    @Test
    public void testSelect() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        List<?> artists = runProcedureSelect(
                ProcedureCall.query(SELECT_STORED_PROCEDURE)
                .param("aName", "An Artist")
                .param("paintingPrice", 3000)
        ).firstList();

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        DataRow artistRow = (DataRow) artists.get(0);
        Artist a = context.objectFromDataRow(Artist.class, uppercaseConverter(artistRow));
        Painting p = a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        context.invalidateObjects(p);
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    @Test
    public void testFetchLimit() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);
        createArtist(2000.0);
        createArtist(3000.0);

        List<?> artists = runProcedureSelect(
                ProcedureCall.query(SELECT_STORED_PROCEDURE)
                .param("aName", "An Artist")
                .param("paintingPrice", 3000)
                .limit(2)
        ).firstList();

        assertEquals(2, artists.size());
    }

    @Test
    public void testFetchOffset() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);
        createArtist(2000.0);
        createArtist(3000.0);

        List<?> artists = runProcedureSelect(
                ProcedureCall.query(SELECT_STORED_PROCEDURE)
                .param("aName", "An Artist")
                .param("paintingPrice", 3000)
                .offset(2)
        ).firstList();

        assertEquals(1, artists.size());
    }

    @Test
    public void testColumnNameCapitalization() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        List<DataRow> artists = runProcedureSelect(
                ProcedureCall.query(SELECT_STORED_PROCEDURE)
                .param("aName", "An Artist")
                .capsStrategy(CapsStrategy.LOWER)
        ).firstList();

        List<DataRow> artists1 = runProcedureSelect(ProcedureCall.query(SELECT_STORED_PROCEDURE)
                .param("aName", "An Artist")
                .capsStrategy(CapsStrategy.UPPER)
        ).firstList();

        assertTrue(artists.get(0).containsKey("date_of_birth"));
        assertFalse(artists.get(0).containsKey("DATE_OF_BIRTH"));

        assertFalse(artists1.get(0).containsKey("date_of_birth"));
        assertTrue(artists1.get(0).containsKey("DATE_OF_BIRTH"));

    }

    @Test
    public void testOutParams() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        ProcedureResult result = runProcedureSelect(
                ProcedureCall.query(OUT_STORED_PROCEDURE)
                .param("in_param", 20)
        );

        Number price = (Number) result.getOutParam("out_param");
        assertEquals(40, price.intValue());
    }

    @Test
    public void testSelectPersistentObject() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        if (!accessStackAdapter.canMakeObjectsOutOfProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1101.01);

        List<Artist> artists = runProcedureSelect(ProcedureCall.query(SELECT_STORED_PROCEDURE, Artist.class)
                .param("aName", "An Artist")
        ).firstList();

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        Artist a = (Artist) artists.get(0);
        Painting p = a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        context.invalidateObjects(p);
        assertEquals(1101.01, p.getEstimatedPrice().doubleValue(), 0.02);
    }

    @Test
    public void testSelectWithRowDescriptor() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        // TESTING THIS ***
        // A.ARTIST_ID, A.DATE_OF_BIRTH, A.ARTIST_NAME
        ColumnDescriptor[] columns = new ColumnDescriptor[3];

        // read ID as Long, and everything else as default types
        columns[0] = new ColumnDescriptor("ARTIST_ID", Types.BIGINT);
        columns[1] = new ColumnDescriptor("ARTIST_NAME", Types.CHAR);
        columns[2] = new ColumnDescriptor("DATE_OF_BIRTH", Types.DATE);

        List<?> rows = runProcedureSelect(ProcedureCall.dataRowQuery(SELECT_STORED_PROCEDURE)
                .param("aName", "An Artist")
                .param("paintingPrice", 3000)
                .resultDescriptor(columns)
        ).firstList();

        // check the results
        assertNotNull("Null result from StoredProcedure.", rows);
        assertEquals(1, rows.size());
        DataRow artistRow = (DataRow) rows.get(0);

        assertEquals(3, artistRow.size());

        artistRow = uppercaseConverter(artistRow);

        Object id = artistRow.get("ARTIST_ID");
        assertNotNull(id);
        assertTrue("Expected Long, got: " + id.getClass().getName(), id instanceof Long);
    }

    private <T> ProcedureResult<T> runProcedureSelect(ProcedureCall<T> q) {
        // Sybase blows whenever a transaction wraps a SP, so turn off
        // transactions

        // TODO: it is quite the opposite with PostgreSQL. If an SP returns an
        // open refcursor, it actually expects a TX in progress, so while we
        // don't have refcursor unit tests, this is something to keep in mind
        // e.g.
        // http://stackoverflow.com/questions/16921942/porting-apache-cayenne-from-oracle-to-postgresql

        BaseTransaction t = new ExternalTransaction(jdbcEventLogger);
        BaseTransaction.bindThreadTransaction(t);

        try {
            return q.call(context);
        } finally {
            BaseTransaction.bindThreadTransaction(null);
            t.commit();
        }
    }

    private void createArtist(double paintingPrice) {
        Artist a = context.newObject(Artist.class);
        a.setArtistName("An Artist");

        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("A Painting");
        // converting double to string prevents rounding weirdness...
        p.setEstimatedPrice(new BigDecimal("" + paintingPrice));
        a.addToPaintingArray(p);

        context.commitChanges();
    }

    /**
     * An ugly hack - converting row keys to uppercase ... Tracked via CAY-148.
     */
    private DataRow uppercaseConverter(DataRow row) {
        DataRow converted = new DataRow(row.size());

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            converted.put(entry.getKey().toUpperCase(), entry.getValue());
        }

        return converted;
    }
}
