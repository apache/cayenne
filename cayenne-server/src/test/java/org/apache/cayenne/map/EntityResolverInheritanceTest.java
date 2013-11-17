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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.PEOPLE_PROJECT)
public class EntityResolverInheritanceTest extends ServerCase {

    @Inject
    private EntityResolver resolver;

    public void testGetAbstractPersonTree() throws Exception {
        EntityInheritanceTree tree = resolver.getInheritanceTree("AbstractPerson");
        assertNotNull(tree);
        assertEquals(2, tree.getChildrenCount());
        assertSame(resolver.getObjEntity("AbstractPerson"), tree.getEntity());
    }

    public void testGetEmployeeTree() throws Exception {
        EntityInheritanceTree tree = resolver.getInheritanceTree("Employee");
        assertNotNull(tree);
        assertEquals(1, tree.getChildrenCount());
        assertSame(resolver.getObjEntity("Employee"), tree.getEntity());
    }

    public void testGetManagerTree() throws Exception {
        EntityInheritanceTree tree = resolver.getInheritanceTree("Manager");
        assertNotNull(tree);
        assertEquals(0, tree.getChildrenCount());
    }

    public void testLookupTreeRefresh() throws Exception {
        ObjEntity super1 = new ObjEntity("super1");
        ObjEntity sub1 = new ObjEntity("sub1");
        ObjEntity sub2 = new ObjEntity("sub2");

        super1.setClassName("java.lang.Float");

        sub1.setSuperEntityName("super1");
        sub1.setClassName("java.lang.Object");

        sub2.setSuperEntityName("super1");
        sub2.setClassName("java.lang.Integer");

        DataMap map = new DataMap("test");
        map.addObjEntity(super1);
        map.addObjEntity(sub1);
        map.addObjEntity(sub2);

        assertNull(resolver.getInheritanceTree("super1"));

        resolver.addDataMap(map);
        EntityInheritanceTree tree = resolver.getInheritanceTree("super1");
        assertNotNull(tree);
        assertEquals(2, tree.getChildrenCount());
        assertSame(super1, tree.getEntity());
    }
}
