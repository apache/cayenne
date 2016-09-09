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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.exp.parser.PatternMatchNode;
import org.apache.cayenne.reflect.TstJavaBean;
import org.junit.Test;

public class PropertyTest {

	@Test
	public void testIn() {
		Property<String> p = new Property<String>("x.y");

		Expression e1 = p.in("a");
		assertEquals("x.y in (\"a\")", e1.toString());

		Expression e2 = p.in("a", "b");
		assertEquals("x.y in (\"a\", \"b\")", e2.toString());

		Expression e3 = p.in(Arrays.asList("a", "b"));
		assertEquals("x.y in (\"a\", \"b\")", e3.toString());
	}

	@Test
	public void testGetFrom() {
		TstJavaBean bean = new TstJavaBean();
		bean.setIntField(7);
		final Property<Integer> INT_FIELD = new Property<Integer>("intField");
		assertEquals(Integer.valueOf(7), INT_FIELD.getFrom(bean));
	}

	@Test
	public void testGetFromNestedProperty() {
		TstJavaBean bean = new TstJavaBean();
		TstJavaBean nestedBean = new TstJavaBean();
		nestedBean.setIntField(7);
		bean.setObjectField(nestedBean);
		final Property<Integer> OBJECT_FIELD_INT_FIELD = new Property<Integer>("objectField.intField");
		assertEquals(Integer.valueOf(7), OBJECT_FIELD_INT_FIELD.getFrom(bean));
	}

	@Test
	public void testGetFromNestedNull() {
		TstJavaBean bean = new TstJavaBean();
		bean.setObjectField(null);
		Property<Integer> OBJECT_FIELD_INT_FIELD = new Property<Integer>("objectField.intField");
		assertNull(OBJECT_FIELD_INT_FIELD.getFrom(bean));
	}

	@Test
	public void testGetFromAll() {
		TstJavaBean bean = new TstJavaBean();
		bean.setIntField(7);

		TstJavaBean bean2 = new TstJavaBean();
		bean2.setIntField(8);

		List<TstJavaBean> beans = Arrays.asList(bean, bean2);

		final Property<Integer> INT_FIELD = new Property<Integer>("intField");
		assertEquals(Arrays.asList(7, 8), INT_FIELD.getFromAll(beans));
	}

	@Test
	public void testSetIn() {
		TstJavaBean bean = new TstJavaBean();
		final Property<Integer> INT_FIELD = new Property<Integer>("intField");
		INT_FIELD.setIn(bean, 7);
		assertEquals(7, bean.getIntField());
	}

	@Test
	public void testSetInNestedProperty() {
		TstJavaBean bean = new TstJavaBean();
		bean.setObjectField(new TstJavaBean());

		final Property<Integer> OBJECT_FIELD_INT_FIELD = new Property<Integer>("objectField.intField");

		OBJECT_FIELD_INT_FIELD.setIn(bean, 7);
		assertEquals(7, ((TstJavaBean) bean.getObjectField()).getIntField());
	}

	@Test
	public void testSetInNestedNull() {
		TstJavaBean bean = new TstJavaBean();
		bean.setObjectField(null);
		final Property<Integer> OBJECT_FIELD_INT_FIELD = new Property<Integer>("objectField.intField");
		OBJECT_FIELD_INT_FIELD.setIn(bean, 7);
	}

	@Test
	public void testSetInAll() {
		TstJavaBean bean = new TstJavaBean();
		TstJavaBean bean2 = new TstJavaBean();
		List<TstJavaBean> beans = Arrays.asList(bean, bean2);

		final Property<Integer> INT_FIELD = new Property<Integer>("intField");
		INT_FIELD.setInAll(beans, 7);
		assertEquals(7, bean.getIntField());
		assertEquals(7, bean2.getIntField());
	}

	@Test
	public void testEquals() {
		final Property<Integer> INT_FIELD = new Property<Integer>("intField");
		final Property<Integer> INT_FIELD2 = new Property<Integer>("intField");

		assertTrue(INT_FIELD != INT_FIELD2);
		assertTrue(INT_FIELD.equals(INT_FIELD2));
	}

	@Test
	public void testHashCode() {
		final Property<Integer> INT_FIELD = new Property<Integer>("intField");
		final Property<Integer> INT_FIELD2 = new Property<Integer>("intField");
		final Property<Long> LONG_FIELD = new Property<Long>("longField");

		assertTrue(INT_FIELD.hashCode() == INT_FIELD2.hashCode());
		assertTrue(INT_FIELD.hashCode() != LONG_FIELD.hashCode());
	}

	@Test
	public void testOuter() {
		Property<String> inner = new Property<String>("xyz");
		assertEquals("xyz+", inner.outer().getName());

		Property<String> inner1 = new Property<String>("xyz.xxx");
		assertEquals("xyz.xxx+", inner1.outer().getName());

		Property<String> outer = new Property<String>("xyz+");
		assertEquals("xyz+", outer.outer().getName());
	}

	@Test
	public void testLike() {
		Property<String> p = new Property<String>("prop");
		Expression e = p.like("abc");
		assertEquals("prop like \"abc\"", e.toString());
	}

	@Test
	public void testLikeIgnoreCase() {
		Property<String> p = new Property<String>("prop");
		Expression e = p.likeIgnoreCase("abc");
		assertEquals("prop likeIgnoreCase \"abc\"", e.toString());
	}

	@Test
	public void testLike_NoEscape() {
		Property<String> p = new Property<String>("prop");
		Expression e = p.like("ab%c");
		assertEquals("prop like \"ab%c\"", e.toString());
		assertEquals(0, ((PatternMatchNode) e).getEscapeChar());
	}

	@Test
	public void testContains() {
		Property<String> p = new Property<String>("prop");
		Expression e = p.contains("abc");
		assertEquals("prop like \"%abc%\"", e.toString());
		assertEquals(0, ((PatternMatchNode) e).getEscapeChar());
	}

	@Test
	public void testStartsWith() {
		Property<String> p = new Property<String>("prop");
		Expression e = p.startsWith("abc");
		assertEquals("prop like \"abc%\"", e.toString());
		assertEquals(0, ((PatternMatchNode) e).getEscapeChar());
	}

	@Test
	public void testEndsWith() {
		Property<String> p = new Property<String>("prop");
		Expression e = p.endsWith("abc");
		assertEquals("prop like \"%abc\"", e.toString());
		assertEquals(0, ((PatternMatchNode) e).getEscapeChar());
	}
	
	@Test
	public void testContains_Escape1() {
		Property<String> p = new Property<String>("prop");
		Expression e = p.contains("a%bc");
		assertEquals("prop like \"%a!%bc%\"", e.toString());
		assertEquals('!', ((PatternMatchNode) e).getEscapeChar());
	}
	
	@Test
	public void testContains_Escape2() {
		Property<String> p = new Property<String>("prop");
		Expression e = p.contains("a_!bc");
		assertEquals("prop like \"%a#_!bc%\"", e.toString());
		assertEquals('#', ((PatternMatchNode) e).getEscapeChar());
	}
}
