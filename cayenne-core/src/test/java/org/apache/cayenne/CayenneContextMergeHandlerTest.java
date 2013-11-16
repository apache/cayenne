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

package org.apache.cayenne;

import static org.mockito.Mockito.mock;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextMergeHandlerTest extends ClientCase {

    @Inject
    protected CayenneContext context;

    public void testShouldProcessEvent() {

        CayenneContextMergeHandler handler = new CayenneContextMergeHandler(context);

        // 1. Our context initiated the sync:
        // src == channel && postedBy == context
        GraphEvent e1 = new GraphEvent(context.getChannel(), context, null);
        assertFalse(handler.shouldProcessEvent(e1));

        // 2. Another context initiated the sync:
        // postedBy != context && source == channel
        GraphEvent e2 = new GraphEvent(context.getChannel(), mock(ObjectContext.class), null);
        assertTrue(handler.shouldProcessEvent(e2));

        // 2.1 Another object initiated the sync:
        // postedBy != context && source == channel
        GraphEvent e21 = new GraphEvent(context.getChannel(), new Object(), null);
        assertTrue(handler.shouldProcessEvent(e21));

        // 3. Another channel initiated the sync:
        // postedBy == ? && source != channel
        GraphEvent e3 = new GraphEvent(new MockDataChannel(), new Object(), null);
        assertFalse(handler.shouldProcessEvent(e3));

        // 4. inactive
        GraphEvent e4 = new GraphEvent(context.getChannel(), mock(ObjectContext.class), null);
        handler.active = false;
        assertFalse(handler.shouldProcessEvent(e4));
    }

    public void testNodePropertyChanged() {

        ClientMtTable1 o1 = context.newObject(ClientMtTable1.class);

        CayenneContextMergeHandler handler = new CayenneContextMergeHandler(context);

        assertNull(o1.getGlobalAttribute1Direct());

        handler.nodePropertyChanged(o1.getObjectId(), ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, null, "abc");
        assertEquals("abc", o1.getGlobalAttribute1Direct());

        handler.nodePropertyChanged(o1.getObjectId(), ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, "abc", "xyz");

        assertEquals("xyz", o1.getGlobalAttribute1Direct());

        // block if old value is different
        handler.nodePropertyChanged(o1.getObjectId(), ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, "123", "mnk");

        assertEquals("xyz", o1.getGlobalAttribute1Direct());

        handler.nodePropertyChanged(o1.getObjectId(), ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, "xyz", null);

        assertNull(o1.getGlobalAttribute1Direct());
    }
}
