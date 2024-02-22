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

import java.time.LocalDate;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cayenne.exp.ExpressionFactory.exp;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class DatePropertyTest {

    private DateProperty<LocalDate> property;

    @Before
    public void createProperty() {
        property = new DateProperty<>(CayennePath.of("path"), null, LocalDate.class);
    }

    @Test
    public void year() {
        Expression exp = property.year().getExpression();
        assertEquals(exp("year(path)"), exp);
    }

    @Test
    public void month() {
        Expression exp = property.month().getExpression();
        assertEquals(exp("month(path)"), exp);
    }

    @Test
    public void dayOfMonth() {
        Expression exp = property.dayOfMonth().getExpression();
        assertEquals(exp("dayOfMonth(path)"), exp);
    }

    @Test
    public void hour() {
        Expression exp = property.hour().getExpression();
        assertEquals(exp("hour(path)"), exp);
    }

    @Test
    public void minute() {
        Expression exp = property.minute().getExpression();
        assertEquals(exp("minute(path)"), exp);
    }

    @Test
    public void second() {
        Expression exp = property.second().getExpression();
        assertEquals(exp("second(path)"), exp);
    }

    @Test
    public void max() {
        Expression exp = property.max().getExpression();
        assertEquals(exp("max(path)"), exp);
    }

    @Test
    public void min() {
        Expression exp = property.min().getExpression();
        assertEquals(exp("min(path)"), exp);
    }

    @Test
    public void alias() {
        DateProperty<LocalDate> aliased = property.alias("test");
        assertEquals("test", aliased.getAlias());
        assertEquals("test", aliased.getName());
        assertEquals(exp("path"), aliased.getExpression());
    }
}