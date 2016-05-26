/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne;

import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.rop.ROPSerializationService;
import org.apache.cayenne.rop.protostuff.ProtostuffProperties;
import org.apache.cayenne.rop.protostuff.ProtostuffROPSerializationService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ObjectContextChangeLogSubListMessageFactoryTest extends ProtostuffProperties {

    private ROPSerializationService serializationService;

    @Before
    public void setUp() throws Exception {
        serializationService = new ProtostuffROPSerializationService();
    }

    @Test
    public void testGetDiffsSerializable() throws Exception {
        ObjectContextChangeLog recorder = new ObjectContextChangeLog();
        recorder.addOperation(new NodeCreateOperation(new ObjectId("test")));
        CompoundDiff diff = (CompoundDiff) recorder.getDiffs();

        byte[] data = serializationService.serialize(diff);
        CompoundDiff diff0 = serializationService.deserialize(data, CompoundDiff.class);

        assertNotNull(diff0);
        assertEquals(1, diff0.getDiffs().size());
    }

}
