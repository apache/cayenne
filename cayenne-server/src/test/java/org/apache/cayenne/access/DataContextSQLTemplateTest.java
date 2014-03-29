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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.CompoundFkTestEntity;
import org.apache.cayenne.testdo.testmap.CompoundPkTestEntity;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextSQLTemplateTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;
    
    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected SQLTemplateCustomizer sqlTemplateCustomizer;

    protected TableHelper tPainting;
    protected TableHelper tArtist;
    protected TableHelper tCompoundPkTest;
    protected TableHelper tCompoundFkTest;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("COMPOUND_FK_TEST");
        dbHelper.deleteAll("COMPOUND_PK_TEST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.VARCHAR,
                Types.BIGINT,
                Types.DECIMAL);

        tCompoundPkTest = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPkTest.setColumns("KEY1", "KEY2");

        tCompoundFkTest = new TableHelper(dbHelper, "COMPOUND_FK_TEST");
        tCompoundFkTest.setColumns("PKEY", "F_KEY1", "F_KEY2");
    }

    protected void createFourArtists() throws Exception {
        tArtist.insert(11, "artist2");
        tArtist.insert(101, "artist3");
        tArtist.insert(201, "artist4");
        tArtist.insert(3001, "artist5");
    }

    protected void createFourArtistsAndThreePaintingsDataSet() throws Exception {
        createFourArtists();

        tPainting.insert(6, "p_artist3", 11, 1000);
        tPainting.insert(7, "p_artist2", 101, 2000);
        tPainting.insert(8, "p_artist4", null, 3000);
    }

    protected void createTwoCompoundPKsAndCompoundFKsDataSet() throws Exception {
        tCompoundPkTest.insert("a1", "a2");
        tCompoundPkTest.insert("b1", "b2");

        tCompoundFkTest.insert(6, "a1", "a2");
        tCompoundFkTest.insert(7, "b1", "b2");
    }

    public void testSQLResultSetMappingMixed() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        String sql = "SELECT #result('t0.ARTIST_ID' 'long' 'X'), #result('t0.ARTIST_NAME' 'String' 'Y'), #result('t0.DATE_OF_BIRTH' 'Date' 'Z'), #result('count(t1.PAINTING_ID)' 'int' 'C') "
                + "FROM ARTIST t0 LEFT JOIN PAINTING t1 ON (t0.ARTIST_ID = t1.ARTIST_ID) "
                + "GROUP BY t0.ARTIST_ID, t0.ARTIST_NAME, t0.DATE_OF_BIRTH "
                + "ORDER BY t0.ARTIST_ID";

        DataMap map = context.getEntityResolver().getDataMap("tstmap");
        SQLTemplate query = new SQLTemplate(map, sql, false);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);

        EntityResult artistResult = new EntityResult(Artist.class);
        artistResult.addDbField(Artist.ARTIST_ID_PK_COLUMN, "X");
        artistResult.addObjectField(Artist.ARTIST_NAME_PROPERTY, "Y");
        artistResult.addObjectField(Artist.DATE_OF_BIRTH_PROPERTY, "Z");

        SQLResult rsMap = new SQLResult();
        rsMap.addEntityResult(artistResult);
        rsMap.addColumnResult("C");
        query.setResult(rsMap);

        List<?> objects = context.performQuery(query);
        assertEquals(4, objects.size());

        Object o1 = objects.get(0);
        assertTrue("Expected Object[]: " + o1, o1 instanceof Object[]);
        Object[] array1 = (Object[]) o1;
        assertEquals(2, array1.length);
        Object[] array2 = (Object[]) objects.get(1);
        assertEquals(2, array2.length);
        Object[] array3 = (Object[]) objects.get(2);
        assertEquals(2, array3.length);
        Object[] array4 = (Object[]) objects.get(3);
        assertEquals(2, array3.length);

        assertEquals(new Integer(1), array1[1]);
        assertEquals(new Integer(1), array2[1]);
        assertEquals(new Integer(0), array3[1]);
        assertEquals(new Integer(0), array4[1]);
        assertTrue("Unexpected DataObject: " + array1[0], array1[0] instanceof Artist);
    }
    
    public void testRootless_DataNodeName() throws Exception {
        createFourArtists();
        
        SQLTemplate query = new SQLTemplate("SELECT * FROM ARTIST", true);
        query.setDataNodeName("tstmap");
        assertEquals(4, context.performQuery(query).size());
    }
    
    public void testRootless_DefaultDataNode() throws Exception {
        createFourArtists();
        SQLTemplate query = new SQLTemplate("SELECT * FROM ARTIST", true);
        assertEquals(4, context.performQuery(query).size());
    }

    public void testSQLResultSetMappingScalar() throws Exception {
        createFourArtists();

        String sql = "SELECT count(1) AS X FROM ARTIST";

        DataMap map = context.getEntityResolver().getDataMap("tstmap");
        SQLTemplate query = new SQLTemplate(map, sql, false);
        query.setTemplate(
                FrontBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) X FROM ARTIST");
        query.setTemplate(
                OpenBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) X FROM ARTIST");
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);

        SQLResult rsMap = new SQLResult();
        rsMap.addColumnResult("X");
        query.setResult(rsMap);

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Object o = objects.get(0);
        assertTrue("Expected Number: " + o, o instanceof Number);
        assertEquals(4, ((Number) o).intValue());
    }

    public void testSQLResultSetMappingScalarArray() throws Exception {
        createFourArtists();

        String sql = "SELECT count(1) AS X, 77 AS Y FROM ARTIST";

        DataMap map = context.getEntityResolver().getDataMap("tstmap");
        SQLTemplate query = new SQLTemplate(map, sql, false);
        query.setTemplate(
                FrontBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) X, 77 Y FROM ARTIST GROUP BY Y");
        query.setTemplate(
                OpenBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) X, 77 Y FROM ARTIST GROUP BY 77");
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);

        SQLResult rsMap = new SQLResult();
        rsMap.addColumnResult("X");
        rsMap.addColumnResult("Y");
        query.setResult(rsMap);

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Object o = objects.get(0);
        assertTrue(o instanceof Object[]);

        Object[] row = (Object[]) o;
        assertEquals(2, row.length);

        assertEquals(4, ((Number) row[0]).intValue());
        assertEquals(77, ((Number) row[1]).intValue());
    }

    public void testColumnNamesCapitalization() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Artist.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.LOWER);
        query.setFetchingDataRows(true);

        List<DataRow> rows = context.performQuery(query);

        DataRow row1 = rows.get(0);
        assertFalse(row1.containsKey("ARTIST_ID"));
        assertTrue(row1.containsKey("artist_id"));

        DataRow row2 = rows.get(1);
        assertFalse(row2.containsKey("ARTIST_ID"));
        assertTrue(row2.containsKey("artist_id"));

        query.setColumnNamesCapitalization(CapsStrategy.UPPER);

        List<DataRow> rowsUpper = context.performQuery(query);

        DataRow row3 = rowsUpper.get(0);
        assertFalse(row3.containsKey("artist_id"));
        assertTrue(row3.containsKey("ARTIST_ID"));

        DataRow row4 = rowsUpper.get(1);
        assertFalse(row4.containsKey("artist_id"));
        assertTrue(row4.containsKey("ARTIST_ID"));
    }

    public void testFetchDataRows() throws Exception {
        createFourArtists();

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Artist.class, template);

        sqlTemplateCustomizer.updateSQLTemplate(query);

        query.setFetchingDataRows(true);

        List<DataRow> rows = context.performQuery(query);
        assertEquals(4, rows.size());

        DataRow row2 = rows.get(1);
        assertEquals(3, row2.size());
        Object id = row2.get("ARTIST_ID");
        assertEquals(new Integer(101), new Integer(id.toString()));
    }

    public void testFetchObjects() throws Exception {
        createFourArtists();

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = sqlTemplateCustomizer.createSQLTemplate(
                Artist.class,
                template);

        query.setFetchingDataRows(false);

        List<?> objects = context.performQuery(query);
        assertEquals(4, objects.size());
        assertTrue(objects.get(1) instanceof Artist);

        Artist artist2 = (Artist) objects.get(1);
        assertEquals("artist3", artist2.getArtistName());
    }

    public void testBindObjectEqualShort() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        Artist a = Cayenne.objectForPK(context, Artist.class, 101);

        String template = "SELECT * FROM PAINTING "
                + "WHERE #bindObjectEqual($a) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(7, Cayenne.intPKForObject(p));
    }

    public void testBindObjectNotEqualShort() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        Artist a = Cayenne.objectForPK(context, Artist.class, 101);

        String template = "SELECT * FROM PAINTING "
                + "WHERE #bindObjectNotEqual($a) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List<?> objects = context.performQuery(query);

        // null comparison is unpredictable across DB's ... some would return true on null
        // <> value, some - false
        assertTrue(objects.size() == 1 || objects.size() == 2);

        Painting p = (Painting) objects.get(0);
        assertEquals(6, Cayenne.intPKForObject(p));
    }

    public void testBindObjectEqualFull() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        Artist a = Cayenne.objectForPK(context, Artist.class, 101);

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(7, Cayenne.intPKForObject(p));
    }

    public void testBindObjectEqualFullNonArray() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        Artist a = Cayenne.objectForPK(context, Artist.class, 101);

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectEqual($a 't0.ARTIST_ID' 'ARTIST_ID' ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(7, Cayenne.intPKForObject(p));
    }

    public void testBindObjectEqualNull() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", null));

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(8, Cayenne.intPKForObject(p));
    }

    public void testBindObjectNotEqualFull() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        Artist a = Cayenne.objectForPK(context, Artist.class, 101);

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List<?> objects = context.performQuery(query);
        // null comparison is unpredictable across DB's ... some would return true on null
        // <> value, some - false
        assertTrue(objects.size() == 1 || objects.size() == 2);

        Painting p = (Painting) objects.get(0);
        assertEquals(6, Cayenne.intPKForObject(p));
    }

    public void testBindObjectEqualCompound() throws Exception {
        createTwoCompoundPKsAndCompoundFKsDataSet();

        Map<String, String> pk = new HashMap<String, String>();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List<CompoundFkTestEntity> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = objects.get(0);
        assertEquals(6, Cayenne.intPKForObject(p));
    }

    public void testBindObjectNotEqualCompound() throws Exception {
        createTwoCompoundPKsAndCompoundFKsDataSet();

        Map<String, String> pk = new HashMap<String, String>();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List<CompoundFkTestEntity> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = objects.get(0);
        assertEquals(7, Cayenne.intPKForObject(p));
    }

    public void testBindObjectNotEqualNull() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", null));

        List<Painting> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Painting p1 = objects.get(0);
        assertEquals(6, Cayenne.intPKForObject(p1));

        Painting p2 = objects.get(1);
        assertEquals(7, Cayenne.intPKForObject(p2));
    }

    public void testBindEqualNull() throws Exception {
        createFourArtistsAndThreePaintingsDataSet();

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE t0.ARTIST_ID #bindEqual($id) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("id", null));

        List<Painting> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = objects.get(0);
        assertEquals(8, Cayenne.intPKForObject(p));
    }

    public void testFetchLimit() throws Exception {
        createFourArtists();

        int fetchLimit = 2;

        // sanity check
        assertTrue(fetchLimit < 4);
        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = sqlTemplateCustomizer.createSQLTemplate(
                Artist.class,
                template);
        query.setFetchLimit(fetchLimit);

        List<?> objects = context.performQuery(query);
        assertEquals(fetchLimit, objects.size());
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testFetchOffset() throws Exception {
        createFourArtists();

        int fetchOffset = 2;

        // sanity check
        assertTrue(fetchOffset < 4);
        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = sqlTemplateCustomizer.createSQLTemplate(
                Artist.class,
                template);
        query.setFetchOffset(fetchOffset);

        List<?> objects = context.performQuery(query);
        assertEquals(4 - fetchOffset, objects.size());
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testFetchOffsetFetchLimit() throws Exception {
        createFourArtists();

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = sqlTemplateCustomizer.createSQLTemplate(
                Artist.class,
                template);
        query.setFetchOffset(1);
        query.setFetchLimit(2);

        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testPageSize() throws Exception {
        createFourArtists();

        int pageSize = 3;

        // sanity check
        assertTrue(pageSize < 4);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = sqlTemplateCustomizer.createSQLTemplate(
                Artist.class,
                template);

        query.setPageSize(pageSize);

        List<?> objects = context.performQuery(query);

        assertEquals(4, objects.size());
        assertTrue(objects.get(0) instanceof Artist);

        assertTrue(objects instanceof IncrementalFaultList<?>);
        IncrementalFaultList<?> pagedList = (IncrementalFaultList<?>) objects;
        assertEquals(4 - pageSize, pagedList.getUnfetchedObjects());

        // check if we can resolve subsequent pages
        Artist artist = (Artist) objects.get(pageSize);

        int expectUnresolved = 4 - pageSize - pageSize;
        if (expectUnresolved < 0) {
            expectUnresolved = 0;
        }
        assertEquals(expectUnresolved, pagedList.getUnfetchedObjects());
        assertEquals("artist" + (pageSize + 2), artist.getArtistName());
    }

    public void testIteratedQuery() throws Exception {
        createFourArtists();

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = sqlTemplateCustomizer.createSQLTemplate(
                Artist.class,
                template);

        ResultIterator it = context.performIteratedQuery(query);

        try {
            long i = 0;

            while (it.hasNextRow()) {
                i++;

                DataRow row = (DataRow) it.nextRow();
                assertEquals(3, row.size());
                assertEquals("artist" + (1 + i), row.get("ARTIST_NAME"));
            }

            assertEquals(4, i);
        }
        finally {
            it.close();
        }
    }

    public void testQueryWithLineBreakAfterMacro() throws Exception {
        createFourArtists();

        // see CAY-726 for details
        String template = "SELECT #result('count(*)' 'int' 'X')"
                + System.getProperty("line.separator")
                + "FROM ARTIST";
        SQLTemplate query = sqlTemplateCustomizer.createSQLTemplate(
                Artist.class,
                template);
        query.setFetchingDataRows(true);

        List<?> result = context.performQuery(query);

        assertEquals(4, ((DataRow) result.get(0)).get("X"));
    }
}
