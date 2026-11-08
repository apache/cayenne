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
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.NamedProcedureQuery;
import org.apache.cayenne.map.NamedQuery;
import org.apache.cayenne.map.NamedSQLQuery;
import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledQuery;
import org.apache.cayenne.map.compiled.TestMappings;
import org.apache.cayenne.ql.ParsedObjectSelect;
import org.apache.cayenne.query.resolved.ResolvedProcedure;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.apache.cayenne.query.resolved.ResolvedSQL;
import org.apache.cayenne.query.resolved.ResolvedSelect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MappedQueryTest {

    private static NamedSelectQuery selectQuery(String ql) {
        NamedSelectQuery namedQuery = new NamedSelectQuery();
        namedQuery.setName("q");
        namedQuery.setQl(ql);
        return namedQuery;
    }

    private static NamedProcedureQuery procedureQuery(Object root) {
        NamedProcedureQuery namedQuery = new NamedProcedureQuery();
        namedQuery.setName("q");
        namedQuery.setRoot(root);
        return namedQuery;
    }

    private static CompiledMapping mapping(DataMap... maps) {
        return TestMappings.compile(maps);
    }

    private static CompiledMapping mapping(DataMap map, NamedQuery namedQuery) {
        map.addNamedQuery(namedQuery);
        return mapping(map);
    }

    private static CompiledMapping fakeRootMapping() {
        DataMap map = new DataMap("map");
        map.addObjEntity(new ObjEntity("FakeRoot"));
        return mapping(map);
    }

    private static CompiledMapping procedureMapping(String procedureName) {
        DataMap map = new DataMap("m");
        map.addProcedure(new Procedure(procedureName));
        return mapping(map);
    }

    // compiles the query within its map, or an ad hoc one, as the runtime only sees compiled queries
    private static CompiledQuery compiled(NamedQuery namedQuery) {
        if (namedQuery.getName() == null) {
            namedQuery.setName("q");
        }
        DataMap map = namedQuery.getDataMap() != null ? namedQuery.getDataMap() : new DataMap("m");
        map.addNamedQuery(namedQuery);
        return mapping(map).query(namedQuery.getName());
    }

    private static Query<?> build(NamedQuery namedQuery) {
        return MappedQuery.query("q").resolveNamedQuery(compiled(namedQuery));
    }

    private static Query<?> build(NamedQuery namedQuery, Map<String, ?> params) {
        return MappedQuery.query("q").params(params).resolveNamedQuery(compiled(namedQuery));
    }

    @Test
    public void resolve() {
        DataMap map = new DataMap("m");
        map.addObjEntity(new ObjEntity("FakeRoot"));
        CompiledMapping mapping = mapping(map, selectQuery("from FakeRoot limit 5"));

        ResolvedQuery resolved = MappedQuery.query("q").resolve(mapping);
        assertInstanceOf(ResolvedSelect.class, resolved);
        assertEquals(5, resolved.fetchLimit());

        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class,
                () -> MappedQuery.query("unknown").resolve(mapping));
        assertTrue(e.getUnlabeledMessage().contains("Unknown query name"), e.getUnlabeledMessage());
    }

    // ---- NamedSelectQuery ----

    @Test
    public void select() {
        ObjectSelect<?> query = (ObjectSelect<?>) build(selectQuery("from FakeRoot where abc = 5"));
        assertEquals("FakeRoot", query.entityName);
        assertEquals(ExpressionFactory.exp("abc = 5"), query.where);
    }

    @Test
    public void selectIsACopy() {
        NamedSelectQuery namedQuery = selectQuery("from FakeRoot");
        assertNotSame(build(namedQuery), build(namedQuery));
    }

    @Test
    public void selectWithParameters() {
        NamedSelectQuery namedQuery = selectQuery("from FakeRoot where abc = $a and def = $b");

        // parameters with no matching value are pruned from the qualifier
        ObjectSelect<?> query = (ObjectSelect<?>) build(namedQuery, Map.of("a", 5));
        assertEquals(ExpressionFactory.exp("abc = 5"), query.where);
    }

    @Test
    public void selectWithoutParameters() {
        NamedSelectQuery namedQuery = selectQuery("from FakeRoot where abc = $a");
        assertNull(((ObjectSelect<?>) build(namedQuery)).where);
        assertEquals(ExpressionFactory.exp("abc = $a"), ParsedObjectSelect.parseUnbound(namedQuery.getQl()).where);
    }

    @Test
    public void selectWithSubqueryParameters() {
        NamedSelectQuery namedQuery = selectQuery(
                "from Artist where exists (from Painting where artist = enclosing(self) and price > $price)");

        ObjectSelect<?> boundQuery = (ObjectSelect<?>) build(namedQuery, Map.of("price", 5));
        SubqueryExp bound = (SubqueryExp) boundQuery.where.getOperand(0);
        assertEquals(ExpressionFactory.exp("artist = enclosing(self) and price > 5"), bound.getQuery().where);

        ObjectSelect<?> prunedQuery = (ObjectSelect<?>) build(namedQuery);
        SubqueryExp pruned = (SubqueryExp) prunedQuery.where.getOperand(0);
        assertEquals(ExpressionFactory.exp("artist = enclosing(self)"), pruned.getQuery().where);
    }

    @Test
    public void selectColumns() {
        NamedSelectQuery namedQuery = selectQuery("select name, count(*) from Artist having count(*) > $min");
        namedQuery.setPageSize(10);
        namedQuery.setFetchingDataRows(true);

        ObjectSelect<?> query = (ObjectSelect<?>) build(namedQuery, Map.of("min", 2));
        assertNotNull(query.columns);
        assertEquals(ExpressionFactory.exp("count(*) > 2"), query.having);
    }

    @Test
    public void selectDbEntity() {
        DataMap map = new DataMap();
        DbEntity entity = new DbEntity("ARTIST");
        map.addDbEntity(entity);

        NamedSelectQuery namedQuery = selectQuery("from db:ARTIST where NAME = 'a'");
        namedQuery.setDataMap(map);

        ObjectSelect<?> query = (ObjectSelect<?>) build(namedQuery);
        assertEquals("ARTIST", query.dbEntityName);
        assertTrue(query.resolve(mapping(map)).fetchingDataRows());
    }

    @Test
    public void selectInvalid() {
        NamedSelectQuery namedQuery = selectQuery("from Artist where");

        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, () -> build(namedQuery));
        assertTrue(e.getUnlabeledMessage().startsWith("Query 'q' can't be parsed"), e.getUnlabeledMessage());
    }

    @Test
    public void selectEmpty() {
        NamedSelectQuery namedQuery = selectQuery(null);
        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class, () -> build(namedQuery));
        assertEquals("Query 'q' is empty", e.getUnlabeledMessage());
    }

    @Test
    public void selectProperties() {
        NamedSelectQuery namedQuery = selectQuery("select distinct self from FakeRoot limit 5 offset 2");
        namedQuery.setPageSize(10);
        namedQuery.setStatementFetchSize(6);
        namedQuery.setFetchingDataRows(true);
        namedQuery.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        namedQuery.setCacheGroup("g1");

        ObjectSelect<?> query = (ObjectSelect<?>) build(namedQuery);
        assertEquals("select distinct self from FakeRoot limit 5 offset 2", query.toQL());

        // the settings that the query String has no syntax for are only visible once the query is resolved
        ResolvedSelect resolved = query.resolve(fakeRootMapping());
        assertEquals(5, resolved.fetchLimit());
        assertEquals(2, resolved.fetchOffset());
        assertEquals(10, resolved.pageSize());
        assertEquals(6, resolved.statementFetchSize());
        assertTrue(resolved.fetchingDataRows());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, resolved.cacheStrategy());
        assertEquals("g1", resolved.cacheGroup());
    }

    @Test
    public void selectPropertiesDefaults() {
        ObjectSelect<?> query = (ObjectSelect<?>) build(selectQuery("from FakeRoot"));
        assertEquals("from FakeRoot", query.toQL());

        ResolvedSelect resolved = query.resolve(fakeRootMapping());
        assertEquals(0, resolved.pageSize());
        assertEquals(0, resolved.statementFetchSize());
        assertFalse(resolved.fetchingDataRows());
        assertEquals(QueryCacheStrategy.NO_CACHE, resolved.cacheStrategy());
        assertNull(resolved.cacheGroup());
    }

    @Test
    public void selectPropertiesOverridden() {
        NamedSelectQuery namedQuery = selectQuery("from FakeRoot limit 5 offset 2");
        namedQuery.setPageSize(10);
        namedQuery.setStatementFetchSize(6);
        namedQuery.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        namedQuery.setCacheGroup("g1");

        ObjectSelect<?> query = (ObjectSelect<?>) MappedQuery.query("q")
                .limit(50)
                .offset(20)
                .pageSize(100)
                .statementFetchSize(60)
                .queryTimeout(7)
                .forceNoCache()
                .resolveNamedQuery(compiled(namedQuery));

        ResolvedSelect resolved = query.resolve(fakeRootMapping());
        assertEquals(50, resolved.fetchLimit());
        assertEquals(20, resolved.fetchOffset());
        assertEquals(100, resolved.pageSize());
        assertEquals(60, resolved.statementFetchSize());
        assertEquals(7, resolved.queryTimeout());
        assertEquals(QueryCacheStrategy.SHARED_CACHE_REFRESH, resolved.cacheStrategy());
        assertEquals("g1", resolved.cacheGroup());
    }

    @Test
    public void forceNoCacheOfNonCachingQuery() {
        NamedSelectQuery namedQuery = selectQuery("from FakeRoot");
        ObjectSelect<?> query = (ObjectSelect<?>) MappedQuery.query("q").forceNoCache().resolveNamedQuery(compiled(namedQuery));
        assertEquals(QueryCacheStrategy.NO_CACHE, query.resolve(fakeRootMapping()).cacheStrategy());

        namedQuery.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        query = (ObjectSelect<?>) MappedQuery.query("q").forceNoCache().resolveNamedQuery(compiled(namedQuery));
        assertEquals(QueryCacheStrategy.LOCAL_CACHE_REFRESH, query.resolve(fakeRootMapping()).cacheStrategy());
    }

    // ---- NamedSQLQuery ----

    @Test
    public void sql() {
        NamedSQLQuery namedQuery = new NamedSQLQuery();
        namedQuery.setSql("abc");

        SQLQuery<?> query = assertInstanceOf(SQLQuery.class, build(namedQuery));
        assertEquals("abc", query.resolve(mapping()).defaultTemplate());
    }

    @Test
    public void sqlRoot() {
        DataMap map = new DataMap("m");
        DbEntity table = new DbEntity("A_T");
        map.addDbEntity(table);
        ObjEntity entity = new ObjEntity("A");
        entity.setDbEntity(table);
        map.addObjEntity(entity);

        NamedSQLQuery namedQuery = new NamedSQLQuery();
        namedQuery.setRoot(entity);

        CompiledMapping mapping = mapping(map);
        ResolvedQuery resolved = build(namedQuery).resolve(mapping);
        assertSame(mapping.schema().table("A_T"), resolved.table());
        assertSame(mapping.schema().map("m"), resolved.dataMap());
    }

    @Test
    public void sqlRootDataMap() {
        DataMap map = new DataMap("m");

        NamedSQLQuery namedQuery = new NamedSQLQuery();
        namedQuery.setRoot(map);

        CompiledMapping mapping = mapping(map);
        ResolvedQuery resolved = build(namedQuery).resolve(mapping);
        assertNull(resolved.table());
        assertSame(mapping.schema().map("m"), resolved.dataMap());
    }

    @Test
    public void sqlRootUnsupported() {
        NamedSQLQuery namedQuery = new NamedSQLQuery();
        namedQuery.setRoot(new DbEntity("T"));
        assertThrows(CayenneRuntimeException.class, () -> build(namedQuery));
    }

    @Test
    public void sqlProperties() {
        NamedSQLQuery namedQuery = new NamedSQLQuery();
        namedQuery.setPageSize(10);
        namedQuery.setStatementFetchSize(6);
        namedQuery.setFetchingDataRows(true);
        namedQuery.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        namedQuery.setCacheGroup("g1");
        namedQuery.setColumnNamesCapitalization(CapsStrategy.LOWER);

        ResolvedSQL resolved = (ResolvedSQL) build(namedQuery).resolve(mapping());
        assertEquals(10, resolved.pageSize());
        assertEquals(6, resolved.statementFetchSize());
        assertTrue(resolved.fetchingDataRows());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, resolved.cacheStrategy());
        assertEquals("g1", resolved.cacheGroup());
        assertEquals(CapsStrategy.LOWER, resolved.columnNameCaps());
    }

    @Test
    public void sqlPropertiesDefaults() {
        ResolvedSQL resolved = (ResolvedSQL) build(new NamedSQLQuery()).resolve(mapping());
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
    public void sqlPropertiesOverridden() {
        NamedSQLQuery namedQuery = new NamedSQLQuery();
        namedQuery.setPageSize(10);
        namedQuery.setStatementFetchSize(6);
        namedQuery.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        namedQuery.setCacheGroup("g1");

        Query<?> query = MappedQuery.query("q")
                .limit(50)
                .offset(20)
                .pageSize(100)
                .statementFetchSize(60)
                .queryTimeout(7)
                .forceNoCache()
                .resolveNamedQuery(compiled(namedQuery));

        ResolvedSQL resolved = (ResolvedSQL) query.resolve(mapping());
        assertEquals(50, resolved.fetchLimit());
        assertEquals(20, resolved.fetchOffset());
        assertEquals(100, resolved.pageSize());
        assertEquals(60, resolved.statementFetchSize());
        assertEquals(7, resolved.queryTimeout());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE_REFRESH, resolved.cacheStrategy());
        assertEquals("g1", resolved.cacheGroup());
    }

    @Test
    public void sqlWithParameters() {
        NamedSQLQuery namedQuery = new NamedSQLQuery();
        namedQuery.setSql("SELECT * FROM ARTIST WHERE ARTIST_NAME = #bind($name)");

        Map<String, Object> params = Map.of("name", "artist1");
        ResolvedSQL resolved = (ResolvedSQL) build(namedQuery, params).resolve(mapping());
        assertEquals(params, resolved.params());
    }

    @Test
    public void sqlAdapterSql() {
        NamedSQLQuery namedQuery = new NamedSQLQuery();
        namedQuery.getAdapterSql().put("adapter", "abc");

        ResolvedSQL resolved = (ResolvedSQL) build(namedQuery).resolve(mapping());
        assertNull(resolved.defaultTemplate());
        assertEquals("abc", resolved.templates().get("adapter"));
    }

    @Test
    public void sqlPrefetches() {
        NamedSQLQuery namedQuery = new NamedSQLQuery();
        namedQuery.setRoot("A");
        namedQuery.addPrefetch("paintings", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        DataMap map = new DataMap("m");
        map.addObjEntity(new ObjEntity("A"));

        PrefetchTreeNode prefetchTree = build(namedQuery).resolve(mapping(map)).prefetchTree();
        assertNotNull(prefetchTree);
        PrefetchTreeNode prefetch = prefetchTree.getNode("paintings");
        assertNotNull(prefetch);
        assertEquals(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS, prefetch.getSemantics());
    }

    // ---- NamedProcedureQuery ----

    @Test
    public void procedureWithParameters() {
        Map<String, Object> params = Map.of("a", "1", "b", "2");
        ResolvedProcedure resolved = (ResolvedProcedure) build(procedureQuery("p1"), params)
                .resolve(procedureMapping("p1"));
        assertEquals(params, resolved.params());
    }

    @Test
    public void procedureRoot() {
        DataMap map = new DataMap("m");
        Procedure procedure = new Procedure("p1");
        map.addProcedure(procedure);
        DbEntity table = new DbEntity("A_T");
        map.addDbEntity(table);
        ObjEntity entity = new ObjEntity("A");
        entity.setDbEntity(table);
        map.addObjEntity(entity);

        NamedProcedureQuery namedQuery = procedureQuery(procedure);
        namedQuery.setResultEntityName("A");

        CompiledMapping mapping = mapping(map);
        ResolvedProcedure resolved = assertInstanceOf(ResolvedProcedure.class,
                build(namedQuery).resolve(mapping));
        assertSame(mapping.schema().procedure("p1"), resolved.procedure());
        assertSame(mapping.schema().table("A_T"), resolved.table());
        assertSame(mapping.schema().map("m"), resolved.dataMap());
    }

    @Test
    public void procedureRootName() {
        DataMap map = new DataMap("m");
        Procedure procedure = new Procedure("p1");
        map.addProcedure(procedure);

        CompiledMapping mapping = mapping(map);
        ResolvedProcedure resolved = (ResolvedProcedure) build(procedureQuery("p1")).resolve(mapping);
        assertSame(mapping.schema().procedure("p1"), resolved.procedure());
        assertNull(resolved.table());
        assertSame(mapping.schema().map("m"), resolved.dataMap());
    }

    @Test
    public void procedureNoRoot() {
        assertThrows(CayenneRuntimeException.class, () -> build(procedureQuery(null)));
        assertThrows(CayenneRuntimeException.class, () -> build(procedureQuery(new DataMap("m"))));
    }

    @Test
    public void procedureProperties() {
        NamedProcedureQuery namedQuery = procedureQuery("p1");
        namedQuery.setResultEntityName("A");
        namedQuery.setFetchLimit(5);
        namedQuery.setFetchOffset(2);
        namedQuery.setPageSize(10);
        namedQuery.setStatementFetchSize(6);
        namedQuery.setFetchingDataRows(true);
        namedQuery.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        namedQuery.setCacheGroup("g1");
        namedQuery.setColumnNamesCapitalization(CapsStrategy.UPPER);

        DataMap map = new DataMap("m");
        map.addProcedure(new Procedure("p1"));
        map.addObjEntity(new ObjEntity("A"));

        ResolvedProcedure resolved = (ResolvedProcedure) build(namedQuery).resolve(mapping(map));
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
    public void procedurePropertiesOverridden() {
        NamedProcedureQuery namedQuery = procedureQuery("p1");
        namedQuery.setFetchLimit(5);
        namedQuery.setFetchOffset(2);
        namedQuery.setPageSize(10);
        namedQuery.setStatementFetchSize(6);
        namedQuery.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        namedQuery.setCacheGroup("g1");

        Query<?> query = MappedQuery.query("q")
                .limit(50)
                .offset(20)
                .pageSize(100)
                .statementFetchSize(60)
                .queryTimeout(7)
                .forceNoCache()
                .resolveNamedQuery(compiled(namedQuery));

        ResolvedProcedure resolved = (ResolvedProcedure) query.resolve(procedureMapping("p1"));
        assertEquals(50, resolved.fetchLimit());
        assertEquals(20, resolved.fetchOffset());
        assertEquals(100, resolved.pageSize());
        assertEquals(60, resolved.statementFetchSize());
        assertEquals(7, resolved.queryTimeout());
        assertEquals(QueryCacheStrategy.SHARED_CACHE_REFRESH, resolved.cacheStrategy());
        assertEquals("g1", resolved.cacheGroup());
    }

    @Test
    public void procedureColumnNamesCapitalization() {
        NamedProcedureQuery namedQuery = procedureQuery("p1");
        namedQuery.setColumnNamesCapitalization(CapsStrategy.LOWER);

        ResolvedProcedure resolved = (ResolvedProcedure) build(namedQuery).resolve(procedureMapping("p1"));
        assertEquals(CapsStrategy.LOWER, resolved.columnNameCaps());
    }
}
