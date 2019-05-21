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

package org.apache.cayenne.query;

import org.apache.cayenne.rop.ROPSerializationService;
import org.apache.cayenne.rop.protostuff.ProtostuffProperties;
import org.apache.cayenne.rop.protostuff.ProtostuffROPSerializationService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PrefetchTreeNodeSchemaTest extends ProtostuffProperties {

    private ROPSerializationService serializationService;

    @Before
    public void setUp() throws Exception {
        serializationService = new ProtostuffROPSerializationService();
    }

    @Test
    public void testPrefetchTreeNodeSchema() throws IOException {
        PrefetchTreeNode parent = new PrefetchTreeNode(null, "parent");
        PrefetchTreeNode child = new PrefetchTreeNode(parent, "child");
        parent.addChild(child);

        byte[] data = serializationService.serialize(parent);
        PrefetchTreeNode parent0 = serializationService.deserialize(data, PrefetchTreeNode.class);

        assertNotNull(parent0);
        assertTrue(parent0.hasChildren());

        PrefetchTreeNode child0 = parent0.getChild("child");
        assertNotNull(child0);
        assertNotNull(child0.parent);
        assertEquals(child0.parent, parent0);
    }

}
