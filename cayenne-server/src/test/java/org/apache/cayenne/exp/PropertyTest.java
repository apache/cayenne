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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.exp.parser.ASTAbs;
import org.apache.cayenne.exp.parser.ASTAvg;
import org.apache.cayenne.exp.parser.ASTConcat;
import org.apache.cayenne.exp.parser.ASTCount;
import org.apache.cayenne.exp.parser.ASTDistinct;
import org.apache.cayenne.exp.parser.ASTLength;
import org.apache.cayenne.exp.parser.ASTLocate;
import org.apache.cayenne.exp.parser.ASTLower;
import org.apache.cayenne.exp.parser.ASTMax;
import org.apache.cayenne.exp.parser.ASTMin;
import org.apache.cayenne.exp.parser.ASTMod;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.exp.parser.ASTSqrt;
import org.apache.cayenne.exp.parser.ASTSubstring;
import org.apache.cayenne.exp.parser.ASTSum;
import org.apache.cayenne.exp.parser.ASTTrim;
import org.apache.cayenne.exp.parser.ASTUpper;
import org.apache.cayenne.exp.parser.PatternMatchNode;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.reflect.TstJavaBean;
import org.junit.Test;

@Deprecated
public class PropertyTest {

    @Test
    public void testPath() {
        Property<String> p = Property.create("x.y", String.class);
        Expression pp = p.path();
        assertEquals(ExpressionFactory.exp("x.y"), pp);
    }

    @Test
    public void testIn() {
        Property<String> p = Property.create("x.y", String.class);

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
        Property<Integer> INT_FIELD = Property.create("intField", Integer.class);
        assertEquals(Integer.valueOf(7), INT_FIELD.getFrom(bean));
    }

    @Test
    public void testGetFromNestedProperty() {
        TstJavaBean bean = new TstJavaBean();
        TstJavaBean nestedBean = new TstJavaBean();
        nestedBean.setIntField(7);
        bean.setObjectField(nestedBean);
        Property<Integer> OBJECT_FIELD_INT_FIELD = Property.create("objectField.intField", Integer.class);
        assertEquals(Integer.valueOf(7), OBJECT_FIELD_INT_FIELD.getFrom(bean));
    }

    @Test
    public void testGetFromNestedNull() {
        TstJavaBean bean = new TstJavaBean();
        bean.setObjectField(null);
        Property<Integer> OBJECT_FIELD_INT_FIELD = Property.create("objectField.intField", Integer.class);
        assertNull(OBJECT_FIELD_INT_FIELD.getFrom(bean));
    }

    @Test
    public void testGetFromAll() {
        TstJavaBean bean = new TstJavaBean();
        bean.setIntField(7);

        TstJavaBean bean2 = new TstJavaBean();
        bean2.setIntField(8);

        List<TstJavaBean> beans = Arrays.asList(bean, bean2);

        Property<Integer> INT_FIELD = Property.create("intField", Integer.class);
        assertEquals(Arrays.asList(7, 8), INT_FIELD.getFromAll(beans));
    }

    @Test
    public void testSetIn() {
        TstJavaBean bean = new TstJavaBean();
        Property<Integer> INT_FIELD = Property.create("intField", Integer.class);
        INT_FIELD.setIn(bean, 7);
        assertEquals(7, bean.getIntField());
    }

    @Test
    public void testSetInNestedProperty() {
        TstJavaBean bean = new TstJavaBean();
        bean.setObjectField(new TstJavaBean());

        Property<Integer> OBJECT_FIELD_INT_FIELD = Property.create("objectField.intField", Integer.class);

        OBJECT_FIELD_INT_FIELD.setIn(bean, 7);
        assertEquals(7, ((TstJavaBean) bean.getObjectField()).getIntField());
    }

    @Test
    public void testSetInNestedNull() {
        TstJavaBean bean = new TstJavaBean();
        bean.setObjectField(null);
        Property<Integer> OBJECT_FIELD_INT_FIELD = Property.create("objectField.intField", Integer.class);
        OBJECT_FIELD_INT_FIELD.setIn(bean, 7);
    }

    @Test
    public void testSetInAll() {
        TstJavaBean bean = new TstJavaBean();
        TstJavaBean bean2 = new TstJavaBean();
        List<TstJavaBean> beans = Arrays.asList(bean, bean2);

        Property<Integer> INT_FIELD = Property.create("intField", Integer.class);
        INT_FIELD.setInAll(beans, 7);
        assertEquals(7, bean.getIntField());
        assertEquals(7, bean2.getIntField());
    }

    @Test
    public void testEqualsWithName() {
        Property<Integer> INT_FIELD = Property.create("intField", Integer.class);
        Property<Integer> INT_FIELD2 = Property.create("intField", Integer.class);

        assertTrue(INT_FIELD != INT_FIELD2);
        assertTrue(INT_FIELD.equals(INT_FIELD2));
    }

    @Test
    public void testHashCodeWithName() {
        Property<Integer> INT_FIELD = Property.create("intField", Integer.class);
        Property<Integer> INT_FIELD2 = Property.create("intField", Integer.class);
        Property<Long> LONG_FIELD = Property.create("longField", Long.class);

        assertTrue(INT_FIELD.hashCode() == INT_FIELD2.hashCode());
        assertTrue(INT_FIELD.hashCode() != LONG_FIELD.hashCode());
    }

