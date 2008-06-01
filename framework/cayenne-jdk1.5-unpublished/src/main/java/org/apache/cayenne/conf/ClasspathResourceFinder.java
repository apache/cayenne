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

import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * A ResourceFinder that looks up resources in the classpath.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class ClasspathResourceFinder implements ResourceFinder {

    protected ClassLoader classLoader;
    protected Collection<String> rootPaths;

    public ClasspathResourceFinder() {
        rootPaths = new LinkedHashSet<String>(2);
        rootPaths.add("");
    }

    public URL getResource(String name) {
        return null;
    }

    public Collection<URL> getResources(String name) {
        return null;
    }

    /**
     * Adds a base path to be used for resource lookup. In the context of
     * ClasspathResourceFinder, a "path" corresponds to a package name, only separated by
     * "/" instead of ".". Default root path is empty String. This method allows to add
     * more lookup roots.
     */
    public void addRootPath(String path) {
        if (path == null) {
            throw new NullPointerException("Null path");
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        rootPaths.add(path);
    }

    /**
     * Returns ClassLoader override initialized via {@link #setClassLoader(ClassLoader)}.
     * Null by default.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets an overriding ClassLoader for this resource finder. Setting it is only needed
     * if the default thread class loader is not appropriate for looking up the resources.
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Returns a non-null ClassLoader that should be used to locate resources. The lookup
     * following order is used to find it: explicitly set class loader, current thread
     * class loader, this class class loader, system class loader.
     */
    protected ClassLoader getResourceClassLoader() {
        ClassLoader loader = this.classLoader;

        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }

        if (loader == null) {
            loader = getClass().getClassLoader();
        }

        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }

        if (loader == null) {
            throw new IllegalStateException(
                    "Can't detect ClassLoader to use for resouyrce location");
        }

        return loader;
    }
}
