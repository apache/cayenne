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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.art.Artist;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLJoinsTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testThetaJoins() throws Exception {
        createTestData("testThetaJoins");

        String ejbql = "SELECT DISTINCT a "
                + "FROM Artist a, Painting b "
                + "WHERE a.artistName = b.paintingTitle";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List artists = createDataContext().performQuery(query);
        assertEquals(2, artists.size());

        Set names = new HashSet(2);
        Iterator it = artists.iterator();
        while (it.hasNext()) {
            Artist a = (Artist) it.next();
            names.add(a.getArtistName());
        }

        assertTrue(names.contains("AA1"));
        assertTrue(names.contains("BB2"));
    }

    public void testInnerJoins() throws Exception {
        createTestData("testInnerJoins");

        String ejbql = "SELECT a "
                + "FROM Artist a INNER JOIN a.paintingArray p "
                + "WHERE a.artistName = 'AA1'";

        List artists = createDataContext().performQuery(new EJBQLQuery(ejbql));
        assertEquals(1, artists.size());
        assertEquals(33001, DataObjectUtils.intPKForObject((Artist) artists.get(0)));
    }

    public void testOuterJoins() throws Exception {
        createTestData("testInnerJoins");

        String ejbql = "SELECT a "
                + "FROM Artist a LEFT JOIN a.paintingArray p "
                + "WHERE a.artistName = 'AA1'";

        List artists = createDataContext().performQuery(new EJBQLQuery(ejbql));
        assertEquals(2, artists.size());
        Set ids = new HashSet(2);
        Iterator it = artists.iterator();
        while (it.hasNext()) {
            Artist a = (Artist) it.next();
            ids.add(DataObjectUtils.pkForObject(a));
        }

        assertTrue(ids.contains(33001l));
        assertTrue(ids.contains(33005l));
    }

    public void testChainedJoins() throws Exception {
        createTestData("testChainedJoins");
        String ejbql = "SELECT a "
                + "FROM Artist a JOIN a.paintingArray p JOIN p.toGallery g "
                + "WHERE g.galleryName = 'gallery2'";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        System.out.println(""
                + query.getExpression(getDomain().getEntityResolver()).getExpression());
        List artists = createDataContext().performQuery(query);
        assertEquals(1, artists.size());
        assertEquals(33002, DataObjectUtils.intPKForObject((Artist) artists.get(0)));
    }

    public void testImplicitJoins() throws Exception {
        createTestData("testChainedJoins");
        String ejbql = "SELECT a "
                + "FROM Artist a "
                + "WHERE a.paintingArray.toGallery.galleryName = 'gallery2'";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        System.out.println(""
                + query.getExpression(getDomain().getEntityResolver()).getExpression());

        List artists = createDataContext().performQuery(query);
        assertEquals(1, artists.size());
        assertEquals(33002, DataObjectUtils.intPKForObject((Artist) artists.get(0)));
    }

    public void testPartialImplicitJoins1() throws Exception {
        createTestData("testChainedJoins");
        String ejbql = "SELECT a "
                + "FROM Artist a JOIN a.paintingArray b "
                + "WHERE a.paintingArray.toGallery.galleryName = 'gallery2'";

        List artists = createDataContext().performQuery(new EJBQLQuery(ejbql));
        assertEquals(1, artists.size());
        assertEquals(33002, DataObjectUtils.intPKForObject((Artist) artists.get(0)));
    }

    public void testPartialImplicitJoins2() throws Exception {
        createTestData("testChainedJoins");
        String ejbql = "SELECT a "
                + "FROM Artist a JOIN a.paintingArray b "
                + "WHERE a.paintingArray.paintingTitle = 'CC2'";

        List artists = createDataContext().performQuery(new EJBQLQuery(ejbql));
        assertEquals(1, artists.size());
        assertEquals(33002, DataObjectUtils.intPKForObject((Artist) artists.get(0)));
    }

    public void testMultipleJoinsToTheSameTable() throws Exception {
        createTestData("testMultipleJoinsToTheSameTable");
        String ejbql = "SELECT a "
                + "FROM Artist a JOIN a.paintingArray b JOIN a.paintingArray c "
                + "WHERE b.paintingTitle = 'P1' AND c.paintingTitle = 'P2'";

        List artists = createDataContext().performQuery(new EJBQLQuery(ejbql));
        assertEquals(1, artists.size());
        assertEquals(33001, DataObjectUtils.intPKForObject((Artist) artists.get(0)));
    }
}
