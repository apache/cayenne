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

import org.apache.cayenne.unit.PeopleCase;

/**
 */
public class EntityResolverInheritanceTest extends PeopleCase {
    protected EntityResolver resolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        resolver = new EntityResolver(getDomain().getDataMaps());
    }

    private ObjEntity getAbstractPerson() {
        return getObjEntity("AbstractPerson");
    }

    private ObjEntity getEmployee() {
        return getObjEntity("Employee");
    }

    private ObjEntity getManager() {
        return getObjEntity("Manager");
    }

    public void testLookupAbstractPersonTree() throws Exception {
        EntityInheritanceTree tree = resolver.lookupInheritanceTree(getAbstractPerson());
        assertNotNull(tree);
        assertEquals(2, tree.getChildrenCount());
        assertSame(getAbstractPerson(), tree.getEntity());
    }

    public void testLookupEmployeeTree() throws Exception {
        EntityInheritanceTree tree = resolver.lookupInheritanceTree(getEmployee());
        assertNotNull(tree);
        assertEquals(1, tree.getChildrenCount());
        assertSame(getEmployee(), tree.getEntity());
    }

    public void testLookupManagerTree() throws Exception {
        EntityInheritanceTree tree = resolver.lookupInheritanceTree(getManager());
        assertNull(tree);
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

        assertNull(resolver.lookupInheritanceTree(super1));

        resolver.addDataMap(map);
        EntityInheritanceTree tree = resolver.lookupInheritanceTree(super1);
        assertNotNull(tree);
        assertEquals(2, tree.getChildrenCount());
        assertSame(super1, tree.getEntity());
    }
}
