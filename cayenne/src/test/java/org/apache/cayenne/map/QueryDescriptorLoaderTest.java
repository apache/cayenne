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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class QueryDescriptorLoaderTest {

    protected QueryDescriptorLoader builder;

    @BeforeEach
    public void setUp() throws Exception {
        builder = new QueryDescriptorLoader();
    }

    @Test
    public void setName() throws Exception {
        builder.setName("aaa");
        assertEquals("aaa", builder.name);
    }

    @Test
    public void setRootInfoDbEntity() throws Exception {
        DataMap map = new DataMap("map");
        DbEntity entity = new DbEntity("DB1");
        map.addDbEntity(entity);

        builder.setRoot(map, QueryDescriptor.DB_ENTITY_ROOT, "DB1");
        assertSame(entity, builder.getRoot());
    }

    @Test
    public void setRootObjEntity() throws Exception {
        DataMap map = new DataMap("map");
        ObjEntity entity = new ObjEntity("OBJ1");
        map.addObjEntity(entity);

        builder.setRoot(map, QueryDescriptor.OBJ_ENTITY_ROOT, "OBJ1");
        assertSame(entity, builder.getRoot());
    }

    @Test
    public void setRootDataMap() throws Exception {
        DataMap map = new DataMap("map");

        builder.setRoot(map, QueryDescriptor.DATA_MAP_ROOT, null);
        assertSame(map, builder.getRoot());
    }

    @Test
    public void setSelect() {
        DataMap map = new DataMap("map");
        ObjEntity entity = new ObjEntity("Artist");
        map.addObjEntity(entity);

        builder.setName("q");
        builder.setQueryType(QueryDescriptor.SELECT_QUERY);
        builder.setRoot(map, null, null);
        builder.setSelect("select distinct self from Artist where artistName like $name "
                + "order by artistName desc insensitive, dateOfBirth limit 10 offset 20 "
                + "prefetch paintings joint, paintings.gallery disjoint, exhibits disjointById, groups");

        SelectQueryDescriptor descriptor = (SelectQueryDescriptor) builder.buildQueryDescriptor();
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
    public void setSelectMinimal() {
        DataMap map = new DataMap("map");
        ObjEntity entity = new ObjEntity("Artist");
        map.addObjEntity(entity);

        builder.setName("q");
        builder.setQueryType(QueryDescriptor.SELECT_QUERY);
        builder.setRoot(map, null, null);
        builder.setSelect("from Artist");

        SelectQueryDescriptor descriptor = (SelectQueryDescriptor) builder.buildQueryDescriptor();
        assertSame(entity, descriptor.getRoot());
        assertNull(descriptor.getQualifier());
        assertTrue(descriptor.getOrderings().isEmpty());
        assertTrue(descriptor.getPrefetchesMap().isEmpty());
        assertTrue(descriptor.getProperties().isEmpty());
    }

    @Test
    public void setSelectUnsupported() {
        builder.setName("q");
        builder.setQueryType(QueryDescriptor.SELECT_QUERY);
        builder.setRoot(new DataMap("map"), null, null);

        // TODO: column queries, "having" and DbEntity roots are not supported by the descriptor yet
        assertThrows(ConfigurationException.class, () -> builder.setSelect("select artistName from Artist"));
        assertThrows(ConfigurationException.class, () -> builder.setSelect("from Artist having count(paintings) > 1"));
        assertThrows(ConfigurationException.class, () -> builder.setSelect("from db:ARTIST"));
        assertThrows(ConfigurationException.class, () -> builder.setSelect("from Artist where"));
    }
}
