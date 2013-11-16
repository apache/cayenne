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
package org.apache.cayenne.di;

import junit.framework.TestCase;

public class KeyTest extends TestCase {

    public void testEquals() {
        Key<String> key1 = Key.get(String.class);
        Key<String> key2 = Key.get(String.class);
        Key<Integer> key3 = Key.get(Integer.class);
        Key<Integer> key31 = Key.get(Integer.class, "");
        Key<Integer> key4 = Key.get(Integer.class, "a");
        Key<Integer> key5 = Key.get(Integer.class, "a");
        Key<Integer> key6 = Key.get(Integer.class, "b");
        Key<String> key7 = Key.get(String.class, "a");

        assertTrue(key1.equals(key2));

        assertFalse(key1.equals(key3));

        assertTrue(key3.equals(key31));
        assertTrue(key31.equals(key3));

        assertFalse(key3.equals(key4));
        assertFalse(key4.equals(key3));

        assertTrue(key4.equals(key5));
        assertTrue(key5.equals(key4));

        assertFalse(key5.equals(key6));
        assertFalse(key6.equals(key5));

        assertFalse(key4.equals(key7));
        assertFalse(key7.equals(key4));
    }

    public void testHashCode() {
        Key<String> key1 = Key.get(String.class);
        Key<String> key2 = Key.get(String.class);
        Key<Integer> key3 = Key.get(Integer.class);
        Key<Integer> key4 = Key.get(Integer.class, "a");
        Key<Integer> key5 = Key.get(Integer.class, "a");
        Key<Integer> key6 = Key.get(Integer.class, "b");
        Key<String> key7 = Key.get(String.class, "a");

        assertTrue(
                "generated different hashcode on second inocation",
                key1.hashCode() == key1.hashCode());
        assertTrue(key1.hashCode() == key2.hashCode());
        assertTrue(key4.hashCode() == key5.hashCode());

        // these are not technically required for hashCode() validity, but as things stand
        // now, these tests will all succeed.
        assertFalse(key1.hashCode() == key3.hashCode());
        assertFalse(key4.hashCode() == key3.hashCode());
        assertFalse(key5.hashCode() == key6.hashCode());
        assertFalse(key7.hashCode() == key4.hashCode());
    }

    public void testToString() {
        assertEquals("<BindingKey: java.lang.String>", Key.get(String.class).toString());
        assertEquals("<BindingKey: java.lang.String, 'xyz'>", Key
                .get(String.class, "xyz")
                .toString());
    }
}
