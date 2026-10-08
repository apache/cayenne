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
package org.apache.cayenne.project.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SQLTemplateDescriptorHandlerTest extends BaseHandlerTest {

    private DataMap map;

    @BeforeEach
    public void createMap() {
        map = new DataMap("map");
    }

    private SQLTemplateDescriptor parseQuery(String xml) throws Exception {
        parse("sqlQuery", xml, parent -> new SQLTemplateDescriptorHandler(parent, map));
        return (SQLTemplateDescriptor) map.getQueryDescriptor("q");
    }

    @Test
    public void sqlQuery() throws Exception {
        SQLTemplateDescriptor descriptor = parseQuery("""
                <sqlQuery name="q" cacheStrategy="LOCAL_CACHE" dataRows="true" pageSize="5" statementFetchSize="7"
                          columnNameCapitalization="lower">
                    <sql>select * from ARTIST</sql>
                    <sql adapterClass="org.apache.cayenne.dba.postgres.PostgresAdapter">select * from ARTIST_PG</sql>
                    <cacheGroup>g</cacheGroup>
                    <prefetch type="joint">paintings</prefetch>
                    <prefetch type="disjoint"> paintings.gallery </prefetch>
                    <prefetch type="disjointById">exhibits</prefetch>
                    <prefetch>groups</prefetch>
                    <prefetch type="joint"> </prefetch>
                </sqlQuery>
                """);

        assertEquals("q", descriptor.getName());
        assertSame(map, descriptor.getDataMap());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, descriptor.getCacheStrategy());
        assertEquals("g", descriptor.getCacheGroup());
        assertTrue(descriptor.isFetchingDataRows());
        assertEquals(5, descriptor.getPageSize());
        assertEquals(7, descriptor.getStatementFetchSize());
        assertEquals(CapsStrategy.LOWER, descriptor.getColumnNamesCapitalization());

        assertEquals("select * from ARTIST", descriptor.getSql());
        assertEquals(Map.of("org.apache.cayenne.dba.postgres.PostgresAdapter", "select * from ARTIST_PG"),
                descriptor.getAdapterSql());
        assertEquals(Map.of(
                        "paintings", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS,
                        "paintings.gallery", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS,
                        "exhibits", PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS,
                        "groups", PrefetchTreeNode.UNDEFINED_SEMANTICS),
                descriptor.getPrefetchesMap());
    }

    @Test
    public void sqlQueryMinimal() throws Exception {
        SQLTemplateDescriptor descriptor = parseQuery("""
                <sqlQuery name="q"/>
                """);

        assertSame(map, descriptor.getRoot());
        assertNull(descriptor.getSql());
        assertTrue(descriptor.getAdapterSql().isEmpty());
        assertTrue(descriptor.getPrefetchesMap().isEmpty());
        assertNull(descriptor.getColumnNamesCapitalization());
    }

    @Test
    public void rootDbEntity() throws Exception {
        DbEntity entity = new DbEntity("DB1");
        map.addDbEntity(entity);

        assertSame(entity, parseQuery("""
                <sqlQuery name="q" root="dbEntity" rootName="DB1"/>
                """).getRoot());
    }

    @Test
    public void rootObjEntity() throws Exception {
        ObjEntity entity = new ObjEntity("OBJ1");
        map.addObjEntity(entity);

        assertSame(entity, parseQuery("""
                <sqlQuery name="q" root="objEntity" rootName="OBJ1"/>
                """).getRoot());
    }

    @Test
    public void rootJavaClass() throws Exception {
        ObjEntity entity = new ObjEntity("OBJ1");
        entity.setClassName("org.example.Obj1");
        map.addObjEntity(entity);

        assertSame(entity, parseQuery("""
                <sqlQuery name="q" root="javaClass" rootName="org.example.Obj1"/>
                """).getRoot());
    }

    @Test
    public void rootDataMap() throws Exception {
        assertSame(map, parseQuery("""
                <sqlQuery name="q" root="dataMap"/>
                """).getRoot());
    }

    @Test
    public void rootNotFound() throws Exception {
        assertSame(map, parseQuery("""
                <sqlQuery name="q" root="objEntity" rootName="NoSuchEntity"/>
                """).getRoot());
    }
}
