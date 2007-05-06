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

public class FieldAccessorTst extends TestCase {

    public void testConstructor() {
        ClassDescriptor d = new MockClassDescriptor();
        SimpleProperty property = new SimpleProperty(d, new FieldAccessor(
                TestBean.class,
                "string",
                String.class));

        assertEquals("string", property.getName());
    }

    public void testGet() {
        ClassDescriptor d = new MockClassDescriptor();
        SimpleProperty property = new SimpleProperty(d, new FieldAccessor(
                TestBean.class,
                "string",
                String.class));

        TestBean object = new TestBean();
        object.setString("abc");

        assertEquals("abc", property.readPropertyDirectly(object));
    }

    public void testWriteValue() {
        ClassDescriptor d = new MockClassDescriptor();
        TestFields object = new TestFields();

        // string
        new SimpleProperty(d,
                new FieldAccessor(TestFields.class, "stringField", String.class))
                .writePropertyDirectly(object, null, "aaa");
        assertEquals("aaa", object.stringField);

        // primitive array
        byte[] bytes = new byte[] {
                1, 2, 3
        };
        new SimpleProperty(d, new FieldAccessor(
                TestFields.class,
                "byteArrayField",
                byte[].class)).writePropertyDirectly(object, null, bytes);
        assertSame(bytes, object.byteArrayField);

        // object array
        String[] strings = new String[] {
                "a", "b"
        };
        new SimpleProperty(d, new FieldAccessor(
                TestFields.class,
                "stringArrayField",
                String[].class)).writePropertyDirectly(object, null, strings);
        assertSame(strings, object.stringArrayField);
    }

    public void testWriteValuePrimitive() {
        ClassDescriptor d = new MockClassDescriptor();
        TestFields object = new TestFields();

        // primitive int .. write non-null
        new SimpleProperty(d, new FieldAccessor(TestFields.class, "intField", Integer.TYPE))
                .writePropertyDirectly(object, null, new Integer(6));
        assertEquals(6, object.intField);

        // primitive int .. write null
        object.intField = 55;
        new SimpleProperty(d, new FieldAccessor(TestFields.class, "intField", Integer.TYPE))
                .writePropertyDirectly(object, null, null);

        assertEquals(0, object.intField);
    }

    public void testCopy() {
        ClassDescriptor d = new MockClassDescriptor();
        SimpleProperty property = new SimpleProperty(d, new FieldAccessor(
                TestBean.class,
                "string",
                String.class));
        TestBean from = new TestBean();
        from.setString("123");

        TestBean to = new TestBean();

        property.shallowMerge(from, to);
        assertEquals("123", to.getString());
    }
}
