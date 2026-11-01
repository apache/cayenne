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
import org.apache.cayenne.exp.ExistsExp;
import org.apache.cayenne.exp.NotExistsExp;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class SelfPropertyTest {

    private SelfProperty<Artist> property;

    @BeforeEach
    public void createProperty() {
        property = new SelfProperty<>(CayennePath.of("path"), null, Artist.class);
    }

    @Test
    public void query() {
        ObjectSelect<Artist> query = property.query();

        assertNotNull(query);
        assertEquals("from Artist", query.toQL());
    }

    @Test
    public void queryWithExp() {
        ObjectSelect<Artist> query = property.query(Artist.ARTIST_NAME.eq("test"));

        assertNotNull(query);
        assertEquals("from Artist where artistName = \"test\"", query.toQL());
    }

    @Test
    public void columnQuery() {
        ObjectSelect<String> query = property.columnQuery(Artist.ARTIST_NAME);

        assertNotNull(query);
        assertEquals("select artistName from Artist", query.toQL());
    }

    @Test
    public void columnsQuery() {
        ObjectSelect<Object[]> query = property.columnQuery(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH);

        assertNotNull(query);
        assertEquals("select artistName, dateOfBirth from Artist", query.toQL());
    }

    @Test
    public void exists() {
        Expression exp = property.exists(Artist.ARTIST_NAME.eq("test"));

        assertNotNull(exp);
        assertTrue(exp instanceof ExistsExp);

        ExistsExp exists = (ExistsExp) exp;
        Object node = exists.getOperand(0);
        assertTrue(node instanceof SubqueryExp);

        SubqueryExp subquery = (SubqueryExp) node;
        assertTrue(subquery.getQuery() instanceof ObjectSelect);

        ObjectSelect<?> subSelect = (ObjectSelect<?>) subquery.getQuery();
        assertEquals("from Artist where artistName = \"test\"", subSelect.toQL());
    }

    @Test
    public void notExists() {
        Expression exp = property.notExists(Artist.ARTIST_NAME.eq("test"));

        assertNotNull(exp);
        assertTrue(exp instanceof NotExistsExp);

        NotExistsExp exists = (NotExistsExp) exp;
        Object node = exists.getOperand(0);
        assertTrue(node instanceof SubqueryExp);

        SubqueryExp subquery = (SubqueryExp) node;
        assertTrue(subquery.getQuery() instanceof ObjectSelect);

        ObjectSelect<?> subSelect = (ObjectSelect<?>) subquery.getQuery();
        assertEquals("from Artist where artistName = \"test\"", subSelect.toQL());
    }
}
