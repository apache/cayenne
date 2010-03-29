/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.map;

import junit.framework.TestCase;

import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLTemplate;

/**
 */
public class SQLTemplateBuilderTest extends TestCase {

    public void testGetQueryType() throws Exception {
        SQLTemplateBuilder builder = new MockupRootQueryBuilder();
        assertTrue(builder.getQuery() instanceof SQLTemplate);
    }

    public void testGetQueryName() throws Exception {
        SQLTemplateBuilder builder = new MockupRootQueryBuilder();
        builder.setName("xyz");

        assertEquals("xyz", builder.getQuery().getName());
    }

    public void testGetQueryRoot() throws Exception {
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("A");
        map.addObjEntity(entity);

        SQLTemplateBuilder builder = new SQLTemplateBuilder();
        builder.setRoot(map, MapLoader.OBJ_ENTITY_ROOT, "A");

        Query query = builder.getQuery();
        assertTrue(query instanceof SQLTemplate);
        assertSame(entity, ((SQLTemplate) query).getRoot());
    }

    public void testGetQueryProperties() throws Exception {
        SQLTemplateBuilder builder = new MockupRootQueryBuilder();
        builder.addProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, "5");
        builder.addProperty(QueryMetadata.STATEMENT_FETCH_SIZE_PROPERTY, "6");

        Query query = builder.getQuery();
        assertTrue(query instanceof SQLTemplate);
        assertEquals(5, ((SQLTemplate) query).getFetchLimit());
        
        assertEquals(6, ((SQLTemplate) query).getStatementFetchSize());

        // TODO: test other properties...
    }

    public void testGetQuerySql() throws Exception {
        SQLTemplateBuilder builder = new MockupRootQueryBuilder();
        builder.addSql("abc", null);

        SQLTemplate query = (SQLTemplate) builder.getQuery();
        assertEquals("abc", query.getDefaultTemplate());
    }

    public void testGetQueryAdapterSql() throws Exception {
        SQLTemplateBuilder builder = new MockupRootQueryBuilder();
        builder.addSql("abc", "adapter");

        SQLTemplate query = (SQLTemplate) builder.getQuery();
        assertNull(query.getDefaultTemplate());
        assertEquals("abc", query.getTemplate("adapter"));
    }

    class MockupRootQueryBuilder extends SQLTemplateBuilder {

        @Override
        public Object getRoot() {
            return "FakeRoot";
        }
    }
}
