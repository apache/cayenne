/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.exp;

import java.math.BigDecimal;
import java.util.Iterator;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.util.TestBean;

/**
 * Test case for in memory expression evaluation by ASTNode produced from
 * an expression. To reduce the number of fixtures, negated expressions are
 * tested within the same test method as the expressions they negate.
 * 
 * @since 1.0.6
 * @deprecated Old expression API is deprecated.
 * @author Andrei Adamchik
 */
public class ASTNodeInMemoryTst extends CayenneTestCase {
    public void testEvaluateOBJ_PATH_DataObject() throws Exception {
        ASTNode node =
            ASTCompiler.compile(
                ExpressionFactory.unaryExp(Expression.OBJ_PATH, "artistName"));

        Artist a1 = new Artist();
        a1.setArtistName("abc");
        assertEquals("abc", node.evaluateASTChain(a1));

        Artist a2 = new Artist();
        a2.setArtistName("123");
        assertEquals("123", node.evaluateASTChain(a2));
    }

    public void testEvaluateOBJ_PATH_JavaBean() throws Exception {
        ASTNode node =
            ASTCompiler.compile(
                ExpressionFactory.unaryExp(Expression.OBJ_PATH, "property2"));

        TestBean b1 = new TestBean();
        b1.setProperty2(1);
        assertEquals(new Integer(1), node.evaluateASTChain(b1));

        TestBean b2 = new TestBean();
        b2.setProperty2(-3);
        assertEquals(new Integer(-3), node.evaluateASTChain(b2));
    }

    public void testEvaluateOBJ_PATH_ObjEntity() throws Exception {
        ASTNode node =
            ASTCompiler.compile(Expression.fromString("paintingArray.paintingTitle"));

        ObjEntity ae = getDomain().getEntityResolver().lookupObjEntity(Artist.class);

        Object target = node.evaluateASTChain(ae);
        assertTrue(target instanceof Iterator);

        Iterator it = (Iterator) target;
        assertTrue(it.next() instanceof ObjRelationship);
        assertTrue(it.next() instanceof ObjAttribute);
        assertFalse(it.hasNext());
    }

    public void testEvaluateDB_PATH_DbEntity() throws Exception {
        ASTNode node =
            ASTCompiler.compile(Expression.fromString("db:paintingArray.PAINTING_TITLE"));

        ObjEntity ae = getDomain().getEntityResolver().lookupObjEntity(Artist.class);
        DbEntity ade = ae.getDbEntity();

        Object objTarget = node.evaluateASTChain(ae);
        assertTrue(objTarget instanceof Iterator);

        Iterator it = (Iterator) objTarget;
        assertTrue(it.next() instanceof DbRelationship);
        assertTrue(it.next() instanceof DbAttribute);
        assertFalse(it.hasNext());

        Object dbTarget = node.evaluateASTChain(ade);
        assertTrue(dbTarget instanceof Iterator);

        it = (Iterator) dbTarget;
        assertTrue(it.next() instanceof DbRelationship);
        assertTrue(it.next() instanceof DbAttribute);
        assertFalse(it.hasNext());
    }

    public void testEvaluateEQUAL_TO() throws Exception {
        ASTNode equalTo =
            ASTCompiler.compile(ExpressionFactory.matchExp("artistName", "abc"));

        ASTNode notEqualTo =
            ASTCompiler.compile(ExpressionFactory.noMatchExp("artistName", "abc"));

        Artist match = new Artist();
        match.setArtistName("abc");
        assertTrue(equalTo.evaluateBooleanASTChain(match));
        assertFalse(notEqualTo.evaluateBooleanASTChain(match));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertFalse("Failed: " + equalTo, equalTo.evaluateBooleanASTChain(noMatch));
        assertTrue("Failed: " + notEqualTo, notEqualTo.evaluateBooleanASTChain(noMatch));
    }

