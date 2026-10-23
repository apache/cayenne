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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcedureQueryTest {

    @Test
    public void constructorDataRows() {
        ProcedureQuery<DataRow> query = new ProcedureQuery<>("SomeProcedure");
        assertEquals("SomeProcedure", query.getProcedureName());
        assertNull(query.getResultType());
        assertNull(query.getResultEntityName());
        assertTrue(query.isFetchingDataRows());
    }

    @Test
    public void constructorResultType() {
        ProcedureQuery<Artist> query = new ProcedureQuery<>("SomeProcedure", Artist.class);
        assertEquals("SomeProcedure", query.getProcedureName());
        assertSame(Artist.class, query.getResultType());
        assertNull(query.getResultEntityName());
        assertFalse(query.isFetchingDataRows());
    }

    @Test
    public void query() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("SomeProcedure");
        assertEquals("SomeProcedure", query.getProcedureName());
        assertNull(query.getResultType());
        assertNull(query.getResultEntityName());
        assertTrue(query.isFetchingDataRows());
    }

    @Test
    public void queryResultType() {
        ProcedureQuery<Artist> query = ProcedureQuery.query("SomeProcedure", Artist.class);
        assertSame(Artist.class, query.getResultType());
        assertNull(query.getResultEntityName());
        assertFalse(query.isFetchingDataRows());
    }

    @Test
    public void entityQuery() {
        ProcedureQuery<Persistent> query = ProcedureQuery.entityQuery("SomeProcedure", "abc.AAAA");
        assertEquals("SomeProcedure", query.getProcedureName());
        assertNull(query.getResultType());
        assertEquals("abc.AAAA", query.getResultEntityName());
        assertFalse(query.isFetchingDataRows());
    }

    @Test
    public void columnNameCapitalization() {
        ProcedureQuery q1 = new ProcedureQuery("SomeProcedure");
        assertSame(CapsStrategy.DEFAULT, q1.getColumnNamesCapitalization());
        q1.setColumnNamesCapitalization(CapsStrategy.UPPER);
        assertEquals(CapsStrategy.UPPER, q1.getColumnNamesCapitalization());
    }

    @Test
    public void resultDescriptors() {
        ProcedureQuery query = new ProcedureQuery("SomeProcedure");

        assertNotNull(query.getResultDescriptors());
        assertTrue(query.getResultDescriptors().isEmpty());

        ProcedureColumn[] descriptor = new ProcedureColumn[5];
        query.addResultDescriptor(descriptor);
        assertEquals(1, query.getResultDescriptors().size());
        assertTrue(query.getResultDescriptors().contains(descriptor));

        query.removeResultDescriptor(descriptor);
        assertNotNull(query.getResultDescriptors());
        assertTrue(query.getResultDescriptors().isEmpty());
    }
}
