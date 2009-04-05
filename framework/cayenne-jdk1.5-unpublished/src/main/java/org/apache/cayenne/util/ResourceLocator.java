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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.conf.ResourceFinder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class to find resources (files, etc.), using a preconfigured strategy.
 * 
 */
public class ResourceLocator implements ResourceFinder {

    private static Log logObj = LogFactory.getLog(ResourceLocator.class);

    // properties for enabling/disabling certain lookup strategies
    protected boolean skipAbsolutePath;
    protected boolean skipClasspath;
    protected boolean skipCurrentDirectory;
    protected boolean skipHomeDirectory;

    // additional lookup paths (as Strings)
    protected List<String> additionalClassPaths;
    protected List<String> additionalFilesystemPaths;

    // ClassLoader used for resource loading
    protected ClassLoader classLoader;

    /**
     * Returns a resource as InputStream if it is found in CLASSPATH or <code>null</code>
     * otherwise. Lookup is normally performed in all JAR and ZIP files and directories
     * available to the ClassLoader.
     * 
     * @deprecated since 3.0 unused.
     */
    public static InputStream findResourceInClasspath(String name) {
        try {
            URL url = findURLInClasspath(name);
            if (url != null) {
                logObj.debug("resource found in classpath: " + url);
                return url.openStream();
            }
            else {
                logObj.debug("resource not found in classpath: " + name);
                return null;
            }
        }
        catch (IOException ioex) {
            return null;
        }
    }

    /**
     * Returns a resource as InputStream if it is found in the filesystem or
     * <code>null</code> otherwise. Lookup is first performed relative to the user's
     * home directory (as defined by "user.home" system property), and then relative to
     * the current directory.
     * 
     * @deprecated since 3.0 unused
     */
    public static InputStream findResourceInFileSystem(String name) {
        try {
            File file = findFileInFileSystem(name);
            if (file != null) {
                logObj.debug("resource found in file system: " + file);
                return new FileInputStream(file);
            }
            else {
                logObj.debug("resource not found in file system: " + name);
                return null;
            }
        }
        catch (IOException ioex) {
            return null;
        }
    }

    /**
     * Looks up a file in the filesystem. First looks in the user home directory, then in
     * the current directory.
     * 
     * @return file object matching the name, or null if file can not be found or if it is
     *         not readable.
     * @see #findFileInHomeDirectory(String)
     * @see #findFileInCurrentDirectory(String)
     */
    public static File findFileInFileSystem(String name) {
        File file = findFileInHomeDirectory(name);

        if (file == null) {
            file = findFileInCurrentDirectory(name);
        }

        if (file != null) {
            logObj.debug("file found in file system: " + file);
        }
        else {
            logObj.debug("file not found in file system: " + name);
        }

        return file;
    }

