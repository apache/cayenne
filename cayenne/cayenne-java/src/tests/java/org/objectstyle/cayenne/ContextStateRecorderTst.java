/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne;

import junit.framework.TestCase;

import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.graph.MockGraphManager;

/**
 * @author Andrus Adamchik
 */
public class ContextStateRecorderTst extends TestCase {

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
