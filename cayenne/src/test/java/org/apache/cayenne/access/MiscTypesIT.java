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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.misc_types.ArraysEntity;
import org.apache.cayenne.testdo.misc_types.CharacterEntity;
import org.apache.cayenne.testdo.misc_types.SerializableEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.MISC_TYPES_PROJECT)
public class MiscTypesIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Test
    public void testSerializable() throws Exception {

        // this test requires BLOB support
        if(!accessStackAdapter.supportsLobs()) {
            return;
        }

        SerializableEntity test = context
                .newObject(SerializableEntity.class);

        MockSerializable i = new MockSerializable("abc");
        test.setSerializableField(i);
        context.commitChanges();

        SerializableEntity testRead = ObjectSelect
                .query(SerializableEntity.class)
                .selectFirst(context);
        assertNotNull(testRead.getSerializableField());
        assertEquals(i.getName(), testRead.getSerializableField().getName());

        test.setSerializableField(null);
        context.commitChanges();
    }

    @Test
    public void testByteArray() {
        ArraysEntity test = context.newObject(ArraysEntity.class);

        byte[] a = new byte[] {
                1, 2, 3
        };
        test.setByteArray(a);
        context.commitChanges();

        ArraysEntity testRead = ObjectSelect
                .query(ArraysEntity.class)
                .selectFirst(context);
        assertNotNull(testRead.getByteArray());
        assertArraysEqual(a, testRead.getByteArray());

        test.setByteArray(null);
        context.commitChanges();
    }

    @Test
    public void testCharArray() {
        ArraysEntity test = context.newObject(ArraysEntity.class);

        char[] a = new char[] {
                'x', 'y', 'z'
        };
        test.setCharArray(a);
        context.commitChanges();

        ArraysEntity testRead = ObjectSelect.query(ArraysEntity.class)
                .selectFirst(context);
        assertNotNull(testRead.getCharArray());
        assertArraysEqual(a, testRead.getCharArray());

        test.setCharArray(null);
        context.commitChanges();
    }

    @Test
    public void testCharacterArray() {
        ArraysEntity test = context.newObject(ArraysEntity.class);

        Character[] a = new Character[] {
                'x', 'y', 'z'
        };
        test.setCharWrapperArray(a);
        context.commitChanges();

        ArraysEntity testRead = ObjectSelect.query(ArraysEntity.class)
                .selectFirst(context);
        assertNotNull(testRead.getCharWrapperArray());
        assertArraysEqual(a, testRead.getCharWrapperArray());

        test.setCharWrapperArray(null);
        context.commitChanges();
    }

    @Test
    public void testCharacter() {
        CharacterEntity test = context.newObject(CharacterEntity.class);

        test.setCharacterField('c');
        context.commitChanges();

        CharacterEntity testRead = ObjectSelect
                .query(CharacterEntity.class)
                .selectFirst(context);
        assertNotNull(testRead.getCharacterField());
        assertEquals((Character) 'c', testRead.getCharacterField());

        test.setCharacterField(null);
        context.commitChanges();
    }

    @Test
    public void testByteWrapperArray() {
        ArraysEntity test = context.newObject(ArraysEntity.class);

        Byte[] a = new Byte[] {
                (byte) 1, (byte) 2, (byte) 3
        };
        test.setByteWrapperArray(a);
        context.commitChanges();

        ArraysEntity testRead = ObjectSelect
                .query(ArraysEntity.class)
                .selectFirst(context);
        assertNotNull(testRead.getByteWrapperArray());
        assertArraysEqual(a, testRead.getByteWrapperArray());

        test.setByteWrapperArray(null);
        context.commitChanges();
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
            assertEquals("Difference at index " + i, Array.get(a1, i), Array.get(a2, i));
        }
    }
}
