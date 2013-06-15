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
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextEJBQLDeleteTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected ServerRuntime runtime;

    protected TableHelper tPainting;

    protected TableHelper tMeaningfulPKTest1Table;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("MEANINGFUL_PK_DEP");
        dbHelper.deleteAll("MEANINGFUL_PK_TEST1");

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

        tMeaningfulPKTest1Table = new TableHelper(dbHelper, "MEANINGFUL_PK_TEST1");
        tMeaningfulPKTest1Table.setColumns("PK_ATTRIBUTE", "DESCR");
    }

    protected void createPaintingsDataSet() throws Exception {
        tPainting.insert(33001, null, "P1", 3000);
        tPainting.insert(33002, null, "P2", 5000);
    }

    protected void createMeaningfulPKDataSet() throws Exception {

        for (int i = 1; i <= 33; i++) {
            tMeaningfulPKTest1Table.insert(i, "a" + i);
        }
    }

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

    public void testDeleteIdVar() throws Exception {

        createMeaningfulPKDataSet();

        EJBQLQuery q = new EJBQLQuery("select m.pkAttribute from MeaningfulPKTest1 m");

        List<Integer> id = context.performQuery(q);

        String ejbql = "delete from MeaningfulPKTest1 m WHERE m.pkAttribute in (:id)";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("id", id);
        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(33, count[0]);
    }
}
