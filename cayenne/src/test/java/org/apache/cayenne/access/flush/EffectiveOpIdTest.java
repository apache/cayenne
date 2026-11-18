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

package org.apache.cayenne.access.flush;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EffectiveOpIdTest {

    @Test
    public void equalsTempGeneratedId() {
        IdGenerationMarker marker = IdGenerationMarker.marker();
        ObjectId id1 = ObjectId.of("test");
        id1.getReplacementIdMap().put("pk", marker);
        EffectiveOpId effectiveOpId1 = new EffectiveOpId("test", Collections.singletonMap("pk", marker));

        // the deferred value of the PK resolves to the marker
        EffectiveOpId effectiveOpId2 = new EffectiveOpId(
                "test",
                Collections.singletonMap("pk", ObjectIdValueSupplier.getFor(id1, "pk")));
        assertEquals(effectiveOpId1, effectiveOpId2);

        // the PKs not generated yet are all different (CAY-2801)
        assertNotEquals(id1.getReplacementIdMap().get("pk"), IdGenerationMarker.marker());
        EffectiveOpId effectiveOpId3 = new EffectiveOpId("test", Collections.singletonMap("pk", IdGenerationMarker.marker()));
        assertNotEquals(effectiveOpId1, effectiveOpId3);
    }

    @Test
    public void equalsBinaryKey() {
        EffectiveOpId id1 = new EffectiveOpId("test", Map.of("pk", new byte[]{1, 2, 3}));
        EffectiveOpId id2 = new EffectiveOpId("test", Map.of("pk", new byte[]{1, 2, 3}));
        EffectiveOpId id3 = new EffectiveOpId("test", Map.of("pk", new byte[]{1, 2, 4}));

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertNotEquals(id1, id3);
    }

    @Test
    public void equalsCompoundBinaryKey() {
        Map<String, Object> s1 = new HashMap<>();
        s1.put("a", new byte[]{1, 2, 3});
        s1.put("b", 5);
        Map<String, Object> s2 = new HashMap<>();
        s2.put("a", new byte[]{1, 2, 3});
        s2.put("b", 5L);

        EffectiveOpId id1 = new EffectiveOpId("test", s1);
        EffectiveOpId id2 = new EffectiveOpId("test", s2);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }
}
