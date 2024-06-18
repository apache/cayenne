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
package org.apache.cayenne.runtime;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.TransactionDescriptor;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionalOperation;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CayenneRuntimeTest {

    @Test
    public void testPerformInTransaction() {

        final BaseTransaction tx = mock(BaseTransaction.class);
        final TransactionFactory txFactory = mock(TransactionFactory.class);
        when(txFactory.createTransaction()).thenReturn(tx);
        when(txFactory.createTransaction(any(TransactionDescriptor.class))).thenReturn(tx);

        Module module = binder -> binder.bind(TransactionFactory.class).toInstance(txFactory);

        CayenneRuntime runtime = CayenneRuntime.builder().addConfig("xxxx").addModule(module).build();
        try {

            final Object expectedResult = new Object();
            Object result = runtime.performInTransaction(new TransactionalOperation<Object>() {
                public Object perform() {
                    assertSame(tx, BaseTransaction.getThreadTransaction());
                    return expectedResult;
                }
            });

            assertSame(expectedResult, result);
        } finally {
            runtime.shutdown();
        }

    }

    @Test
    public void testConstructor_Modules() {

        final boolean[] configured = new boolean[2];

        Module m1 = binder -> configured[0] = true;

        Module m2 = binder -> configured[1] = true;

        CayenneRuntime runtime = new CayenneRuntime(asList(m1, m2));

        Collection<Module> modules = runtime.getModules();
        assertEquals(2, modules.size());

        assertTrue(configured[0]);
        assertTrue(configured[1]);
    }

    @Test
    public void testGetDataChannel_CustomModule() {
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

            public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType) {
                return null;
            }
        };

        Module module = binder -> binder.bind(DataChannel.class).toInstance(channel);

        CayenneRuntime runtime = new CayenneRuntime(Collections.singleton(module));
        assertSame(channel, runtime.getChannel());
    }

    @Test
    public void testGetObjectContext_CustomModule() {
        final ObjectContext context = new DataContext();
        final ObjectContextFactory factory = new ObjectContextFactory() {

            public ObjectContext createContext(DataChannel parent) {
                return context;
            }

            public ObjectContext createContext() {
                return context;
            }
        };

        Module module = binder -> binder.bind(ObjectContextFactory.class).toInstance(factory);

        CayenneRuntime runtime = new CayenneRuntime(Collections.singleton(module));
        assertSame(context, runtime.newContext());
        assertSame(context, runtime.newContext());
    }

    @Test
    public void testBindThreadInjector() {

        Injector injector = mock(Injector.class);

        assertNull(CayenneRuntime.getThreadInjector());

        try {
            CayenneRuntime.bindThreadInjector(injector);
            assertSame(injector, CayenneRuntime.getThreadInjector());
        }
        finally {
            CayenneRuntime.bindThreadInjector(null);
        }

        assertNull(CayenneRuntime.getThreadInjector());
    }
}
