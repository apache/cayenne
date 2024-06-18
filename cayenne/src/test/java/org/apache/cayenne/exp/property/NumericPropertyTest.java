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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.cayenne.exp.ExpressionFactory.exp;

/**
 * @since 4.2
 */
public class NumericPropertyTest {

    private NumericProperty<Integer> property;
    private NumericProperty<Integer> other;

    @Before
    public void createProperty() {
        property = new NumericProperty<>(CayennePath.of("path"), null, Integer.class);
        other = new NumericProperty<>(CayennePath.of("other"), null, Integer.class);
    }

    @Test
    public void avg() {
        assertEquals(exp("avg(path)"), property.avg().getExpression());
    }

    @Test
    public void min() {
        assertEquals(exp("min(path)"), property.min().getExpression());
    }

    @Test
    public void max() {
        assertEquals(exp("max(path)"), property.max().getExpression());
    }

    @Test
    public void sum() {
        assertEquals(exp("sum(path)"), property.sum().getExpression());
    }

    @Test
    public void count() {
        assertEquals(exp("count(path)"), property.count().getExpression());
    }

    @Test
    public void modNumber() {
        assertEquals(exp("mod(path, 3)"), property.mod(3).getExpression());
    }

    @Test
    public void modProp() {
        assertEquals(exp("mod(path, other)"), property.mod(other).getExpression());
    }

    @Test
    public void abs() {
        assertEquals(exp("abs(path)"), property.abs().getExpression());
    }

    @Test
    public void sqrt() {
        assertEquals(exp("sqrt(path)"), property.sqrt().getExpression());
    }

    @Test
    public void add() {
        assertEquals(exp("path + 42"), property.add(42).getExpression());
    }

    @Test
    public void addProp() {
        assertEquals(exp("path + other"), property.add(other).getExpression());
    }

    @Test
    public void sub() {
        assertEquals(exp("path - 42"), property.sub(42).getExpression());
    }

    @Test
    public void subProp() {
        assertEquals(exp("path - other"), property.sub(other).getExpression());
    }

    @Test
    public void div() {
        assertEquals(exp("path / 42"), property.div(42).getExpression());
    }

    @Test
    public void divProp() {
        assertEquals(exp("path / other"), property.div(other).getExpression());
    }

    @Test
    public void mul() {
        assertEquals(exp("path * 42"), property.mul(42).getExpression());
    }

    @Test
    public void mulProp() {
        assertEquals(exp("path * other"), property.mul(other).getExpression());
    }

    @Test
    public void neg() {
        assertEquals(exp("- path"), property.neg().getExpression());
    }

    @Test
    public void between() {
        Expression exp1 = exp("path between 42 and 123");
        Expression exp2 = property.between(42, 123);
        assertEquals(exp1, exp2);
    }

    @Test
    public void betweenProp() {
        assertEquals(exp("path between -other and other"), property.between(other.neg(), other));
    }

    @Test
    public void eq() {
        assertEquals(exp("path = 123"), property.eq(123));
    }

    @Test
    public void eqProp() {
        assertEquals(exp("path = other"), property.eq(other));
    }

    @Test
    public void ne() {
        assertEquals(exp("path != 123"), property.ne(123));
    }

    @Test
    public void neProp() {
        assertEquals(exp("path != other"), property.ne(other));
    }

    @Test
    public void inArray() {
        assertEquals(exp("path in (1, 2, 3)"), property.in(1, 2, 3));
    }

    @Test
    public void inCollection() {
        assertEquals(exp("path in (1, 2, 3)"), property.in(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void ninArray() {
        assertEquals(exp("path not in (1, 2, 3)"), property.nin(1, 2, 3));
    }

    @Test
    public void ninCollection() {
        assertEquals(exp("path not in (1, 2, 3)"), property.nin(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void gt() {
        assertEquals(exp("path > 123"), property.gt(123));
    }

    @Test
    public void gtProp() {
        assertEquals(exp("path > other"), property.gt(other));
    }

    @Test
    public void gte() {
        assertEquals(exp("path >= 123"), property.gte(123));
    }

    @Test
    public void gteProp() {
        assertEquals(exp("path >= other"), property.gte(other));
    }

    @Test
    public void lt() {
        assertEquals(exp("path < 42"), property.lt(42));
    }

    @Test
    public void ltProp() {
        assertEquals(exp("path < other"), property.lt(other));
    }

    @Test
    public void lte() {
        assertEquals(exp("path <= 42"), property.lte(42));
    }

    @Test
    public void lteProp() {
        assertEquals(exp("path <= other"), property.lte(other));
    }

    @Test
    public void isTrue() {
        assertEquals(exp("path = true"), property.isTrue());
    }

    @Test
    public void isFalse() {
        assertEquals(exp("path = false"), property.isFalse());
    }

    @Test
    public void alias() {
        assertEquals("path", property.getName());
        assertNull(property.getAlias());

        property = property.alias("alias");

        assertEquals("alias", property.getName());
        assertEquals("alias", property.getAlias());
    }
}