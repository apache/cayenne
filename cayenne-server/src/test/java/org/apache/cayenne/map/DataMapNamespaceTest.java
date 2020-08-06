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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Test case for recursive lookup of DataMap resources via a parent
 * namespace.
 * 
 */
public class DataMapNamespaceTest {

    protected DataMap map;

    @Before
    public void setUp() throws Exception {
        map = new DataMap();
    }

    @Test
    public void testNamespace() {
        assertNull(map.getNamespace());

        MappingNamespace namespace = new MockMappingNamespace();
        map.setNamespace(namespace);
        assertSame(namespace, map.getNamespace());

        map.setNamespace(null);
        assertNull(map.getNamespace());
    }

    @Test
    public void testGetDbEntity()  {
        MockMappingNamespace namespace = new MockMappingNamespace();
        map.setNamespace(namespace);

        DbEntity e1 = new DbEntity("entity");
        DbEntity e2 = new DbEntity("entity");
        namespace.addDbEntity(e1);

        assertSame(e1, map.getDbEntity("entity"));

        map.addDbEntity(e2);
        assertSame(e2, map.getDbEntity("entity"));

        map.removeDbEntity("entity", true);
        assertSame(e1, map.getDbEntity("entity"));
    }

    @Test
    public void testGetObjEntity() throws Exception {
        MockMappingNamespace namespace = new MockMappingNamespace();
        map.setNamespace(namespace);

        ObjEntity e1 = new ObjEntity("entity");
        ObjEntity e2 = new ObjEntity("entity");
        namespace.addObjEntity(e1);

        assertSame(e1, map.getObjEntity("entity"));

        map.addObjEntity(e2);
        assertSame(e2, map.getObjEntity("entity"));

        map.removeObjEntity("entity", true);
        assertSame(e1, map.getObjEntity("entity"));
    }

    @Test
    public void testGetProcedure() throws Exception {
        MockMappingNamespace namespace = new MockMappingNamespace();
        map.setNamespace(namespace);

        Procedure p1 = new Procedure("procedure");
        Procedure p2 = new Procedure("procedure");
        namespace.addProcedure(p1);

        assertSame(p1, map.getProcedure("procedure"));

        map.addProcedure(p2);
        assertSame(p2, map.getProcedure("procedure"));

        map.removeProcedure("procedure");
        assertSame(p1, map.getProcedure("procedure"));
    }

    @Test
    public void testGetQuery() throws Exception {
        MockMappingNamespace namespace = new MockMappingNamespace();
        map.setNamespace(namespace);

        QueryDescriptor q1 = QueryDescriptor.selectQueryDescriptor();
        q1.setName("query");
        QueryDescriptor q2 = QueryDescriptor.selectQueryDescriptor();
        q2.setName("query");
        namespace.addQueryDescriptor(q1);

        assertSame(q1, map.getQueryDescriptor("query"));

        map.addQueryDescriptor(q2);
        assertSame(q2, map.getQueryDescriptor("query"));

        map.removeQueryDescriptor("query");
        assertSame(q1, map.getQueryDescriptor("query"));
    }
}
