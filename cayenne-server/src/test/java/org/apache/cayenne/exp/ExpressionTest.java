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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

public class ExpressionTest {

	@Test
	public void testToEJBQL_numericType_integer() throws IOException {
		Expression e = ExpressionFactory.matchExp("consignment.parts", new Integer(123));
		assertEquals("x.consignment.parts = 123", e.toEJBQL("x"));
	}

	@Test
	public void testToEJBQL_numericType_long() throws IOException {
		Expression e = ExpressionFactory.matchExp("consignment.parts", new Long(1418342400));
		assertEquals("x.consignment.parts = 1418342400", e.toEJBQL("x"));
	}

	@Test
	public void testToEJBQL_numericType_float() throws IOException {
		Expression e = ExpressionFactory.greaterOrEqualExp("consignment.parts", new Float("3.145"));
		assertEquals("x.consignment.parts >= 3.145", e.toEJBQL("x"));
	}

	@Test
	public void testToEJBQL_numericType_double() throws IOException {
		Expression e = ExpressionFactory.greaterOrEqualExp("consignment.parts", new Double(3.14));
		assertEquals("x.consignment.parts >= 3.14", e.toEJBQL("x"));
	}

	@Test
	public void testAppendAsEJBQL_Timestamp_ParameterCapture() throws IOException {
		Date now = new Date();

		Expression e = ExpressionFactory.greaterOrEqualExp("dateOfBirth", now);

		StringBuilder buffer = new StringBuilder();
		List<Object> parametersAccumulator = new ArrayList<Object>();

		e.appendAsEJBQL(parametersAccumulator, buffer, "x");

		String ejbql = buffer.toString();

		assertEquals("x.dateOfBirth >= ?1", ejbql);
		assertEquals(parametersAccumulator.size(), 1);
		assertEquals(parametersAccumulator.get(0), now);

	}

	@Test
	public void testAppendAsEJBQL_in_EncodeListOfParameters_ParameterCapture() throws IOException {

		Expression e = ExpressionFactory.inExp("artistName", "a", "b", "c");

		StringBuilder buffer = new StringBuilder();
		List<Object> parametersAccumulator = new ArrayList<Object>();

		e.appendAsEJBQL(parametersAccumulator, buffer, "x");

		String ejbql = buffer.toString();

		assertEquals("x.artistName in (?1, ?2, ?3)", ejbql);
		assertEquals(parametersAccumulator.size(), 3);
		assertEquals(parametersAccumulator.get(0), "a");
		assertEquals(parametersAccumulator.get(1), "b");
		assertEquals(parametersAccumulator.get(2), "c");

	}

	@Test
	public void testAppendAsEJBQL_in_EncodeListOfParameters() throws IOException {

		Expression e = ExpressionFactory.inExp("artistName", "a", "b", "c");

		StringBuilder buffer = new StringBuilder();

		e.appendAsEJBQL(buffer, "x");

		String ejbql = buffer.toString();

		assertEquals("x.artistName in ('a', 'b', 'c')", ejbql);
	}

	@Test
	public void testAppendAsEJBQL_PersistentParamater() throws IOException {

		Artist a = new Artist();
		ObjectId aId = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 1);
		a.setObjectId(aId);

		Expression e = ExpressionFactory.matchExp("artist", a);

		StringBuilder buffer = new StringBuilder();

		e.appendAsEJBQL(buffer, "x");

		String ejbql = buffer.toString();

