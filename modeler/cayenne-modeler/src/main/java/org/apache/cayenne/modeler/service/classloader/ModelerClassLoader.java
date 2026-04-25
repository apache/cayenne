/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.service.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A mutable class loader for the Modeler to dynamically access adapters, drivers and other classes and resources.
 */
public class ModelerClassLoader {

    private Set<File> files;
    private FileClassLoader delegate;

    /**
     * Returns a class for given class name.
     */
    public <T> Class<T> loadClass(Class<T> interfaceType, String className) throws ClassNotFoundException {
        return (Class<T>) getClassLoader().loadClass(className);
    }


    public ClassLoader getClassLoader() {
        if (delegate == null) {
            delegate = new FileClassLoader(getClass().getClassLoader(), getFiles());
        }

        return delegate;
    }

    public Set<File> getFiles() {
        Set<File> files = this.files;
        return files != null ? files : Set.of();
    }

    public void setFiles(Collection<File> files) {
        Set<File> resolved = files.stream().map(File::getAbsoluteFile).collect(Collectors.toSet());

        this.files = resolved;
        this.delegate = null;
    }

    // URLClassLoader with addURL method exposed.
    private static class FileClassLoader extends URLClassLoader {

        FileClassLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        FileClassLoader(ClassLoader parent, Set<File> files) {
            this(parent);

            for (File file : files) {
                // I guess here we have to quetly ignore invalid URLs...
                try {
                    addURL(file.toURI().toURL());
                } catch (MalformedURLException ignored) {
                }
            }
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }
    }
}
