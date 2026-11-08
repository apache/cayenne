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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledProcedure;
import org.apache.cayenne.map.compiled.TestMappings;
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

    private final CompiledMapping mapping;
    private final CompiledProcedure procedure;
    private final CompiledEntity entity;

    public ProcedureQueryTest() {
        DataMap map = new DataMap("m");
        map.addProcedure(new Procedure("SomeProcedure"));
        DbEntity table = new DbEntity("ARTIST");
        map.addDbEntity(table);
        ObjEntity artist = new ObjEntity("Artist");
        artist.setDbEntity(table);
        artist.setClassName(Object.class.getName());
        map.addObjEntity(artist);

        mapping = TestMappings.compile(map);
        procedure = mapping.schema().procedure("SomeProcedure");
        entity = mapping.entity("Artist");
    }

    @Test
    public void query() {
        ResolvedProcedure resolved = ProcedureQuery.query("SomeProcedure").resolve(mapping);
        assertSame(procedure, resolved.procedure());
        assertNull(resolved.entity());
        assertTrue(resolved.fetchingDataRows());
    }

    @Test
    public void queryResultType() {
        ResolvedProcedure resolved = ProcedureQuery.query("SomeProcedure", Object.class).resolve(mapping);
        assertSame(entity, resolved.entity());
        assertFalse(resolved.fetchingDataRows());
    }

    @Test
    public void entityQuery() {
        ProcedureQuery<Persistent> query = ProcedureQuery.entityQuery("SomeProcedure", "Artist");
        ResolvedProcedure resolved = query.resolve(mapping);
        assertSame(procedure, resolved.procedure());
        assertSame(entity, resolved.entity());
        assertFalse(resolved.fetchingDataRows());
    }

    @Test
    public void unknownProcedure() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("NoSuchProcedure");
        assertThrows(CayenneRuntimeException.class, () -> query.resolve(mapping));
    }

    @Test
    public void capsStrategy() {
        ProcedureQuery<DataRow> q1 = ProcedureQuery.query("SomeProcedure");
        assertSame(CapsStrategy.DEFAULT, q1.resolve(mapping).columnNameCaps());
        q1.capsStrategy(CapsStrategy.UPPER);
        assertEquals(CapsStrategy.UPPER, q1.resolve(mapping).columnNameCaps());
    }

    @Test
    public void resultDescriptors() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("SomeProcedure");
        assertTrue(query.resolve(mapping).resultDescriptors().isEmpty());

        ProcedureColumn[] descriptor = new ProcedureColumn[5];
        query.resultDescriptor(descriptor);
        List<ProcedureColumn[]> descriptors = query.resolve(mapping).resultDescriptors();
        assertEquals(1, descriptors.size());
        assertSame(descriptor, descriptors.get(0));
    }

    @Test
    public void params() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("SomeProcedure").param("a", 1).param("b", null);
        ResolvedProcedure resolved = query.resolve(mapping);
        assertEquals(2, resolved.params().size());
        assertEquals(1, resolved.params().get("a"));
        assertNull(resolved.params().get("b"));
    }

    @Test
    public void cacheStrategyResetsGroup() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("SomeProcedure").sharedCache("g1");
        assertEquals(QueryCacheStrategy.SHARED_CACHE, query.resolve(mapping).cacheStrategy());
        assertEquals("g1", query.resolve(mapping).cacheGroup());

        query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, query.resolve(mapping).cacheStrategy());
        assertNull(query.resolve(mapping).cacheGroup());
    }

    @Test
    public void resolveReflectsLaterChanges() {
        ProcedureQuery<DataRow> query = ProcedureQuery.query("SomeProcedure").localCache();
        String key1 = query.resolve(mapping).cacheKey(null);
        query.param("a", 1);
        String key2 = query.resolve(mapping).cacheKey(null);
        assertFalse(key1.equals(key2), "parameters added after the first resolve must change the cache key");
    }
}