    @Test
    public void testEqualsWithNameAndType() {
        Property<Integer> INT_FIELD = Property.create("intField", Integer.class);
        Property<Integer> INT_FIELD2 = Property.create("intField", Integer.class);

        assertTrue(INT_FIELD != INT_FIELD2);
        assertTrue(INT_FIELD.equals(INT_FIELD2));
    }

    @Test
    public void testHashCodeWithNameAndType() {
        Property<Integer> INT_FIELD = Property.create("intField", Integer.class);
        Property<Integer> INT_FIELD2 = Property.create("intField", Integer.class);
        Property<Long> LONG_FIELD = Property.create("longField", Long.class);

        assertTrue(INT_FIELD.hashCode() == INT_FIELD2.hashCode());
        assertTrue(INT_FIELD.hashCode() != LONG_FIELD.hashCode());
    }

    @Test
    public void testEqualsWithExpAndType() {
        Property<Integer> INT_FIELD = new Property<>(null, ExpressionFactory.exp("1"), Integer.class);
        Property<Integer> INT_FIELD2 = new Property<>(null, ExpressionFactory.exp("1"), Integer.class);

        assertTrue(INT_FIELD != INT_FIELD2);
        assertTrue(INT_FIELD.equals(INT_FIELD2));
    }

    @Test
    public void testHashCodeWithExpAndType() {
        Property<Integer> INT_FIELD = new Property<>(null, ExpressionFactory.exp("1"), Integer.class);
        Property<Integer> INT_FIELD2 = new Property<>(null, ExpressionFactory.exp("1"), Integer.class);
        Property<Integer> INT_FIELD3 = new Property<>(null, ExpressionFactory.exp("2"), Integer.class);

        assertEquals(INT_FIELD.hashCode(), INT_FIELD2.hashCode());
        assertNotEquals(INT_FIELD.hashCode(), INT_FIELD3.hashCode());
    }

    @Test
    public void testOuter() {
        Property<String> inner = Property.create("xyz", String.class);
        assertEquals("xyz+", inner.outer().getName());

        Property<String> inner1 = Property.create("xyz.xxx", String.class);
        assertEquals("xyz.xxx+", inner1.outer().getName());

        Property<String> outer = Property.create("xyz+", String.class);
        assertEquals("xyz+", outer.outer().getName());
    }

    @Test
    public void testLike() {
        Property<String> p = Property.create("prop", String.class);
        Expression e = p.like("abc");
        assertEquals("prop like \"abc\"", e.toString());
    }

    @Test
    public void testLikeIgnoreCase() {
        Property<String> p = Property.create("prop", String.class);
        Expression e = p.likeIgnoreCase("abc");
        assertEquals("prop likeIgnoreCase \"abc\"", e.toString());
    }

    @Test
    public void testLike_NoEscape() {
        Property<String> p = Property.create("prop", String.class);
        Expression e = p.like("ab%c");
        assertEquals("prop like \"ab%c\"", e.toString());
        assertEquals(0, ((PatternMatchNode) e).getEscapeChar());
    }

    @Test
    public void testContains() {
        Property<String> p = Property.create("prop", String.class);
        Expression e = p.contains("abc");
        assertEquals("prop like \"%abc%\"", e.toString());
        assertEquals(0, ((PatternMatchNode) e).getEscapeChar());
    }

    @Test
    public void testStartsWith() {
        Property<String> p = Property.create("prop", String.class);
        Expression e = p.startsWith("abc");
        assertEquals("prop like \"abc%\"", e.toString());
        assertEquals(0, ((PatternMatchNode) e).getEscapeChar());
    }

    @Test
    public void testEndsWith() {
        Property<String> p = Property.create("prop", String.class);
        Expression e = p.endsWith("abc");
        assertEquals("prop like \"%abc\"", e.toString());
        assertEquals(0, ((PatternMatchNode) e).getEscapeChar());
    }

    @Test
    public void testContains_Escape1() {
        Property<String> p = Property.create("prop", String.class);
        Expression e = p.contains("a%bc");
        assertEquals("prop like \"%a!%bc%\"", e.toString());
        assertEquals('!', ((PatternMatchNode) e).getEscapeChar());
    }

    @Test
    public void testContains_Escape2() {
        Property<String> p = Property.create("prop", String.class);
        Expression e = p.contains("a_!bc");
        assertEquals("prop like \"%a#_!bc%\"", e.toString());
        assertEquals('#', ((PatternMatchNode) e).getEscapeChar());
    }

    @Test
    public void testExpressionConstructor() {
        Property<Integer> p = Property.create("testPath", new ASTObjPath("test.path"), Integer.class);
        assertEquals("testPath", p.getName());
        Expression ex = p.getExpression();
        assertEquals("test.path", ex.toString());
    }

