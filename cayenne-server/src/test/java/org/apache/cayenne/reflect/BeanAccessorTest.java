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

import org.apache.cayenne.reflect.BeanAccessor;

import junit.framework.TestCase;

public class BeanAccessorTest extends TestCase {

    public void testByteArrayProperty() {

        BeanAccessor accessor = new BeanAccessor(
                TstJavaBean.class,
                "byteArrayField",
                byte[].class);

        byte[] bytes = new byte[] {
                5, 6, 7
        };
        TstJavaBean o1 = new TstJavaBean();

        assertNull(o1.getByteArrayField());
        accessor.setValue(o1, bytes);
        assertSame(bytes, o1.getByteArrayField());
        assertSame(bytes, accessor.getValue(o1));
    }

    public void testStringProperty() {

        BeanAccessor accessor = new BeanAccessor(
                TstJavaBean.class,
                "stringField",
                String.class);

        TstJavaBean o1 = new TstJavaBean();

        assertNull(o1.getStringField());
        accessor.setValue(o1, "ABC");
        assertSame("ABC", o1.getStringField());
        assertSame("ABC", accessor.getValue(o1));
    }

    public void testIntProperty() {

        BeanAccessor accessor = new BeanAccessor(
                TstJavaBean.class,
                "intField",
                Integer.TYPE);

        TstJavaBean o1 = new TstJavaBean();

        assertEquals(0, o1.getIntField());
        accessor.setValue(o1, new Integer(5));
        assertEquals(5, o1.getIntField());
        assertEquals(new Integer(5), accessor.getValue(o1));

        accessor.setValue(o1, null);
        assertEquals("Incorrectly set null default", 0, o1.getIntField());
    }

}
