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
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.resolved.ResolvedProcedure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NamedProcedureQueryTest {

    private static EntityResolver resolver(String procedureName) {
        DataMap map = new DataMap("m");
        map.addProcedure(new Procedure(procedureName));
        return new EntityResolver(List.of(map));
    }

    @Test
    public void buildQueryWithParameters() {
        NamedProcedureQuery namedQuery = new NamedProcedureQuery();
        namedQuery.setRoot("p1");

        Map<String, Object> params = Map.of("a", "1", "b", "2");
        ProcedureQuery<?> query = namedQuery.buildQuery(params);

        assertEquals(params, query.resolve(resolver("p1")).params());
    }

    @Test
    public void getQueryRoot() {
        DataMap map = new DataMap();
        Procedure procedure = new Procedure("p1");
        map.addProcedure(procedure);
        DbEntity table = new DbEntity("A_T");
        map.addDbEntity(table);
        ObjEntity entity = new ObjEntity("A");
        entity.setDbEntity(table);
        map.addObjEntity(entity);

        NamedProcedureQuery builder = new NamedProcedureQuery();
        builder.setRoot(procedure);
        builder.setResultEntityName("A");

        ResolvedProcedure metadata = builder.buildQuery().resolve(new EntityResolver(List.of(map)));
        assertSame(procedure, metadata.procedure());
        assertSame(table, metadata.dbEntity());
        assertSame(map, metadata.dataMap());
    }

    @Test
    public void getQueryRootName() {
        DataMap map = new DataMap();
        Procedure procedure = new Procedure("p1");
        map.addProcedure(procedure);

        NamedProcedureQuery builder = new NamedProcedureQuery();
        builder.setRoot("p1");

        ResolvedProcedure metadata = builder.buildQuery().resolve(new EntityResolver(List.of(map)));
        assertSame(procedure, metadata.procedure());
        assertNull(metadata.dbEntity());
        assertSame(map, metadata.dataMap());
    }

    @Test
    public void getQueryNoRoot() {
        NamedProcedureQuery builder = new NamedProcedureQuery();
        builder.setName("q");
        assertThrows(CayenneRuntimeException.class, builder::buildQuery);

        builder.setRoot(new DataMap("m"));
        assertThrows(CayenneRuntimeException.class, builder::buildQuery);
    }

    @Test
    public void getQueryProperties() {
        NamedProcedureQuery builder = new NamedProcedureQuery();
        builder.setRoot("p1");
        builder.setFetchLimit(5);
        builder.setFetchOffset(2);
        builder.setPageSize(10);
        builder.setStatementFetchSize(6);
        builder.setFetchingDataRows(true);
        builder.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        builder.setCacheGroup("g1");
        builder.setColumnNamesCapitalization(CapsStrategy.UPPER);

        ResolvedProcedure resolved = builder.buildQuery().resolve(resolver("p1"));
        assertEquals(5, resolved.fetchLimit());
        assertEquals(2, resolved.fetchOffset());
        assertEquals(10, resolved.pageSize());
        assertEquals(6, resolved.statementFetchSize());
        assertTrue(resolved.fetchingDataRows());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, resolved.cacheStrategy());
        assertEquals("g1", resolved.cacheGroup());
        assertEquals(CapsStrategy.UPPER, resolved.columnNameCaps());
    }

    @Test
    public void columnNamesCapitalization() {
        NamedProcedureQuery builder = new NamedProcedureQuery();
        builder.setRoot("p1");
        assertNull(builder.getColumnNamesCapitalization());

        builder.setColumnNamesCapitalization(CapsStrategy.LOWER);
        assertEquals(CapsStrategy.LOWER, builder.getColumnNamesCapitalization());
        assertEquals(CapsStrategy.LOWER, builder.buildQuery().resolve(resolver("p1")).columnNameCaps());

        builder.setColumnNamesCapitalization(null);
        assertNull(builder.getColumnNamesCapitalization());
    }
}
