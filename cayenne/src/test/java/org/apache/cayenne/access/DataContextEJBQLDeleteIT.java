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
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextEJBQLDeleteIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected CayenneRuntime runtime;

    protected TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
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

    protected void createPaintingsDataSet() throws Exception {
        tPainting.insert(33001, null, "P1", 3000);
        tPainting.insert(33002, null, "P2", 5000);
    }

    @Test
    public void testDeleteNoIdVar() throws Exception {
        createPaintingsDataSet();

        String ejbql = "delete from Painting";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);
    }

    @Test
    public void testDeleteNoQualifier() throws Exception {
        createPaintingsDataSet();

        String ejbql = "delete from Painting AS p";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);
    }

    @Test
    public void testDeleteSameEntityQualifier() throws Exception {
        createPaintingsDataSet();

        String ejbql = "delete from Painting AS p WHERE p.paintingTitle = 'P2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(1, count[0]);

        ObjectContext freshContext = runtime.newContext();

        assertNotNull(Cayenne.objectForPK(freshContext, Painting.class, 33001));
        assertNull(Cayenne.objectForPK(freshContext, Painting.class, 33002));
    }

}
