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

import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EJBQLQueryDescriptorTest {

    @Test
    public void buildQueryWithParameters() {
        EJBQLQueryDescriptor descriptor = QueryDescriptor.ejbqlQueryDescriptor();
        descriptor.setEjbql("select a from Artist a where a.name = :name");

        EJBQLQuery query = descriptor.buildQuery(Map.of("name", "artist1"));

        assertEquals(Map.of("name", "artist1"), query.getNamedParameters());
    }

    @Test
    public void getQueryProperties() {
        EJBQLQueryDescriptor builder = QueryDescriptor.ejbqlQueryDescriptor();
        builder.setEjbql("select a from Artist a");
        builder.setProperty(QueryDescriptor.FETCH_LIMIT_PROPERTY, "5");
        builder.setProperty(QueryDescriptor.FETCH_OFFSET_PROPERTY, "2");
        builder.setProperty(QueryDescriptor.PAGE_SIZE_PROPERTY, "10");
        builder.setProperty(QueryDescriptor.STATEMENT_FETCH_SIZE_PROPERTY, "6");
        builder.setProperty(QueryDescriptor.FETCHING_DATA_ROWS_PROPERTY, "true");
        builder.setProperty(QueryDescriptor.CACHE_STRATEGY_PROPERTY, "SHARED_CACHE");
        builder.setProperty(QueryDescriptor.CACHE_GROUPS_PROPERTY, "g1");

        EJBQLQuery<?> query = builder.buildQuery();
        assertEquals("select a from Artist a", query.getEjbqlStatement());
        assertEquals(5, query.getFetchLimit());
        assertEquals(2, query.getFetchOffset());
        assertEquals(10, query.getPageSize());
        assertEquals(6, query.getStatementFetchSize());
        assertTrue(query.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, query.getCacheStrategy());
        assertEquals("g1", query.getCacheGroup());
    }
}
