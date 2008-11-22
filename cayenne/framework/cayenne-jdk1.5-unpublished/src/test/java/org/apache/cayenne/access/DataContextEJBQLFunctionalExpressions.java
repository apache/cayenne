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
import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.BigDecimalEntity;
import org.apache.art.BigIntegerEntity;
import org.apache.art.DateTestEntity;
import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLFunctionalExpressions extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testCURRENT_DATE() {

        ObjectContext context = createDataContext();
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year - 3, 1, 1);
        o1.setDateColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year + 3, 1, 1);
        o2.setDateColumn(cal.getTime());

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM DateTestEntity d WHERE d.dateColumn > CURRENT_DATE");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o2));
    }

    public void testCURRENT_TIME() {

        ObjectContext context = createDataContext();
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year, 1, 1, 0, 0, 0);
        o1.setTimeColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year, 1, 1, 23, 59, 59);
        o2.setTimeColumn(cal.getTime());

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM DateTestEntity d WHERE d.timeColumn < CURRENT_TIME");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o1));
    }

    public void testCURRENT_TIMESTAMP() {

        ObjectContext context = createDataContext();
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int date = cal.get(Calendar.DATE);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year, month, date, 0, 0, 0);
        o1.setTimestampColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year, month, date, 23, 59, 59);
        o2.setTimestampColumn(cal.getTime());

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM DateTestEntity d WHERE d.timestampColumn < CURRENT_TIMESTAMP");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o1));
    }

    public void testABS() {
        ObjectContext context = createDataContext();

        BigDecimalEntity o1 = context
                .newObject(BigDecimalEntity.class);
        o1.setBigDecimalField(new BigDecimal("4.1"));

        BigDecimalEntity o2 = context
                .newObject(BigDecimalEntity.class);
        o2.setBigDecimalField(new BigDecimal("-5.1"));

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM BigDecimalEntity d WHERE ABS(d.bigDecimalField) > 4.5");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o2));
    }

    public void testSQRT() {
        ObjectContext context = createDataContext();

        BigDecimalEntity o1 = context
                .newObject(BigDecimalEntity.class);
        o1.setBigDecimalField(new BigDecimal("9"));

        BigDecimalEntity o2 = context
                .newObject(BigDecimalEntity.class);
        o2.setBigDecimalField(new BigDecimal("16"));

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM BigDecimalEntity d WHERE SQRT(d.bigDecimalField) > 3.1");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o2));
    }

    public void testMOD() {
        ObjectContext context = createDataContext();

        BigIntegerEntity o1 = context
                .newObject(BigIntegerEntity.class);
        o1.setBigIntegerField(new BigInteger("9"));

        BigIntegerEntity o2 = context
                .newObject(BigIntegerEntity.class);
        o2.setBigIntegerField(new BigInteger("10"));

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM BigIntegerEntity d WHERE MOD(d.bigIntegerField, 4) = 2");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o2));
    }

    public void testSIZE() {
        ObjectContext context = createDataContext();

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

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM Artist d WHERE SIZE(d.paintingArray) = 2");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a2));

        EJBQLQuery query2 = new EJBQLQuery(
                "SELECT d FROM Artist d WHERE SIZE(d.paintingArray) = 0");
        List objects2 = context.performQuery(query2);
        assertEquals(1, objects2.size());
        assertTrue(objects2.contains(a1));
    }

    public void testCONCAT() {
        ObjectContext context = createDataContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a1");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("a2");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE CONCAT(a.artistName, a.artistName) = 'a1a1'");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a1));
    }

    public void testSUBSTRING() {
        ObjectContext context = createDataContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("12345678");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("abcdefg");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE SUBSTRING(a.artistName, 2, 3) = 'bcd'");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a2));
    }

    public void testLOWER() {
        ObjectContext context = createDataContext();

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
        List objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a1));
        assertTrue(objects.contains(a2));
    }

    public void testUPPER() {
        ObjectContext context = createDataContext();

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
        List objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a1));
        assertTrue(objects.contains(a2));
    }

    public void testLENGTH() {
        ObjectContext context = createDataContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("1234567");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("1234567890");

        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("1234567890-=");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE LENGTH(a.artistName) > 7");
        List objects = context.performQuery(query);
        assertEquals(2, objects.size());
        assertTrue(objects.contains(a3));
        assertTrue(objects.contains(a2));
    }

    public void testLOCATE() {
        ObjectContext context = createDataContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("___A___");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("_A_____");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE LOCATE('A', a.artistName) = 2");
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(a2));
    }

    public void testTRIM() {

        ObjectContext context = createDataContext();

        // insert via a SQL template to prevent adapter trimming and such...
        QueryChain inserts = new QueryChain();
        inserts.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID,ARTIST_NAME) VALUES(1, '  A')"));
        inserts.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID,ARTIST_NAME) VALUES(2, 'A  ')"));
        context.performGenericQuery(inserts);

        Artist a1 = DataObjectUtils.objectForPK(context, Artist.class, 1);
        Artist a2 = DataObjectUtils.objectForPK(context, Artist.class, 2);

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM(a.artistName) = 'A'");
        List objects = context.performQuery(query);
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

    public void testTRIMChar() {

        if (!getAccessStackAdapter().supportsTrimChar()) {
            return;
        }
        
        ObjectContext context = createDataContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XXXA");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("AXXX");
        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT a FROM Artist a WHERE TRIM('X' FROM a.artistName) = 'A'");
        List objects = context.performQuery(query);
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
