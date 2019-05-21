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
package org.apache.cayenne.remote;

import org.apache.cayenne.LifecycleListener;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.lifecycles.ClientLifecycles;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.LIFECYCLES_PROJECT)
@RunWith(value=Parameterized.class)
public class RemoteCallbacksIT extends RemoteCayenneCase implements LifecycleListener {
    private int added, loaded, prePersisted, postPersisted, preRemoved, postRemoved, preUpdated, postUpdated;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {LocalConnection.HESSIAN_SERIALIZATION},
                {LocalConnection.JAVA_SERIALIZATION},
                {LocalConnection.NO_SERIALIZATION},
        });
    }

    public RemoteCallbacksIT(int serializationPolicy) {
        super.serializationPolicy = serializationPolicy;
    }

    @Before
    public void setUp() throws Exception {
        added = 0;
        loaded = 0;
        prePersisted = 0;
        postPersisted = 0;
        preRemoved = 0;
        postRemoved = 0;
        preUpdated = 0;
        postUpdated = 0;
    }

    @Test
    public void testDefault() throws InterruptedException {
        ObjectContext context = createROPContext();
        context.getEntityResolver().getCallbackRegistry().addListener(ClientLifecycles.class, this);
        
        assertAll(0, 0, 0, 0, 0, 0, 0, 0);
        ClientLifecycles l1 = context.newObject(ClientLifecycles.class);
        
        assertAll(1, 0, 0, 0, 0, 0, 0, 0);
        l1.setName("x");
        assertAll(1, 0, 0, 0, 0, 0, 0, 0);
        
        context.commitChanges();
        Thread.sleep(5); //until commit
        assertAll(1, 0, 1, 1, 0, 0, 0, 0);
        
        l1.setName("x2");
        assertAll(1, 0, 1, 1, 0, 0, 0, 0);
        
        context.commitChanges();
        Thread.sleep(5); //until commit
        assertAll(1, 0, 1, 1, 1, 1, 0, 0);
        
        context.deleteObjects(l1);
        assertAll(1, 0, 1, 1, 1, 1, 1, 0);
        
        context.commitChanges();
        Thread.sleep(5); //until commit
        assertAll(1, 0, 1, 1, 1, 1, 1, 1);
    }
    
    private void assertAll(int added, int loaded, int prePersisted, int postPersisted,
            int preUpdated, int postUpdated, int preRemoved, int postRemoved) {
        assertEquals(this.added, added);
        assertEquals(this.loaded, loaded);
        assertEquals(this.prePersisted, prePersisted);
        assertEquals(this.postPersisted, postPersisted);
        assertEquals(this.preRemoved, preRemoved);
        assertEquals(this.postRemoved, postRemoved);
        assertEquals(this.preUpdated, preUpdated);
        assertEquals(this.postUpdated, postUpdated);
    }

    public void postAdd(Object entity) {
        added++;
    }

    public void postLoad(Object entity) {
        loaded++;
    }

    public void postPersist(Object entity) {
        postPersisted++;
    }

    public void postRemove(Object entity) {
        postRemoved++;
    }

    public void postUpdate(Object entity) {
        postUpdated++;
    }

    public void prePersist(Object entity) {
        prePersisted++;
    }

    public void preRemove(Object entity) {
        preRemoved++;
    }

    public void preUpdate(Object entity) {
        preUpdated++;
    }
}
