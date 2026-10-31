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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.resolved.ResolvedProcedure;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcedureQueryTest {

    private final DataMap map = new DataMap("m");
    private final Procedure procedure = new Procedure("SomeProcedure");
    private final DbEntity table = new DbEntity("ARTIST");
    private final ObjEntity entity = new ObjEntity("Artist");
    private final EntityResolver resolver;

    public ProcedureQueryTest() {
        map.addProcedure(procedure);
        map.addDbEntity(table);
        entity.setDbEntity(table);
        entity.setClassName(Object.class.getName());
        map.addObjEntity(entity);
        resolver = new EntityResolver(List.of(map));
    }

    @Test
    public void query() {
        ResolvedProcedure resolved = ProcedureQuery.query("SomeProcedure").resolve(resolver);
        assertSame(procedure, resolved.procedure());
        assertNull(resolved.objEntity());
        assertTrue(resolved.fetchingDataRows());
    }

    @Test
    public void queryResultType() {
        ResolvedProcedure resolved = ProcedureQuery.query("SomeProcedure", Object.class).resolve(resolver);
        assertSame(entity, resolved.objEntity());
        assertFalse(resolved.fetchingDataRows());
    }

    @Test
    public void entityQuery() {
        ProcedureQuery<Persistent> query = ProcedureQuery.entityQuery("SomeProcedure", "Artist");
        ResolvedProcedure resolved = query.resolve(resolver);
        assertSame(procedure, resolved.procedure());
        assertSame(entity, resolved.objEntity());
        assertFalse(resolved.fetchingDataRows());
    }

    @Test
    public void unknownProcedure() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("NoSuchProcedure");
        assertThrows(CayenneRuntimeException.class, () -> query.resolve(resolver));
    }

    @Test
    public void capsStrategy() {
        ProcedureQuery<DataRow> q1 = ProcedureQuery.query("SomeProcedure");
        assertSame(CapsStrategy.DEFAULT, q1.resolve(resolver).columnNameCaps());
        q1.capsStrategy(CapsStrategy.UPPER);
        assertEquals(CapsStrategy.UPPER, q1.resolve(resolver).columnNameCaps());
    }

    @Test
    public void resultDescriptors() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("SomeProcedure");
        assertTrue(query.resolve(resolver).resultDescriptors().isEmpty());

        ProcedureColumn[] descriptor = new ProcedureColumn[5];
        query.resultDescriptor(descriptor);
        List<ProcedureColumn[]> descriptors = query.resolve(resolver).resultDescriptors();
        assertEquals(1, descriptors.size());
        assertSame(descriptor, descriptors.get(0));
    }

    @Test
    public void params() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("SomeProcedure").param("a", 1).param("b", null);
        ResolvedProcedure resolved = query.resolve(resolver);
        assertEquals(2, resolved.params().size());
        assertEquals(1, resolved.params().get("a"));
        assertNull(resolved.params().get("b"));
    }

    @Test
    public void cacheStrategyResetsGroup() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("SomeProcedure").sharedCache("g1");
        assertEquals(QueryCacheStrategy.SHARED_CACHE, query.resolve(resolver).cacheStrategy());
        assertEquals("g1", query.resolve(resolver).cacheGroup());

        query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, query.resolve(resolver).cacheStrategy());
        assertNull(query.resolve(resolver).cacheGroup());
    }

    @Test
    public void resolveReflectsLaterChanges() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("SomeProcedure").localCache();
        String key1 = query.resolve(resolver).cacheKey();
        query.param("a", 1);
        String key2 = query.resolve(resolver).cacheKey();
        assertFalse(key1.equals(key2), "parameters added after the first resolve must change the cache key");
    }
}
