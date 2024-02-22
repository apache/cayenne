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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTExists;
import org.apache.cayenne.exp.parser.ASTNotExists;
import org.apache.cayenne.exp.parser.ASTSubquery;
import org.apache.cayenne.exp.parser.Node;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class SelfPropertyTest {

    private SelfProperty<Artist> property;

    @Before
    public void createProperty() {
        property = new SelfProperty<>(CayennePath.of("path"), null, Artist.class);
    }

    @Test
    public void testQuery() {
        ObjectSelect<Artist> query = property.query();

        assertNotNull(query);
        assertEquals(Artist.class, query.getEntityType());
        assertNull(query.getWhere());
    }

    @Test
    public void testQueryWithExp() {
        ObjectSelect<Artist> query = property.query(Artist.ARTIST_NAME.eq("test"));

        assertNotNull(query);
        assertEquals(Artist.class, query.getEntityType());
        assertEquals(ExpressionFactory.exp("artistName = 'test'"), query.getWhere());
    }

    @Test
    public void testColumnQuery() {
        ColumnSelect<String> query = property.columnQuery(Artist.ARTIST_NAME);

        assertNotNull(query);
        assertEquals(Artist.class, query.getEntityType());
        assertEquals(1, query.getColumns().size());
        assertEquals(Artist.ARTIST_NAME, query.getColumns().iterator().next());
        assertNull(query.getWhere());
    }

    @Test
    public void testColumnsQuery() {
        ColumnSelect<Object[]> query = property.columnQuery(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);

        assertNotNull(query);
        assertEquals(Artist.class, query.getEntityType());
        assertEquals(2, query.getColumns().size());
        Iterator<Property<?>> iterator = query.getColumns().iterator();
        assertEquals(Artist.ARTIST_NAME, iterator.next());
        assertEquals(Artist.DATE_OF_BIRTH, iterator.next());
        assertNull(query.getWhere());
    }

    @Test
    public void testExists() {
        Expression exp = property.exists(Artist.ARTIST_NAME.eq("test"));

        assertNotNull(exp);
        assertTrue(exp instanceof ASTExists);

        ASTExists exists = (ASTExists) exp;
        Node node = exists.jjtGetChild(0);
        assertTrue(node instanceof ASTSubquery);

        ASTSubquery subquery = (ASTSubquery) node;
        assertTrue(subquery.getQuery().unwrap() instanceof ObjectSelect);

        ObjectSelect<?> subSelect = (ObjectSelect<?>) subquery.getQuery().unwrap();
        assertEquals(Artist.class, subSelect.getEntityType());
        assertEquals(ExpressionFactory.exp("artistName = 'test'"), subSelect.getWhere());
    }

    @Test
    public void testNotExists() {
        Expression exp = property.notExists(Artist.ARTIST_NAME.eq("test"));

        assertNotNull(exp);
        assertTrue(exp instanceof ASTNotExists);

        ASTNotExists exists = (ASTNotExists) exp;
        Node node = exists.jjtGetChild(0);
        assertTrue(node instanceof ASTSubquery);

        ASTSubquery subquery = (ASTSubquery) node;
        assertTrue(subquery.getQuery().unwrap() instanceof ObjectSelect);

        ObjectSelect<?> subSelect = (ObjectSelect<?>) subquery.getQuery().unwrap();
        assertEquals(Artist.class, subSelect.getEntityType());
        assertEquals(ExpressionFactory.exp("artistName = 'test'"), subSelect.getWhere());
    }
}