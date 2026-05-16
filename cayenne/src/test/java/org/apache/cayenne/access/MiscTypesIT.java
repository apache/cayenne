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
package org.apache.cayenne.access;

import java.lang.reflect.Array;

import org.apache.cayenne.MockSerializable;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.misc_types.ArraysEntity;
import org.apache.cayenne.testdo.misc_types.CharacterEntity;
import org.apache.cayenne.testdo.misc_types.SerializableEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class MiscTypesIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.MISC_TYPES_PROJECT);

    @Test
    public void serializable() throws Exception {

        // this test requires BLOB support
        if(!env.testDbAdapter().supportsLobs()) {
            return;
        }

        SerializableEntity test = env.context()
                .newObject(SerializableEntity.class);

        MockSerializable i = new MockSerializable("abc");
        test.setSerializableField(i);
        env.context().commitChanges();

        SerializableEntity testRead = ObjectSelect
                .query(SerializableEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getSerializableField());
        assertEquals(i.getName(), testRead.getSerializableField().getName());

        test.setSerializableField(null);
        env.context().commitChanges();
    }

    @Test
    public void byteArray() {
        ArraysEntity test = env.context().newObject(ArraysEntity.class);

        byte[] a = new byte[] {
                1, 2, 3
        };
        test.setByteArray(a);
        env.context().commitChanges();

        ArraysEntity testRead = ObjectSelect
                .query(ArraysEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getByteArray());
        assertArraysEqual(a, testRead.getByteArray());

        test.setByteArray(null);
        env.context().commitChanges();
    }

    @Test
    public void charArray() {
        ArraysEntity test = env.context().newObject(ArraysEntity.class);

        char[] a = new char[] {
                'x', 'y', 'z'
        };
        test.setCharArray(a);
        env.context().commitChanges();

        ArraysEntity testRead = ObjectSelect.query(ArraysEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getCharArray());
        assertArraysEqual(a, testRead.getCharArray());

        test.setCharArray(null);
        env.context().commitChanges();
    }

    @Test
    public void characterArray() {
        ArraysEntity test = env.context().newObject(ArraysEntity.class);

        Character[] a = new Character[] {
                'x', 'y', 'z'
        };
        test.setCharWrapperArray(a);
        env.context().commitChanges();

        ArraysEntity testRead = ObjectSelect.query(ArraysEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getCharWrapperArray());
        assertArraysEqual(a, testRead.getCharWrapperArray());

        test.setCharWrapperArray(null);
        env.context().commitChanges();
    }

    @Test
    public void character() {
        CharacterEntity test = env.context().newObject(CharacterEntity.class);

        test.setCharacterField('c');
        env.context().commitChanges();

        CharacterEntity testRead = ObjectSelect
                .query(CharacterEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getCharacterField());
        assertEquals((Character) 'c', testRead.getCharacterField());

        test.setCharacterField(null);
        env.context().commitChanges();
    }

    @Test
    public void byteWrapperArray() {
        ArraysEntity test = env.context().newObject(ArraysEntity.class);

        Byte[] a = new Byte[] {
                (byte) 1, (byte) 2, (byte) 3
        };
        test.setByteWrapperArray(a);
        env.context().commitChanges();

        ArraysEntity testRead = ObjectSelect
                .query(ArraysEntity.class)
                .selectFirst(env.context());
        assertNotNull(testRead.getByteWrapperArray());
        assertArraysEqual(a, testRead.getByteWrapperArray());

        test.setByteWrapperArray(null);
        env.context().commitChanges();
    }

    private void assertArraysEqual(Object a1, Object a2) {

        if (a1 == null && a2 == null) {
            return;
        }

        if (a1 == null && a2 != null) {
            fail("First array is null");
        }

        if (a2 == null && a1 != null) {
            fail("Second array is null");
        }

        assertEquals(Array.getLength(a1), Array.getLength(a2));
        for (int i = 0; i < Array.getLength(a1); i++) {
            assertEquals(Array.get(a1, i), Array.get(a2, i), "Difference at index " + i);
        }
    }
}
