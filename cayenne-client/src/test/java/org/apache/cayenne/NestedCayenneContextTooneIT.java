/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.remote.RemoteCayenneCase;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.toone.ClientTooneDep;
import org.apache.cayenne.testdo.toone.ClientTooneMaster;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@UseServerRuntime(CayenneProjects.TOONE_PROJECT)
@RunWith(value = Parameterized.class)
public class NestedCayenneContextTooneIT extends RemoteCayenneCase {

    @Inject
    private ClientRuntime runtime;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {LocalConnection.HESSIAN_SERIALIZATION},
                {LocalConnection.JAVA_SERIALIZATION},
                {LocalConnection.NO_SERIALIZATION},});
    }

    public NestedCayenneContextTooneIT(int serializationPolicy) {
        super.serializationPolicy = serializationPolicy;
    }

    /*
 * was added for CAY-1636
 */
    @Test
    public void testCAY1636() throws Exception {

        ClientTooneMaster A = clientContext
                .newObject(ClientTooneMaster.class);
        clientContext.commitChanges();

        ClientTooneDep B = clientContext.newObject(ClientTooneDep.class);
        A.setToDependent(B);
        clientContext.commitChanges();

        ObjectContext child = runtime.newContext(clientContext);

        ObjectSelect<ClientTooneMaster> query = ObjectSelect.query(ClientTooneMaster.class);
        List<ClientTooneMaster> objects = child.select(query);

        assertEquals(1, objects.size());

        ClientTooneMaster childDeleted = objects.get(0);

        child.deleteObjects(childDeleted);

        child.commitChangesToParent();

        ClientTooneMaster parentDeleted = (ClientTooneMaster) clientContext
                .getGraphManager().getNode(childDeleted.getObjectId());

        assertNotNull(parentDeleted);
        assertEquals(PersistenceState.DELETED,
                parentDeleted.getPersistenceState());

        clientContext.commitChanges();

        ObjectSelect<ClientTooneMaster> query2 = ObjectSelect.query(ClientTooneMaster.class);
        List<ClientTooneMaster> objects2 = child.select(query2);

        assertEquals(0, objects2.size());

    }

    @Test
    public void testCAY1636_2() throws Exception {

        ClientTooneMaster A = clientContext
                .newObject(ClientTooneMaster.class);
        clientContext.commitChanges();

        ClientTooneDep B = clientContext.newObject(ClientTooneDep.class);
        A.setToDependent(B);
        clientContext.commitChanges();

        ObjectContext child = runtime.newContext(clientContext);

        ObjectSelect<ClientTooneDep> queryB = ObjectSelect.query(ClientTooneDep.class);
        List<?> objectsB = child.performQuery(queryB);

        assertEquals(1, objectsB.size());

        ClientTooneDep childBDeleted = (ClientTooneDep) objectsB.get(0);
        child.deleteObjects(childBDeleted);

        ObjectSelect<ClientTooneMaster> query = ObjectSelect.query(ClientTooneMaster.class);
        List<ClientTooneMaster> objects = child.select(query);

        assertEquals(1, objects.size());

        ClientTooneMaster childDeleted = objects.get(0);

        child.deleteObjects(childDeleted);

        child.commitChangesToParent();

        ClientTooneMaster parentDeleted = (ClientTooneMaster) clientContext
                .getGraphManager().getNode(childDeleted.getObjectId());

        assertNotNull(parentDeleted);
        assertEquals(PersistenceState.DELETED,
                parentDeleted.getPersistenceState());

        clientContext.commitChanges();

        ObjectSelect<ClientTooneMaster> query2 = ObjectSelect.query(ClientTooneMaster.class);
        List<ClientTooneMaster> objects2 = child.select(query2);

        assertEquals(0, objects2.size());

    }

    @Test
    public void testCommitChangesToParentOneToOne() throws Exception {
        ObjectContext child = runtime.newContext(clientContext);

        ClientTooneMaster master = child.newObject(ClientTooneMaster.class);
        ClientTooneDep dep = child.newObject(ClientTooneDep.class);
        master.setToDependent(dep);

        child.commitChangesToParent();

        ClientTooneMaster masterParent = (ClientTooneMaster) clientContext
                .getGraphManager().getNode(master.getObjectId());
        ClientTooneDep depParent = (ClientTooneDep) clientContext
                .getGraphManager().getNode(dep.getObjectId());

        assertNotNull(masterParent);
        assertNotNull(depParent);

        assertSame(masterParent, depParent.getToMaster());
        assertSame(depParent, masterParent.getToDependent());

        // check that arc changes got recorded in the parent context
        GraphDiff diffs = clientContext.internalGraphManager().getDiffs();

        final int[] arcDiffs = new int[1];
        final int[] newNodes = new int[1];

        diffs.apply(new GraphChangeHandler() {

            @Override
            public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
                arcDiffs[0]++;
            }

            @Override
            public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {
                arcDiffs[0]--;
            }

            @Override
            public void nodeCreated(Object nodeId) {
                newNodes[0]++;
            }

            @Override
            public void nodeIdChanged(Object nodeId, Object newId) {
            }

            @Override
            public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
            }

            @Override
            public void nodeRemoved(Object nodeId) {
                newNodes[0]--;
            }
        });

        assertEquals(2, newNodes[0]);
        assertEquals(2, arcDiffs[0]);
    }

}
