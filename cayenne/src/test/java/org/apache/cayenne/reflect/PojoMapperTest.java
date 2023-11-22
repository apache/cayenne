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

import org.apache.cayenne.CayenneRuntimeException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class PojoMapperTest {

    @Test
    public void testObjectCreation() {
        PojoMapper<C1> descriptor = new PojoMapper<>(C1.class);

        Object o = new Object();
        Object[] data = {"123", o, 42};
        C1 object = descriptor.apply(data);
        assertEquals("123", object.a);
        assertSame(o, object.b);
        assertEquals(42, object.c);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testNonPublicClass() {
        new PojoMapper<>(C2.class);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testNonPublicConstructor() {
        new PojoMapper<>(C3.class);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testNonDefaultConstructor() {
        new PojoMapper<>(C4.class);
    }

    @Test(expected = CayenneRuntimeException.class)
    public void testWrongArgumentCount() {
        PojoMapper<C1> descriptor = new PojoMapper<>(C1.class);

        Object[] data = {"123", new Object(), 42, 32};
        descriptor.apply(data);
    }

    public static class C1 {
        String a;
        Object b;
        int c;
    }

    private static class C2 {
        int a;
    }

    public static class C3 {
        int a;
        private C3() {
        }
    }

    public static class C4 {
        int a;
        public C4(int a) {
        }
    }
}