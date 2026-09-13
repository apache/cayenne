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

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SelectQueryDescriptorTest {

    @Test
    public void getQueryType() {
        SelectQueryDescriptor builder = QueryDescriptor.selectQueryDescriptor();
        builder.setRoot("FakeRoot");
        assertTrue(builder.buildQuery() instanceof ObjectSelect);
    }

    @Test
    public void getQueryRoot() {
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("A");
        map.addObjEntity(entity);

        SelectQueryDescriptor builder = QueryDescriptor.selectQueryDescriptor();
        builder.setRoot(entity);

        assertTrue(builder.buildQuery() instanceof ObjectSelect);
        assertEquals(entity.getName(), builder.buildQuery().getEntityName());
    }

    @Test
    public void getQueryQualifier() {
        SelectQueryDescriptor builder = QueryDescriptor.selectQueryDescriptor();
        builder.setRoot("FakeRoot");
        builder.setQualifier(ExpressionFactory.exp("abc = 5"));

        ObjectSelect<?> query = builder.buildQuery();

        assertEquals(ExpressionFactory.exp("abc = 5"), query.getWhere());
    }

    @Test
    public void buildQueryWithParameters() {
        SelectQueryDescriptor builder = QueryDescriptor.selectQueryDescriptor();
        builder.setRoot("FakeRoot");
        builder.setQualifier(ExpressionFactory.exp("abc = $a and def = $b"));

        ObjectSelect<?> query = builder.buildQuery(Map.of("a", 5));

        // parameters with no matching value are pruned from the qualifier
        assertEquals(ExpressionFactory.exp("abc = 5"), query.getWhere());
    }

    @Test
    public void buildQueryWithoutParameters() {
        SelectQueryDescriptor builder = QueryDescriptor.selectQueryDescriptor();
        builder.setRoot("FakeRoot");

        assertNull(builder.buildQuery(Map.of("a", 5)).getWhere());
    }

    @Test
    public void getQueryProperties() {
        SelectQueryDescriptor builder = QueryDescriptor.selectQueryDescriptor();
        builder.setRoot("FakeRoot");
        builder.setProperty(QueryDescriptor.FETCH_LIMIT_PROPERTY, "5");
        builder.setProperty(QueryDescriptor.FETCH_OFFSET_PROPERTY, "2");
        builder.setProperty(QueryDescriptor.PAGE_SIZE_PROPERTY, "10");
        builder.setProperty(QueryDescriptor.STATEMENT_FETCH_SIZE_PROPERTY, "6");
        builder.setProperty(QueryDescriptor.FETCHING_DATA_ROWS_PROPERTY, "true");
        builder.setProperty(QueryDescriptor.CACHE_STRATEGY_PROPERTY, "SHARED_CACHE");
        builder.setProperty(QueryDescriptor.CACHE_GROUPS_PROPERTY, "g1");
        builder.setProperty(SelectQueryDescriptor.DISTINCT_PROPERTY, "true");

        ObjectSelect<?> query = builder.buildQuery();
        assertEquals(5, query.getLimit());
        assertEquals(2, query.getOffset());
        assertEquals(10, query.getPageSize());
        assertEquals(6, query.getStatementFetchSize());
        assertTrue(query.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, query.getCacheStrategy());
        assertEquals("g1", query.getCacheGroup());
        assertTrue(query.isDistinct());
    }

    @Test
    public void getQueryPropertiesDefaults() {
        SelectQueryDescriptor builder = QueryDescriptor.selectQueryDescriptor();
        builder.setRoot("FakeRoot");

        ObjectSelect<?> query = builder.buildQuery();
        assertEquals(0, query.getLimit());
        assertEquals(0, query.getOffset());
        assertEquals(0, query.getPageSize());
        assertEquals(0, query.getStatementFetchSize());
        assertFalse(query.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.NO_CACHE, query.getCacheStrategy());
        assertNull(query.getCacheGroup());
        assertFalse(query.isDistinct());
    }

    @Test
    public void typedSetters() {
        SelectQueryDescriptor builder = QueryDescriptor.selectQueryDescriptor();
        builder.setRoot("FakeRoot");
        builder.setFetchLimit(5);
        builder.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        builder.setCacheGroup("g1");
        builder.setFetchingDataRows(true);

        ObjectSelect<?> query = builder.buildQuery();
        assertEquals(5, query.getLimit());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, query.getCacheStrategy());
        assertEquals("g1", query.getCacheGroup());
        assertTrue(query.isFetchingDataRows());
    }
}
