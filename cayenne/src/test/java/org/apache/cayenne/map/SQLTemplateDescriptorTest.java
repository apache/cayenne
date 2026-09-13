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

import org.apache.cayenne.query.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLTemplateDescriptorTest {

    @Test
    public void getQueryType() throws Exception {
        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        assertTrue(builder.buildQuery() instanceof SQLTemplate);
    }

    @Test
    public void getQueryRoot() throws Exception {
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("A");
        map.addObjEntity(entity);

        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        builder.setRoot(entity);

        Query query = builder.buildQuery();
        assertTrue(query instanceof SQLTemplate);
        assertSame(entity, ((SQLTemplate) query).getRoot());
    }

    @Test
    public void getQueryProperties() throws Exception {
        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        builder.setProperty(QueryDescriptor.FETCH_LIMIT_PROPERTY, "5");
        builder.setProperty(QueryDescriptor.FETCH_OFFSET_PROPERTY, "2");
        builder.setProperty(QueryDescriptor.PAGE_SIZE_PROPERTY, "10");
        builder.setProperty(QueryDescriptor.STATEMENT_FETCH_SIZE_PROPERTY, "6");
        builder.setProperty(QueryDescriptor.FETCHING_DATA_ROWS_PROPERTY, "true");
        builder.setProperty(QueryDescriptor.CACHE_STRATEGY_PROPERTY, "LOCAL_CACHE");
        builder.setProperty(QueryDescriptor.CACHE_GROUPS_PROPERTY, "g1");
        builder.setProperty(SQLTemplateDescriptor.COLUMN_NAME_CAPITALIZATION_PROPERTY, "lower");

        SQLTemplate<?> query = builder.buildQuery();
        assertEquals(5, query.getFetchLimit());
        assertEquals(2, query.getFetchOffset());
        assertEquals(10, query.getPageSize());
        assertEquals(6, query.getStatementFetchSize());
        assertTrue(query.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, query.getCacheStrategy());
        assertEquals("g1", query.getCacheGroup());
        assertEquals(CapsStrategy.LOWER, query.getColumnNamesCapitalization());
    }

    @Test
    public void getQueryPropertiesDefaults() throws Exception {
        SQLTemplate<?> query = QueryDescriptor.sqlTemplateDescriptor().buildQuery();
        assertEquals(0, query.getFetchLimit());
        assertEquals(0, query.getFetchOffset());
        assertEquals(0, query.getPageSize());
        assertEquals(0, query.getStatementFetchSize());
        assertFalse(query.isFetchingDataRows());
        assertEquals(QueryCacheStrategy.NO_CACHE, query.getCacheStrategy());
        assertNull(query.getCacheGroup());
        assertEquals(CapsStrategy.DEFAULT, query.getColumnNamesCapitalization());
    }

    @Test
    public void getQuerySql() throws Exception {
        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        builder.setSql("abc");

        SQLTemplate query = builder.buildQuery();
        assertEquals("abc", query.getDefaultTemplate());
    }

    @Test
    public void buildQueryWithParameters() {
        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        builder.setSql("SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($name)");

        Map<String, Object> params = Map.of("name", "artist1");
        SQLTemplate query = builder.buildQuery(params);

        assertEquals(params, query.getParams());
    }

    @Test
    public void getQueryAdapterSql() throws Exception {
        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        builder.getAdapterSql().put("adapter", "abc");

        SQLTemplate query = builder.buildQuery();
        assertNull(query.getDefaultTemplate());
        assertEquals("abc", query.getTemplate("adapter"));
    }
}
