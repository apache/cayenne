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

import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;

public class BaseContextTest extends TestCase {

    public void testUserPropertiesLazyInit() {
        BaseContext context = new MockBaseContext();
        assertNull(context.userProperties);

        Map<String, Object> properties = context.getUserProperties();
        assertNotNull(properties);
        assertSame(properties, context.getUserProperties());
    }

    public void testAttachToRuntimeIfNeeded() {

        final DataChannel channel = mock(DataChannel.class);
        final QueryCache cache = mock(QueryCache.class);

        Module testModule = new Module() {

            public void configure(Binder binder) {
                binder.bind(DataChannel.class).toInstance(channel);
                binder.bind(QueryCache.class).toInstance(cache);
            }
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        BaseContext context = new MockBaseContext();
        assertNull(context.channel);
        assertNull(context.queryCache);

        Injector oldInjector = CayenneRuntime.getThreadInjector();
        try {

            CayenneRuntime.bindThreadInjector(injector);

            assertTrue(context.attachToRuntimeIfNeeded());
            assertSame(channel, context.channel);

            assertFalse(context.attachToRuntimeIfNeeded());
            assertFalse(context.attachToRuntimeIfNeeded());
        }
        finally {
            CayenneRuntime.bindThreadInjector(oldInjector);
        }
    }

    public void testAttachToRuntimeIfNeeded_NoStack() {

        BaseContext context = new MockBaseContext();
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
