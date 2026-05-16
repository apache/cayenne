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

package org.apache.cayenne.reflect;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.unit.util.TestObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

public class PersistentDescriptorTest {

    @Test
    public void constructor() {
        PersistentDescriptor d1 = new PersistentDescriptor();
        assertNull(d1.getSuperclassDescriptor());

        PersistentDescriptor d2 = new PersistentDescriptor();
        d2.setSuperclassDescriptor(d1);
        assertNull(d1.getSuperclassDescriptor());
        assertSame(d1, d2.getSuperclassDescriptor());
    }

    @Test
    public void copyObjectProperties() {
        PersistentDescriptor d1 = new PersistentDescriptor();

        ObjAttribute attribute = mock(ObjAttribute.class);
        FieldAccessor accessor = new FieldAccessor(TestObject.class, "string",
                String.class);
        PropertyDescriptor property = new SimpleAttributeProperty(d1, accessor,
                attribute);

        d1.addDeclaredProperty(property);

        TestObject from = new TestObject();
        from.setString("123");

        TestObject to = new TestObject();

        d1.shallowMerge(from, to);
        assertEquals("123", to.getString());
    }

}
