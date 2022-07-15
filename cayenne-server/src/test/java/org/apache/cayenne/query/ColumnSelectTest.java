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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.0
 */
public class ColumnSelectTest {

    @Test
    public void query() {
        ColumnSelect<Artist> q = new ColumnSelect<>();
        assertNull(q.getColumns());
        assertNull(q.getHaving());
        assertNull(q.getWhere());
    }

    @Test
    public void queryWithOneColumn() {
        ColumnSelect<String> q = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME);
        assertEquals(Collections.singletonList(Artist.ARTIST_NAME), q.getColumns());
        assertTrue(q.singleColumn);
        assertNull(q.getHaving());
        assertNull(q.getWhere());
    }

    @Test
    public void queryWithOneColumn2() {
        ColumnSelect<String> q = ObjectSelect.query(Artist.class).column(Artist.ARTIST_NAME);
        assertEquals(Collections.singletonList(Artist.ARTIST_NAME), q.getColumns());
        assertTrue(q.singleColumn);
        assertNull(q.getHaving());
        assertNull(q.getWhere());
    }

    @Test
    public void queryWithOneColumn3() {
        ColumnSelect<Object[]> q = ObjectSelect.query(Artist.class).columns(Artist.ARTIST_NAME);
        assertEquals(Collections.singletonList(Artist.ARTIST_NAME), q.getColumns());
        assertFalse(q.singleColumn);
        assertNull(q.getHaving());
        assertNull(q.getWhere());
    }

    @Test
    public void queryWithMultipleColumns() {
        ColumnSelect<Object[]> q = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH), q.getColumns());
        assertFalse(q.singleColumn);
        assertNull(q.getHaving());
        assertNull(q.getWhere());
    }

    @Test
    public void queryCount() {
        ColumnSelect<Long> q = ObjectSelect.query(Artist.class).count();
        assertEquals(Collections.singletonList(PropertyFactory.COUNT), q.getColumns());
        assertNull(q.getHaving());
        assertNull(q.getWhere());
    }

    @Test
    public void queryCountWithProperty() {
        ColumnSelect<Long> q = ObjectSelect.query(Artist.class).count(Artist.ARTIST_NAME);
        assertEquals(Collections.singletonList(Artist.ARTIST_NAME.count()), q.getColumns());
        assertNull(q.getHaving());
        assertNull(q.getWhere());
    }

    @Test
    public void queryMinWithProperty() {
        ColumnSelect<BigDecimal> q = ObjectSelect.query(Artist.class).min(Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE));
        assertEquals(Collections.singletonList(Artist.PAINTING_ARRAY.dot(Painting.ESTIMATED_PRICE).min()), q.getColumns());
        assertNull(q.getHaving());
        assertNull(q.getWhere());
    }

    @Test
    public void columns() {
        ColumnSelect<?> q = new ColumnSelect<>();
        assertNull(q.getColumns());
        q.columns(Artist.ARTIST_NAME, Artist.PAINTING_ARRAY);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.PAINTING_ARRAY), q.getColumns());

        q = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH), q.getColumns());
        q.columns(Artist.PAINTING_ARRAY);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY), q.getColumns());
    }


    @Test
    public void havingExpression() {
        ColumnSelect<?> q = new ColumnSelect<>();
        assertNull(q.getHaving());
        assertNull(q.getWhere());

        Expression expTrue = ExpressionFactory.expTrue();
        q.where(expTrue);
        assertNull(q.getHaving());
        assertEquals(expTrue, q.getWhere());

        Expression expFalse = ExpressionFactory.expFalse();
        q.having(expFalse);
        assertEquals(expFalse, q.getHaving());
        assertEquals(expTrue, q.getWhere());
    }

    @Test
    public void havingString() {
        ColumnSelect<?> q = new ColumnSelect<>();
        assertNull(q.getHaving());
        assertNull(q.getWhere());

        Expression expTrue = ExpressionFactory.expTrue();
        q.where(expTrue);
        assertNull(q.getHaving());
        assertEquals(expTrue, q.getWhere());

        Expression expFalse = ExpressionFactory.expFalse();
        q.having("false");
        assertEquals(expFalse, q.getHaving());
        assertEquals(expTrue, q.getWhere());
    }

    @Test
    public void and() {
        ColumnSelect<?> q = new ColumnSelect<>();
        assertNull(q.getHaving());
        assertNull(q.getWhere());

        Expression expTrue = ExpressionFactory.expTrue();
        q.where(expTrue);
        q.and(expTrue);
        assertNull(q.getHaving());
        assertEquals(ExpressionFactory.exp("true and true"), q.getWhere());

        Expression expFalse = ExpressionFactory.expFalse();
        q.having("false");
        q.and(expFalse);
        assertEquals(ExpressionFactory.exp("false and false"), q.getHaving());
        assertEquals(ExpressionFactory.exp("true and true"), q.getWhere());
    }

    @Test
    public void or() {
        ColumnSelect<?> q = new ColumnSelect<>();
        assertNull(q.getHaving());
        assertNull(q.getWhere());

        Expression expTrue = ExpressionFactory.expTrue();
        q.where(expTrue);
        q.or(expTrue);
        assertNull(q.getHaving());
        assertEquals(ExpressionFactory.exp("true or true"), q.getWhere());

        Expression expFalse = ExpressionFactory.expFalse();
        q.having("false");
        q.or(expFalse);
        assertEquals(ExpressionFactory.exp("false or false"), q.getHaving());
        assertEquals(ExpressionFactory.exp("true or true"), q.getWhere());
    }


    @Test
    public void testColumnsAddByOne() {
        ColumnSelect<Artist> q = new ColumnSelect<>();

        assertNull(q.getColumns());

        q.columns(Artist.ARTIST_NAME);
        q.columns(Artist.DATE_OF_BIRTH);
        q.columns(Artist.PAINTING_ARRAY);

        Collection<Property<?>> properties = Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY);
        assertEquals(properties, q.getColumns());
    }

    @Test
    public void testColumnsAddAll() {
        ColumnSelect<Artist> q = new ColumnSelect<>();

        assertNull(q.getColumns());

        q.columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY);
        q.columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY);

        Collection<Property<?>> properties = Arrays.asList(
                Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY,
                Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY); // should it be Set instead of List?
        assertEquals(properties, q.getColumns());
    }

    @Test
    public void testColumnAddByOne() {
        ColumnSelect<Artist> q = new ColumnSelect<>();

        assertNull(q.getColumns());

        q.column(Artist.ARTIST_NAME);
        q.column(Artist.DATE_OF_BIRTH);
        q.column(Artist.PAINTING_ARRAY);

        Collection<Property<?>> properties = Collections.singletonList(Artist.PAINTING_ARRAY);
        assertEquals(properties, q.getColumns());
    }

    @Test
    public void testDistinct() {
        ColumnSelect<Artist> q = new ColumnSelect<>();

        assertFalse(q.distinct);
        assertFalse(q.metaData.isSuppressingDistinct());

        q.distinct();

        assertTrue(q.distinct);
        assertFalse(q.metaData.isSuppressingDistinct());

        q.suppressDistinct();

        assertFalse(q.distinct);
        assertTrue(q.metaData.isSuppressingDistinct());
    }

    @Test
    public void testOffsetCopyFromObjectSelect() {
        ObjectSelect<Artist> select = ObjectSelect.query(Artist.class).offset(10).limit(10);
        assertEquals(10, select.getOffset());
        assertEquals(10, select.getLimit());

        ColumnSelect<String> columnSelect = select.column(Artist.ARTIST_NAME);
        assertEquals(10, columnSelect.getOffset());
        assertEquals(10, columnSelect.getLimit());
    }

}