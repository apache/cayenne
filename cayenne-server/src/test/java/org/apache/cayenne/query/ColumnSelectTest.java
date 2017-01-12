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

package org.apache.cayenne.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.0
 */
public class ColumnSelectTest {

    @Test
    public void query() throws Exception {
        ColumnSelect<Artist> q = ColumnSelect.query(Artist.class);
        assertNull(q.getColumns());
        assertNull(q.getHaving());
    }

    @Test
    public void queryWithColumn() throws Exception {
        ColumnSelect<String> q = ColumnSelect.query(Artist.class, Artist.ARTIST_NAME);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME), q.getColumns());
        assertNull(q.getHaving());
    }

    @Test
    public void queryWithColumns() throws Exception {
        ColumnSelect<Object[]> q = ColumnSelect.query(Artist.class, Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH), q.getColumns());
        assertNull(q.getHaving());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void columns() throws Exception {
        ColumnSelect q = ColumnSelect.query(Artist.class);
        assertNull(q.getColumns());
        q.columns();
        assertNull(q.getColumns());
        q.columns(Artist.ARTIST_NAME, Artist.PAINTING_ARRAY);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.PAINTING_ARRAY), q.getColumns());

        q = ColumnSelect.query(Artist.class, Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH), q.getColumns());
        q.columns(Artist.PAINTING_ARRAY);
        assertEquals(Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY), q.getColumns());
    }


    @Test
    public void havingExpression() throws Exception {
        ColumnSelect q = ColumnSelect.query(Artist.class);
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
    public void havingString() throws Exception {
        ColumnSelect q = ColumnSelect.query(Artist.class);
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
    public void and() throws Exception {
        ColumnSelect q = ColumnSelect.query(Artist.class);
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
    public void or() throws Exception {
        ColumnSelect q = ColumnSelect.query(Artist.class);
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
        ColumnSelect<Artist> q = ColumnSelect.query(Artist.class);

        assertEquals(null, q.getColumns());

        q.columns(Artist.ARTIST_NAME);
        q.columns();
        q.columns(Artist.DATE_OF_BIRTH);
        q.columns();
        q.columns(Artist.PAINTING_ARRAY);
        q.columns();

        Collection<Property<?>> properties = Arrays.asList(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY);
        assertEquals(properties, q.getColumns());
    }

    @Test
    public void testColumnsAddAll() {
        ColumnSelect<Artist> q = ColumnSelect.query(Artist.class);

        assertEquals(null, q.getColumns());

        q.columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY);
        q.columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY);

        Collection<Property<?>> properties = Arrays.asList(
                Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY,
                Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH, Artist.PAINTING_ARRAY); // should it be Set instead of List?
        assertEquals(properties, q.getColumns());
    }

    @Test
    public void testColumnAddByOne() {
        ColumnSelect<Artist> q = ColumnSelect.query(Artist.class);

        assertEquals(null, q.getColumns());

        q.column(Artist.ARTIST_NAME);
        q.columns();
        q.column(Artist.DATE_OF_BIRTH);
        q.columns();
        q.column(Artist.PAINTING_ARRAY);
        q.columns();

        Collection<Property<?>> properties = Collections.<Property<?>>singletonList(Artist.PAINTING_ARRAY);
        assertEquals(properties, q.getColumns());
    }

}