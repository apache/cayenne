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

public class NamedQueryTest {

    @Test
    public void defaults() {
        NamedQuery namedQuery = new NamedSQLQuery();
        assertEquals(0, namedQuery.getPageSize());
        assertEquals(0, namedQuery.getStatementFetchSize());
        assertFalse(namedQuery.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.NO_CACHE, namedQuery.getCacheStrategy());
        assertNull(namedQuery.getCacheGroup());
    }

    @Test
    public void settings() {
        NamedQuery namedQuery = new NamedSQLQuery();
        namedQuery.setPageSize(10);
        namedQuery.setStatementFetchSize(6);
        namedQuery.setFetchingDataRows(true);
        namedQuery.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        namedQuery.setCacheGroup("g1");

        assertEquals(10, namedQuery.getPageSize());
        assertEquals(6, namedQuery.getStatementFetchSize());
        assertTrue(namedQuery.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, namedQuery.getCacheStrategy());
        assertEquals("g1", namedQuery.getCacheGroup());
    }

    @Test
    public void nullCacheStrategyIsDefault() {
        NamedQuery namedQuery = new NamedSQLQuery();
        namedQuery.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        namedQuery.setCacheGroup("g1");

        namedQuery.setCacheStrategy(null);
        namedQuery.setCacheGroup(null);

        assertEquals(QueryCacheStrategy.NO_CACHE, namedQuery.getCacheStrategy());
        assertNull(namedQuery.getCacheGroup());
    }
}
