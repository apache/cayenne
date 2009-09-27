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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.CompoundFkTestEntity;
import org.apache.art.CompoundPkTestEntity;
import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DataContextSQLTemplateTest extends CayenneCase {

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }

    public void testSQLResultSetMappingMixed() throws Exception {
        createTestData("prepare");

        String sql = "SELECT #result('t0.ARTIST_ID' 'long' 'X'), #result('t0.ARTIST_NAME' 'String' 'Y'), #result('t0.DATE_OF_BIRTH' 'Date' 'Z'), #result('count(t1.PAINTING_ID)' 'int' 'C') "
                + "FROM ARTIST t0 LEFT JOIN PAINTING t1 ON (t0.ARTIST_ID = t1.ARTIST_ID) "
                + "GROUP BY t0.ARTIST_ID, t0.ARTIST_NAME, t0.DATE_OF_BIRTH "
                + "ORDER BY t0.ARTIST_ID";

        DataMap map = getDomain().getMap("testmap");
        SQLTemplate query = new SQLTemplate(map, sql);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);

        EntityResult artistResult = new EntityResult(Artist.class);
        artistResult.addDbField(Artist.ARTIST_ID_PK_COLUMN, "X");
        artistResult.addObjectField(Artist.ARTIST_NAME_PROPERTY, "Y");
        artistResult.addObjectField(Artist.DATE_OF_BIRTH_PROPERTY, "Z");

        SQLResult rsMap = new SQLResult();
        rsMap.addEntityResult(artistResult);
        rsMap.addColumnResult("C");
        query.setResult(rsMap);

        List objects = createDataContext().performQuery(query);
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

    public void testSQLResultSetMappingScalar() throws Exception {
        createTestData("testSQLResultSetMappingScalar");

        String sql = "SELECT count(1) AS X FROM ARTIST";

        DataMap map = getDomain().getMap("testmap");
        SQLTemplate query = new SQLTemplate(map, sql);
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

        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Object o = objects.get(0);
        assertTrue("Expected Number: " + o, o instanceof Number);
        assertEquals(4, ((Number) o).intValue());
    }

    public void testSQLResultSetMappingScalarArray() throws Exception {
        createTestData("testSQLResultSetMappingScalar");

        String sql = "SELECT count(1) AS X, 77 AS Y FROM ARTIST";

        DataMap map = getDomain().getMap("testmap");
        SQLTemplate query = new SQLTemplate(map, sql);
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

        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Object o = objects.get(0);
        assertTrue(o instanceof Object[]);

        Object[] row = (Object[]) o;
        assertEquals(2, row.length);

        assertEquals(4, ((Number) row[0]).intValue());
        assertEquals(77, ((Number) row[1]).intValue());
    }

    public void testColumnNamesCapitalization() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Artist.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.LOWER);
        query.setFetchingDataRows(true);

        List rows = context.performQuery(query);

        DataRow row1 = (DataRow) rows.get(0);
        assertFalse(row1.containsKey("ARTIST_ID"));
        assertTrue(row1.containsKey("artist_id"));

        DataRow row2 = (DataRow) rows.get(1);
        assertFalse(row2.containsKey("ARTIST_ID"));
        assertTrue(row2.containsKey("artist_id"));

        query.setColumnNamesCapitalization(CapsStrategy.UPPER);

        List rowsUpper = context.performQuery(query);

        DataRow row3 = (DataRow) rowsUpper.get(0);
        assertFalse(row3.containsKey("artist_id"));
        assertTrue(row3.containsKey("ARTIST_ID"));

        DataRow row4 = (DataRow) rowsUpper.get(1);
        assertFalse(row4.containsKey("artist_id"));
        assertTrue(row4.containsKey("ARTIST_ID"));
    }

    public void testFetchDataRows() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = new SQLTemplate(Artist.class, template);

        getSQLTemplateBuilder().updateSQLTemplate(query);

        query.setFetchingDataRows(true);

        List rows = context.performQuery(query);
        assertEquals(DataContextCase.artistCount, rows.size());
        assertTrue(
                "Expected DataRow, got this: " + rows.get(1),
                rows.get(1) instanceof DataRow);

        DataRow row2 = (DataRow) rows.get(1);
        assertEquals(3, row2.size());
        Object id = row2.get("ARTIST_ID");
        assertEquals(new Integer(33002), new Integer(id.toString()));
    }

    public void testFetchObjects() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);

        query.setFetchingDataRows(false);

        List objects = context.performQuery(query);
        assertEquals(DataContextCase.artistCount, objects.size());
        assertTrue(objects.get(1) instanceof Artist);

        Artist artist2 = (Artist) objects.get(1);
        assertEquals("artist2", artist2.getArtistName());
    }

    public void testBindObjectEqualShort() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING "
                + "WHERE #bindObjectEqual($a) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectNotEqualShort() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING "
                + "WHERE #bindObjectNotEqual($a) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);

        // null comparison is unpredictable across DB's ... some would return true on null
        // <> value, some - false
        assertTrue(objects.size() == 1 || objects.size() == 2);

        Painting p = (Painting) objects.get(0);
        assertEquals(33001, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectEqualFull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectEqualFullNonArray() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectEqual($a 't0.ARTIST_ID' 'ARTIST_ID' ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectEqualNull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", null));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(33003, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectNotEqualFull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        // null comparison is unpredictable across DB's ... some would return true on null
        // <> value, some - false
        assertTrue(objects.size() == 1 || objects.size() == 2);

        Painting p = (Painting) objects.get(0);
        assertEquals(33001, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectEqualCompound() throws Exception {
        createTestData("testBindObjectEqualCompound");

        ObjectContext context = createDataContext();

        Map<String, String> pk = new HashMap<String, String>();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = DataObjectUtils.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = (CompoundFkTestEntity) objects.get(0);
        assertEquals(33001, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectNotEqualCompound() throws Exception {
        createTestData("testBindObjectEqualCompound");

        ObjectContext context = createDataContext();

        Map<String, String> pk = new HashMap<String, String>();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = DataObjectUtils.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", a));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = (CompoundFkTestEntity) objects.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testBindObjectNotEqualNull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.ARTIST_ID' ] [ 'ARTIST_ID' ] ) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("a", null));

        List objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Painting p1 = (Painting) objects.get(0);
        assertEquals(33001, DataObjectUtils.intPKForObject(p1));

        Painting p2 = (Painting) objects.get(1);
        assertEquals(33002, DataObjectUtils.intPKForObject(p2));
    }
    
    public void testBindEqualNull() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        String template = "SELECT * FROM PAINTING t0"
                + " WHERE t0.ARTIST_ID #bindEqual($id) ORDER BY PAINTING_ID";
        SQLTemplate query = new SQLTemplate(Painting.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParameters(Collections.singletonMap("id", null));

        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p = (Painting) objects.get(0);
        assertEquals(33003, DataObjectUtils.intPKForObject(p));
    }

    public void testFetchLimit() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        int fetchLimit = 3;

        // sanity check
        assertTrue(fetchLimit < DataContextCase.artistCount);
        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);
        query.setFetchLimit(fetchLimit);

        List objects = context.performQuery(query);
        assertEquals(fetchLimit, objects.size());
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testFetchOffset() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        int fetchOffset = 3;

        // sanity check
        assertTrue(fetchOffset < DataContextCase.artistCount);
        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);
        query.setFetchOffset(fetchOffset);

        List objects = context.performQuery(query);
        assertEquals(DataContextCase.artistCount - fetchOffset, objects.size());
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testFetchOffsetFetchLimit() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);
        query.setFetchOffset(1);
        query.setFetchLimit(2);

        List objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testPageSize() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        int pageSize = 3;

        // sanity check
        assertTrue(pageSize < DataContextCase.artistCount);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);

        query.setPageSize(pageSize);

        List objects = context.performQuery(query);
        
        assertEquals(DataContextCase.artistCount, objects.size());
        assertTrue(objects.get(0) instanceof Artist);

        assertTrue(objects instanceof IncrementalFaultList);
        IncrementalFaultList pagedList = (IncrementalFaultList) objects;
        assertEquals(DataContextCase.artistCount - pageSize, pagedList
                .getUnfetchedObjects());

        // check if we can resolve subsequent pages
        Artist artist = (Artist) objects.get(pageSize);

        int expectUnresolved = DataContextCase.artistCount - pageSize - pageSize;
        if (expectUnresolved < 0) {
            expectUnresolved = 0;
        }
        assertEquals(expectUnresolved, pagedList.getUnfetchedObjects());
        assertEquals("artist" + (pageSize + 1), artist.getArtistName());
    }

    public void testIteratedQuery() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String template = "SELECT * FROM ARTIST ORDER BY ARTIST_ID";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);

        ResultIterator it = context.performIteratedQuery(query);

        try {
            long i = 0;

            while (it.hasNextRow()) {
                i++;

                DataRow row = (DataRow) it.nextRow();
                assertEquals(3, row.size());
                Object id = row.get("ARTIST_ID");
                assertEquals(new Integer((int) (33000 + i)), new Integer(id.toString()));
            }

            assertEquals(DataContextCase.artistCount, i);
        }
        finally {
            it.close();
        }
    }

    public void testQueryWithLineBreakAfterMacroCAY726() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        // see CAY-726 for details
        String template = "SELECT #result('count(*)' 'int' 'X')"
                + System.getProperty("line.separator")
                + "FROM ARTIST";
        SQLTemplate query = getSQLTemplateBuilder().createSQLTemplate(
                Artist.class,
                template);
        query.setFetchingDataRows(true);

        List result = context.performQuery(query);

        assertEquals(new Integer(25), ((Map) result.get(0)).get("X"));
    }
}
