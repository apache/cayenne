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

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.property.BaseClassDescriptor;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.MtTable1;

public class EntityDescriptorTst extends TestCase {

    public void testConstructor() {
        ObjEntity e1 = new ObjEntity("TestEntity");
        e1.setClassName(String.class.getName());

        EntityDescriptor d1 = new EntityDescriptor(e1, null);
        assertNull(d1.getSuperclassDescriptor());

        BaseClassDescriptor mockSuper = new BaseClassDescriptor(null) {
        };
        EntityDescriptor d2 = new EntityDescriptor(e1, mockSuper);
        assertSame(mockSuper, d2.getSuperclassDescriptor());
    }

    public void testCompile() {
        ObjEntity e1 = new ObjEntity("TestEntity");
        e1.setClassName(CayenneDataObject.class.getName());

        // compilation must be done in constructor...
        EntityDescriptor d1 = new EntityDescriptor(e1, null);
        d1.compile(new EntityResolver());
        assertTrue(d1.isValid());
    }

    public void testProperties() {
        DataMap map = new DataMap();

        ObjEntity e1 = new ObjEntity("TestEntity");
        map.addObjEntity(e1);
        e1.setClassName(ClientMtTable1.class.getName());
        e1.addAttribute(new ObjAttribute(
                ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY,
                String.class.getName(),
                e1));

        ObjRelationship toMany = new ObjRelationship(MtTable1.TABLE2ARRAY_PROPERTY) {

            public boolean isToMany() {
                return true;
            }
        };
        toMany.setTargetEntityName(e1.getName());

        e1.addRelationship(toMany);

        EntityDescriptor d1 = new EntityDescriptor(e1, null);
        d1.compile(new EntityResolver());

        assertSame(e1, d1.getEntity());
        assertNotNull(d1.getObjectClass());
        assertEquals(ClientMtTable1.class.getName(), d1.getObjectClass().getName());

        // properties that exist in the entity must be included
        assertNotNull(d1.getDeclaredProperty(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY));

        // properties not described in the entity must not be included
        assertNull(d1.getDeclaredProperty(ClientMtTable1.SERVER_ATTRIBUTE1_PROPERTY));

        // collection properties must be returned just as simple properties do...
        assertNotNull(d1.getDeclaredProperty(ClientMtTable1.TABLE2ARRAY_PROPERTY));
    }
}
