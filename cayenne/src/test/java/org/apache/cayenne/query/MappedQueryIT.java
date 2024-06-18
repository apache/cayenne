/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/
package org.apache.cayenne.query;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
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
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class MappedQueryIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private JdbcEventLogger jdbcEventLogger;

    protected void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        long dateBase = System.currentTimeMillis();

        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
        }
    }

    @Test
    public void testSelectQuery() throws Exception {
        createArtistsDataSet();

        Artist a = MappedSelect.query("ParameterizedQueryWithLocalCache", Artist.class)
                .param("name", "artist14").select(context).get(0);
        assertNotNull(a);
        assertEquals("artist14", a.getArtistName());
    }

    @Test
    public void testButchIterator() throws Exception {
        createArtistsDataSet();

        try (ResultBatchIterator<Artist> iterator = MappedSelect
                .query("ParameterizedQueryWithSharedCache", Artist.class)
                .param("name", "artist14")
                .batchIterator(context, 1)) {
            int count = 0;
            while (iterator.hasNext()) {
                count++;
                List<Artist> artists = iterator.next();
                for (Artist artist : artists) {
                    //noinspection ConstantConditions
                    assertTrue(artist instanceof Artist);
                    assertEquals("artist14", artist.readPropertyDirectly("artistName"));
                }
            }
            assertEquals(1,count);
        }

    }

    @Test
    public void testSQLTemplateSelect() throws Exception {
        createArtistsDataSet();

        List<DataRow> result = MappedSelect.query("SelectTestLower", DataRow.class).select(context);
        assertEquals(20, result.size());
        assertThat(result.get(0), instanceOf(DataRow.class));
    }

    @Test
    public void testSQLTemplateUpdate() throws Exception {
        int updated = MappedExec.query("NonSelectingQuery").update(context)[0];

        assertEquals(1, updated);

        Painting painting = ObjectSelect.query(Painting.class).selectOne(context);
        assertEquals("No Painting Like This", painting.getPaintingTitle());
        assertEquals(12.5, painting.getEstimatedPrice().doubleValue(), 0);
    }

    @Test
    public void testProcedureQuery() throws Exception {
        if (!accessStackAdapter.supportsStoredProcedures()) {
            return;
        }

        if (!accessStackAdapter.canMakeObjectsOutOfProcedures()) {
            return;
        }

        // create an artist with painting in the database
        Artist a = context.newObject(Artist.class);
        a.setArtistName("An Artist");

        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("A Painting");
        // converting double to string prevents rounding weirdness...
        p.setEstimatedPrice(new BigDecimal(1000));
        a.addToPaintingArray(p);

        context.commitChanges();

        List<?> artists = runProcedureSelect(MappedSelect.query("ProcedureQuery", Artist.class)
                .param("aName", "An Artist")
                .param("paintingPrice", 3000).forceNoCache()).firstList();

        // check the results
        assertNotNull("Null result from StoredProcedure.", artists);
        assertEquals(1, artists.size());
        Artist artistRow = (Artist) artists.get(0);
        assertEquals("An Artist", artistRow.getArtistName());
    }

    @Test
    public void testEJBQLQuery() throws Exception {
        createArtistsDataSet();

        List result = MappedSelect.query("EjbqlQueryTest").select(context);
        assertEquals(20, result.size());
        assertThat(result.get(0), instanceOf(DataRow.class));
    }

    @Test
    public void testCacheKey() {
        // ensure queries initialized with different parameters receive different cache keys
        MappedSelect<Artist> query1 = MappedSelect
                .query("ParameterizedQueryWithLocalCache", Artist.class).param("name", "artist1");
        MappedSelect<Artist> query2 = MappedSelect
                .query("ParameterizedQueryWithLocalCache", Artist.class).param("name", "artist2");
        MappedSelect<Artist> query3 = MappedSelect
                .query("ParameterizedQueryWithLocalCache", Artist.class).param("name", "artist2");

        assertNotEquals(
                query1.getMetaData(context.getEntityResolver()).getCacheKey(),
                query2.getMetaData(context.getEntityResolver()).getCacheKey());
        assertEquals(
                query2.getMetaData(context.getEntityResolver()).getCacheKey(),
                query3.getMetaData(context.getEntityResolver()).getCacheKey());
    }

    protected QueryResponse runProcedureSelect(AbstractMappedQuery q) throws Exception {
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
            return context.performGenericQuery(q);
        } finally {
            BaseTransaction.bindThreadTransaction(null);
            t.commit();
        }
    }
}
