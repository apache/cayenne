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

package org.apache.cayenne.modeler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A default implementation of ClassLoadingService used in CayenneModeler.
 * 
 * @since 1.1
 */
public class FileClassLoadingService implements ClassLoadingService {

    private FileClassLoader classLoader;
    protected List<File> pathFiles;

    public FileClassLoadingService() {
        this.pathFiles = new ArrayList<File>(15);
    }

    /**
     * Returns class for a given name, loading it if needed from configured locations.
     */
    public synchronized Class loadClass(String className) throws ClassNotFoundException {
        return nonNullClassLoader().loadClass(className);
    }

    /**
     * Returns a ClassLoader based on the current configured CLASSPATH settings.
     */
    public ClassLoader getClassLoader() {
        return nonNullClassLoader();
    }

    /**
     * Returns an unmodifiable list of configured CLASSPATH locations.
     */
    public synchronized List<File> getPathFiles() {
        return Collections.unmodifiableList(pathFiles);
    }

    public synchronized void setPathFiles(Collection<File> files) {

        pathFiles.clear();
        classLoader = null;

        for (File file : files) {
            addFile(file);
        }
    }

    /**
     * Adds a new location to the list of configured locations.
     */
    private void addFile(File file) {
        file = file.getAbsoluteFile();

        if (pathFiles.contains(file)) {
            return;
        }

        if (classLoader != null) {
            try {
                classLoader.addURL(file.toURI().toURL());
            }
            catch (MalformedURLException ex) {
                return;
            }
        }

        pathFiles.add(file);
    }

    private synchronized FileClassLoader nonNullClassLoader() {
        // init class loader on demand
        if (classLoader == null) {
            classLoader = new FileClassLoader(getClass().getClassLoader(), pathFiles);
        }

        return classLoader;
    }

    // URLClassLoader with addURL method exposed.
    static class FileClassLoader extends URLClassLoader {

        FileClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        FileClassLoader(ClassLoader parent, List<File> files) {
            this(parent);

            for (File file : files) {
                // I guess here we have to quetly ignore invalid URLs...
                try {
                    addURL(file.toURI().toURL());
                }
                catch (MalformedURLException ex) {
                }
            }
        }

        public void addURL(URL url) {
            super.addURL(url);
        }
    }
}
