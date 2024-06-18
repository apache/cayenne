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
package org.apache.cayenne.lifecycle.id;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.lifecycle.db.E1;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IdCoderTest {

    private CayenneRuntime runtime;

    @Before
    public void setUp() throws Exception {
        runtime = CayenneRuntime.builder().addConfig("cayenne-lifecycle.xml").build();
    }

    @After
    public void tearDown() throws Exception {
        runtime.shutdown();
    }

    @Test
    public void testGetStringId() {
        IdCoder handler = new IdCoder(runtime.getChannel().getEntityResolver());

        E1 e1 = new E1();
        e1.setObjectId(ObjectId.of("E1", "ID", 5));
        assertEquals("E1:5", handler.getStringId(e1));
    }

    @Test
    public void testGetStringId_ObjectId() {
        IdCoder handler = new IdCoder(runtime.getChannel().getEntityResolver());
        assertEquals("E1:5", handler.getStringId(ObjectId.of("E1", "ID", 5)));
    }

    @Test
    public void testGetStringId_Temp() {
        IdCoder handler = new IdCoder(runtime.getChannel().getEntityResolver());

        byte[] key = new byte[] { 1, 2, 10, 100 };

        E1 e1 = new E1();
        e1.setObjectId(ObjectId.of("E1", key));

        assertEquals(".E1:01020A64", handler.getStringId(e1));
    }

    @Test
    public void testGetObjectId_Temp() {
        IdCoder handler = new IdCoder(runtime.getChannel().getEntityResolver());

        byte[] key = new byte[] { 1, (byte) 0xD7, 10, 100 };

        ObjectId decoded = handler.getObjectId(".E1:01D70A64");
        assertEquals(ObjectId.of("E1", key), decoded);
    }

    @Test
    public void testGetSringId_TempWithReplacement() {
        IdCoder handler = new IdCoder(runtime.getChannel().getEntityResolver());

        byte[] key = new byte[] { 5, 2, 11, 99 };
        ObjectId id = ObjectId.of("E1", key);
        id.getReplacementIdMap().put("ID", 6);

        E1 e1 = new E1();
        e1.setObjectId(id);

        assertEquals("E1:6", handler.getStringId(e1));
    }
}
