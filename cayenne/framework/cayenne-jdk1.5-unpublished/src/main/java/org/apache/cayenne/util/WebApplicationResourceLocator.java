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

package org.apache.cayenne.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A ResourceLocator that can find resources relative to web application context.
 * 
 */
public class WebApplicationResourceLocator extends ResourceLocator {

    private static Log logObj = LogFactory.getLog(WebApplicationResourceLocator.class);

    protected ServletContext context;
    protected List<String> additionalContextPaths;

    /**
     * @since 1.2
     */
    public WebApplicationResourceLocator() {
        this.additionalContextPaths = new ArrayList<String>();
        this.addFilesystemPath("/WEB-INF/");
    }

    /**
     * Creates new WebApplicationResourceLocator with default lookup policy including user
     * home directory, current directory and CLASSPATH.
     */
    public WebApplicationResourceLocator(ServletContext context) {
        this();
        setServletContext(context);
    }

    /**
     * Sets the ServletContext used to locate resources.
     */
    public void setServletContext(ServletContext servletContext) {
        this.context = servletContext;
    }

    /**
     * Gets the ServletContext used to locate resources.
     */
    public ServletContext getServletContext() {
        return this.context;
    }

    /**
     * Looks for resources relative to /WEB-INF/ directory or any extra context paths
     * configured. Internal ServletContext is used to find resources.
     */
    @Override
    public URL findResource(String location) {
        if (!additionalContextPaths.isEmpty() && getServletContext() != null) {

            String suffix = location != null ? location : "";
            if (suffix.startsWith("/")) {
                suffix = suffix.substring(1);
            }

            for (String prefix : this.additionalContextPaths) {

                if (!prefix.endsWith("/")) {
                    prefix += "/";
                }

                String fullName = prefix + suffix;
                logObj.debug("searching for: " + fullName);
                try {
                    URL url = getServletContext().getResource(fullName);
                    if (url != null) {
                        return url;
                    }
                }
                catch (MalformedURLException ex) {
                    // ignoring
                    logObj.debug("Malformed URL, ignoring.", ex);
                }
            }
        }

        return super.findResource(location);
    }

    /**
     * Override ResourceLocator.addFilesystemPath(String) to intercept context paths
     * starting with "/WEB-INF/" to place in additionalContextPaths.
     */
    @Override
    public void addFilesystemPath(String path) {
        if (path != null) {
            if (path.startsWith("/WEB-INF/")) {
                this.additionalContextPaths.add(path);
            }
            else {
                super.addFilesystemPath(path);
            }
        }
        else {
            throw new IllegalArgumentException("Path must not be null.");
        }
    }
}
