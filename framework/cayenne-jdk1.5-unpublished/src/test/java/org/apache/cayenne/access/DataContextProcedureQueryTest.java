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

package org.apache.cayenne.access;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DataContextProcedureQueryTest extends CayenneCase {

    public static final String UPDATE_STORED_PROCEDURE = "cayenne_tst_upd_proc";
    public static final String UPDATE_STORED_PROCEDURE_NOPARAM = "cayenne_tst_upd_proc2";
    public static final String SELECT_STORED_PROCEDURE = "cayenne_tst_select_proc";
    public static final String OUT_STORED_PROCEDURE = "cayenne_tst_out_proc";

    protected DataContext ctxt;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        deleteTestData();
        ctxt = createDataContext();
    }

    public void testUpdate() throws Exception {
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        ProcedureQuery q = new ProcedureQuery(UPDATE_STORED_PROCEDURE);
        q.addParameter("paintingPrice", new Integer(3000));

        // since stored procedure commits its stuff, we must use an explicit
        // non-committing transaction

        Transaction t = Transaction.externalTransaction(null);
        Transaction.bindThreadTransaction(t);

        try {
            ctxt.performGenericQuery(q);
        }
        finally {
            Transaction.bindThreadTransaction(null);
            t.commit();
        }

        // check that price have doubled
        SelectQuery select = new SelectQuery(Artist.class);
        select.addPrefetch("paintingArray");

        List artists = ctxt.performQuery(select);
        assertEquals(1, artists.size());

        Artist a = (Artist) artists.get(0);
        Painting p = a.getPaintingArray().get(0);
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testUpdateNoParam() throws Exception {
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        ProcedureQuery q = new ProcedureQuery(UPDATE_STORED_PROCEDURE_NOPARAM);

        // since stored procedure commits its stuff, we must use an explicit
        // non-committing transaction

        Transaction t = Transaction.externalTransaction(null);
        Transaction.bindThreadTransaction(t);

        try {
            ctxt.performGenericQuery(q);
        }
        finally {
            Transaction.bindThreadTransaction(null);
            t.commit();
        }

        // check that price have doubled
        SelectQuery select = new SelectQuery(Artist.class);
        select.addPrefetch("paintingArray");

        List artists = ctxt.performQuery(select);
        assertEquals(1, artists.size());

        Artist a = (Artist) artists.get(0);
        Painting p = a.getPaintingArray().get(0);
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testSelect1() throws Exception {
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        ProcedureQuery q = new ProcedureQuery(SELECT_STORED_PROCEDURE);
        q.addParameter("aName", "An Artist");
        q.addParameter("paintingPrice", new Integer(3000));
        List artists = runProcedureSelect(q);

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        DataRow artistRow = (DataRow) artists.get(0);
        Artist a = ctxt.objectFromDataRow(
                Artist.class,
                uppercaseConverter(artistRow),
                false);
        Painting p = a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testSelect2() throws Exception {
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        ProcedureQuery q = new ProcedureQuery(SELECT_STORED_PROCEDURE);
        q.addParameter("aName", "An Artist");
        q.addParameter("paintingPrice", new Integer(3000));

        List artists = runProcedureSelect(q);

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        DataRow artistRow = (DataRow) artists.get(0);
        Artist a = ctxt.objectFromDataRow(
                Artist.class,
                uppercaseConverter(artistRow),
                false);
        Painting p = a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testSelect3() throws Exception {
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        // test ProcedureQuery with Procedure as root
        Procedure proc = ctxt.getEntityResolver().getProcedure(SELECT_STORED_PROCEDURE);
        ProcedureQuery q = new ProcedureQuery(proc);
        q.addParameter("aName", "An Artist");
        q.addParameter("paintingPrice", new Integer(3000));

        List artists = runProcedureSelect(q);

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        DataRow artistRow = (DataRow) artists.get(0);
        Artist a = ctxt.objectFromDataRow(
                Artist.class,
                uppercaseConverter(artistRow),
                false);
        Painting p = a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testFetchLimit() throws Exception {
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);
        createArtist(2000.0);
        createArtist(3000.0);

        ProcedureQuery q = new ProcedureQuery(SELECT_STORED_PROCEDURE);
        q.addParameter("aName", "An Artist");
        q.addParameter("paintingPrice", new Integer(3000));
        q.setFetchLimit(2);
        List artists = runProcedureSelect(q);

        assertEquals(2, artists.size());
    }
    
    public void testFetchOffset() throws Exception {
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);
        createArtist(2000.0);
        createArtist(3000.0);

        ProcedureQuery q = new ProcedureQuery(SELECT_STORED_PROCEDURE);
        q.addParameter("aName", "An Artist");
        q.addParameter("paintingPrice", new Integer(3000));
        q.setFetchOffset(2);
        List artists = runProcedureSelect(q);

        assertEquals(1, artists.size());
    }

    public void testColumnNameCapitalization() throws Exception{
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }
        
        // create an artist with painting in the database
        createArtist(1000.0);
        ProcedureQuery q = new ProcedureQuery(SELECT_STORED_PROCEDURE);
        
        q.setColumnNamesCapitalization(CapsStrategy.LOWER);
        q.addParameter("aName", "An Artist");
        List<DataRow> artists = runProcedureSelect(q);
        
        ProcedureQuery q1 = new ProcedureQuery(SELECT_STORED_PROCEDURE);
        
        q1.setColumnNamesCapitalization(CapsStrategy.UPPER);
        q1.addParameter("aName", "An Artist");
        List<DataRow> artists1 = runProcedureSelect(q1);
        
        assertTrue(artists.get(0).containsKey("date_of_birth"));
        assertFalse(artists.get(0).containsKey("DATE_OF_BIRTH"));
        
        assertFalse(artists1.get(0).containsKey("date_of_birth"));
        assertTrue(artists1.get(0).containsKey("DATE_OF_BIRTH"));
        
    }

    public void testOutParams() throws Exception {
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        ProcedureQuery q = new ProcedureQuery(OUT_STORED_PROCEDURE);
        q.addParameter("in_param", new Integer(20));

        List rows = runProcedureSelect(q);

        assertEquals(1, rows.size());
        Object row = rows.get(0);
        assertNotNull(row);
        assertTrue(
                "Unexpected row class: " + row.getClass().getName(),
                row instanceof Map);
        Map outParams = (Map) row;
        Number price = (Number) outParams.get("out_param");
        assertNotNull("Null result... row content: " + row, price);
        assertEquals(40, price.intValue());
    }

    public void testSelectDataObject() throws Exception {
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        if (!getAccessStackAdapter().canMakeObjectsOutOfProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1101.01);

        ProcedureQuery q = new ProcedureQuery(SELECT_STORED_PROCEDURE, Artist.class);
        q.addParameter("aName", "An Artist");

        List artists = runProcedureSelect(q);

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        Artist a = (Artist) artists.get(0);
        Painting p = a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(1101.01, p.getEstimatedPrice().doubleValue(), 0.02);
    }

    public void testSelectWithRowDescriptor() throws Exception {

        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        // create an artist with painting in the database
        createArtist(1000.0);

        // test ProcedureQuery with Procedure as root
        Procedure proc = ctxt.getEntityResolver().getProcedure(SELECT_STORED_PROCEDURE);
        ProcedureQuery q = new ProcedureQuery(proc);
        q.setFetchingDataRows(true);
        q.addParameter("aName", "An Artist");
        q.addParameter("paintingPrice", new Integer(3000));

        // TESTING THIS ***
        // A.ARTIST_ID, A.DATE_OF_BIRTH, A.ARTIST_NAME
        ColumnDescriptor[] columns = new ColumnDescriptor[3];

        // read ID as Long, and everything else as default types
        columns[0] = new ColumnDescriptor("ARTIST_ID", Types.BIGINT, Long.class
                .getName());
        columns[1] = new ColumnDescriptor("ARTIST_NAME", Types.CHAR, String.class
                .getName());
        columns[2] = new ColumnDescriptor("DATE_OF_BIRTH", Types.DATE, Date.class
                .getName());
        q.addResultDescriptor(columns);

        List rows = runProcedureSelect(q);

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

    protected List runProcedureSelect(ProcedureQuery q) throws Exception {
        // Sybase blows whenever a transaction wraps a SP, so turn off transactions

        boolean transactionsFlag = ctxt
                .getParentDataDomain()
                .isUsingExternalTransactions();

        ctxt.getParentDataDomain().setUsingExternalTransactions(true);

        try {
            return ctxt.performQuery(q);
        }
        finally {
            ctxt.getParentDataDomain().setUsingExternalTransactions(transactionsFlag);
        }
    }

    protected void createArtist(double paintingPrice) {
        Artist a = ctxt.newObject(Artist.class);
        a.setArtistName("An Artist");

        Painting p = ctxt.newObject(Painting.class);
        p.setPaintingTitle("A Painting");
        // converting double to string prevents rounding weirdness...
        p.setEstimatedPrice(new BigDecimal("" + paintingPrice));
        a.addToPaintingArray(p);

        ctxt.commitChanges();
    }

    /**
     * An ugly hack - converting row keys to uppercase ... Tracked via CAY-148.
     */
    protected DataRow uppercaseConverter(DataRow row) {
        DataRow converted = new DataRow(row.size());

        Iterator it = row.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            converted.put(entry.getKey().toString().toUpperCase(), entry.getValue());
        }

        return converted;
    }
}
