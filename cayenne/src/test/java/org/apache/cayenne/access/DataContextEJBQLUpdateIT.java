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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextEJBQLUpdateIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "ARTIST_ID",
                "PAINTING_TITLE",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.BIGINT,
                Types.VARCHAR,
                Types.DECIMAL);

    }

    private void createThreeArtistsTwoPaintings() throws Exception {
        tArtist.insert(33001, "AA1");
        tArtist.insert(33002, "AA2");
        tArtist.insert(33003, "BB1");
        tPainting.insert(33001, 33001, "P1", 3000);
        tPainting.insert(33002, 33002, "P2", 5000);
    }

    @Test
    public void testUpdateQualifier() throws Exception {
        createThreeArtistsTwoPaintings();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.paintingTitle is NULL or p.paintingTitle <> 'XX'");

        Object notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(2L, notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.paintingTitle = 'XX' WHERE p.paintingTitle = 'P1'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(1, count[0]);

        notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(1L, notUpdated);
    }

    @Test
    public void testUpdateNoQualifierString() throws Exception {
        createThreeArtistsTwoPaintings();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.paintingTitle is NULL or p.paintingTitle <> 'XX'");

        Object notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(2L, notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.paintingTitle = 'XX'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(0L, notUpdated);
    }

    @Test
    public void testUpdateNoQualifierNull() throws Exception {
        createThreeArtistsTwoPaintings();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.estimatedPrice is not null");

        Object notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(2L, notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.estimatedPrice = NULL";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(0L, notUpdated);
    }

    // This fails until we implement arithmetic exps

    // public void testUpdateNoQualifierArithmeticExpression() throws Exception {
    // createThreeArtistsTwoPaintings();
    //
    //
    // EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
    // + "WHERE p.paintingTitle is NULL or p.estimatedPrice <= 5000");
    //
    // Object notUpdated = Cayenne.objectForQuery(context, check);
    // assertEquals(2L, notUpdated);
    //
    // String ejbql = "UPDATE Painting AS p SET p.estimatedPrice = p.estimatedPrice * 2";
    // EJBQLQuery query = new EJBQLQuery(ejbql);
    //
    // QueryResponse result = context.performGenericQuery(query);
    //
    // int[] count = result.firstUpdateCount();
    // assertNotNull(count);
    // assertEquals(1, count.length);
    // assertEquals(2, count[0]);
    //
    // notUpdated = Cayenne.objectForQuery(context, check);
    // assertEquals(0L, notUpdated);
    // }

    @Test
    public void testUpdateNoQualifierMultipleItems() throws Exception {
        createThreeArtistsTwoPaintings();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.estimatedPrice is NULL or p.estimatedPrice <> 1");

        Object notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(2L, notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.paintingTitle = 'XX', p.estimatedPrice = 1";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(0L, notUpdated);
    }

    @Test
    public void testUpdateNoQualifierDecimal() throws Exception {
        createThreeArtistsTwoPaintings();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.estimatedPrice is NULL or p.estimatedPrice <> 1.1");

        Object notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(2L, notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.estimatedPrice = 1.1";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(0L, notUpdated);
    }

    @Test
    public void testUpdateNoQualifierToOne() throws Exception {
        createThreeArtistsTwoPaintings();

        Artist object = Cayenne.objectForPK(context, Artist.class, 33003);

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.toArtist <> :artist");
        check.setParameter("artist", object);

        Object notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(2L, notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.toArtist = :artist";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("artist", object);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(0L, notUpdated);
    }

}
