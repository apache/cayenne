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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLGroupByHavingTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testGroupBy() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.estimatedPrice, count(p) FROM Painting p"
                + " GROUP BY p.estimatedPrice"
                + " ORDER BY p.estimatedPrice";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(2, data.size());
        assertTrue(data.get(0) instanceof Object[]);

        Object[] row0 = (Object[]) data.get(0);
        assertEquals(new BigDecimal(1d), row0[0], 0.001d);
        assertEquals(new Long(3), row0[1]);

        Object[] row1 = (Object[]) data.get(1);
        assertEquals(new BigDecimal(2d), row1[0], 0.001d);
        assertEquals(new Long(2l), row1[1]);
    }

    public void testGroupByMultipleItems() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.estimatedPrice, p.paintingTitle, count(p) FROM Painting p"
                + " GROUP BY p.estimatedPrice, p.paintingTitle"
                + " ORDER BY p.estimatedPrice, p.paintingTitle";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(3, data.size());
        assertTrue(data.get(0) instanceof Object[]);

        Object[] row0 = (Object[]) data.get(0);
        assertEquals(new BigDecimal(1d), row0[0], 0.001d);
        assertEquals("PX", row0[1]);
        assertEquals(new Long(1), row0[2]);

        Object[] row1 = (Object[]) data.get(1);
        assertEquals(new BigDecimal(1), row1[0], 0.001d);
        assertEquals("PZ", row1[1]);
        assertEquals(new Long(2), row1[2]);

        Object[] row2 = (Object[]) data.get(2);
        assertEquals(new BigDecimal(2d), row2[0], 0.001d);
        assertEquals("PY", row2[1]);
        assertEquals(new Long(2), row2[2]);
    }

    public void testGroupByRelatedEntity() throws Exception {

        createTestData("testGroupByRelatedEntity");

        String ejbql = "SELECT COUNT(p), a, a.artistName "
                + "FROM Painting p INNER JOIN p.toArtist a GROUP BY a, a.artistName "
                + "ORDER BY a.artistName";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(2, data.size());

        assertTrue(data.get(0) instanceof Object[]);
        Object[] row0 = (Object[]) data.get(0);
        assertEquals(3, row0.length);
        assertEquals(new Long(1), row0[0]);
        assertEquals("AA1", row0[2]);
        assertTrue(row0[1] instanceof Artist);
    }

    public void testGroupByIdVariable() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT count(p), p FROM Painting p GROUP BY p";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(5, data.size());

        // TODO: andrus, 8/3/2007 the rest of the unit test fails as currently Cayenne
        // does not allow mixed object and scalar results (see CAY-839)

        // assertTrue(data.get(0) instanceof Object[]);
        //
        // for(int i = 0; i < data.size(); i++) {
        // Object[] row = (Object[]) data.get(i);
        // assertEquals(new Long(1), row[0]);
        // }
    }

    public void testGroupByHavingOnColumn() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.estimatedPrice, count(p) FROM Painting p"
                + " GROUP BY p.estimatedPrice"
                + " HAVING p.estimatedPrice > 1";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);

        Object[] row0 = (Object[]) data.get(0);
        assertEquals(new BigDecimal(2d), row0[0], 0.001d);
        assertEquals(new Long(2), row0[1]);
    }

    public void testGroupByHavingOnAggregate() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.estimatedPrice, count(p) FROM Painting p"
                + " GROUP BY p.estimatedPrice"
                + " HAVING count(p) > 2";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);

        Object[] row0 = (Object[]) data.get(0);
        assertEquals(new BigDecimal(1d), row0[0], 0.001d);
        assertEquals(new Long(3l), row0[1]);
    }

    public void testGroupByHavingOnAggregateMultipleConditions() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.estimatedPrice, count(p) FROM Painting p"
                + " GROUP BY p.estimatedPrice"
                + " HAVING count(p) > 2 AND p.estimatedPrice < 10";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);

        Object[] row0 = (Object[]) data.get(0);
        assertEquals(new BigDecimal(1d), row0[0], 0.001d);
        assertEquals(new Long(3l), row0[1]);
    }
    
    public void testGroupByJoinedRelatedEntities() throws Exception {
        createTestData("testGroupByRelatedEntity");
        EJBQLQuery query = new EJBQLQuery(
                "SELECT COUNT(p), p.toArtist FROM Painting p GROUP BY p.toArtist ");
        List<Object[]> data = createDataContext().performQuery(query);
        assertNotNull(data);
        assertEquals(2, data.size());
        
        List<String> expectedArtists=new ArrayList<String>();
        expectedArtists.add("AA1");
        expectedArtists.add("AA2");
        
        Object[]row = data.get(0);
        String artistName = ((Artist)row[1]).getArtistName();
        assertEquals(1L, row[0]);
        assertTrue("error artistName:"+artistName, expectedArtists.contains(artistName));
        
        row = data.get(1);
        artistName = ((Artist)row[1]).getArtistName();
        assertEquals(1L, row[0]);
        assertTrue("error artistName:"+artistName, expectedArtists.contains(artistName));
    }

    public void testGroupByJoinedEntities() throws Exception {
        createTestData("testGroupByEntities");
        EJBQLQuery query = new EJBQLQuery(
                "SELECT COUNT(p), p.toArtist, p.toGallery FROM Painting p " +
                "GROUP BY p.toGallery, p.toArtist ");
        List<Object[]> data = createDataContext().performQuery(query);
        assertNotNull(data);
        assertEquals(2, data.size());
        
        HashSet<List> expectedResults=new HashSet<List>();
        expectedResults.add(Arrays.asList(1L, "AA2","gallery1"));
        expectedResults.add(Arrays.asList(1L, "AA1","gallery2"));
        
        for(Object[] row:data){
            assertFalse(expectedResults.add(Arrays.asList(
                    row[0], 
                    row[1]==null?null:((Artist)row[1]).getArtistName(),
                    row[2]==null?null:((Gallery)row[2]).getGalleryName())));
        }
    }

    public void testGroupByJoinedEntityInCount() throws Exception {
        createTestData("testGroupByEntities");
        EJBQLQuery query = new EJBQLQuery(
                "SELECT COUNT(p.toArtist), p.paintingTitle FROM Painting p " +
                "GROUP BY p.paintingTitle " +
                "HAVING p.paintingTitle LIKE 'P1%'");
        List<Object[]> data = createDataContext().performQuery(query);
        assertNotNull(data);
        assertEquals(3, data.size());
        
        HashSet<List> expectedResults=new HashSet<List>();
        expectedResults.add(Arrays.asList(1L, "P1"));
        expectedResults.add(Arrays.asList(1L, "P111"));
        expectedResults.add(Arrays.asList(1L, "P112"));
        
        for(Object[] row:data){
            assertFalse(expectedResults.add(Arrays.asList(row[0], row[1])));
        }
    }
    
    public void testGroupByChainedJoins() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.painting.toArtist.paintingArray FROM PaintingInfo p"
                + " GROUP BY p.painting.toArtist.paintingArray";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        List data = createDataContext().performQuery(query);
        
        ejbql = "SELECT p.painting.toArtist FROM PaintingInfo p"
            + " GROUP BY p.painting.toArtist";
        query = new EJBQLQuery(ejbql);
        createDataContext().performQuery(query);
    }
    
}
