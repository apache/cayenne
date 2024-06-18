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

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 */
public class ProcedureQueryTest {

    @Test
    public void testCreateQuery() {
        ProcedureQuery template = new ProcedureQuery();
        Query clone = template.createQuery(Collections.EMPTY_MAP);
        assertTrue(clone instanceof ProcedureQuery);
        assertNotSame(template, clone);
    }

    @Test
    public void testColumnNameCapitalization() {
        ProcedureQuery q1 = new ProcedureQuery();
        assertSame(CapsStrategy.DEFAULT, q1.getColumnNamesCapitalization());
        q1.setColumnNamesCapitalization(CapsStrategy.UPPER);
        assertEquals(CapsStrategy.UPPER, q1.getColumnNamesCapitalization());
    }

    @Test
    public void testCreateQueryWithParameters() {
        Map params = new HashMap();
        params.put("a", "1");
        params.put("b", "2");

        ProcedureQuery template = new ProcedureQuery();
        ProcedureQuery clone = (ProcedureQuery) template.createQuery(params);

        assertEquals(params, clone.getParameters());
    }

    @Test
    public void testResultEntityName() {
        ProcedureQuery query = new ProcedureQuery();
        assertNull(query.getResultEntityName());

        query.setResultEntityName("abc.AAAA");
        assertSame("abc.AAAA", query.getResultEntityName());
    }

    @Test
    public void testResultDescriptors() {
        ProcedureQuery query = new ProcedureQuery();

        assertNotNull(query.getResultDescriptors());
        assertTrue(query.getResultDescriptors().isEmpty());

        ColumnDescriptor[] descriptor = new ColumnDescriptor[5];
        query.addResultDescriptor(descriptor);
        assertEquals(1, query.getResultDescriptors().size());
        assertTrue(query.getResultDescriptors().contains(descriptor));

        query.removeResultDescriptor(descriptor);
        assertNotNull(query.getResultDescriptors());
        assertTrue(query.getResultDescriptors().isEmpty());
    }
}
