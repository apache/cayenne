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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SelectQueryDescriptorHandlerTest extends BaseHandlerTest {

    private DataMap map;

    @BeforeEach
    public void createMap() {
        map = new DataMap("map");
    }

    private SelectQueryDescriptor parseQuery(String xml) throws Exception {
        parse("objectQuery", xml, parent -> new SelectQueryDescriptorHandler(parent, map));
        return (SelectQueryDescriptor) map.getQueryDescriptor("q");
    }

    @Test
    public void querySettings() throws Exception {
        SelectQueryDescriptor descriptor = parseQuery("""
                <objectQuery name="q" cacheStrategy="SHARED_CACHE" dataRows="true" pageSize="5" statementFetchSize="7">
                    <ql>from Artist</ql>
                    <cacheGroup>g</cacheGroup>
                </objectQuery>
                """);

        assertEquals("q", descriptor.getName());
        assertSame(map, descriptor.getDataMap());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, descriptor.getCacheStrategy());
        assertEquals("g", descriptor.getCacheGroup());
        assertTrue(descriptor.isFetchingDataRows());
        assertEquals(5, descriptor.getPageSize());
        assertEquals(7, descriptor.getStatementFetchSize());
    }

    @Test
    public void querySettingsDefaults() throws Exception {
        SelectQueryDescriptor descriptor = parseQuery("""
                <objectQuery name="q">
                    <ql>from Artist</ql>
                </objectQuery>
                """);

        assertEquals(QueryCacheStrategy.getDefaultStrategy(), descriptor.getCacheStrategy());
        assertNull(descriptor.getCacheGroup());
        assertFalse(descriptor.isFetchingDataRows());
        assertEquals(0, descriptor.getPageSize());
        assertEquals(0, descriptor.getStatementFetchSize());
    }

    @Test
    public void select() throws Exception {
        ObjEntity entity = new ObjEntity("Artist");
        map.addObjEntity(entity);

        SelectQueryDescriptor descriptor = parseQuery("""
                <objectQuery name="q">
                    <ql>select distinct self from Artist where artistName like $name
                        order by artistName desc insensitive, dateOfBirth limit 10 offset 20
                        prefetch paintings joint, paintings.gallery disjoint, exhibits disjointById, groups</ql>
                </objectQuery>
                """);

        assertSame(entity, descriptor.getRoot());

        // parameters are preserved, to be bound when the query is executed
        assertEquals(ExpressionFactory.exp("artistName like $name"), descriptor.getQualifier());

        assertEquals(List.of(
                        new Ordering("artistName", SortOrder.DESCENDING_INSENSITIVE),
                        new Ordering("dateOfBirth", SortOrder.ASCENDING)),
                descriptor.getOrderings());

        assertEquals(Map.of(
                        "paintings", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS,
                        "paintings.gallery", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS,
                        "exhibits", PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS,
                        "groups", PrefetchTreeNode.UNDEFINED_SEMANTICS),
                descriptor.getPrefetchesMap());

        assertEquals(10, descriptor.getFetchLimit());
        assertEquals(20, descriptor.getFetchOffset());
        assertTrue(descriptor.isDistinct());
    }

    @Test
    public void selectMinimal() throws Exception {
        ObjEntity entity = new ObjEntity("Artist");
        map.addObjEntity(entity);

        SelectQueryDescriptor descriptor = parseQuery("""
                <objectQuery name="q">
                    <ql>from Artist</ql>
                </objectQuery>
                """);

        assertSame(entity, descriptor.getRoot());
        assertNull(descriptor.getQualifier());
        assertTrue(descriptor.getOrderings().isEmpty());
        assertTrue(descriptor.getPrefetchesMap().isEmpty());
        assertEquals(0, descriptor.getFetchLimit());
        assertEquals(0, descriptor.getFetchOffset());
        assertFalse(descriptor.isDistinct());
    }

    @Test
    public void selectWithoutRoot() throws Exception {
        SelectQueryDescriptor descriptor = parseQuery("""
                <objectQuery name="q">
                    <ql> </ql>
                </objectQuery>
                """);

        // unlike the other queries, a select query is never rooted in a DataMap
        assertNull(descriptor.getRoot());
    }

    @Test
    public void selectWithUnknownRoot() throws Exception {
        SelectQueryDescriptor descriptor = parseQuery("""
                <objectQuery name="q">
                    <ql>from Artist where artistName = 'a'</ql>
                </objectQuery>
                """);

        // an entity that is not in the DataMap is kept by name, so that the query is not lost on save
        assertEquals("Artist", descriptor.getRoot());
        assertEquals("from Artist where artistName = \"a\"", descriptor.toQueryString());
    }

    @Test
    public void selectUnsupported() {
        // TODO: column queries, "having" and DbEntity roots are not supported by the descriptor yet
        for (String ql : List.of(
                "select artistName from Artist",
                "from Artist having count(paintings) > 1",
                "from db:ARTIST",
                "from Artist where")) {
            assertThrows(ConfigurationException.class,
                    () -> parseQuery("<objectQuery name=\"q\"><ql>" + ql + "</ql></objectQuery>"), ql);
        }
    }
}
