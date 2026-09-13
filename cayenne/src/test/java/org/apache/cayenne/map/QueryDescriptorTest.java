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

import org.apache.cayenne.query.QueryCacheStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryDescriptorTest {

    @Test
    public void defaults() {
        QueryDescriptor descriptor = QueryDescriptor.descriptor("Unknown");
        assertEquals(0, descriptor.getFetchLimit());
        assertEquals(0, descriptor.getFetchOffset());
        assertEquals(0, descriptor.getPageSize());
        assertEquals(0, descriptor.getStatementFetchSize());
        assertFalse(descriptor.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.NO_CACHE, descriptor.getCacheStrategy());
        assertNull(descriptor.getCacheGroup());
    }

    @Test
    public void typedSettersStoreProperties() {
        QueryDescriptor descriptor = QueryDescriptor.descriptor("Unknown");
        descriptor.setFetchLimit(5);
        descriptor.setFetchOffset(2);
        descriptor.setPageSize(10);
        descriptor.setStatementFetchSize(6);
        descriptor.setFetchingDataRows(true);
        descriptor.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        descriptor.setCacheGroup("g1");

        assertEquals("5", descriptor.getProperty(QueryDescriptor.FETCH_LIMIT_PROPERTY));
        assertEquals("2", descriptor.getProperty(QueryDescriptor.FETCH_OFFSET_PROPERTY));
        assertEquals("10", descriptor.getProperty(QueryDescriptor.PAGE_SIZE_PROPERTY));
        assertEquals("6", descriptor.getProperty(QueryDescriptor.STATEMENT_FETCH_SIZE_PROPERTY));
        assertEquals("true", descriptor.getProperty(QueryDescriptor.FETCHING_DATA_ROWS_PROPERTY));
        assertEquals("SHARED_CACHE", descriptor.getProperty(QueryDescriptor.CACHE_STRATEGY_PROPERTY));
        assertEquals("g1", descriptor.getProperty(QueryDescriptor.CACHE_GROUPS_PROPERTY));

        assertEquals(5, descriptor.getFetchLimit());
        assertEquals(2, descriptor.getFetchOffset());
        assertEquals(10, descriptor.getPageSize());
        assertEquals(6, descriptor.getStatementFetchSize());
        assertTrue(descriptor.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, descriptor.getCacheStrategy());
        assertEquals("g1", descriptor.getCacheGroup());
    }

    @Test
    public void nullRemovesProperty() {
        QueryDescriptor descriptor = QueryDescriptor.descriptor("Unknown");
        descriptor.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        descriptor.setCacheGroup("g1");

        descriptor.setCacheStrategy(null);
        descriptor.setCacheGroup(null);

        assertFalse(descriptor.getProperties().containsKey(QueryDescriptor.CACHE_STRATEGY_PROPERTY));
        assertFalse(descriptor.getProperties().containsKey(QueryDescriptor.CACHE_GROUPS_PROPERTY));
        assertEquals(QueryCacheStrategy.NO_CACHE, descriptor.getCacheStrategy());
        assertNull(descriptor.getCacheGroup());
    }

    @Test
    public void unknownCacheStrategyFallsBackToDefault() {
        QueryDescriptor descriptor = QueryDescriptor.descriptor("Unknown");
        descriptor.setProperty(QueryDescriptor.CACHE_STRATEGY_PROPERTY, "NO_SUCH_STRATEGY");
        assertEquals(QueryCacheStrategy.NO_CACHE, descriptor.getCacheStrategy());
    }

    @Test
    public void legacyCacheGroupList() {
        QueryDescriptor descriptor = QueryDescriptor.descriptor("Unknown");

        descriptor.setProperty(QueryDescriptor.CACHE_GROUPS_PROPERTY, "g1,g2");
        assertEquals("g1", descriptor.getCacheGroup());

        descriptor.setProperty(QueryDescriptor.CACHE_GROUPS_PROPERTY, ",g2");
        assertEquals("g2", descriptor.getCacheGroup());

        descriptor.setProperty(QueryDescriptor.CACHE_GROUPS_PROPERTY, "");
        assertNull(descriptor.getCacheGroup());
    }
}
