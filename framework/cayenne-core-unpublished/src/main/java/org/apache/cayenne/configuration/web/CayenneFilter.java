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

import java.io.IOException;
import java.util.Collection;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Module;

/**
 * A filter that creates a Cayenne server runtime, possibly including custom modules. By
 * default runtime includes {@link ServerModule} and {@link WebModule}. Any custom modules
 * are loaded after the two standard ones to allow custom service overrides. Filter
 * initialization parameters:
 * <ul>
 * <li>configuration-location - (optional) a name of Cayenne configuration XML file that
 * will be used to load Cayenne stack. If missing, the filter name will be used to derive
 * the location. ".xml" extension will be appended to the filter name to get the location,
 * so a filter named "cayenne-foo" will result in location "cayenne-foo.xml".
 * <li>extra-modules - (optional) a comma or space-separated list of class names, with
 * each class implementing {@link Module} interface. These are the custom modules loaded
 * after the two standard ones that allow users to override any Cayenne runtime aspects,
 * e.g. {@link RequestHandler}. Each custom module must have a no-arg constructor.
 * </ul>
 * <p>
 * CayenneFilter is a great utility to quickly start a Cayenne application. More advanced
 * apps most likely will not use it, relying on their own configuration mechanism (such as
 * Guice, Spring, etc.)
 * 
 * @since 3.1
 */
public class CayenneFilter implements Filter {

    protected ServletContext servletContext;

    public void init(FilterConfig config) throws ServletException {

        checkAlreadyConfigured(config.getServletContext());

        this.servletContext = config.getServletContext();

        WebConfiguration configAdapter = new WebConfiguration(config);

        String configurationLocation = configAdapter.getConfigurationLocation();
        Collection<Module> modules = configAdapter.createModules(new WebModule());

        ServerRuntime runtime = new ServerRuntime(
                configurationLocation,
                modules.toArray(new Module[modules.size()]));

        WebUtil.setCayenneRuntime(config.getServletContext(), runtime);
    }

    protected void checkAlreadyConfigured(ServletContext context) throws ServletException {
        // sanity check
        if (WebUtil.getCayenneRuntime(context) != null) {
            throw new ServletException(
                    "CayenneRuntime is already configured in the servlet environment");
        }
    }

    public void destroy() {
        CayenneRuntime runtime = WebUtil.getCayenneRuntime(servletContext);

        if (runtime != null) {
            runtime.shutdown();
        }
    }

    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        CayenneRuntime runtime = WebUtil.getCayenneRuntime(servletContext);
        RequestHandler handler = runtime.getInjector().getInstance(RequestHandler.class);

        handler.requestStart(request, response);
        try {
            chain.doFilter(request, response);
        }
        finally {
            handler.requestEnd(request, response);
        }
    }
}
