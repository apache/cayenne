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

import junit.framework.TestCase;

import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.graph.MockGraphManager;

/**
 */
public class ContextStateRecorderTest extends TestCase {

    public void testDirtyNodesInState() {

        GraphManager map = new MockGraphManager();
        ObjectContextStateLog recorder = new ObjectContextStateLog(map);

        // check for null collections
        assertNotNull(recorder.dirtyNodes(PersistenceState.MODIFIED));
        assertNotNull(recorder.dirtyNodes(PersistenceState.COMMITTED));
        assertNotNull(recorder.dirtyNodes(PersistenceState.DELETED));
        assertNotNull(recorder.dirtyNodes(PersistenceState.NEW));
        assertNotNull(recorder.dirtyNodes(PersistenceState.TRANSIENT));
        assertNotNull(recorder.dirtyNodes(PersistenceState.HOLLOW));

        assertTrue(recorder.dirtyNodes(PersistenceState.MODIFIED).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.COMMITTED).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.DELETED).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.NEW).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.TRANSIENT).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.HOLLOW).isEmpty());

        MockPersistentObject modified = new MockPersistentObject();
        modified.setObjectId(new ObjectId("MockPersistentObject", "key", "value1"));
        modified.setPersistenceState(PersistenceState.MODIFIED);
        map.registerNode(modified.getObjectId(), modified);
        recorder.nodePropertyChanged(modified.getObjectId(), "a", "b", "c");

        assertTrue(recorder.dirtyNodes(PersistenceState.MODIFIED).contains(modified));
        assertTrue(recorder.dirtyNodes(PersistenceState.COMMITTED).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.DELETED).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.NEW).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.TRANSIENT).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.HOLLOW).isEmpty());

        MockPersistentObject deleted = new MockPersistentObject();
        deleted.setObjectId(new ObjectId("MockPersistentObject", "key", "value2"));
        deleted.setPersistenceState(PersistenceState.DELETED);
        map.registerNode(deleted.getObjectId(), deleted);
        recorder.nodeRemoved(deleted.getObjectId());

        assertTrue(recorder.dirtyNodes(PersistenceState.MODIFIED).contains(modified));
        assertTrue(recorder.dirtyNodes(PersistenceState.COMMITTED).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.DELETED).contains(deleted));
        assertTrue(recorder.dirtyNodes(PersistenceState.NEW).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.TRANSIENT).isEmpty());
        assertTrue(recorder.dirtyNodes(PersistenceState.HOLLOW).isEmpty());
    }

    public void testDirtyNodes() {
        GraphManager map = new MockGraphManager();
        ObjectContextStateLog recorder = new ObjectContextStateLog(map);

        assertNotNull(recorder.dirtyNodes());
        assertTrue(recorder.dirtyNodes().isEmpty());

        // introduce a fake dirty object
        MockPersistentObject object = new MockPersistentObject();
        object.setObjectId(new ObjectId("MockPersistentObject", "key", "value"));
        object.setPersistenceState(PersistenceState.MODIFIED);
        map.registerNode(object.getObjectId(), object);
        recorder.nodePropertyChanged(object.getObjectId(), "a", "b", "c");

        assertTrue(recorder.dirtyNodes().contains(object));

        // must go away on clear...
        recorder.clear();
        assertNotNull(recorder.dirtyNodes());
        assertTrue(recorder.dirtyNodes().isEmpty());
    }

    public void testHasChanges() {

        ObjectContextStateLog recorder = new ObjectContextStateLog(new MockGraphManager());
        assertFalse(recorder.hasChanges());

        // introduce a fake dirty object
        MockPersistentObject object = new MockPersistentObject();
        object.setObjectId(new ObjectId("MockPersistentObject", "key", "value"));
        object.setPersistenceState(PersistenceState.MODIFIED);
        recorder.nodePropertyChanged(object.getObjectId(), "xyz", "a", "b");

        assertTrue(recorder.hasChanges());

        // must go away on clear...
        recorder.clear();
        assertFalse(recorder.hasChanges());
    }

}
