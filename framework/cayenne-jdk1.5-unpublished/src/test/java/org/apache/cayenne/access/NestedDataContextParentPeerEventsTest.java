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

package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.relationship.Child;
import org.apache.cayenne.testdo.relationship.Master;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class NestedDataContextParentPeerEventsTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DataContext parentContext1;

    @Inject
    private DataContext parentContext2;

    public void testPeerObjectUpdatedSimpleProperty() throws Exception {
        Master a = parentContext1.newObject(Master.class);
        a.setName("X");
        parentContext1.commitChanges();

        Master a1 = parentContext2.localObject(a);

        final ObjectContext child = runtime.newContext(parentContext1);
        final Master a2 = child.localObject(a);

        a1.setName("Y");
        assertEquals("X", a2.getName());
        parentContext2.commitChangesToParent();

        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals("Y", a2.getName());

                assertFalse(
                        "Peer data context became dirty on event processing",
                        child.hasChanges());
            }
        }.runTest(2000);
    }

    public void testPeerObjectUpdatedToOneRelationship() throws Exception {
        Master a = parentContext1.newObject(Master.class);
        Master altA = parentContext1.newObject(Master.class);

        Child p = parentContext1.newObject(Child.class);
        p.setMaster(a);
        a.setName("X");
        altA.setName("Y");
        parentContext1.commitChanges();

        Child p1 = parentContext2.localObject(p);
        Master altA1 = parentContext2.localObject(altA);

        final ObjectContext childContext1 = runtime.newContext(parentContext1);
        final Child p2 = childContext1.localObject(p);
        final Master altA2 = childContext1.localObject(altA);
        Master a2 = childContext1.localObject(a);

        p1.setMaster(altA1);
        assertSame(a2, p2.getMaster());
        assertNotSame(altA2, p2.getMaster());
        parentContext2.commitChanges();

        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertSame(altA2, p2.getMaster());
                assertFalse(
                        "Peer data context became dirty on event processing",
                        childContext1.hasChanges());
            }
        }.runTest(2000);
    }

    public void testPeerObjectUpdatedToManyRelationship() throws Exception {
        Master a = parentContext1.newObject(Master.class);
        a.setName("X");

        Child px = parentContext1.newObject(Child.class);
        px.setMaster(a);

        Child py = parentContext1.newObject(Child.class);

        parentContext1.commitChanges();

        Child py1 = parentContext2.localObject(py);
        Master a1 = parentContext2.localObject(a);

        final ObjectContext peer2 = runtime.newContext(parentContext1);
        final Child py2 = peer2.localObject(py);
        final Master a2 = peer2.localObject(a);

        a1.addToChildren(py1);
        assertEquals(1, a2.getChildren().size());
        assertFalse(a2.getChildren().contains(py2));
        parentContext2.commitChangesToParent();

        new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertEquals(2, a2.getChildren().size());
                assertTrue(a2.getChildren().contains(py2));

                assertFalse(
                        "Peer data context became dirty on event processing",
                        peer2.hasChanges());
            }
        }.runTest(2000);
    }
}
