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

package org.apache.cayenne.exp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ExpressionTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private ServerRuntime runtime;

    @Override
    protected void setUpAfterInjection() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "Equals");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);

        if (objects.size() > 0) {
            SelectQuery query1 = new SelectQuery(Painting.class);
            Expression e = ExpressionFactory.matchExp(Painting.TO_ARTIST_PROPERTY, objects.get(0));
            query.setQualifier(e);
            objects.addAll(context.performQuery(query1));
        }

        context.deleteObjects(objects);
    }

    public void testFromStringLong() {
        Expression e = Expression.fromString("216201000180L");
        assertEquals(new Long(216201000180L), e.evaluate(new Object()));
    }

    public void testFromStringPath() {
        Expression e1 = Expression.fromString("object.path");
        assertEquals(Expression.OBJ_PATH, e1.getType());

        Expression e2 = Expression.fromString("db:object.path");
        assertEquals(Expression.DB_PATH, e2.getType());

        Expression e3 = Expression.fromString("object+.path");
        assertEquals(Expression.OBJ_PATH, e3.getType());

        Expression e4 = Expression.fromString("db:object.path+");
        assertEquals(Expression.DB_PATH, e4.getType());
    }

    public void testFromStringScalar() {
        Expression e1 = Expression.fromString("a = 'abc'");
        assertEquals("abc", e1.getOperand(1));
    }

    public void testFromStringEnum() {
        Expression e1 = Expression.fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.ONE");
        assertEquals(ExpEnum1.ONE, e1.getOperand(1));

        Expression e2 = Expression.fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO");
        assertEquals(ExpEnum1.TWO, e2.getOperand(1));

        Expression e3 = Expression.fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.THREE");
        assertEquals(ExpEnum1.THREE, e3.getOperand(1));

        try {
            Expression.fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.BOGUS");
            fail("Didn't throw on bad enum");
        } catch (ExpressionException e) {
            // expected
        }

        try {
            Expression.fromString("a = enum:BOGUS");
            fail("Didn't throw on bad enum");
        } catch (ExpressionException e) {
            // expected
        }
    }

    public void testExpWithParametersNullHandling_CAY847() {
        Expression e = Expression.fromString("X = $x");

        e = e.expWithParameters(Collections.singletonMap("x", null));
        assertEquals("X = null", e.toString());
    }

    public void testToEJBQL1() {

        Expression e = Expression.fromString("artistName = \"bla\"");

        // note single quotes - EJBQL does not support doublequotes...
        assertEquals("x.artistName = 'bla'", e.toEJBQL("x"));
    }

    public void testEncodeAsEJBQL1() throws IOException {

        Expression e = Expression.fromString("artistName = 'bla'");

        StringBuilder buffer = new StringBuilder();

        e.appendAsEJBQL(buffer, "x");

        String ejbql = buffer.toString();

        assertEquals("x.artistName = 'bla'", ejbql);
    }

    public void testEncodeAsEJBQL2() throws IOException {

        Expression e = Expression.fromString("artistName.stuff = $name");

        StringBuilder buffer = new StringBuilder();

        e.appendAsEJBQL(buffer, "x");
        String ejbql = buffer.toString();

        assertEquals("x.artistName.stuff = :name", ejbql);
    }

    public void testEncodeAsEJBQL3_EncodeListOfParameters() throws IOException {

        Expression e = ExpressionFactory.inExp("artistName", "a", "b", "c");

        StringBuilder buffer = new StringBuilder();

        e.appendAsEJBQL(buffer, "x");

        String ejbql = buffer.toString();

        assertEquals("x.artistName in ('a', 'b', 'c')", ejbql);
    }

    public void testEncodeAsEJBQL_PersistentParamater() throws IOException {

        Artist a = new Artist();
        ObjectId aId = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 1);
        a.setObjectId(aId);

        Expression e = ExpressionFactory.matchExp("artist", a);

        StringBuilder buffer = new StringBuilder();

        e.appendAsEJBQL(buffer, "x");

        String ejbql = buffer.toString();

        assertEquals("x.artist = 1", ejbql);
    }

    public void testAppendAsEJBQLNotEquals() throws IOException {

        Expression e = Expression.fromString("artistName != 'bla'");

        StringBuilder buffer = new StringBuilder();
        e.appendAsEJBQL(buffer, "x");
        String ejbql = buffer.toString();

        assertEquals("x.artistName <> 'bla'", ejbql);
    }

    public void testEncodeAsEJBQL_Enum() throws IOException {

        Expression e = Expression.fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.THREE");

        StringBuilder buffer = new StringBuilder();
        e.appendAsEJBQL(buffer, "x");

        String ejbql = buffer.toString();

        assertEquals("x.a = enum:org.apache.cayenne.exp.ExpEnum1.THREE", ejbql);
    }

    public void testEncodeAsString_StringLiteral() throws IOException {
        Expression e1 = Expression.fromString("a = 'abc'");

        StringBuilder buffer = new StringBuilder();

        e1.appendAsString(buffer);

        assertEquals("a = \"abc\"", buffer.toString());
    }

    public void testEncodeAsString_Enum() throws IOException {
        Expression e1 = Expression.fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO");

        StringBuilder buffer = new StringBuilder();

        e1.appendAsString(buffer);
        assertEquals("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO", buffer.toString());
    }

    public void testEqualsObjects() {

        assertTrue(context instanceof DataContext);

        DataContext context2 = (DataContext) runtime.newContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("Equals");
        Painting p1 = context.newObject(Painting.class);
        p1.setToArtist(a1);
        p1.setPaintingTitle("painting1");
        
        context.commitChanges();

        SelectQuery query = new SelectQuery(Painting.class);
        Expression e = ExpressionFactory.matchExp(Painting.TO_ARTIST_PROPERTY, a1);
        query.setQualifier(e);

        assertNotSame(context2, context);

        List<Artist> objects = context2.performQuery(query);
        assertEquals(1, objects.size());

        // 2 same objects in different contexts
        assertTrue(e.match(objects.get(0)));

        // we change one object - so the objects are different now
        // (PersistenceState different)
        a1.setArtistName("newName");

        SelectQuery q2 = new SelectQuery(Painting.class);
        Expression ex2 = ExpressionFactory.matchExp(Painting.TO_ARTIST_PROPERTY, a1);
        q2.setQualifier(ex2);

        assertTrue(ex2.match(objects.get(0)));

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("Equals");

        context.commitChanges();
        
        SelectQuery q = new SelectQuery(Painting.class);
        Expression ex = ExpressionFactory.matchExp(Painting.TO_ARTIST_PROPERTY, a2);
        q.setQualifier(ex);

        // 2 different objects in different contexts
        assertFalse(ex.match(objects.get(0)));
    }

    public void testFirst() {
        List<Painting> paintingList = new ArrayList<Painting>();
        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("x1");
        paintingList.add(p1);

        Painting p2 = context.newObject(Painting.class);
        p2.setPaintingTitle("x2");
        paintingList.add(p2);

        Painting p3 = context.newObject(Painting.class);
        p3.setPaintingTitle("x3");
        paintingList.add(p3);

        Expression e1 = ExpressionFactory.likeExp("paintingTitle", "x%");
        assertSame(p1, e1.first(paintingList));

        Expression e3 = ExpressionFactory.matchExp("paintingTitle", "x3");
        assertSame(p3, e3.first(paintingList));

        Expression e4 = ExpressionFactory.matchExp("paintingTitle", "x4");
        assertNull(e4.first(paintingList));
    }

    public void testAndExp() {
        Expression e1 = ExpressionFactory.matchExp("name", "Picasso");
        Expression e2 = ExpressionFactory.matchExp("age", 30);

        Expression exp = e1.andExp(e2);
        assertEquals(exp.getType(), Expression.AND);
        assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
    }

    public void testOrExp() {
        Expression e1 = ExpressionFactory.matchExp("name", "Picasso");
        Expression e2 = ExpressionFactory.matchExp("age", 30);

        Expression exp = e1.orExp(e2);
        assertEquals(exp.getType(), Expression.OR);
        assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
    }

    public void testAndExpVarArgs() {
        Expression e1 = ExpressionFactory.matchExp("name", "Picasso");
        Expression e2 = ExpressionFactory.matchExp("age", 30);
        Expression e3 = ExpressionFactory.matchExp("height", 5.5);
        Expression e4 = ExpressionFactory.matchExp("numEars", 1);

        Expression exp = e1.andExp(e2, e3, e4);
        assertEquals(exp.getType(), Expression.AND);
        assertEquals(4, ((SimpleNode) exp).jjtGetNumChildren());
    }

    public void testOrExpVarArgs() {
        Expression e1 = ExpressionFactory.matchExp("name", "Picasso");
        Expression e2 = ExpressionFactory.matchExp("age", 30);
        Expression e3 = ExpressionFactory.matchExp("height", 5.5);
        Expression e4 = ExpressionFactory.matchExp("numEars", 1);

        Expression exp = e1.orExp(e2, e3, e4);
        assertEquals(exp.getType(), Expression.OR);
        assertEquals(4, ((SimpleNode) exp).jjtGetNumChildren());
    }

    // bitwise operations test
    public void testBitwiseNegate() {
        Expression exp = Expression.fromString("~7");

        assertEquals(Expression.BITWISE_NOT, exp.getType());
        assertEquals(1, ((SimpleNode) exp).jjtGetNumChildren());
        assertEquals(new Long(-8), exp.evaluate(new Object())); // ~7 = -8 in
                                                                // digital world
    }

    public void testBitwiseAnd() {
        Expression exp = Expression.fromString("1 & 0");

        assertEquals(Expression.BITWISE_AND, exp.getType());
        assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
        assertEquals(new Long(0), exp.evaluate(new Object()));
    }

    public void testBitwiseOr() {
        Expression exp = Expression.fromString("1 | 0");

        assertEquals(Expression.BITWISE_OR, exp.getType());
        assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
        assertEquals(new Long(1), exp.evaluate(new Object()));
    }

    public void testBitwiseXor() {
        Expression exp = Expression.fromString("1 ^ 0");

        assertEquals(Expression.BITWISE_XOR, exp.getType());
        assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
        assertEquals(new Long(1), exp.evaluate(new Object()));
    }

    public void testBitwiseLeftShift() {
        Expression exp = Expression.fromString("7 << 2");

        assertEquals(Expression.BITWISE_LEFT_SHIFT, exp.getType());
        assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
        assertEquals(new Long(28), exp.evaluate(new Object()));
    }

    public void testBitwiseRightShift() {
        Expression exp = Expression.fromString("7 >> 2");

        assertEquals(Expression.BITWISE_RIGHT_SHIFT, exp.getType());
        assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());

        assertEquals(new Long(1), exp.evaluate(new Object()));
    }

    /**
     * (a | b) | c = a | (b | c)
     */
    public void testBitwiseAssociativity() {
        Expression e1 = Expression.fromString("(3010 | 2012) | 4095");
        Expression e2 = Expression.fromString("3010 | (2012 | 4095)");

        assertEquals(e1.evaluate(new Object()), e2.evaluate(new Object()));
    }

    /**
     * a | b = b | a
     */
    public void testBitwiseCommutativity() {
        Expression e1 = Expression.fromString("3010 | 4095");
        Expression e2 = Expression.fromString("4095 | 3010");

        assertEquals(e1.evaluate(new Object()), e2.evaluate(new Object()));
    }

    /**
     * a | (a & b) = a
     */
    public void testBitwiseAbsorption() {
        Expression e1 = Expression.fromString("2012 | (2012 & 3010)");
        Expression e2 = Expression.fromString("2012L"); // scalar becomes Long
                                                        // object

        assertEquals(e1.evaluate(new Object()), e2.evaluate(new Object()));
    }

    /**
     * a | (b & c) = (a | b) & (a | c)
     */
    public void testBitwiseDistributivity() {
        Expression e1 = Expression.fromString("4095 | (7777 & 8888)");
        Expression e2 = Expression.fromString("(4095 | 7777) & (4095 | 8888)");

        assertEquals(e1.evaluate(new Object()), e2.evaluate(new Object()));
    }

    /**
     * a | ~a = 1 But in Java computed result is -1 because of JVM represents
     * negative numbers as positive ones: ~2 = -3; For instance, there are only
     * 4 bits and that is why -3 means '1101' and 3 means '0011' because of
     * '1101' + '0011' = (1)'0000' what is zero; but the same time '1101' is 13.
     */
    public void testBitwiseComplements() {
        Expression e1 = Expression.fromString("5555 | ~5555");
        Expression e2 = Expression.fromString("9999 & ~9999");

        assertEquals(new Long(-1), e1.evaluate(new Object())); // ~0 = -1 that
                                                               // is the way how
                                                               // robots kill
                                                               // humans what
                                                               // means x | ~x =
                                                               // 1 in boolean
                                                               // algebra
                                                               // against java
                                                               // digital
                                                               // bitwise
                                                               // operations
                                                               // logics
        assertEquals(new Long(0), e2.evaluate(new Object()));
    }

    /**
     * Huntington equation n(n(x) + y) + n(n(x) + n(y)) = x where is 'n' is
     * negotation (may be any other unary operation) and '+' is disjunction (OR
     * operation, i.e. '|' bitwise operation).
     */
    public void testBitwiseHuntingtonEquation() {
        Expression theHuntingEquation = Expression.fromString("~(~3748 | 4095) | ~(~3748 | ~4095)");

        assertEquals(new Long(3748), theHuntingEquation.evaluate(new Object()));
    }

    /**
     * Robbins equation n(n(x + y) + n(x + n(y))) = x where is 'n' is negotation
     * and '+' is disjunction (OR operation, i.e. '|' bitwise operation). Every
     * Robbins algebra is a Boolean algebra according to automated reasoning
     * program EQP.
     */
    public void testBitwiseRobbinsEquation() {
        Expression theRobbinsEquation = Expression.fromString("~(~(5111 | 4095) | ~(5111 | ~4095))");

        assertEquals(new Long(5111), theRobbinsEquation.evaluate(new Object()));
    }

    /**
     * Bitwise and math operations are ruled by precedence.
     */
    public void testBitwisePrecedence() {
        Expression e1 = Expression.fromString("1 << 1 & 2"); // 1 << 1 = 2 and
                                                             // after that 2 & 2
                                                             // = 2;
        Expression e2 = Expression.fromString("0 | 1 & ~(3 | ~3)"); // by java
                                                                    // math
                                                                    // precedence
                                                                    // that
                                                                    // means 0 |
                                                                    // (1 & (~(3
                                                                    // | (~3))))
        Expression e3 = Expression.fromString("3 | ~(-3) + 2"); // JVM ~(-3) = 2
                                                                // and then 2 +
                                                                // 2 is 4 what
                                                                // bitwise is
                                                                // 100, then 011
                                                                // | 100 = 111
                                                                // what means 3
                                                                // + 4 = 7
        Expression e4 = Expression.fromString("2 * 2 | 2"); // (2 * 2) | 2 = 4 |
                                                            // 2 = '100' | '10'
                                                            // = '110' = 6
        Expression e5 = Expression.fromString("6 / 2 & 3"); // (6 / 2) & 3 = 3 &
                                                            // 3 = 3

        assertEquals(new Long(2), e1.evaluate(new Object()));
        assertEquals(new Long(0), e2.evaluate(new Object()));
        assertEquals(new Long(7), e3.evaluate(new Object()));
        assertEquals(new Long(6), e4.evaluate(new Object()));
        assertEquals(new Long(3), e5.evaluate(new Object()));
    }
    // bitwise
}
