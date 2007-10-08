/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.access;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.access.jdbc.ColumnDescriptor;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataContextProcedureQueryTst extends CayenneTestCase {

    public static final String UPDATE_STORED_PROCEDURE = "cayenne_tst_upd_proc";
    public static final String SELECT_STORED_PROCEDURE = "cayenne_tst_select_proc";
    public static final String OUT_STORED_PROCEDURE = "cayenne_tst_out_proc";

    protected DataContext ctxt;

    protected void setUp() throws Exception {
        super.setUp();

        // Don't run this on MySQL
        if (!getAccessStackAdapter().supportsStoredProcedures()) {
            return;
        }

        deleteTestData();
        ctxt = createDataContext();
    }

    public void testUpdate() throws Exception {
        // Don't run this on MySQL
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
        Painting p = (Painting) a.getPaintingArray().get(0);
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testSelect1() throws Exception {
        // Don't run this on MySQL
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
        Artist a = (Artist) ctxt.objectFromDataRow(
                Artist.class,
                uppercaseConverter(artistRow),
                false);
        Painting p = (Painting) a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testSelect2() throws Exception {
        // Don't run this on MySQL
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
        Artist a = (Artist) ctxt.objectFromDataRow(
                Artist.class,
                uppercaseConverter(artistRow),
                false);
        Painting p = (Painting) a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testSelect3() throws Exception {
        // Don't run this on MySQL
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
        Artist a = (Artist) ctxt.objectFromDataRow(
                Artist.class,
                uppercaseConverter(artistRow),
                false);
        Painting p = (Painting) a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(2000, p.getEstimatedPrice().intValue());
    }

    public void testOutParams() throws Exception {
        // Don't run this on MySQL
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
        // Don't run this on MySQL
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
        Painting p = (Painting) a.getPaintingArray().get(0);

        // invalidate painting, it may have been updated in the proc
        ctxt.invalidateObjects(Collections.singletonList(p));
        assertEquals(1101.01, p.getEstimatedPrice().doubleValue(), 0.02);

    }

    public void testSelectWithRowDescriptor() throws Exception {
        // Don't run this on MySQL
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
        columns[0] = new ColumnDescriptor("ARTIST_ID", Types.INTEGER, Long.class
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
        Artist a = (Artist) ctxt.createAndRegisterNewObject(Artist.class);
        a.setArtistName("An Artist");

        Painting p = (Painting) ctxt.createAndRegisterNewObject(Painting.class);
        p.setPaintingTitle("A Painting");
        // converting double to stringn prevents rounding weirdness...
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