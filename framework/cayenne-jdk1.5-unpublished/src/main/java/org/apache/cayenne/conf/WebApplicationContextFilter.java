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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.cayenne.BaseContext;
import org.apache.cayenne.access.DataContext;

/**
 * A Servlet Filter that binds session DataContext to the current request thread. During
 * the request application code without any knowledge of the servlet environment can
 * access DataContext via {@link DataContext#getThreadDataContext()} method. <p/> To
 * enable the filter add XML similar to this in the <code>web.xml</code> descriptor of a
 * web application:
 * 
 * <pre>
 *  &lt;filter&gt;
 *   &lt;filter-name&gt;CayenneFilter&lt;/filter-name&gt;
 *   &lt;filter-class&gt;org.apache.cayenne.conf.WebApplicationContextFilter&lt;/filter-class&gt;
 *   &lt;/filter&gt;
 *   &lt;filter-mapping&gt;
 *   &lt;filter-name&gt;CayenneFilter&lt;/filter-name&gt;
 *   &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *   &lt;/filter-mapping&gt;
 * </pre>
 * 
 * @since 1.2
 */
public class WebApplicationContextFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
        ServletUtil.initializeSharedConfiguration(filterConfig.getServletContext());
    }

    /**
     * Cleanup callback method that shuts down shared configuration, specifically
     * EventManager dispatch threads.
     */
    public void destroy() {
        Configuration config = Configuration.getSharedConfiguration();
        if (config != null) {
            config.shutdown();
        }
    }

    /**
     * The main worker method that binds a DataContext to the current thread on entry and
     * unbinds it on exit (regardless of whether any exceptions occured in the request).
     */
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        boolean reset = false;

        if (request instanceof HttpServletRequest) {
            reset = true;

            HttpSession session = ((HttpServletRequest) request).getSession(true);
            DataContext context = ServletUtil.getSessionContext(session);
            BaseContext.bindThreadObjectContext(context);
        }

        try {
            chain.doFilter(request, response);
        }
        finally {
            if (reset) {
                BaseContext.bindThreadObjectContext(null);
            }
        }
    }
}
