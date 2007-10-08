/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.conf.Configuration;

/**
 * A ResourceLocator that can find resources relative to web application context.
 * 
 * @author Andrei Adamchik
 */
public class WebApplicationResourceLocator extends ResourceLocator {

    private static Logger logObj;

    // Create a Predicate that will enable logging only when
    // Configuration.isLoggingConfigured() returns true.
    // The passed predicate argument is ignored.
    static {
        Predicate p = new Predicate() {

            public boolean evaluate(Object o) {
                return Configuration.isLoggingConfigured();
            }
        };

        logObj = new PredicateLogger(WebApplicationResourceLocator.class, p);
    }

    protected ServletContext context;
    protected List additionalContextPaths;

    /**
     * @since 1.2
     */
    public WebApplicationResourceLocator() {
        this.additionalContextPaths = new ArrayList();
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
    public URL findResource(String location) {
        if (!additionalContextPaths.isEmpty() && getServletContext() != null) {

            String suffix = location != null ? location : "";
            if (suffix.startsWith("/")) {
                suffix = suffix.substring(1);
            }

            Iterator cpi = this.additionalContextPaths.iterator();
            while (cpi.hasNext()) {
                String prefix = (String) cpi.next();

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