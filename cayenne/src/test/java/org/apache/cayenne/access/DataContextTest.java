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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.tx.TransactionFactory;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class DataContextTest {
    @Test
    public void testUserPropertiesLazyInit() {
        DataContext context = new DataContext();
        assertNull(context.userProperties);

        Map<String, Object> properties = context.getUserProperties();
        assertNotNull(properties);
        assertSame(properties, context.getUserProperties());
    }

    @Test
    public void testAttachToRuntimeIfNeeded() {

        final DataChannel channel = mock(DataChannel.class);
        final QueryCache cache = mock(QueryCache.class);
        final TransactionFactory factory = mock(TransactionFactory.class);

        Module testModule = binder -> {
            binder.bind(DataChannel.class).toInstance(channel);
            binder.bind(QueryCache.class).toInstance(cache);
            binder.bind(TransactionFactory.class).toInstance(factory);
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DataContext context = new DataContext();
        assertNull(context.channel);
        assertNull(context.queryCache);

        Injector oldInjector = CayenneRuntime.getThreadInjector();
        try {

            CayenneRuntime.bindThreadInjector(injector);

            assertTrue(context.attachToRuntimeIfNeeded());
            assertSame(channel, context.getChannel());

            assertFalse(context.attachToRuntimeIfNeeded());
            assertFalse(context.attachToRuntimeIfNeeded());
        }
        finally {
            CayenneRuntime.bindThreadInjector(oldInjector);
        }
    }

    @Test
    public void testAttachToRuntimeIfNeeded_NoStack() {

        DataContext context = new DataContext();
        assertNull(context.channel);
        assertNull(context.queryCache);

        try {
            context.attachToRuntimeIfNeeded();
            fail("No thread stack, must have thrown");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }
    }
}