    public void testEvaluateEQUAL_TODataObject() throws Exception {
        DataContext context = createDataContext();
        Artist a1 = (Artist) context.createAndRegisterNewObject("Artist");
        Artist a2 = (Artist) context.createAndRegisterNewObject("Artist");
        Painting p1 = (Painting) context.createAndRegisterNewObject("Painting");
        Painting p2 = (Painting) context.createAndRegisterNewObject("Painting");
        Painting p3 = (Painting) context.createAndRegisterNewObject("Painting");

        p1.setToArtist(a1);
        p2.setToArtist(a2);

        ASTNode node = ASTCompiler.compile(ExpressionFactory.matchExp("toArtist", a1));

        assertTrue(node.evaluateBooleanASTChain(p1));
        assertFalse(node.evaluateBooleanASTChain(p2));
        assertFalse(node.evaluateBooleanASTChain(p3));
    }

    public void testEvaluateAND() throws Exception {
        Expression e = ExpressionFactory.matchExp("artistName", "abc");
        e = e.andExp(ExpressionFactory.matchExp("artistName", "abc"));
        ASTNode node = ASTCompiler.compile(e);

        Artist match = new Artist();
        match.setArtistName("abc");
        assertTrue(node.evaluateBooleanASTChain(match));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertFalse(node.evaluateBooleanASTChain(noMatch));
    }

    public void testEvaluateOR() throws Exception {
        Expression e = ExpressionFactory.matchExp("artistName", "abc");
        e = e.orExp(ExpressionFactory.matchExp("artistName", "xyz"));
        ASTNode node = ASTCompiler.compile(e);

        Artist match1 = new Artist();
        match1.setArtistName("abc");
        assertTrue("Failed: " + e, node.evaluateBooleanASTChain(match1));

        Artist match2 = new Artist();
        match2.setArtistName("xyz");
        assertTrue("Failed: " + e, node.evaluateBooleanASTChain(match2));

        Artist noMatch = new Artist();
        noMatch.setArtistName("123");
        assertTrue("Failed: " + e, !node.evaluateBooleanASTChain(noMatch));
    }

    public void testEvaluateNOT() throws Exception {
        ASTNode node =
            ASTCompiler.compile(ExpressionFactory.matchExp("artistName", "abc").notExp());

        Artist noMatch = new Artist();
        noMatch.setArtistName("abc");
        assertFalse(node.evaluateBooleanASTChain(noMatch));

        Artist match = new Artist();
        match.setArtistName("123");
        assertTrue("Failed: " + node, node.evaluateBooleanASTChain(match));
    }

    public void testEvaluateLESS_THAN() throws Exception {
        ASTNode node =
            ASTCompiler.compile(
                ExpressionFactory.lessExp("estimatedPrice", new BigDecimal(10000)));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(10001));
        assertFalse("Failed: " + node, node.evaluateBooleanASTChain(noMatch));

        Painting noMatch1 = new Painting();
        noMatch1.setEstimatedPrice(new BigDecimal(10000));
        assertFalse("Failed: " + node, node.evaluateBooleanASTChain(noMatch1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(9999));
        assertTrue("Failed: " + node, node.evaluateBooleanASTChain(match));
    }

