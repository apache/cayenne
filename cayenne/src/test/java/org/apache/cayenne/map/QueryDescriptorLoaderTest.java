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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 */
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
}
