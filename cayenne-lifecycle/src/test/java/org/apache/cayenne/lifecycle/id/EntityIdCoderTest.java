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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EntityIdCoderTest {

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
    public void testGetEntityName() {
        assertEquals("M", EntityIdCoder.getEntityName("M:N:K"));
        assertEquals("M", EntityIdCoder.getEntityName(".M:N:K"));
    }

    @Test
    public void testTempId() {

        ObjEntity e1 = runtime.getChannel().getEntityResolver()
                .getObjEntity("E1");
        EntityIdCoder coder = new EntityIdCoder(e1);

        byte[] key = new byte[] { 2, 2, 10, 100 };
        ObjectId encoded = ObjectId.of("E1", key);

        String string = coder.toStringId(encoded);
        assertEquals(".E1:02020A64", string);

        ObjectId decoded = coder.toObjectId(string);
        assertTrue(decoded.isTemporary());
        assertEquals(encoded, decoded);
    }

    @Test
    public void testSingleIntPk() {
        DbEntity dbEntity = new DbEntity("X");
        DbAttribute pk = new DbAttribute("ID");
        pk.setType(Types.INTEGER);
        pk.setPrimaryKey(true);
        dbEntity.addAttribute(pk);

        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getName()).thenReturn("x");
        when(entity.getDbEntityName()).thenReturn(dbEntity.getName());
        when(entity.getDbEntity()).thenReturn(dbEntity);

        ObjectId id = ObjectId.of("x", "ID", 3);

        EntityIdCoder coder = new EntityIdCoder(entity);
        assertEquals("x:3", coder.toStringId(id));

        ObjectId parsedId = coder.toObjectId("x:3");
        assertEquals(id, parsedId);
    }

    @Test
    public void testSingleLongPk() {
        DbEntity dbEntity = new DbEntity("X");
        DbAttribute pk = new DbAttribute("ID");
        pk.setType(Types.BIGINT);
        pk.setPrimaryKey(true);
        dbEntity.addAttribute(pk);

        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getName()).thenReturn("x");
        when(entity.getDbEntityName()).thenReturn(dbEntity.getName());
        when(entity.getDbEntity()).thenReturn(dbEntity);

        ObjectId id = ObjectId.of("x", "ID", 3L);

        EntityIdCoder coder = new EntityIdCoder(entity);
        assertEquals("x:3", coder.toStringId(id));

        ObjectId parsedId = coder.toObjectId("x:3");
        assertEquals(id, parsedId);
    }

    @Test
    public void testSingleStringPk() {
        DbEntity dbEntity = new DbEntity("X");
        DbAttribute pk = new DbAttribute("ID");
        pk.setType(Types.VARCHAR);
        pk.setPrimaryKey(true);
        dbEntity.addAttribute(pk);

        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getName()).thenReturn("x");
        when(entity.getDbEntityName()).thenReturn(dbEntity.getName());
        when(entity.getDbEntity()).thenReturn(dbEntity);

        EntityIdCoder coder = new EntityIdCoder(entity);

        ObjectId id = ObjectId.of("x", "ID", "AbC");
        assertEquals("x:AbC", coder.toStringId(id));

        ObjectId parsedId = coder.toObjectId("x:AbC");
        assertEquals(id, parsedId);
    }

    @Test
    public void testIdEncoding() {
        DbEntity dbEntity = new DbEntity("X");
        DbAttribute pk = new DbAttribute("ID");
        pk.setType(Types.VARCHAR);
        pk.setPrimaryKey(true);
        dbEntity.addAttribute(pk);

        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getName()).thenReturn("x");
        when(entity.getDbEntityName()).thenReturn(dbEntity.getName());
        when(entity.getDbEntity()).thenReturn(dbEntity);

        EntityIdCoder coder = new EntityIdCoder(entity);

        ObjectId id = ObjectId.of("x", "ID", "Ab:C");
        assertEquals("x:Ab%3AC", coder.toStringId(id));

        ObjectId parsedId = coder.toObjectId("x:Ab%3AC");
        assertEquals(id, parsedId);
    }

    @Test
    public void testMixedCompoundPk() {
        DbEntity dbEntity = new DbEntity("X");
        DbAttribute pk1 = new DbAttribute("ID");
        pk1.setType(Types.VARCHAR);
        pk1.setPrimaryKey(true);
        dbEntity.addAttribute(pk1);

        DbAttribute pk2 = new DbAttribute("ABC");
        pk2.setType(Types.BIGINT);
        pk2.setPrimaryKey(true);
        dbEntity.addAttribute(pk2);

        DbAttribute pk3 = new DbAttribute("ZZZ");
        pk3.setType(Types.VARCHAR);
        pk3.setPrimaryKey(true);
        dbEntity.addAttribute(pk3);

        ObjEntity entity = mock(ObjEntity.class);
        when(entity.getName()).thenReturn("x");
        when(entity.getDbEntityName()).thenReturn(dbEntity.getName());
        when(entity.getDbEntity()).thenReturn(dbEntity);

        EntityIdCoder coder = new EntityIdCoder(entity);

        Map<String, Object> idMap = new HashMap<>();
        idMap.put("ID", "X;Y");
        idMap.put("ABC", 6783463L);
        idMap.put("ZZZ", "'_'");
        ObjectId id = ObjectId.of("x", idMap);
        assertEquals("x:6783463:X%3BY:%27_%27", coder.toStringId(id));

        ObjectId parsedId = coder.toObjectId("x:6783463:X%3BY:%27_%27");
        assertEquals(id, parsedId);
    }
}
