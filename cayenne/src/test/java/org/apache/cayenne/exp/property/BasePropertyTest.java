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

package org.apache.cayenne.exp.property;

import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTLike;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.reflect.TstJavaBean;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class BasePropertyTest {

    private BaseProperty<Integer> property;

    @Before
    public void createProperty() {
        property = new BaseProperty<>(CayennePath.of("path"), null, Integer.class);
    }


    @Test
    public void testPathExpConstructor() {
        assertEquals(new ASTObjPath("path"), property.getExpression());
    }

    @Test
    public void testCustomExpConstructor() {
        property = new BaseProperty<>(CayennePath.of("path"), new ASTLike(), Integer.class);
        assertEquals(new ASTLike(), property.getExpression());
    }

    @Test
    public void testEq() {
        Expression exp = property.eq(1);
        assertEquals(ExpressionFactory.exp("path = 1"), exp);
    }

    @Test
    public void testNe() {
        Expression exp = property.ne(1);
        assertEquals(ExpressionFactory.exp("path != 1"), exp);
    }

    @Test
    public void testIn() {
        Expression exp = property.in(1, 2, 3);
        assertEquals(ExpressionFactory.exp("path in (1, 2, 3)"), exp);
    }

    @Test
    public void testInCollection() {
        Expression exp = property.in(Arrays.asList(1,2,3));
        assertEquals(ExpressionFactory.exp("path in (1, 2, 3)"), exp);
    }

    @Test
    public void testNin() {
        Expression exp = property.nin(1, 2, 3);
        assertEquals(ExpressionFactory.exp("path not in (1, 2, 3)"), exp);
    }

    @Test
    public void testNinCollection() {
        Expression exp = property.nin(Arrays.asList(1,2,3));
        assertEquals(ExpressionFactory.exp("path not in (1, 2, 3)"), exp);
    }

    @Test
    public void testIsNull() {
        BaseProperty<Integer> property = new BaseProperty<>(CayennePath.of("path"), null, Integer.class);
        Expression exp = property.isNull();
        assertEquals(ExpressionFactory.exp("path = null"), exp);
    }

    @Test
    public void testNotIsNull() {
        Expression exp = property.isNotNull();
        assertEquals(ExpressionFactory.exp("path != null"), exp);
    }

    @Test
    public void testIsTrue() {
        Expression exp = property.isTrue();
        assertEquals(ExpressionFactory.exp("path = true"), exp);
    }

    @Test
    public void testIsFalse() {
        Expression exp = property.isFalse();
        assertEquals(ExpressionFactory.exp("path = false"), exp);
    }

    @Test
    public void testAlias() {
        assertEquals("path", property.getName());
        assertNull(property.getAlias());

        property = property.alias("test");

        assertEquals("test", property.getName());
        assertEquals("test", property.getAlias());
        assertEquals(new ASTObjPath("path"), property.getExpression());
    }

    @Test
    public void testGetFrom() {
        TstJavaBean bean = new TstJavaBean();
        bean.setIntField(7);
        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        assertEquals(Integer.valueOf(7), INT_FIELD.getFrom(bean));
    }

    @Test
    public void testGetFromNestedProperty() {
        TstJavaBean bean = new TstJavaBean();
        TstJavaBean nestedBean = new TstJavaBean();
        nestedBean.setIntField(7);
        bean.setObjectField(nestedBean);
        BaseProperty<Integer> OBJECT_FIELD_INT_FIELD = new BaseProperty<>(CayennePath.of("objectField.intField"), null, Integer.class);
        assertEquals(Integer.valueOf(7), OBJECT_FIELD_INT_FIELD.getFrom(bean));
    }

    @Test
    public void testGetFromNestedNull() {
        TstJavaBean bean = new TstJavaBean();
        bean.setObjectField(null);
        BaseProperty<Integer> OBJECT_FIELD_INT_FIELD = new BaseProperty<>(CayennePath.of("objectField.intField"), null, Integer.class);
        assertNull(OBJECT_FIELD_INT_FIELD.getFrom(bean));
    }

    @Test
    public void testGetFromAll() {
        TstJavaBean bean = new TstJavaBean();
        bean.setIntField(7);

        TstJavaBean bean2 = new TstJavaBean();
        bean2.setIntField(8);

        List<TstJavaBean> beans = Arrays.asList(bean, bean2);

        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        assertEquals(Arrays.asList(7, 8), INT_FIELD.getFromAll(beans));
    }

    @Test
    public void testSetIn() {
        TstJavaBean bean = new TstJavaBean();
        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        INT_FIELD.setIn(bean, 7);
        assertEquals(7, bean.getIntField());
    }

    @Test
    public void testSetInNestedProperty() {
        TstJavaBean bean = new TstJavaBean();
        bean.setObjectField(new TstJavaBean());

        BaseProperty<Integer> OBJECT_FIELD_INT_FIELD = new BaseProperty<>(CayennePath.of("objectField.intField"), null, Integer.class);

        OBJECT_FIELD_INT_FIELD.setIn(bean, 7);
        assertEquals(7, ((TstJavaBean) bean.getObjectField()).getIntField());
    }

    @Test
    public void testSetInNestedNull() {
        TstJavaBean bean = new TstJavaBean();
        bean.setObjectField(null);
        BaseProperty<Integer> OBJECT_FIELD_INT_FIELD = new BaseProperty<>(CayennePath.of("objectField.intField"), null, Integer.class);
        OBJECT_FIELD_INT_FIELD.setIn(bean, 7);
    }

    @Test
    public void testSetInAll() {
        TstJavaBean bean = new TstJavaBean();
        TstJavaBean bean2 = new TstJavaBean();
        List<TstJavaBean> beans = Arrays.asList(bean, bean2);

        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        INT_FIELD.setInAll(beans, 7);
        assertEquals(7, bean.getIntField());
        assertEquals(7, bean2.getIntField());
    }

    @Test
    public void testEqualsWithName() {
        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        BaseProperty<Integer> INT_FIELD2 = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);

        assertNotSame(INT_FIELD, INT_FIELD2);
        assertEquals(INT_FIELD, INT_FIELD2);
    }

    @Test
    public void testHashCodeWithName() {
        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        BaseProperty<Integer> INT_FIELD2 = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        BaseProperty<Long> LONG_FIELD = new BaseProperty<>(CayennePath.of("longField"), null, Long.class);

        assertEquals(INT_FIELD.hashCode(), INT_FIELD2.hashCode());
        assertNotSame(INT_FIELD.hashCode(), LONG_FIELD.hashCode());
    }

    @Test
    public void testEqualsWithNameAndType() {
        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        BaseProperty<Integer> INT_FIELD2 = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);

        assertNotSame(INT_FIELD, INT_FIELD2);
        assertEquals(INT_FIELD, INT_FIELD2);
    }

    @Test
    public void testHashCodeWithNameAndType() {
        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        BaseProperty<Integer> INT_FIELD2 = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        BaseProperty<Long> LONG_FIELD = new BaseProperty<>(CayennePath.of("longField"), null, Long.class);

        assertEquals(INT_FIELD.hashCode(), INT_FIELD2.hashCode());
        assertNotEquals(INT_FIELD.hashCode(), LONG_FIELD.hashCode());
    }

    @Test
    public void testEqualsWithExpAndType() {
        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.EMPTY_PATH, ExpressionFactory.exp("1"), Integer.class);
        BaseProperty<Integer> INT_FIELD2 = new BaseProperty<>(CayennePath.EMPTY_PATH, ExpressionFactory.exp("1"), Integer.class);

        assertNotSame(INT_FIELD, INT_FIELD2);
        assertEquals(INT_FIELD, INT_FIELD2);
    }

    @Test
    public void testHashCodeWithExpAndType() {
        BaseProperty<Integer> INT_FIELD = new BaseProperty<>(CayennePath.EMPTY_PATH, ExpressionFactory.exp("1"), Integer.class);
        BaseProperty<Integer> INT_FIELD2 = new BaseProperty<>(CayennePath.EMPTY_PATH, ExpressionFactory.exp("1"), Integer.class);
        BaseProperty<Integer> INT_FIELD3 = new BaseProperty<>(CayennePath.EMPTY_PATH, ExpressionFactory.exp("2"), Integer.class);

        assertEquals(INT_FIELD.hashCode(), INT_FIELD2.hashCode());
        assertNotEquals(INT_FIELD.hashCode(), INT_FIELD3.hashCode());
    }

    @Test
    public void testFunctionProperty() {
        BaseProperty<Integer> property = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        BaseProperty<Integer> arg = new BaseProperty<>(CayennePath.of("intField2"), null, Integer.class);

        BaseProperty<Integer> operator = property.function("%", Integer.class, arg);
        assertEquals(ExpressionFactory.exp("fn('%', intField, intField2)"), operator.getExpression());
    }

    @Test
    public void testFunctionScalar() {
        BaseProperty<Integer> property = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);

        BaseProperty<Integer> operator = property.function("%", Integer.class, 10);
        assertEquals(ExpressionFactory.exp("fn('%', intField, 10)"), operator.getExpression());
    }

    @Test
    public void testOperatorProperty() {
        BaseProperty<Integer> property = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);
        BaseProperty<Integer> arg = new BaseProperty<>(CayennePath.of("intField2"), null, Integer.class);

        BaseProperty<Integer> operator = property.operator("%", Integer.class, arg);
        assertEquals(ExpressionFactory.exp("op('%', intField, intField2)"), operator.getExpression());
    }

    @Test
    public void testOperatorScalar() {
        BaseProperty<Integer> property = new BaseProperty<>(CayennePath.of("intField"), null, Integer.class);

        BaseProperty<Integer> operator = property.operator("%", Integer.class, 10);
        assertEquals(ExpressionFactory.exp("op('%', intField, 10)"), operator.getExpression());
    }

}