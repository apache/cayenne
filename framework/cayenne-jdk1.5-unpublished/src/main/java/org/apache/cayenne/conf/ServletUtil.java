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
import javax.servlet.http.HttpSession;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.util.ResourceLocator;
import org.apache.cayenne.util.WebApplicationResourceLocator;

/**
 * Configuration class that uses ServletContext to locate resources. This class is
 * intended for use in J2EE servlet containers. It is compatible with containers following
 * servlet specification version 2.2 and newer (e.g. Tomcat can be used starting from
 * version 3).
 * <p>
 * ServletConfiguration resolves configuration file locations relative to the web
 * application "WEB-INF" directory, and does not require them to be in the CLASSPATH
 * (though CLASSPATH locations such as "/WEB-INF/classes" and "/WEB-INF/lib/some.jar" are
 * supported as well). By default search for cayenne.xml is done in /WEB-INF/ folder. To
 * specify an arbitrary context path in the web application (e.g. "/WEB-INF/cayenne"), use
 * <code>cayenne.configuration.path</code> context parameters in <code>web.xml</code>.
 * </p>
 * 
 * @since 1.2
 */
public class ServletUtil {

    /**
     * A name of the web application initialization parameter used to specify extra paths
     * where Cayenne XML files might be located. E.g. "/WEB-INF/cayenne".
     */
    public static final String CONFIGURATION_PATH_KEY = "cayenne.configuration.path";

    /**
     * Used by BasicServletConfiguration as a session attribute for DataContext.
     */
    public static final String DATA_CONTEXT_KEY = "cayenne.datacontext";

    /**
     * Creates a new ServletConfiguration and sets is as a Configuration signleton.
     */
    public synchronized static Configuration initializeSharedConfiguration(
            ServletContext context) {

        // check if this web application is already configured

        // don't use static getter, since it will do initialization on demand!!!
        Configuration oldConfig = Configuration.sharedConfiguration;
        if (oldConfig instanceof DefaultConfiguration) {

            ResourceFinder locator = oldConfig.getResourceFinder();

            if (locator instanceof WebApplicationResourceLocator) {
                if (((WebApplicationResourceLocator) locator).getServletContext() == context) {
                    return oldConfig;
                }
            }
        }

        // create new shared configuration
        DefaultConfiguration conf = new DefaultConfiguration(
                Configuration.DEFAULT_DOMAIN_FILE,
                createLocator(context));
        Configuration.initializeSharedConfiguration(conf);

        return conf;
    }

    /**
     * A helper method to create default ResourceLocator.
     */
    protected static ResourceLocator createLocator(ServletContext context) {
        WebApplicationResourceLocator locator = new WebApplicationResourceLocator();
        locator.setSkipAbsolutePath(true);
        locator.setSkipClasspath(false);
        locator.setSkipCurrentDirectory(true);
        locator.setSkipHomeDirectory(true);

        locator.setServletContext(context);
        String configurationPath = context.getInitParameter(CONFIGURATION_PATH_KEY);
        if (configurationPath != null && configurationPath.trim().length() > 0) {
            locator.addFilesystemPath(configurationPath.trim());
        }

        return locator;
    }

    /**
     * Returns default Cayenne DataContext associated with the HttpSession, creating it on
     * the fly and storing in the session if needed.
     */
    public static DataContext getSessionContext(HttpSession session) {
        synchronized (session) {
            DataContext ctxt = (DataContext) session.getAttribute(DATA_CONTEXT_KEY);

            if (ctxt == null) {
                ctxt = DataContext.createDataContext();
                session.setAttribute(ServletUtil.DATA_CONTEXT_KEY, ctxt);
            }

            return ctxt;
        }
    }
}
