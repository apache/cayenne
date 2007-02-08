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


package org.apache.cayenne.conf;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.unit.util.ThreadedTestHelper;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpSession;
import com.mockrunner.mock.web.MockServletContext;

/**
 * @author Andrus Adamchik
 * @deprecated since 1.2
 */
public class WebApplicationContextProviderTst extends TestCase {

    public void testThreadContext() throws Exception {

        final WebApplicationContextProvider provider = createTestProvider();
        final DataContext dataContext = new DataContext();

        final HttpSession session = new MockHttpSession();
        session.setAttribute(ServletUtil.DATA_CONTEXT_KEY, dataContext);

        ThreadedTestHelper helper = new ThreadedTestHelper() {

            protected void assertResult() throws Exception {
                // reproduce essential parts of normal request processing flow
                ServletContext servletContext = new MockServletContext();
                MockHttpServletRequest servletRequest = new MockHttpServletRequest();
                servletRequest.setSession(session);
                ServletRequestEvent event = new ServletRequestEvent(
                        servletContext,
                        servletRequest);

                provider.requestInitialized(event);

                // assert that context is bound to thread...
                assertEquals(dataContext, DataContext.getThreadDataContext());
            }
        };
        helper.assertWithTimeout(2000);
    }

    public void testThreadContextDifferentThread() throws Exception {
        final ServletContext servletContext = new MockServletContext();
        final WebApplicationContextProvider provider = createTestProvider();
        final DataContext dataContext1 = new DataContext();
        final DataContext dataContext2 = new DataContext();

        final HttpSession session1 = new MockHttpSession();
        session1.setAttribute(ServletUtil.DATA_CONTEXT_KEY, dataContext1);

        final HttpSession session2 = new MockHttpSession();
        session2.setAttribute(ServletUtil.DATA_CONTEXT_KEY, dataContext2);

        ThreadedTestHelper helper1 = new ThreadedTestHelper() {

            protected void assertResult() throws Exception {
                // reproduce essential parts of normal request processing flow
                MockHttpServletRequest servletRequest = new MockHttpServletRequest();
                servletRequest.setSession(session1);
                ServletRequestEvent event = new ServletRequestEvent(
                        servletContext,
                        servletRequest);

                provider.requestInitialized(event);

                // assert that context is bound to thread...
                assertEquals(dataContext1, DataContext.getThreadDataContext());

                provider.requestDestroyed(event);
                try {
                    DataContext.getThreadDataContext();
                    fail("DataContext Must have been unbound...");
                }
                catch (IllegalStateException ex) {
                    // expected, we are outside the request thread...
                }
            }
        };
        helper1.assertWithTimeout(2000);

        // same thing again in a different thread....
        ThreadedTestHelper helper2 = new ThreadedTestHelper() {

            protected void assertResult() throws Exception {
                // reproduce essential parts of normal request processing flow

                MockHttpServletRequest servletRequest = new MockHttpServletRequest();
                servletRequest.setSession(session2);
                ServletRequestEvent event = new ServletRequestEvent(
                        servletContext,
                        servletRequest);

                provider.requestInitialized(event);

                // assert that context is bound to thread...
                assertEquals(dataContext2, DataContext.getThreadDataContext());

                provider.requestDestroyed(event);
                try {
                    DataContext.getThreadDataContext();
                    fail("DataContext Must have been unbound...");
                }
                catch (IllegalStateException ex) {
                    // expected, we are outside the request thread...
                }
            }
        };
        helper2.assertWithTimeout(2000);

        // TODO: test the same worker thread changing contexts instead of separate ones...
    }

    protected WebApplicationContextProvider createTestProvider() throws Exception {
        // configure mockup objects for the web listener environment...

        final Configuration config = new MockConfiguration();
        config.addDomain(new DataDomain("mockup"));
        return new WebApplicationContextProvider() {

            protected Configuration getConfiguration() {
                return config;
            }
        };
    }
}
