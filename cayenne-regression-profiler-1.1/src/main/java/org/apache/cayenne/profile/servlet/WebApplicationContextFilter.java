/*
 *  Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.cayenne.profile.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.conf.BasicServletConfiguration;

/**
 * This filter is a part of 1.2, although it was not available in 1.1.
 */
public class WebApplicationContextFilter implements Filter {

    /**
     * Does nothing. As per the servlet specification, gets called by the container when
     * the filter is taken out of service.
     */
    public void destroy() {

    }

    /**
     * Initializes the <code>BasicServletConfiguration</code> via the
     * initializeConfiguration() method. Also saves the FilterConfing to a private local
     * variable for possible later access. This method is part of the <code>Filter</code>
     * interface and is called by the container when the filter is placed into service.
     */

    public synchronized void init(FilterConfig config) throws ServletException {
        BasicServletConfiguration.initializeConfiguration(config.getServletContext());
    }

    /**
     * Retrieves the <code>DataContext</code> bound to the <code>HttpSession</code>
     * via <code>BasicServletConfiguration.

     * getDefaultContext()</code>, and binds it to
     * the current thread.
     */

    public void doFilter(ServletRequest request, ServletResponse response,

    FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {

            HttpSession session = ((HttpServletRequest) request).getSession(true);
            DataContext ctx = BasicServletConfiguration.getDefaultContext(session);
            DataContext.bindThreadDataContext(ctx);

            if (ctx == null) {
                throw new ServletException("DataContext was null and could "
                        + "not be bound to thred");
            }
        }

        chain.doFilter(request, response);
        return;
    }
}
