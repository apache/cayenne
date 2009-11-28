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
package org.apache.cayenne.configuration;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Scopes;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * A DI module containing all Cayenne runtime configurations. To customize Cayenne runtime
 * configuration, either extend this module, or supply an extra custom module when
 * creating {@link CayenneRuntime}.
 * 
 * @since 3.1
 */
public class CayenneModule implements Module {

    protected String runtimeName;

    public CayenneModule(String runtimeName) {
        this.runtimeName = runtimeName;
    }

    public void configure(Binder binder) {

        // configure global stack properties
        binder.bindMap(RuntimeProperties.class).put(
                RuntimeProperties.CAYENNE_RUNTIME_NAME,
                runtimeName);

        // configure various services
        binder.bind(DataChannelLoader.class).to(XMLDataChannelLoader.class).in(
                Scopes.SINGLETON);
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class).in(
                Scopes.SINGLETON);
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class).in(
                Scopes.SINGLETON);
    }
}