		assertEquals("x.artist = 1", ejbql);
	}

	@Test
	public void testAppendAsEJBQLNotEquals() throws IOException {

		Expression e = ExpressionFactory.exp("artistName != 'bla'");

		StringBuilder buffer = new StringBuilder();
		e.appendAsEJBQL(buffer, "x");
		String ejbql = buffer.toString();

		assertEquals("x.artistName <> 'bla'", ejbql);
	}

	@Test
	public void testIsNotNullEx() {
		Expression e = Artist.ARTIST_NAME.isNotNull();
		String ejbql = e.toEJBQL("x");
		assertEquals("not (x.artistName is null)", ejbql);
	}

	@Test
	public void testAndExp() {
		Expression e1 = ExpressionFactory.matchExp("name", "Picasso");
		Expression e2 = ExpressionFactory.matchExp("age", 30);

		Expression exp = e1.andExp(e2);
		assertEquals(exp.getType(), Expression.AND);
		assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
	}

	@Test
	public void testOrExp() {
		Expression e1 = ExpressionFactory.matchExp("name", "Picasso");
		Expression e2 = ExpressionFactory.matchExp("age", 30);

		Expression exp = e1.orExp(e2);
		assertEquals(exp.getType(), Expression.OR);
		assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
	}

	@Test
	public void testAndExpVarArgs() {
		Expression e1 = ExpressionFactory.matchExp("name", "Picasso");
		Expression e2 = ExpressionFactory.matchExp("age", 30);
		Expression e3 = ExpressionFactory.matchExp("height", 5.5);
		Expression e4 = ExpressionFactory.matchExp("numEars", 1);

		Expression exp = e1.andExp(e2, e3, e4);
		assertEquals(exp.getType(), Expression.AND);
		assertEquals(4, ((SimpleNode) exp).jjtGetNumChildren());
	}

	@Test
	public void testOrExpVarArgs() {
		Expression e1 = ExpressionFactory.matchExp("name", "Picasso");
		Expression e2 = ExpressionFactory.matchExp("age", 30);
		Expression e3 = ExpressionFactory.matchExp("height", 5.5);
		Expression e4 = ExpressionFactory.matchExp("numEars", 1);

		Expression exp = e1.orExp(e2, e3, e4);
		assertEquals(exp.getType(), Expression.OR);
		assertEquals(4, ((SimpleNode) exp).jjtGetNumChildren());
	}

	@Test
	public void testBitwiseNegate() {
		Expression exp = ExpressionFactory.exp("~7");

		assertEquals(Expression.BITWISE_NOT, exp.getType());
		assertEquals(1, ((SimpleNode) exp).jjtGetNumChildren());
		assertEquals(new Long(-8), exp.evaluate(new Object())); // ~7 = -8 in
																// digital world
	}

	@Test
	public void testBitwiseAnd() {
		Expression exp = ExpressionFactory.exp("1 & 0");

		assertEquals(Expression.BITWISE_AND, exp.getType());
		assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
		assertEquals(new Long(0), exp.evaluate(new Object()));
	}

	@Test
	public void testBitwiseOr() {
		Expression exp = ExpressionFactory.exp("1 | 0");

		assertEquals(Expression.BITWISE_OR, exp.getType());
		assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
		assertEquals(new Long(1), exp.evaluate(new Object()));
	}

	@Test
	public void testBitwiseXor() {
		Expression exp = ExpressionFactory.exp("1 ^ 0");

		assertEquals(Expression.BITWISE_XOR, exp.getType());
		assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
		assertEquals(new Long(1), exp.evaluate(new Object()));
	}

	@Test
	public void testBitwiseLeftShift() {
		Expression exp = ExpressionFactory.exp("7 << 2");

		assertEquals(Expression.BITWISE_LEFT_SHIFT, exp.getType());
		assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());
		assertEquals(new Long(28), exp.evaluate(new Object()));
	}

	@Test
	public void testBitwiseRightShift() {
		Expression exp = ExpressionFactory.exp("7 >> 2");

		assertEquals(Expression.BITWISE_RIGHT_SHIFT, exp.getType());
		assertEquals(2, ((SimpleNode) exp).jjtGetNumChildren());

		assertEquals(new Long(1), exp.evaluate(new Object()));
	}

	/**
	 * (a | b) | c = a | (b | c)
	 */
	@Test
	public void testBitwiseAssociativity() {
		Expression e1 = ExpressionFactory.exp("(3010 | 2012) | 4095");
		Expression e2 = ExpressionFactory.exp("3010 | (2012 | 4095)");

		assertEquals(e1.evaluate(new Object()), e2.evaluate(new Object()));
	}

	/**
	 * a | b = b | a
	 */
	@Test
	public void testBitwiseCommutativity() {
		Expression e1 = ExpressionFactory.exp("3010 | 4095");
		Expression e2 = ExpressionFactory.exp("4095 | 3010");

		assertEquals(e1.evaluate(new Object()), e2.evaluate(new Object()));
	}

	/**
	 * a | (a & b) = a
	 */
	@Test
	public void testBitwiseAbsorption() {
		Expression e1 = ExpressionFactory.exp("2012 | (2012 & 3010)");
		Expression e2 = ExpressionFactory.exp("2012L"); // scalar becomes Long
														// object

		assertEquals(e1.evaluate(new Object()), e2.evaluate(new Object()));
	}

	/**
	 * a | (b & c) = (a | b) & (a | c)
	 */
	@Test
	public void testBitwiseDistributivity() {
		Expression e1 = ExpressionFactory.exp("4095 | (7777 & 8888)");
		Expression e2 = ExpressionFactory.exp("(4095 | 7777) & (4095 | 8888)");

		assertEquals(e1.evaluate(new Object()), e2.evaluate(new Object()));
	}

	/**
	 * a | ~a = 1 But in Java computed result is -1 because of JVM represents
	 * negative numbers as positive ones: ~2 = -3; For instance, there are only
	 * 4 bits and that is why -3 means '1101' and 3 means '0011' because of
	 * '1101' + '0011' = (1)'0000' what is zero; but the same time '1101' is 13.
	 */
	@Test
	public void testBitwiseComplements() {
		Expression e1 = ExpressionFactory.exp("5555 | ~5555");
		Expression e2 = ExpressionFactory.exp("9999 & ~9999");

		assertEquals(new Long(-1), e1.evaluate(new Object())); // ~0 = -1 that
																// is the way
																// how
																// robots kill
																// humans what
																// means x | ~x
																// =
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
	@Test
	public void testBitwiseHuntingtonEquation() {
		Expression theHuntingEquation = ExpressionFactory.exp("~(~3748 | 4095) | ~(~3748 | ~4095)");

		assertEquals(new Long(3748), theHuntingEquation.evaluate(new Object()));
	}

	/**
	 * Robbins equation n(n(x + y) + n(x + n(y))) = x where is 'n' is negotation
	 * and '+' is disjunction (OR operation, i.e. '|' bitwise operation). Every
	 * Robbins algebra is a Boolean algebra according to automated reasoning
	 * program EQP.
	 */
	@Test
	public void testBitwiseRobbinsEquation() {
		Expression theRobbinsEquation = ExpressionFactory.exp("~(~(5111 | 4095) | ~(5111 | ~4095))");

		assertEquals(new Long(5111), theRobbinsEquation.evaluate(new Object()));
	}

	/**
	 * Bitwise and math operations are ruled by precedence.
	 */
	@Test
	public void testBitwisePrecedence() {
		Expression e1 = ExpressionFactory.exp("1 << 1 & 2"); // 1 << 1 = 2 and
																// after that 2
																// & 2
																// = 2;
		Expression e2 = ExpressionFactory.exp("0 | 1 & ~(3 | ~3)"); // by java
																	// math
																	// precedence
																	// that
																	// means 0 |
																	// (1 & (~(3
																	// | (~3))))
		Expression e3 = ExpressionFactory.exp("3 | ~(-3) + 2"); // JVM ~(-3) = 2
																// and then 2 +
																// 2 is 4 what
																// bitwise is
																// 100, then 011
																// | 100 = 111
																// what means 3
																// + 4 = 7
		Expression e4 = ExpressionFactory.exp("2 * 2 | 2"); // (2 * 2) | 2 = 4 |
															// 2 = '100' | '10'
															// = '110' = 6
		Expression e5 = ExpressionFactory.exp("6 / 2 & 3"); // (6 / 2) & 3 = 3 &
															// 3 = 3

		assertEquals(new Long(2), e1.evaluate(new Object()));
		assertEquals(new Long(0), e2.evaluate(new Object()));
		assertEquals(new Long(7), e3.evaluate(new Object()));
		assertEquals(new Long(6), e4.evaluate(new Object()));
		assertEquals(new Long(3), e5.evaluate(new Object()));
	}

	@Test
	public void testAppendAsEJBQL_NotEquals_ParameterCapture() throws IOException {
		Expression e = ExpressionFactory.exp("artistName != 'bla'");

		StringBuilder buffer = new StringBuilder();
		List<Object> parametersAccumulator = new ArrayList<Object>();
		e.appendAsEJBQL(parametersAccumulator, buffer, "x");
		String ejbql = buffer.toString();

		assertEquals("x.artistName <> ?1", ejbql);
		assertEquals(parametersAccumulator.size(), 1);
		assertEquals(parametersAccumulator.get(0), "bla");
	}

	@Test
	public void testAppendAsEJBQL_Enum() throws IOException {

		Expression e = ExpressionFactory.exp("a = enum:org.apache.cayenne.exp.ExpEnum1.THREE");

		StringBuilder buffer = new StringBuilder();
		e.appendAsEJBQL(buffer, "x");

		String ejbql = buffer.toString();

		assertEquals("x.a = enum:org.apache.cayenne.exp.ExpEnum1.THREE", ejbql);
	}

	@Test
	public void testAppendAsString_StringLiteral() throws IOException {
		Expression e1 = ExpressionFactory.exp("a = 'abc'");

		StringBuilder buffer = new StringBuilder();

		e1.appendAsString(buffer);

		assertEquals("a = \"abc\"", buffer.toString());
	}

	@Test
	public void testAppendAsString_Enum() throws IOException {
		Expression e1 = ExpressionFactory.exp("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO");

		StringBuilder buffer = new StringBuilder();

		e1.appendAsString(buffer);
		assertEquals("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO", buffer.toString());
	}

}
