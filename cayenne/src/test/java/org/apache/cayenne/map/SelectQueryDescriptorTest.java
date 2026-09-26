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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.apache.cayenne.configuration.EmptyConfigurationNodeVisitor;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.util.XMLEncoder;
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

    @Test
    public void toQueryString() {
        SelectQueryDescriptor descriptor = QueryDescriptor.selectQueryDescriptor();
        descriptor.setRoot(new ObjEntity("Artist"));
        descriptor.setQualifier(ExpressionFactory.exp("artistName like $name"));
        descriptor.addOrdering(new Ordering("artistName", SortOrder.DESCENDING_INSENSITIVE));
        descriptor.addOrdering(new Ordering("dateOfBirth", SortOrder.ASCENDING));
        descriptor.addPrefetch("paintings", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
        descriptor.addPrefetch("paintings.gallery", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
        descriptor.setFetchLimit(10);
        descriptor.setFetchOffset(20);
        descriptor.setDistinct(true);

        // properties outside the query String don't affect it
        descriptor.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        descriptor.setPageSize(5);

        assertEquals("select distinct self from Artist where artistName like $name "
                + "order by artistName desc insensitive, dateOfBirth limit 10 offset 20 "
                + "prefetch paintings joint, paintings.gallery disjoint", descriptor.toQueryString());
    }

    @Test
    public void toQueryStringNoRoot() {
        SelectQueryDescriptor descriptor = QueryDescriptor.selectQueryDescriptor();
        descriptor.setQualifier(ExpressionFactory.exp("artistName like $name"));
        assertNull(descriptor.toQueryString());
    }

    @Test
    public void encodeAsXML() {
        SelectQueryDescriptor descriptor = QueryDescriptor.selectQueryDescriptor();
        descriptor.setName("q");
        descriptor.setRoot("Artist");
        descriptor.setQualifier(ExpressionFactory.exp("artistName = $name"));
        descriptor.addOrdering(new Ordering("artistName", SortOrder.DESCENDING));
        descriptor.setFetchOffset(20);
        descriptor.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        // the root, limit, offset and distinct are a part of the query String, and are not stored separately
        assertEquals("""
                <query name="q" type="SelectQuery">
                <property name="cayenne.GenericSelectQuery.cacheStrategy" value="LOCAL_CACHE"/>
                <select><![CDATA[from Artist where artistName = $name order by artistName desc offset 20]]></select>
                </query>
                """, encode(descriptor));
    }

    @Test
    public void encodeAsXMLNoRoot() {
        SelectQueryDescriptor descriptor = QueryDescriptor.selectQueryDescriptor();
        descriptor.setName("q");
        descriptor.setQualifier(ExpressionFactory.exp("artistName = $name"));

        assertEquals("""
                <query name="q" type="SelectQuery"/>
                """, encode(descriptor));
    }

    private static String encode(SelectQueryDescriptor descriptor) {
        StringWriter out = new StringWriter();
        descriptor.encodeAsXML(new XMLEncoder(new PrintWriter(out)), new EmptyConfigurationNodeVisitor());
        return out.toString();
    }
}
