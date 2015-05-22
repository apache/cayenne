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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.util.TstBean;
import org.junit.Test;

public class ExpressionEvaluateInMemoryTest {

	@Test
	public void testEvaluateOBJ_PATH_DataObject() {
		ASTObjPath node = new ASTObjPath("artistName");

		Artist a1 = new Artist();
		a1.setArtistName("abc");
		assertEquals("abc", node.evaluate(a1));

		Artist a2 = new Artist();
		a2.setArtistName("123");
		assertEquals("123", node.evaluate(a2));
	}

	@Test
	public void testEvaluateOBJ_PATH_JavaBean() {
		ASTObjPath node = new ASTObjPath("property2");

		TstBean b1 = new TstBean();
		b1.setProperty2(1);
		assertEquals(new Integer(1), node.evaluate(b1));

		TstBean b2 = new TstBean();
		b2.setProperty2(-3);
		assertEquals(new Integer(-3), node.evaluate(b2));
	}

	@Test
	public void testEvaluateEQUAL_TOBigDecimal() {
		BigDecimal bd1 = new BigDecimal("2.0");
		BigDecimal bd2 = new BigDecimal("2.0");
		BigDecimal bd3 = new BigDecimal("2.00");
		BigDecimal bd4 = new BigDecimal("2.01");

		Expression equalTo = new ASTEqual(new ASTObjPath(Painting.ESTIMATED_PRICE.getName()), bd1);

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

	@Test
	public void testEvaluateEQUAL_TO() {
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

	@Test
	public void testEvaluateEQUAL_TO_Null() {
		Expression equalToNull = new ASTEqual(new ASTObjPath("artistName"), null);
		Expression equalToNotNull = new ASTEqual(new ASTObjPath("artistName"), "abc");

		Artist match = new Artist();
		assertTrue(equalToNull.match(match));
		assertFalse(equalToNotNull.match(match));

		Artist noMatch = new Artist();
		noMatch.setArtistName("abc");
		assertFalse(equalToNull.match(noMatch));
	}

	@Test
	public void testEvaluateNOT_EQUAL_TONull() {
		Expression notEqualToNull = new ASTNotEqual(new ASTObjPath("artistName"), null);
		Expression notEqualToNotNull = new ASTNotEqual(new ASTObjPath("artistName"), "abc");

		Artist match = new Artist();
		assertFalse(notEqualToNull.match(match));
		assertTrue(notEqualToNotNull.match(match));

		Artist noMatch = new Artist();
		noMatch.setArtistName("123");
		assertTrue("Failed: " + notEqualToNull, notEqualToNull.match(noMatch));
	}

	@Test
	public void testEvaluateAND() {
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

	@Test
	public void testEvaluateOR() {
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

	@Test
	public void testEvaluateNOT() {
		ASTNot e = new ASTNot(new ASTEqual(new ASTObjPath("artistName"), "abc"));

		Artist noMatch = new Artist();
		noMatch.setArtistName("abc");
		assertFalse(e.match(noMatch));

		Artist match = new Artist();
		match.setArtistName("123");
		assertTrue("Failed: " + e, e.match(match));
	}

	@Test
	public void testEvaluateLESS_THAN() {
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

	@Test
	public void testEvaluateLESS_THAN_Null() {
		Expression ltNull = new ASTLess(new ASTObjPath("estimatedPrice"), null);
		Expression ltNotNull = new ASTLess(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

		Painting noMatch = new Painting();
		assertFalse(ltNull.match(noMatch));
		assertFalse(ltNotNull.match(noMatch));
	}

	@Test
	public void testEvaluateLESS_THAN_EQUAL_TO() {
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

	@Test
	public void testEvaluateLESS_THAN_EQUAL_TO_Null() {
		Expression ltNull = new ASTLessOrEqual(new ASTObjPath("estimatedPrice"), null);
		Expression ltNotNull = new ASTLessOrEqual(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

		Painting noMatch = new Painting();
		assertFalse(ltNull.match(noMatch));
		assertFalse(ltNotNull.match(noMatch));
	}

	@Test
	public void testEvaluateGREATER_THAN() {
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

	@Test
	public void testEvaluateGREATER_THAN_Null() {
		Expression gtNull = new ASTGreater(new ASTObjPath("estimatedPrice"), null);
		Expression gtNotNull = new ASTGreater(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

		Painting noMatch = new Painting();
		assertFalse(gtNull.match(noMatch));
		assertFalse(gtNotNull.match(noMatch));
	}

	@Test
	public void testEvaluateGREATER_THAN_EQUAL_TO() {
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

	@Test
	public void testEvaluateGREATER_THAN_EQUAL_TO_Null() {
		Expression gtNull = new ASTGreaterOrEqual(new ASTObjPath("estimatedPrice"), null);
		Expression gtNotNull = new ASTGreaterOrEqual(new ASTObjPath("estimatedPrice"), new BigDecimal(10000d));

		Painting noMatch = new Painting();
		assertFalse(gtNull.match(noMatch));
		assertFalse(gtNotNull.match(noMatch));
	}

	@Test
	public void testEvaluateBETWEEN() {
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

	@Test
	public void testEvaluateBETWEEN_Null() {
		Expression btNull = new ASTBetween(new ASTObjPath("estimatedPrice"), new BigDecimal(10d), new BigDecimal(20d));
		Expression btNotNull = new ASTNotBetween(new ASTObjPath("estimatedPrice"), new BigDecimal(10d), new BigDecimal(
				20d));

		Painting noMatch = new Painting();
		assertFalse(btNull.match(noMatch));
		assertFalse(btNotNull.match(noMatch));
	}

	@Test
	public void testEvaluateIN() {
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

	@Test
	public void testEvaluateIN_Null() {
		Expression in = new ASTIn(new ASTObjPath("estimatedPrice"), new ASTList(new Object[] { new BigDecimal("10"),
				new BigDecimal("20") }));
		Expression notIn = new ASTNotIn(new ASTObjPath("estimatedPrice"), new ASTList(new Object[] {
				new BigDecimal("10"), new BigDecimal("20") }));

		Painting noMatch = new Painting();
		assertFalse(in.match(noMatch));
		assertFalse(notIn.match(noMatch));
	}

	@Test
	public void testEvaluateLIKE1() {
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

	@Test
	public void testEvaluateLIKE2() {
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

	@Test
	public void testEvaluateLIKE3() {
		// test special chars
		Expression like = new ASTLike(new ASTObjPath("artistName"), "/./");

		Artist noMatch1 = new Artist();
		noMatch1.setArtistName("/a/");
		assertFalse(like.match(noMatch1));

		Artist match = new Artist();
		match.setArtistName("/./");
		assertTrue("Failed: " + like, like.match(match));
	}

	@Test
	public void testEvaluateLIKE_IGNORE_CASE() {
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

	@Test
	public void testEvaluateADD() {
		Expression add = new ASTAdd(new Object[] { new Integer(1), new Double(5.5) });
		assertEquals(6.5, ((Number) add.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateSubtract() {
		Expression subtract = new ASTSubtract(new Object[] { new Integer(1), new Double(0.1), new Double(0.2) });
		assertEquals(0.7, ((Number) subtract.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateMultiply() {
		Expression multiply = new ASTMultiply(new Object[] { new Integer(2), new Double(3.5) });
		assertEquals(7, ((Number) multiply.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateDivide() {
		Expression divide = new ASTDivide(new Object[] { new BigDecimal("7.0"), new BigDecimal("2.0") });
		assertEquals(3.5, ((Number) divide.evaluate(null)).doubleValue(), 0.0001);
	}

	@Test
	public void testEvaluateNegate() {
		assertEquals(-3, ((Number) new ASTNegate(new Integer(3)).evaluate(null)).intValue());
		assertEquals(5, ((Number) new ASTNegate(new Integer(-5)).evaluate(null)).intValue());
	}

	@Test
	public void testEvaluateTrue() {
		assertEquals(Boolean.TRUE, new ASTTrue().evaluate(null));
	}

	@Test
	public void testEvaluateFalse() {
		assertEquals(Boolean.FALSE, new ASTFalse().evaluate(null));
	}

}
