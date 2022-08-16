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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

import static org.apache.cayenne.exp.ExpressionFactory.*;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class PropertyFactoryTest {

    @Test
    public void createBase() {
        BaseProperty<Integer> property = PropertyFactory
                .createBase("path", Integer.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path"), property.getExpression());
        assertEquals(Integer.class, property.getType());
    }

    @Test
    public void createBase1() {
        BaseProperty<Boolean> property = PropertyFactory
                .createBase("path", exp("path = 1"), Boolean.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path = 1"), property.getExpression());
        assertEquals(Boolean.class, property.getType());
    }

    @Test
    public void createBase2() {
        BaseProperty<Boolean> property = PropertyFactory
                .createBase(exp("path = 1"), Boolean.class);

        assertNull(property.getName());
        assertEquals(exp("path = 1"), property.getExpression());
        assertEquals(Boolean.class, property.getType());
    }

    @Test
    public void createString() {
        StringProperty<String> property = PropertyFactory
                .createString("path", String.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path"), property.getExpression());
        assertEquals(String.class, property.getType());
    }

    @Test
    public void createString1() {
        StringProperty<String> property = PropertyFactory
                .createString(exp("path"), String.class);

        assertNull(property.getName());
        assertEquals(exp("path"), property.getExpression());
        assertEquals(String.class, property.getType());
    }

    @Test
    public void createString2() {
        StringProperty<StringBuilder> property = PropertyFactory
                .createString("path", exp("concat(path, 'abc')"), StringBuilder.class);

        assertEquals("path", property.getName());
        assertEquals(exp("concat(path, 'abc')"), property.getExpression());
        assertEquals(StringBuilder.class, property.getType());
    }

    @Test
    public void createNumeric() {
        NumericProperty<Integer> property = PropertyFactory
                .createNumeric("path", Integer.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path"), property.getExpression());
        assertEquals(Integer.class, property.getType());
    }

    @Test
    public void createNumeric1() {
        NumericProperty<Integer> property = PropertyFactory
                .createNumeric(exp("path + 1"), Integer.class);

        assertNull(property.getName());
        assertEquals(exp("path + 1"), property.getExpression());
        assertEquals(Integer.class, property.getType());
    }

    @Test
    public void createNumeric2() {
        NumericProperty<Double> property = PropertyFactory
                .createNumeric("path", exp("path / 2"), Double.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path / 2"), property.getExpression());
        assertEquals(Double.class, property.getType());
    }

    @Test
    public void createDate() {
        DateProperty<LocalDate> property = PropertyFactory
                .createDate("path", LocalDate.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path"), property.getExpression());
        assertEquals(LocalDate.class, property.getType());
    }

    @Test
    public void createDate1() {
        DateProperty<LocalDate> property = PropertyFactory
                .createDate(exp("year(path)"), LocalDate.class);

        assertNull(property.getName());
        assertEquals(exp("year(path)"), property.getExpression());
        assertEquals(LocalDate.class, property.getType());
    }

    @Test
    public void createDate2() {
        DateProperty<LocalDate> property = PropertyFactory
                .createDate("path", exp("year(path)"), LocalDate.class);

        assertEquals("path", property.getName());
        assertEquals(exp("year(path)"), property.getExpression());
        assertEquals(LocalDate.class, property.getType());
    }

    @Test
    public void createEntity() {
        EntityProperty<Artist> property = PropertyFactory
                .createEntity("path", Artist.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path"), property.getExpression());
        assertEquals(Artist.class, property.getType());
    }

    @Test
    public void createEntity1() {
        EntityProperty<Artist> property = PropertyFactory
                .createEntity(exp("path+.subpath"), Artist.class);

        assertNull(property.getName());
        assertEquals(exp("path+.subpath"), property.getExpression());
        assertEquals(Artist.class, property.getType());
    }

    @Test
    public void createEntity2() {
        EntityProperty<Artist> property = PropertyFactory
                .createEntity("path", exp("path+.subpath"), Artist.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path+.subpath"), property.getExpression());
        assertEquals(Artist.class, property.getType());
    }

    @Test
    public void createSelf() {
        EntityProperty<Artist> property = PropertyFactory.createSelf(Artist.class);

        assertNull(property.getName());
        assertEquals(fullObjectExp(), property.getExpression());
        assertEquals(Artist.class, property.getType());
    }

    @Test
    public void createSelf1() {
        EntityProperty<Artist> property = PropertyFactory
                .createSelf(exp("path"), Artist.class);

        assertNull(property.getName());
        assertEquals(fullObjectExp(exp("path")), property.getExpression());
        assertEquals(Artist.class, property.getType());
    }

    @Test
    public void createList() {
        ListProperty<Artist> property = PropertyFactory
                .createList("path", Artist.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path"), property.getExpression());
        assertEquals(List.class, property.getType());
        assertEquals(Artist.class, property.getEntityType());
    }

    @Test
    public void createList1() {
        ListProperty<Artist> property = PropertyFactory
                .createList("path", exp("path+.sub"), Artist.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path+.sub"), property.getExpression());
        assertEquals(List.class, property.getType());
        assertEquals(Artist.class, property.getEntityType());
    }

    @Test
    public void createSet() {
        SetProperty<Artist> property = PropertyFactory
                .createSet("path", Artist.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path"), property.getExpression());
        assertEquals(Set.class, property.getType());
        assertEquals(Artist.class, property.getEntityType());
    }

    @Test
    public void createSet1() {
        SetProperty<Artist> property = PropertyFactory
                .createSet("path", exp("path+.sub"), Artist.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path+.sub"), property.getExpression());
        assertEquals(Set.class, property.getType());
        assertEquals(Artist.class, property.getEntityType());
    }

    @Test
    public void createMap() {
        MapProperty<Integer, Artist> property = PropertyFactory
                .createMap("path", Integer.class, Artist.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path"), property.getExpression());
        assertEquals(Map.class, property.getType());
        assertEquals(Integer.class, property.getKeyType());
        assertEquals(Artist.class, property.getEntityType());
    }

    @Test
    public void createMap1() {
        MapProperty<Integer, Artist> property = PropertyFactory
                .createMap("path", exp("path+.sub"), Integer.class, Artist.class);

        assertEquals("path", property.getName());
        assertEquals(exp("path+.sub"), property.getExpression());
        assertEquals(Map.class, property.getType());
        assertEquals(Integer.class, property.getKeyType());
        assertEquals(Artist.class, property.getEntityType());
    }
}