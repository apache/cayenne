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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

import org.apache.cayenne.access.DataContext;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockHttpSession;

/**
 */
public class WebApplicationContextFilterTest extends TestCase {

    public void testDoFilter() throws Exception {

        WebApplicationContextFilter filter = new WebApplicationContextFilter();

        // assemble session
        DataContext dataContext = new DataContext();
        HttpSession session = new MockHttpSession();
        session.setAttribute(ServletUtil.DATA_CONTEXT_KEY, dataContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        TestFilter testFilter = new TestFilter();

        chain.addFilter(filter);
        chain.addFilter(testFilter);

        // check no thread DC before
        try {
            DataContext.getThreadDataContext();
            fail("There is a DataContext bound to thread already.");
        }
        catch (IllegalStateException ex) {
            // expected
        }

        chain.doFilter(request, response);

        assertSame(dataContext, testFilter.threadContext);

        // check no thread DC after
        try {
            DataContext.getThreadDataContext();
            fail("DataContext was not unbound from the thread.");
        }
        catch (IllegalStateException ex) {
            // expected
        }
    }

    class TestFilter implements Filter {

        DataContext threadContext;

        public void destroy() {
        }

        public void doFilter(
                ServletRequest request,
                ServletResponse response,
                FilterChain chain) throws IOException, ServletException {
            threadContext = DataContext.getThreadDataContext();
        }

        public void init(FilterConfig arg0) throws ServletException {
        }
    }
}
