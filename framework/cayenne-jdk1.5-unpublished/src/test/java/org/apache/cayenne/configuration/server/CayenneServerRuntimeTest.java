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
package org.apache.cayenne.configuration.server;

import junit.framework.TestCase;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.server.CayenneServerRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

public class CayenneServerRuntimeTest extends TestCase {

    public void testDefaultConstructor() {
        CayenneServerRuntime runtime = new CayenneServerRuntime("xxxx");

        assertEquals("xxxx", runtime
                .getInjector()
                .getInstance(RuntimeProperties.class)
                .get(RuntimeProperties.CONFIGURATION_LOCATION));

        assertEquals(1, runtime.getModules().length);

        Module m0 = runtime.getModules()[0];
        assertTrue(m0 instanceof CayenneServerModule);
        assertEquals("xxxx", ((CayenneServerModule) m0).configurationLocation);
    }

    public void testConstructor_Modules() {

        final boolean[] configured = new boolean[2];

        Module m1 = new Module() {

            public void configure(Binder binder) {
                configured[0] = true;
            }
        };

        Module m2 = new Module() {

            public void configure(Binder binder) {
                configured[1] = true;
            }
        };

        CayenneServerRuntime runtime = new CayenneServerRuntime(m1, m2);
        assertEquals(2, runtime.getModules().length);

        for (int i = 0; i < configured.length; i++) {
            assertTrue(configured[i]);
        }
    }

    public void testGetDataChannel() {
        final DataChannel channel = new DataChannel() {

            public EntityResolver getEntityResolver() {
                return null;
            }

            public EventManager getEventManager() {
                return null;
            }

            public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
                return null;
            }

            public GraphDiff onSync(
                    ObjectContext originatingContext,
                    GraphDiff changes,
                    int syncType) {
                return null;
            }
        };

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(DataChannel.class).toInstance(channel);
            }
        };

        CayenneServerRuntime runtime = new CayenneServerRuntime(module);
        assertSame(channel, runtime.getDataChannel());
    }

    public void testGetObjectContext() {
        final ObjectContext context = new DataContext();
        final ObjectContextFactory factory = new ObjectContextFactory() {
            
            public ObjectContext createContext(DataChannel parent) {
                return context;
            }
            
            public ObjectContext createContext() {
                return context;
            }
        };

        Module module = new Module() {

            public void configure(Binder binder) {
                binder.bind(ObjectContextFactory.class).toInstance(factory);
            }
        };

        CayenneServerRuntime runtime = new CayenneServerRuntime(module);
        assertSame(context, runtime.getContext());
        assertSame(context, runtime.getContext());
    }
}
