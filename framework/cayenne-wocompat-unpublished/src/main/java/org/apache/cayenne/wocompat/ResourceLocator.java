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

package org.apache.cayenne.wocompat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class to find resources (files, etc.), using a preconfigured strategy.
 * 
 * @deprecated since 3.1 deprecated in favor of injectable
 *             org.apache.cayenne.resource.ResourceLocator.
 */
@Deprecated
class ResourceLocator {

    private static Log logObj = LogFactory.getLog(ResourceLocator.class);

    // properties for enabling/disabling certain lookup strategies
    protected boolean skipAbsolutePath;
    protected boolean skipClasspath;
    protected boolean skipCurrentDirectory;

    // ClassLoader used for resource loading
    protected ClassLoader classLoader;

    /**
     * Looks up a file in the current directory.
     * 
     * @return file object matching the name, or <code>null</code> if <code>file</code>
     *         can not be found is not readable.
     */
    public static File findFileInCurrentDirectory(String name) {
        // look in the current directory
        String currentDirPath = System.getProperty("user.dir") + File.separator + name;

        try {

            File file = new File(currentDirPath);

            if (file.exists() && file.canRead()) {
                logObj.debug("file found in current directory: " + file);
            }
            else {
                logObj.debug("file not found in current directory: " + name);
                file = null;
            }

            return file;
        }
        catch (SecurityException se) {
            logObj.debug("permission denied reading file: " + currentDirPath, se);
            return null;
        }
    }

    /**
     * Looks up the URL for the named resource using the specified ClassLoader.
     */
    public static URL findURLInClassLoader(String name, ClassLoader loader) {
        URL url = loader.getResource(name);

        if (url != null) {
            logObj.debug("URL found with classloader: " + url);
        }
        else {
            logObj.debug("URL not found with classloader: " + name);
        }

        return url;
    }

    /**
     * @since 3.0
     */
    public URL getResource(String name) {
        return findResource(name);
    }

    /**
     * Returns a resource URL using the lookup strategy configured for this
     * Resourcelocator or <code>null</code> if no readable resource can be found for the
     * given name.
     */
    public URL findResource(String name) {
        if (!willSkipAbsolutePath()) {
            File f = new File(name);
            if (f.isAbsolute() && f.exists()) {
                logObj.debug("File found at absolute path: " + name);
                try {
                    return f.toURL();
                }
                catch (MalformedURLException ex) {
                    // ignoring
                    logObj.debug("Malformed url, ignoring.", ex);
                }
            }
            else {
                logObj.debug("No file at absolute path: " + name);
            }
        }

        if (!willSkipCurrentDirectory()) {
            File f = findFileInCurrentDirectory(name);
            if (f != null) {

                try {
                    return f.toURL();
                }
                catch (MalformedURLException ex) {
                    // ignoring
                    logObj.debug("Malformed url, ignoring", ex);
                }
            }
        }

        if (!willSkipClasspath()) {

            URL url = findURLInClassLoader(name, getClassLoader());
            if (url != null) {
                return url;
            }
        }

        return null;
    }

    /**
     * Returns a directory resource URL using the lookup strategy configured for this
     * ResourceLocator or <code>null</code> if no readable resource can be found for the
     * given name. The returned resource is assumed to be a directory, so the returned URL
     * will be in a directory format (with "/" at the end).
     */
    public URL findDirectoryResource(String name) {
        URL url = findResource(name);
        if (url == null) {
            return null;
        }

        try {
            String urlSt = url.toExternalForm();
            return (urlSt.endsWith("/")) ? url : new URL(urlSt + "/");
        }
        catch (MalformedURLException ex) {
            // ignoring...
            logObj.debug("Malformed URL, ignoring.", ex);
            return null;
        }
    }

    /**
     * Returns true if no lookups are performed in the current directory.
     */
    public boolean willSkipCurrentDirectory() {
        return skipCurrentDirectory;
    }

    /**
     * Sets "skipCurrentDirectory" property.
     */
    public void setSkipCurrentDirectory(boolean skipCurDir) {
        this.skipCurrentDirectory = skipCurDir;
    }

    /**
     * Returns true if no lookups are performed in the classpath.
     */
    public boolean willSkipClasspath() {
        return skipClasspath;
    }

    /**
     * Sets "skipClasspath" property.
     */
    public void setSkipClasspath(boolean skipClasspath) {
        this.skipClasspath = skipClasspath;
    }

    /**
     * Returns the ClassLoader associated with this ResourceLocator.
     */
    private ClassLoader getClassLoader() {
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

        return loader;
    }

    /**
     * Returns true if no lookups are performed using path as absolute path.
     */
    public boolean willSkipAbsolutePath() {
        return skipAbsolutePath;
    }

    /**
     * Sets "skipAbsolutePath" property.
     */
    public void setSkipAbsolutePath(boolean skipAbsPath) {
        this.skipAbsolutePath = skipAbsPath;
    }

}
