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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextEJBQLArrayResultIT extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Before
    public void setUp() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
        tArtist.insert(33001, "AA1");
        tArtist.insert(33002, "AA2");
        tArtist.insert(33003, "BB1");
        tArtist.insert(33004, "BB2");

        TableHelper tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "ARTIST_ID",
                "PAINTING_TITLE",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.BIGINT,
                Types.VARCHAR,
                Types.DECIMAL);
        tPainting.insert(33001, 33001, "P1", 3000);
        tPainting.insert(33002, 33002, "P2", 5000);
        tPainting.insert(33003, null, "P3", 5000);
    }

    @Test
    public void testSQLResultSetMappingScalar() throws Exception {

        String ejbql = "SELECT count(p) FROM Painting p JOIN p.toArtist a";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Object o1 = objects.get(0);
        assertEquals(2L, o1);
    }

    @Test
    public void testSQLResultSetMappingScalars() throws Exception {

        String ejbql = "SELECT count(p), sum(p.estimatedPrice) FROM Painting p JOIN p.toArtist a";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Object o1 = objects.get(0);
        assertTrue("Expected Object[]: " + o1, o1 instanceof Object[]);
        Object[] array1 = (Object[]) o1;
        assertEquals(2, array1.length);

        assertEquals(2L, array1[0]);
        assertEquals(0, new BigDecimal(8000).compareTo((BigDecimal) array1[1]));
    }

    @Test
    public void testSQLResultSetMappingMixed() throws Exception {

        String ejbql = "SELECT count(p), a, sum(p.estimatedPrice) "
                + "FROM Artist a LEFT JOIN a.paintingArray p "
                + "GROUP BY a ORDER BY a.artistName";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> objects = context.performQuery(query);
        assertEquals(4, objects.size());

        Object o1 = objects.get(0);
        assertTrue("Expected Object[]: " + o1, o1 instanceof Object[]);
        Object[] array1 = (Object[]) o1;
        assertEquals(3, array1.length);

        assertEquals(1L, array1[0]);
        assertTrue("Expected Artist, got: " + array1[1], array1[1] instanceof Artist);
        assertEquals(0, new BigDecimal(3000).compareTo((BigDecimal) array1[2]));
    }

}
