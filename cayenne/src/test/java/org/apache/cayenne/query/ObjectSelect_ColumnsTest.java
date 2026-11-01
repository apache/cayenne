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

package org.apache.cayenne.query;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectSelect_ColumnsTest {

    @Test
    public void query() {
        ObjectSelect<Artist> q = new ObjectSelect<>();
        assertNull(q.columns);
        assertNull(q.having);
        assertNull(q.where);
    }

    @Test
    public void queryWithOneColumn() {
        ObjectSelect<String> q = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME);
        assertEquals(Collections.singletonList(Artist.ARTIST_NAME), q.columns);
        assertTrue(q.singleColumn);
        assertNull(q.having);
        assertNull(q.where);
    }

    @Test
    public void queryWithOneColumn2() {
        ObjectSelect<String> q = ObjectSelect.query(Artist.class).column(Artist.ARTIST_NAME);
        assertEquals(Collections.singletonList(Artist.ARTIST_NAME), q.columns);
        assertTrue(q.singleColumn);
        assertNull(q.having);
        assertNull(q.where);
    }

    @Test
    public void queryWithOneColumn3() {
        ObjectSelect<Object[]> q = ObjectSelect.query(Artist.class).columns(Artist.ARTIST_NAME);
        assertEquals(Collections.singletonList(Artist.ARTIST_NAME), q.columns);
        assertFalse(q.singleColumn);
        assertNull(q.having);
        assertNull(q.where);
    }

    @Test
    public void queryWithMultipleColumns() {
        ObjectSelect<Object[]> q = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH), q.columns);
        assertFalse(q.singleColumn);
        assertNull(q.having);
        assertNull(q.where);
    }

    @Test
    public void queryCount() {
        ObjectSelect<Long> q = ObjectSelect.query(Artist.class).count();
        assertEquals(Collections.singletonList(PropertyFactory.COUNT), q.columns);
        assertNull(q.having);
        assertNull(q.where);
    }

    @Test
    public void queryCountWithProperty() {
        ObjectSelect<Long> q = ObjectSelect.query(Artist.class).count(Artist.ARTIST_NAME);
        assertEquals(Collections.singletonList(Artist.ARTIST_NAME.count()), q.columns);
        assertNull(q.having);
        assertNull(q.where);
    }

    @Test
    public void queryMinWithProperty() {
        ObjectSelect<BigDecimal> q = ObjectSelect.query(Artist.class).min(Artist.PAINTINGS.dot(Painting.ESTIMATED_PRICE));
        assertEquals(Collections.singletonList(Artist.PAINTINGS.dot(Painting.ESTIMATED_PRICE).min()), q.columns);
        assertNull(q.having);
        assertNull(q.where);
    }

    @Test
    public void columns() {
        ObjectSelect<?> q = new ObjectSelect<>();
        assertNull(q.columns);
        q.columns(Artist.ARTIST_NAME, Artist.PAINTINGS);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.PAINTINGS), q.columns);

        q = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH), q.columns);
        q.columns(Artist.PAINTINGS);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTINGS), q.columns);
    }

    @Test
    public void havingExpression() {
        ObjectSelect<?> q = new ObjectSelect<>();
        assertNull(q.having);
        assertNull(q.where);

        Expression expTrue = ExpressionFactory.expTrue();
        q.where(expTrue);
        assertNull(q.having);
        assertEquals(expTrue, q.where);

        Expression expFalse = ExpressionFactory.expFalse();
        q.having(expFalse);
        assertEquals(expFalse, q.having);
        assertEquals(expTrue, q.where);
    }

    @Test
    public void havingString() {
        ObjectSelect<?> q = new ObjectSelect<>();
        assertNull(q.having);
        assertNull(q.where);

        Expression expTrue = ExpressionFactory.expTrue();
        q.where(expTrue);
        assertNull(q.having);
        assertEquals(expTrue, q.where);

        Expression expFalse = ExpressionFactory.expFalse();
        q.having("false");
        assertEquals(expFalse, q.having);
        assertEquals(expTrue, q.where);
    }

    @Test
    public void and() {
        ObjectSelect<?> q = new ObjectSelect<>();
        assertNull(q.having);
        assertNull(q.where);

        Expression expTrue = ExpressionFactory.expTrue();
        q.where(expTrue);
        q.and(expTrue);
        assertNull(q.having);
        assertEquals(ExpressionFactory.exp("true and true"), q.where);

        Expression expFalse = ExpressionFactory.expFalse();
        q.having("false");
        q.and(expFalse);
        assertEquals(ExpressionFactory.exp("false and false"), q.having);
        assertEquals(ExpressionFactory.exp("true and true"), q.where);
    }

    @Test
    public void or() {
        ObjectSelect<?> q = new ObjectSelect<>();
        assertNull(q.having);
        assertNull(q.where);

        Expression expTrue = ExpressionFactory.expTrue();
        q.where(expTrue);
        q.or(expTrue);
        assertNull(q.having);
        assertEquals(ExpressionFactory.exp("true or true"), q.where);

        Expression expFalse = ExpressionFactory.expFalse();
        q.having("false");
        q.or(expFalse);
        assertEquals(ExpressionFactory.exp("false or false"), q.having);
        assertEquals(ExpressionFactory.exp("true or true"), q.where);
    }

    @Test
    public void testColumnsAddByOne() {
        ObjectSelect<Artist> q = new ObjectSelect<>();

        assertNull(q.columns);

        q.columns(Artist.ARTIST_NAME);
        q.columns(Artist.DATE_OF_BIRTH);
        q.columns(Artist.PAINTINGS);

        Collection<Property<?>> properties = Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTINGS);
        assertEquals(properties, q.columns);
    }

    @Test
    public void testColumnsAddAll() {
        ObjectSelect<Artist> q = new ObjectSelect<>();

        assertNull(q.columns);

        q.columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTINGS);
        q.columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTINGS);

        Collection<Property<?>> properties = Arrays.asList(
                Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTINGS,
                Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTINGS); // should it be Set instead of List?
        assertEquals(properties, q.columns);
    }

    @Test
    public void testColumnAddByOne() {
        ObjectSelect<Artist> q = new ObjectSelect<>();

        assertNull(q.columns);

        q.column(Artist.ARTIST_NAME);
        q.column(Artist.DATE_OF_BIRTH);
        Property<Painting> paintings = Artist.PAINTINGS.flat();
        q.column(paintings);

        Collection<Property<?>> properties = Collections.singletonList(paintings);
        assertEquals(properties, q.columns);
    }

    @Test
    public void testDistinct() {
        ObjectSelect<Artist> q = new ObjectSelect<>();

        assertFalse(q.distinct);
        assertFalse(q.suppressingDistinct);

        q.distinct();

        assertTrue(q.distinct);
        assertFalse(q.suppressingDistinct);

        q.suppressDistinct();

        assertFalse(q.distinct);
        assertTrue(q.suppressingDistinct);
    }

    @Test
    public void testOffsetCopyFromObjectSelect() {
        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class).offset(10).limit(10);
        assertEquals(10, select.fetchOffset);
        assertEquals(10, select.fetchLimit);

        ObjectSelect<String> columnSelect = select.column(Artist.ARTIST_NAME);
        assertEquals(10, columnSelect.fetchOffset);
        assertEquals(10, columnSelect.fetchLimit);
    }

}