    @Test
    public void testCreationWithName() {
        Property<String> p1 = new Property<>("p1", String.class);
        assertEquals(String.class, p1.getType());
        assertEquals("p1", p1.getName());
        assertEquals(new ASTObjPath("p1"), p1.getExpression());

        Property<String> p2 = Property.create("p1", String.class);
        assertEquals(p1, p2);
    }

    @Test
    public void testCreationWithExp() {
        Expression exp = FunctionExpressionFactory.currentTime();

        Property<String> p1 = new Property<>(null, exp, String.class);
        assertEquals(String.class, p1.getType());
        assertEquals(null, p1.getName());
        assertEquals(exp, p1.getExpression());

        Property<String> p2 = Property.create(exp, String.class);
        assertEquals(p1, p2);
    }

    @Test
    public void testCreationWithNameAndExp() {
        Expression exp = FunctionExpressionFactory.currentTime();

        Property<String> p1 = new Property<>("p1", exp, String.class);
        assertEquals(String.class, p1.getType());
        assertEquals("p1", p1.getName());
        assertEquals(exp, p1.getExpression());

        Property<String> p2 = Property.create("p1", exp, String.class);
        assertEquals(p1, p2);
    }

    @Test
    public void testAlias() {
        Expression exp = FunctionExpressionFactory.currentTime();

        Property<String> p1 = new Property<>("p1", exp, String.class);
        assertEquals(String.class, p1.getType());
        assertEquals("p1", p1.getName());
        assertEquals(exp, p1.getExpression());

        Property<String> p2 = p1.alias("p2");
        assertEquals(String.class, p2.getType());
        assertEquals("p2", p2.getName());
        assertEquals(exp, p2.getExpression());
    }

    @Test
    public void testCount() {
        Property<String> p = Property.create("test", String.class);
        NumericProperty<Long> newProp = p.count();
        assertTrue(newProp.getExpression() instanceof ASTCount);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }
    
    @Test
    public void testCountDistinct() {
        Property<String> p = Property.create("test", String.class);
        NumericProperty<Long> newProp = p.countDistinct();
        assertTrue(newProp.getExpression() instanceof ASTCount);
        assertTrue(newProp.getExpression().getOperand(0) instanceof ASTDistinct);
        assertEquals(p.getExpression(), ((ASTDistinct)newProp.getExpression().getOperand(0)).getOperand(0));
    }

    @Test
    public void testMin() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.min();
        assertTrue(newProp.getExpression() instanceof ASTMin);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testMax() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.max();
        assertTrue(newProp.getExpression() instanceof ASTMax);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testSum() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.sum();
        assertTrue(newProp.getExpression() instanceof ASTSum);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testAvg() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.avg();
        assertTrue(newProp.getExpression() instanceof ASTAvg);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testAbs() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.abs();
        assertTrue(newProp.getExpression() instanceof ASTAbs);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testMod() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.mod(3.0);
        assertTrue(newProp.getExpression() instanceof ASTMod);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
        assertEquals(3.0, newProp.getExpression().getOperand(1));
    }

    @Test
    public void testSqrt() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.sqrt();
        assertTrue(newProp.getExpression() instanceof ASTSqrt);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testLength() {
        Property<String> p = Property.create("test", String.class);
        Property<Integer> newProp = p.length();
        assertTrue(newProp.getExpression() instanceof ASTLength);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testLocateString() {
        Property<String> p = Property.create("test", String.class);
        Property<Integer> newProp = p.locate("test");
        assertTrue(newProp.getExpression() instanceof ASTLocate);
        assertEquals("test", newProp.getExpression().getOperand(0));
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(1));
    }

    @Test
    public void testLocateProperty() {
        Property<String> p = Property.create("test", String.class);
        Property<String> p2 = Property.create("test2", String.class);
        Property<Integer> newProp = p.locate(p2);
        assertTrue(newProp.getExpression() instanceof ASTLocate);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(1));
        assertEquals(p2.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testSustring() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.substring(1, 2);
        assertTrue(newProp.getExpression() instanceof ASTSubstring);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
        assertEquals(1, newProp.getExpression().getOperand(1));
        assertEquals(2, newProp.getExpression().getOperand(2));
    }

    @Test
    public void testTrim() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.trim();
        assertTrue(newProp.getExpression() instanceof ASTTrim);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testLower() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.lower();
        assertTrue(newProp.getExpression() instanceof ASTLower);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testUpper() {
        Property<String> p = Property.create("test", String.class);
        Property<String> newProp = p.upper();
        assertTrue(newProp.getExpression() instanceof ASTUpper);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
    }

    @Test
    public void testConcat() {
        Property<String> p = Property.create("test", String.class);
        Property<String> p2 = Property.create("concat", String.class);
        Expression exp = new ASTScalar(3);

        Property<String> newProp = p.concat("string", exp, p2);
        assertTrue(newProp.getExpression() instanceof ASTConcat);
        assertEquals(p.getExpression(), newProp.getExpression().getOperand(0));
        assertEquals("string", newProp.getExpression().getOperand(1));
        assertEquals(3, newProp.getExpression().getOperand(2)); // getOperand unwrapping ASTScalar
        assertEquals(p2.getExpression(), newProp.getExpression().getOperand(3));
    }
}
