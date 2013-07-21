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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;

/**
 */
public class SelectQueryBuilderTest extends TestCase {

    public void testGetQueryType() throws Exception {
        SelectQueryBuilder builder = new MockupRootQueryBuilder();
        assertTrue(builder.getQuery() instanceof SelectQuery);
    }

    public void testGetQueryName() throws Exception {
        SelectQueryBuilder builder = new MockupRootQueryBuilder();
        builder.setName("xyz");

        assertEquals("xyz", builder.getQuery().getName());
    }

    public void testGetQueryRoot() throws Exception {
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("A");
        map.addObjEntity(entity);

        SelectQueryBuilder builder = new SelectQueryBuilder();
        builder.setRoot(map, MapLoader.OBJ_ENTITY_ROOT, "A");

        assertTrue(builder.getQuery() instanceof SelectQuery);
        assertSame(entity, ((SelectQuery) builder.getQuery()).getRoot());
    }

    public void testGetQueryQualifier() throws Exception {
        SelectQueryBuilder builder = new MockupRootQueryBuilder();
        builder.setQualifier("abc = 5");

        SelectQuery query = (SelectQuery) builder.getQuery();

        assertEquals(Expression.fromString("abc = 5"), query.getQualifier());
    }

    public void testGetQueryProperties() throws Exception {
        SelectQueryBuilder builder = new MockupRootQueryBuilder();
        builder.addProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, "5");
        builder.addProperty(QueryMetadata.STATEMENT_FETCH_SIZE_PROPERTY, "6");

        Query query = builder.getQuery();
        assertTrue(query instanceof SelectQuery);
        assertEquals(5, ((SelectQuery) query).getFetchLimit());
        
        assertEquals(6, ((SelectQuery) query).getStatementFetchSize());

        // TODO: test other properties...
    }

    class MockupRootQueryBuilder extends SelectQueryBuilder {

        @Override
        public Object getRoot() {
            return "FakeRoot";
        }
    }
}
