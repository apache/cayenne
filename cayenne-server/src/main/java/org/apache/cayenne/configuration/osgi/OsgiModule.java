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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.Module;

/**
 * A DI module that helps to bootstrap Cayenne in OSGi environment.
 * 
 * @since 3.2
 */
public class OsgiModule implements Module {

    private Class<?> typeFromProjectBundle;
    private Map<String, ClassLoader> perTypeClassLoaders;

    OsgiModule() {
        this.perTypeClassLoaders = new HashMap<String, ClassLoader>();
    }

    void setTypeFromProjectBundle(Class<?> typeFromProjectBundle) {
        this.typeFromProjectBundle = typeFromProjectBundle;
    }

    void putClassLoader(String type, ClassLoader classLoader) {
        perTypeClassLoaders.put(type, classLoader);
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ClassLoaderManager.class).toInstance(createClassLoaderManager());
        binder.bind(DataDomain.class).toProvider(OsgiDataDomainProvider.class);
    }

    private ClassLoaderManager createClassLoaderManager() {
        return new OsgiClassLoaderManager(typeFromProjectBundle.getClassLoader(), perTypeClassLoaders);
    }
}
