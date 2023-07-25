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

package org.apache.cayenne.exp;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.parser.ASTLike;
import org.apache.cayenne.exp.parser.ASTLikeIgnoreCase;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTTrim;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ExpressionFactoryTest {

	private TstTraversalHandler handler;

	@Before
	public void before() {
		handler = new TstTraversalHandler();
	}

	@Test(expected = ExpressionException.class)
	public void testExpressionOfBadType() {
		// non existing type
		int badType = -50;
		ExpressionFactory.expressionOfType(badType);
	}

	@Test
	public void testBetweenExp() {
		Object v1 = new Object();
		Object v2 = new Object();
		Expression exp = ExpressionFactory.betweenExp("abc", v1, v2);
		assertEquals(Expression.BETWEEN, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.OBJ_PATH, path.getType());
	}

	@Test
	public void testBetweenDbExp() {
		Object v1 = new Object();
		Object v2 = new Object();
		Expression exp = ExpressionFactory.betweenDbExp("abc", v1, v2);
		assertEquals(Expression.BETWEEN, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DB_PATH, path.getType());
	}

	@Test
	public void testNotBetweenExp() {
		Object v1 = new Object();
		Object v2 = new Object();
		Expression exp = ExpressionFactory.notBetweenExp("abc", v1, v2);
		assertEquals(Expression.NOT_BETWEEN, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.OBJ_PATH, path.getType());
	}

	@Test
	public void testNotBetweenDbExp() {
		Object v1 = new Object();
		Object v2 = new Object();
		Expression exp = ExpressionFactory.notBetweenDbExp("abc", v1, v2);
		assertEquals(Expression.NOT_BETWEEN, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DB_PATH, path.getType());
	}

	@Test
	public void testGreaterExp() {
		Object v = new Object();
		Expression exp = ExpressionFactory.greaterExp("abc", v);
		assertEquals(Expression.GREATER_THAN, exp.getType());
	}

	@Test
	public void testGreaterDbExp() {
		Object v = new Object();
		Expression exp = ExpressionFactory.greaterDbExp("abc", v);
		assertEquals(Expression.GREATER_THAN, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DB_PATH, path.getType());
	}

	@Test
	public void testGreaterOrEqualExp() {
		Object v = new Object();
		Expression exp = ExpressionFactory.greaterOrEqualExp("abc", v);
		assertEquals(Expression.GREATER_THAN_EQUAL_TO, exp.getType());
	}

	@Test
	public void testGreaterOrEqualDbExp() {
		Object v = new Object();
		Expression exp = ExpressionFactory.greaterOrEqualDbExp("abc", v);
		assertEquals(Expression.GREATER_THAN_EQUAL_TO, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DB_PATH, path.getType());
	}

	@Test
	public void testLessExp() {
		Object v = new Object();
		Expression exp = ExpressionFactory.lessExp("abc", v);
		assertEquals(Expression.LESS_THAN, exp.getType());
	}

	@Test
	public void testLessDbExp() {
		Object v = new Object();
		Expression exp = ExpressionFactory.lessDbExp("abc", v);
		assertEquals(Expression.LESS_THAN, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DB_PATH, path.getType());
	}

	@Test
	public void testLessOrEqualExp() {
		Object v = new Object();
		Expression exp = ExpressionFactory.lessOrEqualExp("abc", v);
		assertEquals(Expression.LESS_THAN_EQUAL_TO, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.OBJ_PATH, path.getType());
	}

	@Test
	public void testLessOrEqualDbExp() {
		Object v = new Object();
		Expression exp = ExpressionFactory.lessOrEqualDbExp("abc", v);
		assertEquals(Expression.LESS_THAN_EQUAL_TO, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DB_PATH, path.getType());
	}

	@Test
	public void testInExp1() {
		Expression exp = ExpressionFactory.inExp("abc", "a", "b");
		assertEquals(Expression.IN, exp.getType());
	}

	@Test
	public void testInExp2() {
		List<Object> v = new ArrayList<>();
		v.add("a");
		v.add("b");
		Expression exp = ExpressionFactory.inExp("abc", v);
		assertEquals(Expression.IN, exp.getType());
	}

	@Test
	public void testInExp3() {
		List<Object> v = new ArrayList<>();
		Expression exp = ExpressionFactory.inExp("abc", v);
		assertEquals(Expression.FALSE, exp.getType());
	}

	@Test
	public void testNotInExp1() {
		Expression exp = ExpressionFactory.notInExp("abc", "a", "b");
		assertEquals(Expression.NOT_IN, exp.getType());
	}

	@Test
	public void testNotInExp2() {
		List<Object> v = new ArrayList<>();
		v.add("a");
		v.add("b");
		Expression exp = ExpressionFactory.notInExp("abc", v);
		assertEquals(Expression.NOT_IN, exp.getType());
	}

	@Test
	public void testNotInExp3() {
		List<Object> v = new ArrayList<>();
		Expression exp = ExpressionFactory.notInExp("abc", v);
		assertEquals(Expression.TRUE, exp.getType());
	}

	@Test
	public void testLikeExp() {
		String v = "abc";
		Expression exp = ExpressionFactory.likeExp("abc", v);
		assertEquals(Expression.LIKE, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.OBJ_PATH, path.getType());
	}

	@Test
	public void testLikeDbExp() {
		String v = "abc";
		Expression exp = ExpressionFactory.likeDbExp("abc", v);
		assertEquals(Expression.LIKE, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DB_PATH, path.getType());
	}

	@Test
	public void testLikeExpEscape() {
		String v = "abc";
		Expression exp = ExpressionFactory.likeExp("=abc", v, '=');
		assertEquals(Expression.LIKE, exp.getType());

		assertEquals('=', ((ASTLike) exp).getEscapeChar());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.OBJ_PATH, path.getType());
	}

	@Test
	public void testLikeIgnoreCaseExp() {
		String v = "abc";
		Expression exp = ExpressionFactory.likeIgnoreCaseExp("abc", v);
		assertEquals(Expression.LIKE_IGNORE_CASE, exp.getType());
		assertEquals(0, ((ASTLikeIgnoreCase) exp).getEscapeChar());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.OBJ_PATH, path.getType());
	}

	@Test
	public void testLikeIgnoreCaseExpEscape() {
		String v = "abc";
		Expression exp = ExpressionFactory.likeIgnoreCaseExp("=abc", v, '=');
		assertEquals(Expression.LIKE_IGNORE_CASE, exp.getType());
		assertEquals('=', ((ASTLikeIgnoreCase) exp).getEscapeChar());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.OBJ_PATH, path.getType());
	}

	@Test
	public void testLikeIgnoreCaseDbExp() {
		String v = "abc";
		Expression exp = ExpressionFactory.likeIgnoreCaseDbExp("abc", v);
		assertEquals(Expression.LIKE_IGNORE_CASE, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DB_PATH, path.getType());
	}

	@Test
	public void testNotLikeIgnoreCaseExp() {
		String v = "abc";
		Expression exp = ExpressionFactory.notLikeIgnoreCaseExp("abc", v);
		assertEquals(Expression.NOT_LIKE_IGNORE_CASE, exp.getType());
	}

	// testing CAY-941 bug
	@Test
	public void testLikeExpNull() {
		Expression exp = ExpressionFactory.likeExp("abc", null);
		assertEquals(Expression.LIKE, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.OBJ_PATH, path.getType());
		assertNull(exp.getOperand(1));
	}

	@Test
	public void testMatchAllExp() {
		// create expressions and check the counts,
		// leaf count should be (2N) : 2 leafs for each pair
		// node count should be (2N + 1) for nodes with more than 1 pair
		// and 2N for a single pair : 2 nodes for each pair + 1 list node
		// where N is map size

		// check for N in (1..3)
		for (int n = 1; n <= 3; n++) {
			Map<String, Object> map = new HashMap<>();

			// populate map
			for (int i = 1; i <= n; i++) {
				map.put("k" + i, "v" + i);
			}

			Expression exp = ExpressionFactory.matchAllExp(map, Expression.LESS_THAN);
			assertNotNull(exp);
			handler.traverseExpression(exp);

			// assert statistics
			handler.assertConsistency();
			assertEquals("Failed: " + exp, 2 * n, handler.getLeafs());
			assertEquals("Failed: " + exp, n < 2 ? 2 * n : 2 * n + 1, handler.getNodeCount());
		}
	}

	@Test
	public void testJoinExp() {
		// create expressions and check the counts,
		// leaf count should be (2N) : 2 leafs for each expression
		// node count should be N > 1 ? 2 * N + 1 : 2 * N
		// where N is map size

		// check for N in (1..5)
		for (int n = 1; n <= 5; n++) {
			Collection<Expression> list = new ArrayList<>();

			// populate map
			for (int i = 1; i <= n; i++) {
				list.add(ExpressionFactory.matchExp(("k" + i), "v" + i));
			}

			Expression exp = ExpressionFactory.joinExp(Expression.AND, list);
			assertNotNull(exp);
			handler.traverseExpression(exp);

			// assert statistics
			handler.assertConsistency();
			assertEquals("Failed: " + exp, 2 * n, handler.getLeafs());
			assertEquals("Failed: " + exp, n > 1 ? 2 * n + 1 : 2 * n, handler.getNodeCount());
		}
	}

	@Test
	public void testAnd_Collection() {
		Expression e1 = ExpressionFactory.matchExp("a", 1);
		Expression e2 = ExpressionFactory.matchExp("b", 2);
		Expression e3 = ExpressionFactory.matchExp("c", "C");

		Collection<Expression> c = Arrays.asList(e1, e2, e3);
		Expression e = ExpressionFactory.and(c);

		assertEquals("(a = 1) and (b = 2) and (c = \"C\")", e.toString());
	}

	@Test
	public void testAnd_Collection_OneElement() {
		Expression e1 = ExpressionFactory.matchExp("a", 1);

		Collection<Expression> c = Collections.singletonList(e1);
		Expression e = ExpressionFactory.and(c);

		assertEquals("a = 1", e.toString());
	}

	@Test
	public void testAnd_Collection_Empty() {

		Expression e = ExpressionFactory.and(Collections.emptyList());

		// hmm... is this really a valid return value?
		assertNull(e);
	}

	@Test
	public void testAnd_Vararg() {
		Expression e1 = ExpressionFactory.matchExp("a", 1);
		Expression e2 = ExpressionFactory.matchExp("b", 2);
		Expression e3 = ExpressionFactory.matchExp("c", "C");

		Expression e = ExpressionFactory.and(e1, e2, e3);

		assertEquals("(a = 1) and (b = 2) and (c = \"C\")", e.toString());
	}

	@Test
	public void testAnd_Vararg_OneElement() {
		Expression e1 = ExpressionFactory.matchExp("a", 1);
		Expression e = ExpressionFactory.and(e1);
		assertEquals("a = 1", e.toString());
	}

	@Test
	public void testAnd_Vararg_Empty() {

		Expression e = ExpressionFactory.and();

		// hmm... is this really a valid return value?
		assertNull(e);
	}

	@Test
	public void testOr_Collection() {
		Expression e1 = ExpressionFactory.matchExp("a", 1);
		Expression e2 = ExpressionFactory.matchExp("b", 2);
		Expression e3 = ExpressionFactory.matchExp("c", "C");

		Collection<Expression> c = Arrays.asList(e1, e2, e3);
		Expression e = ExpressionFactory.or(c);

		assertEquals("(a = 1) or (b = 2) or (c = \"C\")", e.toString());
	}

	@Test
	public void testOr_Vararg() {
		Expression e1 = ExpressionFactory.matchExp("a", 1);
		Expression e2 = ExpressionFactory.matchExp("b", 2);
		Expression e3 = ExpressionFactory.matchExp("c", "C");

		Expression e = ExpressionFactory.or(e1, e2, e3);

		assertEquals("(a = 1) or (b = 2) or (c = \"C\")", e.toString());
	}

	@Test
	public void testExp_Long() {
		Expression e = ExpressionFactory.exp("216201000180L");
		assertEquals(216201000180L, e.evaluate(new Object()));
	}

	@Test
	public void testExp_Path() {
		Expression e1 = ExpressionFactory.exp("object.path");
		assertEquals(Expression.OBJ_PATH, e1.getType());

		Expression e2 = ExpressionFactory.exp("db:object.path");
		assertEquals(Expression.DB_PATH, e2.getType());

		Expression e3 = ExpressionFactory.exp("object+.path");
		assertEquals(Expression.OBJ_PATH, e3.getType());

		Expression e4 = ExpressionFactory.exp("db:object.path+");
		assertEquals(Expression.DB_PATH, e4.getType());
	}

	@Test
	public void testExp_Scalar() {
		Expression e1 = ExpressionFactory.exp("a = 'abc'");
		assertEquals("abc", e1.getOperand(1));
	}

	@Test
	public void testExp_Enum() {
		Expression e1 = ExpressionFactory.exp("a = enum:org.apache.cayenne.exp.ExpEnum1.ONE");
		assertEquals(ExpEnum1.ONE, e1.getOperand(1));

		Expression e2 = ExpressionFactory.exp("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO");
		assertEquals(ExpEnum1.TWO, e2.getOperand(1));

		Expression e3 = ExpressionFactory.exp("a = enum:org.apache.cayenne.exp.ExpEnum1.THREE");
		assertEquals(ExpEnum1.THREE, e3.getOperand(1));
	}

	@Test
	public void testExp_EnumValid1() {
		Bean a = new Bean();
		a.setA(ExpEnum1.TWO);
		Expression exp = ExpressionFactory.exp("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO");
		Object result = exp.evaluate(a);
		assertEquals(Boolean.TRUE, result);
	}

	@Test(expected = ExpressionException.class)
	public void testExp_EnumInvalid1() {
		Bean a = new Bean();
		a.setA(ExpEnum1.TWO);
		Expression exp = ExpressionFactory.exp("a = enum:org.apache.cayenne.exp.ExpEnum1.BOGUS");
		exp.evaluate(a);
	}

	@Test(expected = ExpressionException.class)
	public void testExp_EnumInvalid2() {
		ExpressionFactory.exp("a = enum:BOGUS");
	}

	@Test
	public void testExp_Vararg_InAsValues() {
		Expression e = ExpressionFactory.exp("k1 in ($ap, $bp)", "a", "b");
		assertEquals("k1 in (\"a\", \"b\")", e.toString());
	}

	@Test
	public void testPathExp() {
		assertEquals("abc.xyz", ExpressionFactory.pathExp("abc.xyz").toString());
	}

	@Test
	public void testDbPathExp() {
		assertEquals("db:abc.xyz", ExpressionFactory.dbPathExp("abc.xyz").toString());
	}

	@Test
	public void testFuncExp() {
		Expression e = ExpressionFactory.exp("trim(abc.xyz)");
		assertEquals(ASTTrim.class, e.getClass());
	}

	@Test
	public void testExpWithAlias() {
		Expression expression = ExpressionFactory.exp("paintings#p1.galleries#p2.name = 'Test'");
		assertEquals("p1.p2.name", expression.getOperand(0).toString());
		assertEquals("paintings", ((ASTObjPath)expression.getOperand(0)).getPathAliases().get("p1"));
		assertEquals("galleries", ((ASTObjPath)expression.getOperand(0)).getPathAliases().get("p2"));
	}

	@Test
	public void testExpWithAliasAndOuterJoin() {
		Expression expression = ExpressionFactory.exp("paintings#p1+.name = 'Test'");
		assertEquals("p1.name", expression.getOperand(0).toString());
		assertEquals("paintings+", ((ASTObjPath)expression.getOperand(0)).getPathAliases().get("p1"));
	}

	@Test(expected = CayenneRuntimeException.class)
	public void testExpWithTheSameAliasToDiffSegments() {
		ExpressionFactory.exp("paintings#p1.gallery#p1.name = 'Test'");
	}

    // CAY-2081
    @Test(expected = ExpressionException.class)
    public void testExceptionInParse() {
        ExpressionFactory.exp("name like %32_65415'");
    }

	@Test
	public void testMatchDbIdExp() {
		String v = "abc";
		Expression exp = ExpressionFactory.matchDbIdExp("abc", v);
		assertEquals(Expression.EQUAL_TO, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DBID_PATH, path.getType());
	}

	@Test
	public void testNoMatchDbIdExp() {
		String v = "abc";
		Expression exp = ExpressionFactory.noMatchDbIdExp("abc", v);
		assertEquals(Expression.NOT_EQUAL_TO, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DBID_PATH, path.getType());
	}

	@Test
	public void testInDbIdExp() {
		String v = "abc";
		Expression exp = ExpressionFactory.inDbIdExp("abc", v);
		assertEquals(Expression.IN, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DBID_PATH, path.getType());
	}

	@Test
	public void testNotInDbIdExp() {
		String v = "abc";
		Expression exp = ExpressionFactory.notInDbIdExp("abc", v);
		assertEquals(Expression.NOT_IN, exp.getType());

		Expression path = (Expression) exp.getOperand(0);
		assertEquals(Expression.DBID_PATH, path.getType());
	}

	@Test
	public void testExp_StringLiteral_SingleQuoted() {
		assertEquals("p = \"a\"", ExpressionFactory.exp("p = 'a'").toString());
		assertEquals("p = \"\\\\\"", ExpressionFactory.exp("p = '\\\\'").toString());
		assertEquals("p = \"+\"", ExpressionFactory.exp("p = '+'").toString());
		assertEquals("p = \"\\'\"", ExpressionFactory.exp("p = '\\''").toString());
		assertEquals("p = \"\\\"\"", ExpressionFactory.exp("p = '\"'").toString());
		assertEquals("p = \"/\"", ExpressionFactory.exp("p = '/'").toString());
	}

	@Test
	public void testExp_StringLiteral_DoubleQuoted() {
		assertEquals("p = \"a\"", ExpressionFactory.exp("p = \"a\"").toString());
		assertEquals("p = \"\\\\\"", ExpressionFactory.exp("p = \"\\\\\"").toString());
		assertEquals("p = \"+\"", ExpressionFactory.exp("p = \"+\"").toString());
		assertEquals("p = \"\\'\"", ExpressionFactory.exp("p = \"\\'\"").toString());
		assertEquals("p = \"\\\"\"", ExpressionFactory.exp("p = \"\\\"\"").toString());
		assertEquals("p = \"/\"", ExpressionFactory.exp("p = \"/\"").toString());
	}

	public static class Bean {
		public ExpEnum1 a;

		public ExpEnum1 getA() {
			return a;
		}

		public void setA(ExpEnum1 a) {
			this.a = a;
		}
	}
}
