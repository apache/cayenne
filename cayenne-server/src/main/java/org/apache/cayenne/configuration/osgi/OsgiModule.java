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

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;

/**
 * A DI module that helps to bootstrap Cayenne in OSGi environment.
 * 
 * @since 3.2
 */
public class OsgiModule implements Module {

    /**
     * A factory method that creates a new OsgiModule, initialized with any
     * class from the OSGi bundle that contains Cayenne mapping and persistent
     * classes. This is likely the the bundle that is calling this method.
     */
    public static OsgiModule forProject(Class<?> typeFromProjectBundle) {

        if (typeFromProjectBundle == null) {
            throw new NullPointerException("Null 'typeFromProjectBundle'");
        }

        OsgiModule module = new OsgiModule();
        module.typeFromProjectBundle = typeFromProjectBundle;
        return module;
    }

    private Class<?> typeFromProjectBundle;

    private OsgiModule() {
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(OsgiEnvironment.class).toInstance(
                new DefaultOsgiEnvironment(typeFromProjectBundle.getClassLoader()));

        binder.bind(AdhocObjectFactory.class).to(SplitClassLoaderAdhocObjectFactory.class);
        binder.bind(DataDomain.class).toProvider(OsgiDataDomainProvider.class);
    }
}
