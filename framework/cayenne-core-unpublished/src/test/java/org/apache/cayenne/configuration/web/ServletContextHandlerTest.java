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
package org.apache.cayenne.configuration.web;

import static org.mockito.Mockito.mock;
import junit.framework.TestCase;

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.MockDataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockHttpSession;

public class ServletContextHandlerTest extends TestCase {

    public void testRequestStart_bindContext() {

        Module module = new Module() {

            public void configure(Binder binder) {

                binder.bind(DataChannel.class).to(MockDataChannel.class);
                binder.bind(ObjectContextFactory.class).toInstance(
                        new ObjectContextFactory() {

                            public ObjectContext createContext(DataChannel parent) {
                                return mock(ObjectContext.class);
                            }

                            public ObjectContext createContext() {
                                return mock(ObjectContext.class);
                            }
                        });
            }
        };
        Injector injector = DIBootstrap.createInjector(module);
        SessionContextRequestHandler handler = new SessionContextRequestHandler();
        injector.injectMembers(handler);

        MockHttpSession session = new MockHttpSession();

        BaseContext.bindThreadObjectContext(null);

        try {

            MockHttpServletRequest request1 = new MockHttpServletRequest();
            MockHttpServletResponse response1 = new MockHttpServletResponse();
            request1.setSession(session);
            handler.requestStart(request1, response1);

            ObjectContext c1 = BaseContext.getThreadObjectContext();
            assertNotNull(c1);

            handler.requestEnd(request1, response1);

            try {
                BaseContext.getThreadObjectContext();
                fail("thread context not null");
            }
            catch (IllegalStateException e) {
                // expected
            }

            MockHttpServletRequest request2 = new MockHttpServletRequest();
            MockHttpServletResponse response2 = new MockHttpServletResponse();
            request2.setSession(session);
            handler.requestStart(request2, response2);

            ObjectContext c2 = BaseContext.getThreadObjectContext();
            assertSame(c1, c2);

            handler.requestEnd(request2, response2);
            try {
                BaseContext.getThreadObjectContext();
                fail("thread context not null");
            }
            catch (IllegalStateException e) {
                // expected
            }

            MockHttpServletRequest request3 = new MockHttpServletRequest();
            MockHttpServletResponse response3 = new MockHttpServletResponse();
            request3.setSession(new MockHttpSession());
            handler.requestStart(request3, response3);

            ObjectContext c3 = BaseContext.getThreadObjectContext();
            assertNotNull(c3);
            assertNotSame(c1, c3);

            handler.requestEnd(request3, response3);
            try {
                BaseContext.getThreadObjectContext();
                fail("thread context not null");
            }
            catch (IllegalStateException e) {
                // expected
            }
        }
        finally {
            BaseContext.bindThreadObjectContext(null);
        }
    }
}
