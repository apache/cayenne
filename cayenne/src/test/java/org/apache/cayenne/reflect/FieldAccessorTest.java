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

import org.apache.cayenne.unit.util.TstBean;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class FieldAccessorTest {

    @Test
    public void testConstructor() {
        FieldAccessor accessor = new FieldAccessor(TstBean.class, "string", String.class);
        assertEquals("string", accessor.getName());
    }

    @Test
    public void testGet() {
        FieldAccessor accessor = new FieldAccessor(TstBean.class, "string", String.class);

        TstBean object = new TstBean();
        object.setString("abc");
        assertEquals("abc", accessor.getValue(object));
    }

    @Test
    public void testSetValue() {
        TstFields object = new TstFields();

        // string
        new FieldAccessor(TstFields.class, "stringField", String.class).setValue(
                object,
                "aaa");
        assertEquals("aaa", object.stringField);

        // primitive array
        byte[] bytes = new byte[] {
                1, 2, 3
        };
        new FieldAccessor(TstFields.class, "byteArrayField", byte[].class).setValue(
                object,
                bytes);
        assertSame(bytes, object.byteArrayField);

        // object array
        String[] strings = new String[] {
                "a", "b"
        };
        new FieldAccessor(TstFields.class, "stringArrayField", String[].class).setValue(
                object,
                strings);
        assertSame(strings, object.stringArrayField);
    }

    @Test
    public void testSetValuePrimitive() {
        TstFields object = new TstFields();

        // primitive int .. write non-null
        new FieldAccessor(TstFields.class, "intField", Integer.TYPE).setValue(
                object,
                6);
        assertEquals(6, object.intField);

        // primitive int .. write null
        object.intField = 55;
        new FieldAccessor(TstFields.class, "intField", Integer.TYPE).setValue(
                object,
                null);

        assertEquals(0, object.intField);
    }
}
