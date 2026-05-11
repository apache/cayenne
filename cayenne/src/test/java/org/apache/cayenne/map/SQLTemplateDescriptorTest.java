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

import org.apache.cayenne.query.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 */
public class SQLTemplateDescriptorTest {

    @Test
    public void getQueryType() throws Exception {
        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        assertTrue(builder.buildQuery() instanceof SQLTemplate);
    }

    @Test
    public void getQueryRoot() throws Exception {
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("A");
        map.addObjEntity(entity);

        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        builder.setRoot(entity);

        Query query = builder.buildQuery();
        assertTrue(query instanceof SQLTemplate);
        assertSame(entity, ((SQLTemplate) query).getRoot());
    }

    @Test
    public void getQueryProperties() throws Exception {
        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        builder.setProperty(QueryMetadata.FETCH_LIMIT_PROPERTY, "5");
        builder.setProperty(QueryMetadata.STATEMENT_FETCH_SIZE_PROPERTY, "6");

        Query query = builder.buildQuery();
        assertTrue(query instanceof SQLTemplate);
        assertEquals(5, ((SQLTemplate) query).getFetchLimit());
        
        assertEquals(6, ((SQLTemplate) query).getStatementFetchSize());

        // TODO: test other properties...
    }

    @Test
    public void getQuerySql() throws Exception {
        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        builder.setSql("abc");

        SQLTemplate query = builder.buildQuery();
        assertEquals("abc", query.getDefaultTemplate());
    }

    @Test
    public void getQueryAdapterSql() throws Exception {
        SQLTemplateDescriptor builder = QueryDescriptor.sqlTemplateDescriptor();
        builder.getAdapterSql().put("adapter", "abc");

        SQLTemplate query = builder.buildQuery();
        assertNull(query.getDefaultTemplate());
        assertEquals("abc", query.getTemplate("adapter"));
    }
}
