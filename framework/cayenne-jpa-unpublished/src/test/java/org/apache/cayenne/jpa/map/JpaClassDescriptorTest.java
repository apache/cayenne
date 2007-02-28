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

package org.apache.cayenne.jpa.map;

import junit.framework.TestCase;

import org.apache.cayenne.jpa.conf.MockFieldRegressionBean;
import org.apache.cayenne.jpa.conf.MockPropertyRegressionBean;

public class JpaClassDescriptorTest extends TestCase {

    public void testPropertyNameForGetter() {

        assertNull(JpaClassDescriptor.propertyNameForGetter("xxx"));
        assertEquals("a", JpaClassDescriptor.propertyNameForGetter("isA"));
        assertEquals("this", JpaClassDescriptor.propertyNameForGetter("getThis"));
    }

    public void testPropertyNameForSetter() {
        assertNull(JpaClassDescriptor.propertyNameForSetter("xxx"));
        assertNull(JpaClassDescriptor.propertyNameForSetter("set"));
        assertEquals("this", JpaClassDescriptor.propertyNameForSetter("setThis"));
    }

    public void testGetMemberDescriptorsProperty() throws Exception {

        JpaClassDescriptor descriptor = new JpaClassDescriptor(
                MockPropertyRegressionBean.class);
        descriptor.setAccess(AccessType.PROPERTY);

        assertEquals(2, descriptor.getPropertyDescriptors().size());

        assertNotNull(descriptor.getPropertyForMember(MockPropertyRegressionBean.class
                .getDeclaredMethod("getP2", new Class[] {})));

        assertNotNull(descriptor.getPropertyForMember(MockPropertyRegressionBean.class
                .getDeclaredMethod("getP3", new Class[] {})));
    }
    
    public void testGetMemberDescriptorsField() throws Exception {

        JpaClassDescriptor descriptor = new JpaClassDescriptor(
                MockFieldRegressionBean.class);
        descriptor.setAccess(AccessType.FIELD);

        assertEquals(1, descriptor.getFieldDescriptors().size());

        assertNotNull(descriptor.getPropertyForMember(MockFieldRegressionBean.class
                .getDeclaredField("p1")));

        assertNull(descriptor.getPropertyForMember(MockFieldRegressionBean.class
                .getDeclaredField("p2")));
        assertNull(descriptor.getPropertyForMember(MockFieldRegressionBean.class
                .getDeclaredField("$cay_x")));
    }
}
