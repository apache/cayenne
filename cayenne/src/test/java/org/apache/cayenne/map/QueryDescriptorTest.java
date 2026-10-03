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
        QueryDescriptor descriptor = new SQLTemplateDescriptor();
        assertEquals(0, descriptor.getPageSize());
        assertEquals(0, descriptor.getStatementFetchSize());
        assertFalse(descriptor.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.NO_CACHE, descriptor.getCacheStrategy());
        assertNull(descriptor.getCacheGroup());
    }

    @Test
    public void settings() {
        QueryDescriptor descriptor = new SQLTemplateDescriptor();
        descriptor.setPageSize(10);
        descriptor.setStatementFetchSize(6);
        descriptor.setFetchingDataRows(true);
        descriptor.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        descriptor.setCacheGroup("g1");

        assertEquals(10, descriptor.getPageSize());
        assertEquals(6, descriptor.getStatementFetchSize());
        assertTrue(descriptor.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, descriptor.getCacheStrategy());
        assertEquals("g1", descriptor.getCacheGroup());
    }

    @Test
    public void nullCacheStrategyIsDefault() {
        QueryDescriptor descriptor = new SQLTemplateDescriptor();
        descriptor.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        descriptor.setCacheGroup("g1");

        descriptor.setCacheStrategy(null);
        descriptor.setCacheGroup(null);

        assertEquals(QueryCacheStrategy.NO_CACHE, descriptor.getCacheStrategy());
        assertNull(descriptor.getCacheGroup());
    }
}
