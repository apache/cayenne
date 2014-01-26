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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.parser.ASTLike;
import org.apache.cayenne.exp.parser.ASTLikeIgnoreCase;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ExpressionFactoryTest extends ServerCase {

    @Inject
    private ObjectContext context;

    public void testExpressionOfBadType() throws Exception {

        // non existing type
        int badType = -50;

        try {
            ExpressionFactory.expressionOfType(badType);
            fail();
        }
        catch (ExpressionException ex) {
            // exception expected
        }
    }

    public void testBetweenExp() throws Exception {
        Object v1 = new Object();
        Object v2 = new Object();
        Expression exp = ExpressionFactory.betweenExp("abc", v1, v2);
        assertEquals(Expression.BETWEEN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testBetweenDbExp() throws Exception {
        Object v1 = new Object();
        Object v2 = new Object();
        Expression exp = ExpressionFactory.betweenDbExp("abc", v1, v2);
        assertEquals(Expression.BETWEEN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testNotBetweenExp() throws Exception {
        Object v1 = new Object();
        Object v2 = new Object();
        Expression exp = ExpressionFactory.notBetweenExp("abc", v1, v2);
        assertEquals(Expression.NOT_BETWEEN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testNotBetweenDbExp() throws Exception {
        Object v1 = new Object();
        Object v2 = new Object();
        Expression exp = ExpressionFactory.notBetweenDbExp("abc", v1, v2);
        assertEquals(Expression.NOT_BETWEEN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testGreaterExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterExp("abc", v);
        assertEquals(Expression.GREATER_THAN, exp.getType());
    }

    public void testGreaterDbExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterDbExp("abc", v);
        assertEquals(Expression.GREATER_THAN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testGreaterOrEqualExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterOrEqualExp("abc", v);
        assertEquals(Expression.GREATER_THAN_EQUAL_TO, exp.getType());
    }

    public void testGreaterOrEqualDbExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterOrEqualDbExp("abc", v);
        assertEquals(Expression.GREATER_THAN_EQUAL_TO, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testLessExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessExp("abc", v);
        assertEquals(Expression.LESS_THAN, exp.getType());
    }

    public void testLessDbExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessDbExp("abc", v);
        assertEquals(Expression.LESS_THAN, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testLessOrEqualExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessOrEqualExp("abc", v);
        assertEquals(Expression.LESS_THAN_EQUAL_TO, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testLessOrEqualDbExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessOrEqualDbExp("abc", v);
        assertEquals(Expression.LESS_THAN_EQUAL_TO, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testInExp1() throws Exception {
        Expression exp = ExpressionFactory.inExp("abc", "a", "b");
        assertEquals(Expression.IN, exp.getType());
    }

    public void testInExp2() throws Exception {
        List<Object> v = new ArrayList<Object>();
        v.add("a");
        v.add("b");
        Expression exp = ExpressionFactory.inExp("abc", v);
        assertEquals(Expression.IN, exp.getType());
    }

    public void testInExp3() throws Exception {
        List<Object> v = new ArrayList<Object>();
        Expression exp = ExpressionFactory.inExp("abc", v);
        assertEquals(Expression.FALSE, exp.getType());
    }

    public void testLikeExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeExp("abc", v);
        assertEquals(Expression.LIKE, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testLikeDbExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeDbExp("abc", v);
        assertEquals(Expression.LIKE, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testLikeExpEscape() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeExp("=abc", v, '=');
        assertEquals(Expression.LIKE, exp.getType());

        assertEquals('=', ((ASTLike) exp).getEscapeChar());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testLikeIgnoreCaseExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeIgnoreCaseExp("abc", v);
        assertEquals(Expression.LIKE_IGNORE_CASE, exp.getType());
        assertEquals(0, ((ASTLikeIgnoreCase) exp).getEscapeChar());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testLikeIgnoreCaseExpEscape() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeIgnoreCaseExp("=abc", v, '=');
        assertEquals(Expression.LIKE_IGNORE_CASE, exp.getType());
        assertEquals('=', ((ASTLikeIgnoreCase) exp).getEscapeChar());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
    }

    public void testLikeIgnoreCaseDbExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeIgnoreCaseDbExp("abc", v);
        assertEquals(Expression.LIKE_IGNORE_CASE, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.DB_PATH, path.getType());
    }

    public void testNotLikeIgnoreCaseExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.notLikeIgnoreCaseExp("abc", v);
        assertEquals(Expression.NOT_LIKE_IGNORE_CASE, exp.getType());
    }

    // testing CAY-941 bug
    public void testLikeExpNull() throws Exception {
        Expression exp = ExpressionFactory.likeExp("abc", null);
        assertEquals(Expression.LIKE, exp.getType());

        Expression path = (Expression) exp.getOperand(0);
        assertEquals(Expression.OBJ_PATH, path.getType());
        assertNull(exp.getOperand(1));
    }

    // CAY-416
    public void testCollectionMatch() {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("artist");
        Painting p1 = context.newObject(Painting.class), p2 = context
                .newObject(Painting.class), p3 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        p2.setPaintingTitle("p2");
        p3.setPaintingTitle("p3");
        artist.addToPaintingArray(p1);
        artist.addToPaintingArray(p2);
        
        context.commitChanges();

        assertTrue(ExpressionFactory.matchExp("paintingArray", p1).match(artist));
        assertFalse(ExpressionFactory.matchExp("paintingArray", p3).match(artist));
        assertFalse(ExpressionFactory.noMatchExp("paintingArray", p1).match(artist));
        assertTrue(ExpressionFactory.noMatchExp("paintingArray", p3).match(artist));

        assertTrue(ExpressionFactory.matchExp("paintingArray.paintingTitle", "p1").match(
                artist));
        assertFalse(ExpressionFactory
                .matchExp("paintingArray.paintingTitle", "p3")
                .match(artist));
        assertFalse(ExpressionFactory
                .noMatchExp("paintingArray.paintingTitle", "p1")
                .match(artist));
        assertTrue(ExpressionFactory
                .noMatchExp("paintingArray.paintingTitle", "p3")
                .match(artist));

        assertTrue(ExpressionFactory.inExp("paintingTitle", "p1").match(p1));
        assertFalse(ExpressionFactory.notInExp("paintingTitle", "p3").match(p3));
    }

    public void testIn() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a1");
        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("p1");
        Painting p2 = context.newObject(Painting.class);
        p2.setPaintingTitle("p2");
        a1.addToPaintingArray(p1);
        a1.addToPaintingArray(p2);

        Expression in = ExpressionFactory.inExp("paintingArray", p1);
        assertTrue(in.match(a1));
    }
    
    public void testEscapeCharacter() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("A_1");
        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("A_2");
        context.commitChanges();
        
        Expression ex1 = ExpressionFactory.likeIgnoreCaseDbExp("ARTIST_NAME", "A*_1", '*');
        SelectQuery q1 = new SelectQuery(Artist.class, ex1);
        List<Artist> artists = context.performQuery(q1);
        assertEquals(1, artists.size());
        
        Expression ex2 = ExpressionFactory.likeExp("artistName", "A*_2", '*');
        SelectQuery q2 = new SelectQuery(Artist.class, ex2);
        artists = context.performQuery(q2);
        assertEquals(1, artists.size());
    }
}
