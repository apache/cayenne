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

import org.apache.cayenne.exp.path.CayennePath;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cayenne.exp.ExpressionFactory.exp;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class StringPropertyTest {

    private StringProperty<String> property;
    private StringProperty<String> other;

    @Before
    public void createProperty() {
        property = new StringProperty<>(CayennePath.of("path"), null, String.class);
        other = new StringProperty<>(CayennePath.of("other"), null, String.class);
    }

    @Test
    public void like() {
        assertEquals(exp("path like 'abc'"), property.like("abc"));
    }

    @Test
    public void likeProp() {
        assertEquals(exp("path like other"), property.like(other));
    }

    @Test
    public void likeWithEscape() {
        assertEquals(exp("path like 'abc'"), property.like("abc", '|'));
    }

    @Test
    public void likeIgnoreCase() {
        assertEquals(exp("path likeIgnoreCase 'abc'"), property.likeIgnoreCase("abc"));
    }

    @Test
    public void likeIgnoreCaseProp() {
        assertEquals(exp("path likeIgnoreCase other"), property.likeIgnoreCase(other));
    }

    @Test
    public void nlike() {
        assertEquals(exp("path not like 'abc'"), property.nlike("abc"));
    }

    @Test
    public void nlikeProp() {
        assertEquals(exp("path not like other"), property.nlike(other));
    }

    @Test
    public void nlikeIgnoreCase() {
        assertEquals(exp("path not likeIgnoreCase 'abc'"), property.nlikeIgnoreCase("abc"));
    }

    @Test
    public void nlikeIgnoreCaseProp() {
        assertEquals(exp("path not likeIgnoreCase other"), property.nlikeIgnoreCase(other));
    }

    @Test
    public void contains() {
        assertEquals(exp("path like '%abc%'"), property.contains("abc"));
    }

    @Test
    public void startsWith() {
        assertEquals(exp("path like 'abc%'"), property.startsWith("abc"));
    }

    @Test
    public void endsWith() {
        assertEquals(exp("path like '%abc'"), property.endsWith("abc"));
    }

    @Test
    public void containsIgnoreCase() {
        assertEquals(exp("path likeIgnoreCase '%abc%'"), property.containsIgnoreCase("abc"));
    }

    @Test
    public void startsWithIgnoreCase() {
        assertEquals(exp("path likeIgnoreCase 'abc%'"), property.startsWithIgnoreCase("abc"));
    }

    @Test
    public void endsWithIgnoreCase() {
        assertEquals(exp("path likeIgnoreCase '%abc'"), property.endsWithIgnoreCase("abc"));
    }

    @Test
    public void length() {
        assertEquals(exp("length(path)"), property.length().getExpression());
    }

    @Test
    public void locate() {
        assertEquals(exp("locate('abc', path)"), property.locate("abc").getExpression());
    }

    @Test
    public void locateProp() {
        assertEquals(exp("locate(other, path)"), property.locate(other).getExpression());
    }

    @Test
    public void trim() {
        assertEquals(exp("trim(path)"), property.trim().getExpression());
    }

    @Test
    public void upper() {
        assertEquals(exp("upper(path)"), property.upper().getExpression());
    }

    @Test
    public void lower() {
        assertEquals(exp("lower(path)"), property.lower().getExpression());
    }

    @Test
    public void concat() {
        assertEquals(exp("concat(path, 'abc', ' ', 'def', other)"),
                property.concat("abc", ' ', exp("'def'"), other).getExpression());
    }

    @Test
    public void substring() {
        assertEquals(exp("substring(path, 10, 30)"), property.substring(10, 30).getExpression());
    }

    @Test
    public void substringProp() {
        NumericProperty<Integer> offset = PropertyFactory.createNumeric("offset", Integer.class);
        // length is a function name, so use len here
        NumericProperty<Integer> length = PropertyFactory.createNumeric("len", Integer.class);
        assertEquals(exp("substring(path, offset, len)"), property.substring(offset, length).getExpression());
    }

    @Test
    public void compare() {
        assertEquals(exp("path > 'abc'"),  property.gt("abc"));
        assertEquals(exp("path > other"),  property.gt(other));
        assertEquals(exp("path >= 'abc'"), property.gte("abc"));
        assertEquals(exp("path >= other"), property.gte(other));
        assertEquals(exp("path < 'abc'"),  property.lt("abc"));
        assertEquals(exp("path < other"),  property.lt(other));
        assertEquals(exp("path <= 'abc'"), property.lte("abc"));
        assertEquals(exp("path <= other"), property.lte(other));
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