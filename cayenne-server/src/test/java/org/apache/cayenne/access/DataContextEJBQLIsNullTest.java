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

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextEJBQLIsNullTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

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

    private void createTwoPaintings() throws Exception {
        tPainting.insert(33001, null, "A", null);
        tPainting.insert(33002, null, "B", 2000);
    }

    private void createTwoPaintingsAndOneArtist() throws Exception {
        tArtist.insert(33001, "A");
        tPainting.insert(33001, null, "A", null);
        tPainting.insert(33003, 33001, "C", 500);
    }

    public void testCompareToNull() throws Exception {

        // the query below can blow up on FrontBase. See CAY-819 for details.
        if (!accessStackAdapter.supportsEqualNullSyntax()) {
            return;
        }

        createTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice = :x";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);
        query1.setParameter("x", null);

        // unlike SelectQuery or SQLTemplate, EJBQL nulls are handled just like SQL.

        // note that some databases (notably Sybase) actually allow = NULL comparison,
        // most do not; per JPA spec the result is undefined.. so we can't make any
        // assertions about the result. Just making sure the query doesn't blow up
        context.performQuery(query1);
    }

    public void testCompareToNull2() throws Exception {

        if (!accessStackAdapter.supportsEqualNullSyntax()) {
            return;
        }

        createTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.toArtist.artistName = :x";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);
        query1.setParameter("x", null);

        context.performQuery(query1);
    }

    public void testCompareToNull3() throws Exception {
        if (!accessStackAdapter.supportsEqualNullSyntax()) {
            return;
        }

        createTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE :x = p.toArtist.artistName";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);
        query1.setParameter("x", null);

        context.performQuery(query1);
    }

    public void testIsNull() throws Exception {

        createTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice IS NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results = context.performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33001, Cayenne.intPKForObject((Persistent) results.get(0)));
    }

    public void testIsNotNull() throws Exception {

        createTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice IS NOT NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results = context.performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results.get(0)));
    }

    public void testToOneIsNull() throws Exception {

        createTwoPaintingsAndOneArtist();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.toArtist IS NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results = context.performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33001, Cayenne.intPKForObject((Persistent) results.get(0)));
    }

    public void testToOneIsNotNull() throws Exception {

        createTwoPaintingsAndOneArtist();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.toArtist IS NOT NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results = context.performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33003, Cayenne.intPKForObject((Persistent) results.get(0)));
    }
}
