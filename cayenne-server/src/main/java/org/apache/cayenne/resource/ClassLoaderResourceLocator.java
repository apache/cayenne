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
package org.apache.cayenne.resource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.Inject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

/**
 * A {@link ResourceLocator} that looks up resources is the application
 * classpath based on the current thread ClassLoader.
 * 
 * @since 3.1
 */
public class ClassLoaderResourceLocator implements ResourceLocator {

    private ClassLoaderManager classLoaderManager;

    public ClassLoaderResourceLocator(@Inject ClassLoaderManager classLoaderManager) {
        this.classLoaderManager = classLoaderManager;
    }

    @Override
    public Collection<Resource> findResources(String name) {

        Collection<Resource> resources = new ArrayList<>(3);

        Enumeration<URL> urls;
        try {
            urls = classLoaderManager.getClassLoader(name).getResources(name);
        } catch (IOException e) {
            throw new ConfigurationException("Error getting resources for ");
        }

        while (urls.hasMoreElements()) {

            // TODO: andrus 11/30/2009 - replace URLResource that resolves
            // relative URL's as truly relative with some kind of
            // ClasspathResource that creates a relative *path* and then
            // resolves it against the entire classpath space.
            resources.add(new URLResource(urls.nextElement()));
        }

        return resources;
    }

}
