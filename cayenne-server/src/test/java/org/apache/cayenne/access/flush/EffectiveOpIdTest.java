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

import org.apache.cayenne.ObjectId;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class EffectiveOpIdTest {

    @Test
    public void testEqualsTempGeneratedId() {
        ObjectId id1 = ObjectId.of("test");
        id1.getReplacementIdMap().put("pk", IdGenerationMarker.marker());
        EffectiveOpId effectiveOpId1 = new EffectiveOpId("test", Collections.singletonMap("pk", IdGenerationMarker.marker()));

        EffectiveOpId effectiveOpId2 = new EffectiveOpId("test", Collections.singletonMap("pk", ObjectIdValueSupplier.getFor(id1, "pk")));

        assertEquals(effectiveOpId1, effectiveOpId2);
        assertNotEquals(id1.getReplacementIdMap().get("pk"), IdGenerationMarker.marker());
    }

}