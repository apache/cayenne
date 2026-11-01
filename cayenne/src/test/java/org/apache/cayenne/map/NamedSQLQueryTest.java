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

import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.query.*;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSQL;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NamedSQLQueryTest {

    @Test
    public void getQueryType() throws Exception {
        NamedSQLQuery builder = new NamedSQLQuery();
        assertTrue(builder.buildQuery() instanceof SQLQuery);
    }

    @Test
    public void getQueryRoot() throws Exception {
        DataMap map = new DataMap();
        DbEntity table = new DbEntity("A_T");
        map.addDbEntity(table);
        ObjEntity entity = new ObjEntity("A");
        entity.setDbEntity(table);
        map.addObjEntity(entity);

        NamedSQLQuery builder = new NamedSQLQuery();
        builder.setRoot(entity);

        ResolvedQuery metadata = builder.buildQuery().resolve(new EntityResolver(List.of(map)));
        assertSame(table, metadata.dbEntity());
        assertSame(map, metadata.dataMap());
    }

    @Test
    public void getQueryRootDataMap() throws Exception {
        DataMap map = new DataMap("m");

        NamedSQLQuery builder = new NamedSQLQuery();
        builder.setRoot(map);

        ResolvedQuery metadata = builder.buildQuery().resolve(new EntityResolver(List.of(map)));
        assertNull(metadata.dbEntity());
        assertSame(map, metadata.dataMap());
    }

    @Test
    public void getQueryRootUnsupported() throws Exception {
        NamedSQLQuery builder = new NamedSQLQuery();
        builder.setRoot(new DbEntity("T"));
        assertThrows(CayenneRuntimeException.class, builder::buildQuery);
    }

    @Test
    public void getQueryProperties() throws Exception {
        NamedSQLQuery builder = new NamedSQLQuery();
        builder.setPageSize(10);
        builder.setStatementFetchSize(6);
        builder.setFetchingDataRows(true);
        builder.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        builder.setCacheGroup("g1");
        builder.setColumnNamesCapitalization(CapsStrategy.LOWER);

        ResolvedSQL resolved = builder.buildQuery().resolve(new EntityResolver());
        assertEquals(10, resolved.pageSize());
        assertEquals(6, resolved.statementFetchSize());
        assertTrue(resolved.fetchingDataRows());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, resolved.cacheStrategy());
        assertEquals("g1", resolved.cacheGroup());
        assertEquals(CapsStrategy.LOWER, resolved.columnNameCaps());
    }

    @Test
    public void getQueryPropertiesDefaults() throws Exception {
        ResolvedSQL resolved = new NamedSQLQuery().buildQuery().resolve(new EntityResolver());
        assertEquals(0, resolved.fetchLimit());
        assertEquals(0, resolved.fetchOffset());
        assertEquals(0, resolved.pageSize());
        assertEquals(0, resolved.statementFetchSize());
        // a rootless query has nothing to map objects to, so it fetches DataRows
        assertTrue(resolved.fetchingDataRows());
        assertEquals(QueryCacheStrategy.NO_CACHE, resolved.cacheStrategy());
        assertNull(resolved.cacheGroup());
        assertEquals(CapsStrategy.DEFAULT, resolved.columnNameCaps());
    }

    @Test
    public void getQuerySql() throws Exception {
        NamedSQLQuery builder = new NamedSQLQuery();
        builder.setSql("abc");

        assertEquals("abc", builder.buildQuery().resolve(new EntityResolver()).defaultTemplate());
    }

    @Test
    public void buildQueryWithParameters() {
        NamedSQLQuery builder = new NamedSQLQuery();
        builder.setSql("SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($name)");

        Map<String, Object> params = Map.of("name", "artist1");
        assertEquals(params, builder.buildQuery(params).resolve(new EntityResolver()).params());
    }

    @Test
    public void getQueryAdapterSql() throws Exception {
        NamedSQLQuery builder = new NamedSQLQuery();
        builder.getAdapterSql().put("adapter", "abc");

        ResolvedSQL resolved = builder.buildQuery().resolve(new EntityResolver());
        assertNull(resolved.defaultTemplate());
        assertEquals("abc", resolved.templates().get("adapter"));
    }
}
