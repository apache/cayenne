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

package org.apache.cayenne.reflect;

import junit.framework.TestCase;

import org.apache.cayenne.reflect.FieldAccessor;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.SimpleAttributeProperty;
import org.apache.cayenne.unit.util.TestBean;

public class PersistentDescriptorTest extends TestCase {

    public void testConstructor() {
        PersistentDescriptor d1 = new PersistentDescriptor();
        assertNull(d1.getSuperclassDescriptor());

        PersistentDescriptor d2 = new PersistentDescriptor();
        d2.setSuperclassDescriptor(d1);
        assertNull(d1.getSuperclassDescriptor());
        assertSame(d1, d2.getSuperclassDescriptor());
    }

    public void testCopyObjectProperties() {
        PersistentDescriptor d1 = new PersistentDescriptor();

        FieldAccessor accessor = new FieldAccessor(TestBean.class, "string", String.class);
        Property property = new SimpleAttributeProperty(d1, accessor, null);

        d1.declaredProperties.put(property.getName(), property);

        TestBean from = new TestBean();
        from.setString("123");

        TestBean to = new TestBean();

        d1.shallowMerge(from, to);
        assertEquals("123", to.getString());
    }
}
