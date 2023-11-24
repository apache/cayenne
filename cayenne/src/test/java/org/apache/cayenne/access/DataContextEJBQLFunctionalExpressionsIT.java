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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// TODO: renamed as it fails on DB's like Derby. See CAY-1480. 
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextEJBQLFunctionalExpressionsIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Test
    public void testSIZE() {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a1");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("a2");

        Painting p12 = context.newObject(Painting.class);
        p12.setPaintingTitle("p12");
        a2.addToPaintingArray(p12);
        Painting p22 = context.newObject(Painting.class);
        p22.setPaintingTitle("p22");
        a2.addToPaintingArray(p22);

        context.commitChanges();

        // this fails:
        // EJBQLQuery query = new EJBQLQuery(
        // "SELECT d FROM Artist d WHERE SIZE(d.paintingArray) = 2");
        // List<?> objects = context.performQuery(query);
        // assertEquals(1, objects.size());
        // assertTrue(objects.contains(a2));
        //
        // EJBQLQuery query2 = new EJBQLQuery(
        // "SELECT d FROM Artist d WHERE SIZE(d.paintingArray) = 0");
        // List<?> objects2 = context.performQuery(query2);
        // assertEquals(1, objects2.size());
        // assertTrue(objects2.contains(a1));
    }

    @Test
    public void testCONCAT() {

        Painting a1 = context.newObject(Painting.class);
        a1.setPaintingTitle("a1");

        Painting a2 = context.newObject(Painting.class);
        a2.setPaintingTitle("a2");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Painting a WHERE CONCAT(a.paintingTitle, a.paintingTitle) = 'a1a1'");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a1));
    }

    @Test
    public void testSUBSTRING() {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("12345678");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("abcdefg");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE SUBSTRING(a.artistName, 2, 3) = 'bcd'");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a2));
    }

    @Test
    public void testLOWER() {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("ABCDEFG");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("abcdefg");
        context.commitChanges();

        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("Xabcdefg");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE LOWER(a.artistName) = 'abcdefg'");
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a1));
        assertTrue(objects.contains(a2));
    }

    @Test
    public void testUPPER() {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("ABCDEFG");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("abcdefg");
        context.commitChanges();

        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("Xabcdefg");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE UPPER(a.artistName) = UPPER('abcdefg')");
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a1));
        assertTrue(objects.contains(a2));
    }

    @Test
    public void testLENGTH() {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("1234567");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("1234567890");

        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("1234567890-=");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE LENGTH(a.artistName) > 7");
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a3));
        assertTrue(objects.contains(a2));
    }

    @Test
    public void testLOCATE() {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("___A___");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("_A_____");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE LOCATE('A', a.artistName) = 2");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a2));
    }

    @Test
    public void testTRIM() {

        // insert via a SQL template to prevent adapter trimming and such...
        QueryChain inserts = new QueryChain();
        inserts.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID,ARTIST_NAME) VALUES(1, '  A')"));
        inserts.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID,ARTIST_NAME) VALUES(2, 'A  ')"));
        context.performGenericQuery(inserts);

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 1);
        Artist a2 = Cayenne.objectForPK(context, Artist.class, 2);

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM(a.artistName) = 'A'");
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a1));
        assertTrue(objects.contains(a2));

        query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM(LEADING FROM a.artistName) = 'A'");
        objects = context.performQuery(query);
        // this is fuzzy cause some DB trim trailing data by default
        assertTrue(objects.size() == 1 || objects.size() == 2);
        assertTrue(objects.contains(a1));

        query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM(TRAILING FROM a.artistName) = 'A'");
        objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a2));

        query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM(BOTH FROM a.artistName) = 'A'");
        objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a1));
        assertTrue(objects.contains(a2));

    }

    @Test
    public void testTRIMChar() {

        if (!accessStackAdapter.supportsTrimChar()) {
            return;
        }

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XXXA");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("AXXX");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM('X' FROM a.artistName) = 'A'");
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a1));
        assertTrue(objects.contains(a2));

        query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM(LEADING 'X' FROM a.artistName) = 'A'");
        objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a1));

        query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM(TRAILING 'X' FROM a.artistName) = 'A'");
        objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a2));

        query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM(BOTH 'X' FROM a.artistName) = 'A'");
        objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a1));
        assertTrue(objects.contains(a2));
    }
}
