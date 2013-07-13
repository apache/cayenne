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

package org.apache.cayenne.remote;

import static org.mockito.Mockito.mock;
import junit.framework.TestCase;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;

public class SyncMessageTest extends TestCase {

    public void testConstructor() {
        ObjectContext source = mock(ObjectContext.class);
        GraphDiff diff = new CompoundDiff();
        SyncMessage message = new SyncMessage(source, DataChannel.FLUSH_NOCASCADE_SYNC, diff);

        assertSame(source, message.getSource());
        assertEquals(DataChannel.FLUSH_NOCASCADE_SYNC, message.getType());
        assertSame(diff, message.getSenderChanges());
    }

    public void testHessianSerialization() throws Exception {
        // id must be a serializable object; source doesn't have to be
        ObjectContext source = mock(ObjectContext.class);
        GraphDiff diff = new NodeCreateOperation("id-string");
        SyncMessage message = new SyncMessage(source, DataChannel.FLUSH_NOCASCADE_SYNC, diff);

        Object d = HessianUtil.cloneViaClientServerSerialization(message, new EntityResolver());
        assertNotNull(d);
        assertTrue(d instanceof SyncMessage);

        SyncMessage ds = (SyncMessage) d;
        assertNull(ds.getSource());
        assertEquals(message.getType(), ds.getType());
        assertNotNull(ds.getSenderChanges());
    }

    public void testConstructorInvalid() {
        ObjectContext source = mock(ObjectContext.class);
        new SyncMessage(source, DataChannel.FLUSH_NOCASCADE_SYNC, new CompoundDiff());
        new SyncMessage(source, DataChannel.FLUSH_CASCADE_SYNC, new CompoundDiff());
        new SyncMessage(null, DataChannel.ROLLBACK_CASCADE_SYNC, new CompoundDiff());

        int bogusType = 45678;
        try {
            new SyncMessage(source, bogusType, new CompoundDiff());
            fail("invalid type was allowed to go unnoticed...");
        }
        catch (IllegalArgumentException e) {

        }
    }
}