    /**
     * Looks up a file in the user home directory.
     * 
     * @return file object matching the name, or <code>null</code> if <code>file</code>
     *         cannot be found or is not readable.
     */
    public static File findFileInHomeDirectory(String name) {
        // look in home directory
        String homeDirPath = System.getProperty("user.home") + File.separator + name;

        try {

            File file = new File(homeDirPath);
            if (file.exists() && file.canRead()) {
                logObj.debug("file found in home directory: " + file);
            }
            else {
                file = null;
                logObj.debug("file not found in home directory: " + name);
            }

            return file;
        }
        catch (SecurityException se) {
            logObj.debug("permission denied reading file: " + homeDirPath, se);
            return null;
        }
    }

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
     * Looks up the URL for the named resource using this class' ClassLoader.
     */
    public static URL findURLInClasspath(String name) {
        ClassLoader classLoader = ResourceLocator.class.getClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        return findURLInClassLoader(name, classLoader);
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
     * Returns a base URL as a String from which this class was loaded. This is normally a
     * JAR or a file URL, but it is ClassLoader dependent.
     * 
     * @deprecated since 3.0 unused.
     */
    public static String classBaseUrl(Class<?> aClass) {
        String pathToClass = aClass.getName().replace('.', '/') + ".class";
        ClassLoader classLoader = aClass.getClassLoader();

        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        URL selfUrl = classLoader.getResource(pathToClass);

        if (selfUrl == null) {
            return null;
        }

        String urlString = selfUrl.toExternalForm();
        return urlString.substring(0, urlString.length() - pathToClass.length());
    }

    /**
     * Creates new ResourceLocator with default lookup policy including user home
     * directory, current directory and CLASSPATH.
     */
    public ResourceLocator() {
        this.additionalClassPaths = new ArrayList<String>();
        this.additionalFilesystemPaths = new ArrayList<String>();
    }

    /**
     * Returns an InputStream on the found resource using the lookup strategy configured
     * for this ResourceLocator or <code>null</code> if no readable resource can be
     * found for the given name.
     */
    public InputStream findResourceStream(String name) {
        URL url = findResource(name);
        if (url == null) {
            return null;
        }

        try {
            return url.openStream();
        }
        catch (IOException ioex) {
            logObj.debug("Error reading URL, ignoring", ioex);
            return null;
        }
    }

    /**
     * @since 3.0
     */
    public URL getResource(String name) {
        return findResource(name);
    }

    /**
     * @since 3.0
     */
    public Collection<URL> getResources(String name) {
        URL resource = getResource(name);
        return resource != null ? Collections.singleton(resource) : Collections
                .<URL> emptySet();
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

        if (!willSkipHomeDirectory()) {
            File f = findFileInHomeDirectory(name);
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

        if (!additionalFilesystemPaths.isEmpty()) {
            logObj.debug("searching additional paths: " + this.additionalFilesystemPaths);
            for (String filePath : this.additionalFilesystemPaths) {
                File f = new File(filePath, name);
                logObj.debug("searching for: " + f.getAbsolutePath());
                if (f.exists()) {
                    try {
                        return f.toURL();
                    }
                    catch (MalformedURLException ex) {
                        // ignoring
                        logObj.debug("Malformed URL, ignoring.", ex);
                    }
                }
            }
        }

        if (!willSkipClasspath()) {

            // start with custom classpaths and then move to the default one
            if (!this.additionalClassPaths.isEmpty()) {
                logObj.debug("searching additional classpaths: "
                        + this.additionalClassPaths);

                for (String classPath : this.additionalClassPaths) {
                    String fullName = classPath + "/" + name;
                    logObj.debug("searching for: " + fullName);
                    URL url = findURLInClassLoader(fullName, getClassLoader());
                    if (url != null) {
                        return url;
                    }
                }
            }

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
     * Returns true if no lookups are performed in the user home directory.
     */
    public boolean willSkipHomeDirectory() {
        return skipHomeDirectory;
    }

    /**
     * Sets "skipHomeDirectory" property.
     */
    public void setSkipHomeDirectory(boolean skipHomeDir) {
        this.skipHomeDirectory = skipHomeDir;
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
    public ClassLoader getClassLoader() {
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
     * Sets ClassLoader used to locate resources. If <code>null</code> is passed, the
     * ClassLoader of the ResourceLocator class will be used.
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
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

    /**
     * Adds a custom path for class path lookups. Format should be "my/package/name"
     * <i>without </i> leading "/".
     */
    public void addClassPath(String customPath) {
        this.additionalClassPaths.add(customPath);
    }

    /**
     * Adds the given String as a custom path for filesystem lookups. The path can be
     * relative or absolute and is <i>not </i> checked for existence.
     * 
     * @throws IllegalArgumentException if <code>path</code> is <code>null</code>.
     */
    public void addFilesystemPath(String path) {
        if (path != null) {
            this.additionalFilesystemPaths.add(path);
        }
        else {
            throw new IllegalArgumentException("Path must not be null.");
        }
    }

    /**
     * Adds the given directory as a path for filesystem lookups. The directory is checked
     * for existence.
     * 
     * @throws IllegalArgumentException if <code>path</code> is <code>null</code>,
     *             not a directory or not readable.
     */
    public void addFilesystemPath(File path) {
        if (path != null && path.isDirectory()) {
            this.addFilesystemPath(path.getPath());
        }
        else {
            throw new IllegalArgumentException("Path '" + path + "' is not a directory.");
        }
    }
}
