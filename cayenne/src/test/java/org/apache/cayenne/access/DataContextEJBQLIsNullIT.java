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
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataContextEJBQLIsNullIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private UnitDbAdapter accessStackAdapter;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    
    @BeforeEach
    public void setUp() throws Exception {
        accessStackAdapter = env.unitDbAdapter();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");

        tPainting = env.table("PAINTING").setColumns(
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

    @Test
    public void compareToNull() throws Exception {

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
        env.context().performQuery(query1);
    }

    @Test
    public void compareToNull2() throws Exception {

        if (!accessStackAdapter.supportsEqualNullSyntax()) {
            return;
        }

        createTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.toArtist.artistName = :x";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);
        query1.setParameter("x", null);

        env.context().performQuery(query1);
    }

    @Test
    public void compareToNull3() throws Exception {
        if (!accessStackAdapter.supportsEqualNullSyntax()) {
            return;
        }

        createTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE :x = p.toArtist.artistName";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);
        query1.setParameter("x", null);

        env.context().performQuery(query1);
    }

    @Test
    public void isNull() throws Exception {

        createTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice IS NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results = env.context().performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33001, Cayenne.intPKForObject((Persistent) results.get(0)));
    }

    @Test
    public void isNotNull() throws Exception {

        createTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice IS NOT NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results = env.context().performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results.get(0)));
    }

    @Test
    public void toOneIsNull() throws Exception {

        createTwoPaintingsAndOneArtist();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.toArtist IS NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results = env.context().performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33001, Cayenne.intPKForObject((Persistent) results.get(0)));
    }

    @Test
    public void toOneIsNotNull() throws Exception {

        createTwoPaintingsAndOneArtist();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.toArtist IS NOT NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results = env.context().performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33003, Cayenne.intPKForObject((Persistent) results.get(0)));
    }
}
