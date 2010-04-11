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
package org.apache.cayenne.resource;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ConfigurationException;

/**
 * A {@link ResourceLocator} that can locate resources in the filesystem. Instances of
 * {@link FilesystemResourceLocator} are explicitly created by the user and then bound to
 * a DI registry. E.g.:
 * 
 * <pre>
 * class MyModule implements Module {
 * 
 *     public void configure(Binder binder) {
 *        File dir1 = ...
 *        File dir2 = ...
 *        binder.bind(ResourceLocator.class).
 *           toInstance(new FilesystemResourceLocator(dir1, dir2);
 *     }
 * </pre>
 * 
 * @since 3.1
 */
public class FilesystemResourceLocator implements ResourceLocator {

    protected File[] roots;

    /**
     * Creates a new {@link FilesystemResourceLocator}, using an array of base locations
     * ("roots"). If a location is a file, its parent directory is used for resolving. If
     * location is a directory, it is used as is. If no locations are specified, current
     * application directory is used as a single base.
     */
    public FilesystemResourceLocator(File... roots) {
        init(roots);
    }

    /**
     * Creates a new {@link FilesystemResourceLocator}, using a collection of base
     * locations. If a location is a file, its parent directory is used for resolving. If
     * location is a directory, it is used as is. If no locations are specified, current
     * application directory is used as a single base.
     */
    public FilesystemResourceLocator(Collection<File> roots) {
        if (roots == null) {
            throw new NullPointerException("Null roots");
        }

        init(roots.toArray(new File[roots.size()]));
    }

    private void init(File[] roots) {

        if (roots == null || roots.length == 0) {
            roots = new File[] {
                new File(System.getProperty("user.dir"))
            };
        }

        this.roots = new File[roots.length];
        for (int i = 0; i < roots.length; i++) {
            File root = roots[i].isDirectory() ? roots[i] : roots[i].getParentFile();
            if (root == null) {
                throw new ConfigurationException("Invalid root: %s", roots[i]);
            }

            this.roots[i] = root;
        }
    }

    public Collection<Resource> findResources(String name) {
        Collection<Resource> resources = new ArrayList<Resource>(3);

        for (File root : roots) {

            File resourceFile = new File(root, name);
            if (resourceFile.exists()) {
                try {
                    resources.add(new URLResource(resourceFile.toURL()));
                }
                catch (MalformedURLException e) {
                    throw new CayenneRuntimeException(
                            "Can't convert file to URL: %s",
                            e,
                            resourceFile.getAbsolutePath());
                }
            }
        }

        return resources;
    }
}
