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

package org.apache.cayenne.property;

import junit.framework.TestCase;

import org.apache.cayenne.unit.util.TestBean;

public class BaseClassDescriptorTst extends TestCase {

    public void testConstructor() {
        BaseClassDescriptor d1 = new BaseClassDescriptor(null) {
        };
        assertNull(d1.getSuperclassDescriptor());

        BaseClassDescriptor d2 = new BaseClassDescriptor(d1) {
        };
        assertNull(d1.getSuperclassDescriptor());
        assertSame(d1, d2.getSuperclassDescriptor());
    }

    public void testValid() { // by default BaseClassDescriptor is not compiled...
        BaseClassDescriptor d1 = new BaseClassDescriptor(null) {
        };

        // by default BaseClassDescriptor is not compiled...
        assertFalse(d1.isValid());
    }

    public void testCopyObjectProperties() {
        BaseClassDescriptor d1 = new MockBaseClassDescriptor();
        
        FieldAccessor accessor = new FieldAccessor(TestBean.class, "string", String.class);
        SimpleProperty property = new SimpleProperty(d1, accessor);
       

        d1.declaredProperties.put(property.getName(), property);

        TestBean from = new TestBean();
        from.setString("123");

        TestBean to = new TestBean();

        d1.shallowMerge(from, to);
        assertEquals("123", to.getString());
    }
}
