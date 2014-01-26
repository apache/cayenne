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

package org.apache.cayenne.exp.parser;

import java.math.BigDecimal;
import java.sql.Types;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.unit.util.TestBean;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ExpressionEvaluateInMemoryTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("PAINTING1");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE", "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER, Types.BIGINT, Types.VARCHAR, Types.DECIMAL);
    }

    protected void createTwoArtistsThreePaintings() throws Exception {

        tArtist.insert(1, "artist1");
        tArtist.insert(2, "artist2");
        tPainting.insert(1, 1, "P1", 3000);
        tPainting.insert(2, 2, "P2", 3000);
        tPainting.insert(3, null, "P3", 3000);
    }

    public void testEvaluateOBJ_PATH_DataObject() throws Exception {
        ASTObjPath node = new ASTObjPath("artistName");

        Artist a1 = new Artist();
        a1.setArtistName("abc");
        assertEquals("abc", node.evaluate(a1));

        Artist a2 = new Artist();
        a2.setArtistName("123");
        assertEquals("123", node.evaluate(a2));
    }

    public void testEvaluateOBJ_PATH_JavaBean() throws Exception {
        ASTObjPath node = new ASTObjPath("property2");

        TestBean b1 = new TestBean();
        b1.setProperty2(1);
        assertEquals(new Integer(1), node.evaluate(b1));

        TestBean b2 = new TestBean();
        b2.setProperty2(-3);
        assertEquals(new Integer(-3), node.evaluate(b2));
    }

    public void testEvaluateOBJ_PATH_ObjEntity() throws Exception {
        ASTObjPath node = new ASTObjPath("paintingArray.paintingTitle");

        ObjEntity ae = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);

        Object target = node.evaluate(ae);
        assertTrue(target instanceof ObjAttribute);
    }

    public void testEvaluateDB_PATH_DbEntity() throws Exception {
        Expression e = Expression.fromString("db:paintingArray.PAINTING_TITLE");

        ObjEntity ae = runtime.getDataDomain().getEntityResolver().getObjEntity(Artist.class);
        DbEntity ade = ae.getDbEntity();

        Object objTarget = e.evaluate(ae);
        assertTrue(objTarget instanceof DbAttribute);

        Object dbTarget = e.evaluate(ade);
        assertTrue(dbTarget instanceof DbAttribute);
    }

    public void testEvaluateEQUAL_TOBigDecimal() throws Exception {
        BigDecimal bd1 = new BigDecimal("2.0");
        BigDecimal bd2 = new BigDecimal("2.0");
        BigDecimal bd3 = new BigDecimal("2.00");
        BigDecimal bd4 = new BigDecimal("2.01");

        Expression equalTo = new ASTEqual(new ASTObjPath(Painting.ESTIMATED_PRICE_PROPERTY), bd1);

        Painting p = new Painting();
        p.setEstimatedPrice(bd2);
        assertTrue(equalTo.match(p));

        // BigDecimals must compare regardless of the number of trailing zeros
        // (see CAY-280)
        p.setEstimatedPrice(bd3);
        assertTrue(equalTo.match(p));

        p.setEstimatedPrice(bd4);
        assertFalse(equalTo.match(p));
    }

    public void testEvaluateEQUAL_TO() throws Exception {
        Expression equalTo = new ASTEqual(new ASTObjPath("artistName"), "abc");
        Expression notEqualTo = new ASTNotEqual(new ASTObjPath("artistName"), "abc");

        Artist match = new Artist();
        match.setArtistName("abc");
        assertTrue(equalTo.match(match));
        assertFalse(notEqualTo.match(match));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertFalse("Failed: " + equalTo, equalTo.match(noMatch));
        assertTrue("Failed: " + notEqualTo, notEqualTo.match(noMatch));
    }

    public void testEvaluateEQUAL_TONull() throws Exception {
        Expression equalTo = new ASTEqual(new ASTObjPath("artistName"), null);

        Artist match = new Artist();
        assertTrue(equalTo.match(match));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertFalse("Failed: " + equalTo, equalTo.match(noMatch));
    }

    public void testEvaluateNOT_EQUAL_TONull() throws Exception {
        Expression equalTo = new ASTNotEqual(new ASTObjPath("artistName"), null);

        Artist match = new Artist();
        assertFalse(equalTo.match(match));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertTrue("Failed: " + equalTo, equalTo.match(noMatch));
    }

    public void testEvaluateEQUAL_TODataObject() throws Exception {
        Artist a1 = (Artist) context.newObject("Artist");
        Artist a2 = (Artist) context.newObject("Artist");
        Painting p1 = (Painting) context.newObject("Painting");
        Painting p2 = (Painting) context.newObject("Painting");
        Painting p3 = (Painting) context.newObject("Painting");
        
        a1.setArtistName("a1");
        a2.setArtistName("a2");
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        p3.setPaintingTitle("p3");
        
        context.commitChanges();
        
        p1.setToArtist(a1);
        p2.setToArtist(a2);

        Expression e = new ASTEqual(new ASTObjPath("toArtist"), a1);

        assertTrue(e.match(p1));
        assertFalse(e.match(p2));
        assertFalse(e.match(p3));
    }

    public void testEvaluateEQUAL_TO_Temp_ObjectId() throws Exception {
        Artist a1 = (Artist) context.newObject("Artist");
        Artist a2 = (Artist) context.newObject("Artist");
        Painting p1 = (Painting) context.newObject("Painting");
        Painting p2 = (Painting) context.newObject("Painting");
        Painting p3 = (Painting) context.newObject("Painting");

        p1.setToArtist(a1);
        p2.setToArtist(a2);

        Expression e = new ASTEqual(new ASTObjPath("toArtist"), a1.getObjectId());

        assertTrue(e.match(p1));
        assertFalse(e.match(p2));
        assertFalse(e.match(p3));
    }

    public void testEvaluateEQUAL_TO_Id() throws Exception {

        createTwoArtistsThreePaintings();

        Artist a1 = Cayenne.objectForPK(context, Artist.class, 1);
        Painting p1 = Cayenne.objectForPK(context, Painting.class, 1);
        Painting p2 = Cayenne.objectForPK(context, Painting.class, 2);
        Painting p3 = Cayenne.objectForPK(context, Painting.class, 3);

        Expression e = new ASTEqual(new ASTObjPath("toArtist"), Cayenne.intPKForObject(a1));

        assertTrue(e.match(p1));
        assertFalse(e.match(p2));
        assertFalse(e.match(p3));
    }

    public void testEvaluateAND() throws Exception {
        Expression e1 = new ASTEqual(new ASTObjPath("artistName"), "abc");
        Expression e2 = new ASTEqual(new ASTObjPath("artistName"), "abc");

        ASTAnd e = new ASTAnd(new Object[] { e1, e2 });

        Artist match = new Artist();
        match.setArtistName("abc");
        assertTrue(e.match(match));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertFalse(e.match(noMatch));
    }

    public void testEvaluateOR() throws Exception {
        Expression e1 = new ASTEqual(new ASTObjPath("artistName"), "abc");
        Expression e2 = new ASTEqual(new ASTObjPath("artistName"), "xyz");

        ASTOr e = new ASTOr(new Object[] { e1, e2 });

        Artist match1 = new Artist();
        match1.setArtistName("abc");
        assertTrue("Failed: " + e, e.match(match1));

        Artist match2 = new Artist();
        match2.setArtistName("xyz");
        assertTrue("Failed: " + e, e.match(match2));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertFalse("Failed: " + e, e.match(noMatch));
    }

    public void testEvaluateNOT() throws Exception {
        ASTNot e = new ASTNot(new ASTEqual(new ASTObjPath("artistName"), "abc"));

        Artist noMatch = new Artist();
        noMatch.setArtistName("abc");
        assertFalse(e.match(noMatch));

        Artist match = new Artist();
        match.setArtistName("123");
        assertTrue("Failed: " + e, e.match(match));
    }

    public void testEvaluateLESS_THAN() throws Exception {
        Expression e = new ASTLess(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(10001));
        assertFalse("Failed: " + e, e.match(noMatch));

        Painting noMatch1 = new Painting();
        noMatch1.setEstimatedPrice(new BigDecimal(10000));
        assertFalse("Failed: " + e, e.match(noMatch1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(9999));
        assertTrue("Failed: " + e, e.match(match));
    }

    public void testEvaluateLESS_THAN_EQUAL_TO() throws Exception {
        Expression e = new ASTLessOrEqual(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(10001));
        assertFalse(e.match(noMatch));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(10000));
        assertTrue(e.match(match1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(9999));
        assertTrue("Failed: " + e, e.match(match));
    }

    public void testEvaluateGREATER_THAN() throws Exception {
        Expression e = new ASTGreater(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(9999));
        assertFalse(e.match(noMatch));

        Painting noMatch1 = new Painting();
        noMatch1.setEstimatedPrice(new BigDecimal(10000));
        assertFalse(e.match(noMatch1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(10001));
        assertTrue("Failed: " + e, e.match(match));
    }

    public void testEvaluateGREATER_THAN_EQUAL_TO() throws Exception {
        Expression e = new ASTGreaterOrEqual(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(9999));
        assertFalse(e.match(noMatch));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(10000));
        assertTrue(e.match(match1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(10001));
        assertTrue("Failed: " + e, e.match(match));
    }

    public void testEvaluateBETWEEN() throws Exception {
        // evaluate both BETWEEN and NOT_BETWEEN
        Expression between = new ASTBetween(new ASTObjPath("estimatedPrice"), new BigDecimal(10d), new BigDecimal(20d));
        Expression notBetween = new ASTNotBetween(new ASTObjPath("estimatedPrice"), new BigDecimal(10d),
                new BigDecimal(20d));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(21));
        assertFalse(between.match(noMatch));
        assertTrue(notBetween.match(noMatch));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(20));
        assertTrue(between.match(match1));
        assertFalse(notBetween.match(match1));

        Painting match2 = new Painting();
        match2.setEstimatedPrice(new BigDecimal(10));
        assertTrue("Failed: " + between, between.match(match2));
        assertFalse("Failed: " + notBetween, notBetween.match(match2));

        Painting match3 = new Painting();
        match3.setEstimatedPrice(new BigDecimal(11));
        assertTrue("Failed: " + between, between.match(match3));
        assertFalse("Failed: " + notBetween, notBetween.match(match3));
    }

    public void testEvaluateIN() throws Exception {
        Expression in = new ASTIn(new ASTObjPath("estimatedPrice"), new ASTList(new Object[] { new BigDecimal("10"),
                new BigDecimal("20") }));

        Expression notIn = new ASTNotIn(new ASTObjPath("estimatedPrice"), new ASTList(new Object[] {
                new BigDecimal("10"), new BigDecimal("20") }));

        Painting noMatch1 = new Painting();
        noMatch1.setEstimatedPrice(new BigDecimal("21"));
        assertFalse(in.match(noMatch1));
        assertTrue(notIn.match(noMatch1));

        Painting noMatch2 = new Painting();
        noMatch2.setEstimatedPrice(new BigDecimal("11"));
        assertFalse("Failed: " + in, in.match(noMatch2));
        assertTrue("Failed: " + notIn, notIn.match(noMatch2));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal("20"));
        assertTrue(in.match(match1));
        assertFalse(notIn.match(match1));

        Painting match2 = new Painting();
        match2.setEstimatedPrice(new BigDecimal("10"));
        assertTrue("Failed: " + in, in.match(match2));
        assertFalse("Failed: " + notIn, notIn.match(match2));
    }

    public void testEvaluateLIKE1() throws Exception {
        Expression like = new ASTLike(new ASTObjPath("artistName"), "abc%d");
        Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "abc%d");

        Artist noMatch = new Artist();
        noMatch.setArtistName("dabc");
        assertFalse(like.match(noMatch));
        assertTrue(notLike.match(noMatch));

        Artist match1 = new Artist();
        match1.setArtistName("abc123d");
        assertTrue("Failed: " + like, like.match(match1));
        assertFalse("Failed: " + notLike, notLike.match(match1));

        Artist match2 = new Artist();
        match2.setArtistName("abcd");
        assertTrue("Failed: " + like, like.match(match2));
        assertFalse("Failed: " + notLike, notLike.match(match2));
    }

    public void testEvaluateLIKE2() throws Exception {
        Expression like = new ASTLike(new ASTObjPath("artistName"), "abc?d");
        Expression notLike = new ASTNotLike(new ASTObjPath("artistName"), "abc?d");

        Artist noMatch1 = new Artist();
        noMatch1.setArtistName("dabc");
        assertFalse(like.match(noMatch1));
        assertTrue(notLike.match(noMatch1));

        Artist noMatch2 = new Artist();
        noMatch2.setArtistName("abc123d");
        assertFalse("Failed: " + like, like.match(noMatch2));
        assertTrue("Failed: " + notLike, notLike.match(noMatch2));

        Artist match = new Artist();
        match.setArtistName("abcXd");
        assertTrue("Failed: " + like, like.match(match));
        assertFalse("Failed: " + notLike, notLike.match(match));
    }

    public void testEvaluateLIKE3() throws Exception {
        // test special chars
        Expression like = new ASTLike(new ASTObjPath("artistName"), "/./");

        Artist noMatch1 = new Artist();
        noMatch1.setArtistName("/a/");
        assertFalse(like.match(noMatch1));

        Artist match = new Artist();
        match.setArtistName("/./");
        assertTrue("Failed: " + like, like.match(match));
    }

    public void testEvaluateLIKE_IGNORE_CASE() throws Exception {
        Expression like = new ASTLikeIgnoreCase(new ASTObjPath("artistName"), "aBcD");
        Expression notLike = new ASTNotLikeIgnoreCase(new ASTObjPath("artistName"), "aBcD");

        Artist noMatch1 = new Artist();
        noMatch1.setArtistName("dabc");
        assertFalse(like.match(noMatch1));
        assertTrue(notLike.match(noMatch1));

        Artist match1 = new Artist();
        match1.setArtistName("abcd");
        assertTrue("Failed: " + like, like.match(match1));
        assertFalse("Failed: " + notLike, notLike.match(match1));

        Artist match2 = new Artist();
        match2.setArtistName("ABcD");
        assertTrue("Failed: " + like, like.match(match2));
        assertFalse("Failed: " + notLike, notLike.match(match2));
    }

    public void testEvaluateADD() throws Exception {
        Expression add = new ASTAdd(new Object[] { new Integer(1), new Double(5.5) });
        assertEquals(6.5, ((Number) add.evaluate(null)).doubleValue(), 0.0001);
    }

    public void testEvaluateSubtract() throws Exception {
        Expression subtract = new ASTSubtract(new Object[] { new Integer(1), new Double(0.1), new Double(0.2) });
        assertEquals(0.7, ((Number) subtract.evaluate(null)).doubleValue(), 0.0001);
    }

    public void testEvaluateMultiply() throws Exception {
        Expression multiply = new ASTMultiply(new Object[] { new Integer(2), new Double(3.5) });
        assertEquals(7, ((Number) multiply.evaluate(null)).doubleValue(), 0.0001);
    }

    public void testEvaluateDivide() throws Exception {
        Expression divide = new ASTDivide(new Object[] { new BigDecimal("7.0"), new BigDecimal("2.0") });
        assertEquals(3.5, ((Number) divide.evaluate(null)).doubleValue(), 0.0001);
    }

    public void testEvaluateNegate() throws Exception {
        assertEquals(-3, ((Number) new ASTNegate(new Integer(3)).evaluate(null)).intValue());
        assertEquals(5, ((Number) new ASTNegate(new Integer(-5)).evaluate(null)).intValue());
    }

    public void testEvaluateTrue() throws Exception {
        assertEquals(Boolean.TRUE, new ASTTrue().evaluate(null));
    }

    public void testEvaluateFalse() throws Exception {
        assertEquals(Boolean.FALSE, new ASTFalse().evaluate(null));
    }
}
