/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.rop.protostuff;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.reflect.MapAccessor;
import org.apache.cayenne.rop.ROPSerializationService;
import org.apache.cayenne.util.PersistentObjectList;
import org.apache.cayenne.util.PersistentObjectMap;
import org.apache.cayenne.util.PersistentObjectSet;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ProtostuffPersistentObjectCollectionsTest extends ProtostuffProperties {

    private ROPSerializationService serializationService;

    private TestObject object1;
    private TestObject object2;

    @Before
    public void setUp() throws Exception {
        serializationService = new ProtostuffROPSerializationService();

        object1 = new TestObject();
        object2 = new TestObject();
        object1.name = "object1";
        object2.name = "object2";
        object1.object = object2;
    }

    @Test
    public void testPersistentObjectList() throws IOException {
        PersistentObjectList<TestObject> list = new PersistentObjectList<>(object1, "test");
        list.add(object2);

        byte[] bytes = serializationService.serialize(list);
        PersistentObjectList list0 = serializationService.deserialize(bytes, PersistentObjectList.class);

        assertNotNull(list0);
        assertEquals(list.getRelationshipName(), list0.getRelationshipName());
        assertEquals(list.getRelationshipOwner(), list0.getRelationshipOwner());

        Object object0 = list0.get(0);
        assertEquals(object2, object0);
    }

    @Test
    public void testPersistentObjectListWithWrapper() throws IOException {
        PersistentObjectList<TestObject> list = new PersistentObjectList<>(object1, "test");
        list.add(object2);

        byte[] bytes = serializationService.serialize(new ListWrapper(list));
        ListWrapper lw = serializationService.deserialize(bytes, ListWrapper.class);

        assertNotNull(lw.object);
        assertTrue(lw.object instanceof PersistentObjectList);

        PersistentObjectList list0 = (PersistentObjectList) lw.object;
        assertEquals(list.getRelationshipName(), list0.getRelationshipName());
        assertEquals(list.getRelationshipOwner(), list0.getRelationshipOwner());

        Object object0 = list0.get(0);
        assertEquals(object2, object0);
    }

    @Test
    public void testPersistentObjectSet() throws IOException {
        PersistentObjectSet set = new PersistentObjectSet(object1, "test");
        set.add(object2);

        byte[] bytes = serializationService.serialize(set);
        PersistentObjectSet set0 = serializationService.deserialize(bytes, PersistentObjectSet.class);

        assertNotNull(set0);
        assertEquals(set.getRelationshipName(), set0.getRelationshipName());
        assertEquals(set.getRelationshipOwner(), set0.getRelationshipOwner());

        Object object0 = set0.toArray()[0];
        assertEquals(object2, object0);
    }

    @Test
    public void testPersistentObjectSetWithWrapper() throws IOException {
        PersistentObjectSet set = new PersistentObjectSet(object1, "test");
        set.add(object2);

        byte[] bytes = serializationService.serialize(new SetWrapper(set));
        SetWrapper sw = serializationService.deserialize(bytes, SetWrapper.class);

        assertNotNull(sw.object);
        assertTrue(sw.object instanceof PersistentObjectSet);

        PersistentObjectSet set0 = (PersistentObjectSet) sw.object;
        assertNotNull(set0);
        assertEquals(set.getRelationshipName(), set0.getRelationshipName());
        assertEquals(set.getRelationshipOwner(), set0.getRelationshipOwner());

        Object object0 = set0.toArray()[0];
        assertEquals(object2, object0);
    }

    @Test
    public void testPersistentObjectMap() throws IOException {
        PersistentObjectMap map = new PersistentObjectMap(object1, "test", new MapAccessor("test"));
        map.put(object2.name, object2);

        byte[] bytes = serializationService.serialize(map);
        PersistentObjectMap map0 = serializationService.deserialize(bytes, PersistentObjectMap.class);

        assertNotNull(map0);
        assertEquals(map0.getRelationshipName(), map0.getRelationshipName());
        assertEquals(map0.getRelationshipOwner(), map0.getRelationshipOwner());

        Object object0 = map0.get(object2.name);
        assertEquals(object2, object0);
    }

    @Test
    public void testPersistentObjectMapWithWrapper() throws IOException {
        PersistentObjectMap map = new PersistentObjectMap(object1, "test", new MapAccessor("test"));
        map.put(object2.name, object2);

        byte[] bytes = serializationService.serialize(new MapWrapper(map));
        MapWrapper mw = serializationService.deserialize(bytes, MapWrapper.class);

        assertNotNull(mw.object);
        assertTrue(mw.object instanceof PersistentObjectMap);

        PersistentObjectMap map0 = (PersistentObjectMap) mw.object;
        assertEquals(map0.getRelationshipName(), map0.getRelationshipName());
        assertEquals(map0.getRelationshipOwner(), map0.getRelationshipOwner());

        Object object0 = map0.get(object2.name);
        assertEquals(object2, object0);
    }

    private static class TestObject extends PersistentObject {
        public String name;
        public TestObject object;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestObject)) return false;

            TestObject that = (TestObject) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;
            return object != null ? object.equals(that.object) : that.object == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (object != null ? object.hashCode() : 0);
            return result;
        }
    }

    private static class ListWrapper {
        List<?> object;

        public ListWrapper(List<?> object) {
            this.object = object;
        }
    }

    private static class SetWrapper {
        Set<?> object;

        public SetWrapper(Set<?> object) {
            this.object = object;
        }
    }

    private static class MapWrapper {
        Map<?, ?> object;

        public MapWrapper(Map<?, ?> object) {
            this.object = object;
        }
    }
}
