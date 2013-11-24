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
package org.apache.cayenne.configuration.osgi;

import java.util.Map;

import org.apache.cayenne.di.Injector;

/**
 * @since 3.2
 */
public class DefaultOsgiEnvironment implements OsgiEnvironment {

    private ClassLoader applicationClassLoader;
    private ClassLoader cayenneServerClassLoader;
    private ClassLoader cayenneDiClassLoader;
    private Map<String, ClassLoader> perResourceClassLoaders;

    public DefaultOsgiEnvironment(ClassLoader applicationClassLoader, Map<String, ClassLoader> perResourceClassLoaders) {
        this.applicationClassLoader = applicationClassLoader;
        this.cayenneDiClassLoader = Injector.class.getClassLoader();
        this.cayenneServerClassLoader = DefaultOsgiEnvironment.class.getClassLoader();
        this.perResourceClassLoaders = perResourceClassLoaders;
    }

    @Override
    public ClassLoader resourceClassLoader(String resourceName) {
        ClassLoader cl = perResourceClassLoaders.get(resourceName);
        return cl != null ? cl : applicationClassLoader;
    }

    @Override
    public ClassLoader cayenneDiClassLoader() {
        return cayenneDiClassLoader;
    }

    @Override
    public ClassLoader cayenneServerClassLoader() {
        return cayenneServerClassLoader;
    }

}
