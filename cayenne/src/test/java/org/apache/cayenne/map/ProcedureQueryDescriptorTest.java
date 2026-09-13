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

package org.apache.cayenne.map;

import java.util.Map;

import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcedureQueryDescriptorTest {

    @Test
    public void buildQueryWithParameters() {
        ProcedureQueryDescriptor descriptor = QueryDescriptor.procedureQueryDescriptor();
        descriptor.setRoot("FakeRoot");

        Map<String, Object> params = Map.of("a", "1", "b", "2");
        ProcedureQuery query = descriptor.buildQuery(params);

        assertEquals(params, query.getParameters());
    }

    @Test
    public void getQueryRoot() {
        Procedure procedure = new Procedure("p1");

        ProcedureQueryDescriptor builder = QueryDescriptor.procedureQueryDescriptor();
        builder.setRoot(procedure);
        builder.setResultEntityName("A");

        ProcedureQuery<?> query = builder.buildQuery();
        assertSame(procedure, query.getRoot());
        assertEquals("A", query.getResultEntityName());
    }

    @Test
    public void getQueryProperties() {
        ProcedureQueryDescriptor builder = QueryDescriptor.procedureQueryDescriptor();
        builder.setProperty(QueryDescriptor.FETCH_LIMIT_PROPERTY, "5");
        builder.setProperty(QueryDescriptor.FETCH_OFFSET_PROPERTY, "2");
        builder.setProperty(QueryDescriptor.PAGE_SIZE_PROPERTY, "10");
        builder.setProperty(QueryDescriptor.STATEMENT_FETCH_SIZE_PROPERTY, "6");
        builder.setProperty(QueryDescriptor.FETCHING_DATA_ROWS_PROPERTY, "true");
        builder.setProperty(QueryDescriptor.CACHE_STRATEGY_PROPERTY, "LOCAL_CACHE");
        builder.setProperty(QueryDescriptor.CACHE_GROUPS_PROPERTY, "g1");
        builder.setProperty(ProcedureQueryDescriptor.COLUMN_NAME_CAPITALIZATION_PROPERTY, "UPPER");

        ProcedureQuery<?> query = builder.buildQuery();
        assertEquals(5, query.getFetchLimit());
        assertEquals(2, query.getFetchOffset());
        assertEquals(10, query.getPageSize());
        assertEquals(6, query.getStatementFetchSize());
        assertTrue(query.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, query.getCacheStrategy());
        assertEquals("g1", query.getCacheGroup());
        assertEquals(CapsStrategy.UPPER, query.getColumnNamesCapitalization());
    }

    @Test
    public void columnNamesCapitalization() {
        ProcedureQueryDescriptor builder = QueryDescriptor.procedureQueryDescriptor();
        assertNull(builder.getColumnNamesCapitalization());

        builder.setColumnNamesCapitalization(CapsStrategy.LOWER);
        assertEquals("LOWER", builder.getProperty(ProcedureQueryDescriptor.COLUMN_NAME_CAPITALIZATION_PROPERTY));
        assertEquals(CapsStrategy.LOWER, builder.buildQuery().getColumnNamesCapitalization());

        builder.setColumnNamesCapitalization(null);
        assertNull(builder.getProperty(ProcedureQueryDescriptor.COLUMN_NAME_CAPITALIZATION_PROPERTY));
    }
}