    public void testEvaluateLESS_THAN_EQUAL_TO() throws Exception {
        ASTNode node =
            ASTCompiler.compile(
                ExpressionFactory.lessOrEqualExp(
                    "estimatedPrice",
                    new BigDecimal(10000)));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(10001));
        assertFalse(node.evaluateBooleanASTChain(noMatch));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(10000));
        assertTrue(node.evaluateBooleanASTChain(match1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(9999));
        assertTrue("Failed: " + node, node.evaluateBooleanASTChain(match));
    }

    public void testEvaluateGREATER_THAN() throws Exception {
        ASTNode node =
            ASTCompiler.compile(
                ExpressionFactory.greaterExp("estimatedPrice", new BigDecimal(10000)));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(9999));
        assertFalse(node.evaluateBooleanASTChain(noMatch));

        Painting noMatch1 = new Painting();
        noMatch1.setEstimatedPrice(new BigDecimal(10000));
        assertFalse(node.evaluateBooleanASTChain(noMatch1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(10001));
        assertTrue("Failed: " + node, node.evaluateBooleanASTChain(match));
    }

    public void testEvaluateGREATER_THAN_EQUAL_TO() throws Exception {
        ASTNode node =
            ASTCompiler.compile(
                ExpressionFactory.greaterOrEqualExp(
                    "estimatedPrice",
                    new BigDecimal(10000)));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(9999));
        assertFalse(node.evaluateBooleanASTChain(noMatch));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(10000));
        assertTrue(node.evaluateBooleanASTChain(match1));

        Painting match = new Painting();
        match.setEstimatedPrice(new BigDecimal(10001));
        assertTrue("Failed: " + node, node.evaluateBooleanASTChain(match));
    }

    public void testEvaluateBETWEEN() throws Exception {
        // evaluate both BETWEEN and NOT_BETWEEN
        ASTNode between =
            ASTCompiler.compile(
                ExpressionFactory.betweenExp(
                    "estimatedPrice",
                    new BigDecimal(10),
                    new BigDecimal(20)));

        ASTNode notBetween =
            ASTCompiler.compile(
                ExpressionFactory.notBetweenExp(
                    "estimatedPrice",
                    new BigDecimal(10),
                    new BigDecimal(20)));

        Painting noMatch = new Painting();
        noMatch.setEstimatedPrice(new BigDecimal(21));
        assertFalse(between.evaluateBooleanASTChain(noMatch));
        assertTrue(notBetween.evaluateBooleanASTChain(noMatch));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(20));
        assertTrue(between.evaluateBooleanASTChain(match1));
        assertFalse(notBetween.evaluateBooleanASTChain(match1));

        Painting match2 = new Painting();
        match2.setEstimatedPrice(new BigDecimal(10));
        assertTrue("Failed: " + between, between.evaluateBooleanASTChain(match2));
        assertFalse("Failed: " + notBetween, notBetween.evaluateBooleanASTChain(match2));

        Painting match3 = new Painting();
        match3.setEstimatedPrice(new BigDecimal(11));
        assertTrue("Failed: " + between, between.evaluateBooleanASTChain(match3));
        assertFalse("Failed: " + notBetween, notBetween.evaluateBooleanASTChain(match3));
    }

    public void testEvaluateIN() throws Exception {
        ASTNode in =
            ASTCompiler.compile(
                ExpressionFactory.inExp(
                    "estimatedPrice",
                    new Object[] { new BigDecimal(10), new BigDecimal(20)}));
        ASTNode notIn =
            ASTCompiler.compile(
                ExpressionFactory.notInExp(
                    "estimatedPrice",
                    new Object[] { new BigDecimal(10), new BigDecimal(20)}));

        Painting noMatch1 = new Painting();
        noMatch1.setEstimatedPrice(new BigDecimal(21));
        assertFalse(in.evaluateBooleanASTChain(noMatch1));
        assertTrue(notIn.evaluateBooleanASTChain(noMatch1));

        Painting noMatch2 = new Painting();
        noMatch2.setEstimatedPrice(new BigDecimal(11));
        assertFalse("Failed: " + in, in.evaluateBooleanASTChain(noMatch2));
        assertTrue("Failed: " + notIn, notIn.evaluateBooleanASTChain(noMatch2));

        Painting match1 = new Painting();
        match1.setEstimatedPrice(new BigDecimal(20));
        assertTrue(in.evaluateBooleanASTChain(match1));
        assertFalse(notIn.evaluateBooleanASTChain(match1));

        Painting match2 = new Painting();
        match2.setEstimatedPrice(new BigDecimal(10));
        assertTrue("Failed: " + in, in.evaluateBooleanASTChain(match2));
        assertFalse("Failed: " + notIn, notIn.evaluateBooleanASTChain(match2));
    }

    public void testEvaluateLIKE1() throws Exception {
        ASTNode like =
            ASTCompiler.compile(ExpressionFactory.likeExp("artistName", "abc%d"));
        ASTNode notLike =
            ASTCompiler.compile(ExpressionFactory.notLikeExp("artistName", "abc%d"));

        Artist noMatch = new Artist();
        noMatch.setArtistName("dabc");
        assertFalse(like.evaluateBooleanASTChain(noMatch));
        assertTrue(notLike.evaluateBooleanASTChain(noMatch));

        Artist match1 = new Artist();
        match1.setArtistName("abc123d");
        assertTrue("Failed: " + like, like.evaluateBooleanASTChain(match1));
        assertFalse("Failed: " + notLike, notLike.evaluateBooleanASTChain(match1));

        Artist match2 = new Artist();
        match2.setArtistName("abcd");
        assertTrue("Failed: " + like, like.evaluateBooleanASTChain(match2));
        assertFalse("Failed: " + notLike, notLike.evaluateBooleanASTChain(match2));
    }

    public void testEvaluateLIKE2() throws Exception {
        ASTNode like =
            ASTCompiler.compile(ExpressionFactory.likeExp("artistName", "abc?d"));
        ASTNode notLike =
            ASTCompiler.compile(ExpressionFactory.notLikeExp("artistName", "abc?d"));

        Artist noMatch1 = new Artist();
        noMatch1.setArtistName("dabc");
        assertFalse(like.evaluateBooleanASTChain(noMatch1));
        assertTrue(notLike.evaluateBooleanASTChain(noMatch1));

        Artist noMatch2 = new Artist();
        noMatch2.setArtistName("abc123d");
        assertFalse("Failed: " + like, like.evaluateBooleanASTChain(noMatch2));
        assertTrue("Failed: " + notLike, notLike.evaluateBooleanASTChain(noMatch2));

        Artist match = new Artist();
        match.setArtistName("abcXd");
        assertTrue("Failed: " + like, like.evaluateBooleanASTChain(match));
        assertFalse("Failed: " + notLike, notLike.evaluateBooleanASTChain(match));
    }

    public void testEvaluateLIKE3() throws Exception {
        // test special chars
        ASTNode like =
            ASTCompiler.compile(ExpressionFactory.likeExp("artistName", "/./"));

        Artist noMatch1 = new Artist();
        noMatch1.setArtistName("/a/");
        assertFalse(like.evaluateBooleanASTChain(noMatch1));

        Artist match = new Artist();
        match.setArtistName("/./");
        assertTrue("Failed: " + like, like.evaluateBooleanASTChain(match));
    }

    public void testEvaluateLIKE_IGNORE_CASE() throws Exception {
        ASTNode like =
            ASTCompiler.compile(
                ExpressionFactory.likeIgnoreCaseExp("artistName", "aBcD"));
        ASTNode notLike =
            ASTCompiler.compile(
                ExpressionFactory.notLikeIgnoreCaseExp("artistName", "aBcD"));

        Artist noMatch1 = new Artist();
        noMatch1.setArtistName("dabc");
        assertFalse(like.evaluateBooleanASTChain(noMatch1));
        assertTrue(notLike.evaluateBooleanASTChain(noMatch1));

        Artist match1 = new Artist();
        match1.setArtistName("abcd");
        assertTrue("Failed: " + like, like.evaluateBooleanASTChain(match1));
        assertFalse("Failed: " + notLike, notLike.evaluateBooleanASTChain(match1));

        Artist match2 = new Artist();
        match2.setArtistName("ABcD");
        assertTrue("Failed: " + like, like.evaluateBooleanASTChain(match2));
        assertFalse("Failed: " + notLike, notLike.evaluateBooleanASTChain(match2));
    }
}
