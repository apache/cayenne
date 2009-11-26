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
package org.apache.cayenne.runtime;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Scopes;
import org.apache.cayenne.runtime.access.DataChannelProvider;
import org.apache.cayenne.runtime.configuration.ConfigurationProvider;
import org.apache.cayenne.runtime.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.runtime.resource.ResourceLocator;

/**
 * A DI module containing all Cayenne framework configurations.
 * 
 * @since 3.1
 */
public class CayenneModule implements Module {

    public void configure(Binder binder) {

        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class).in(
                Scopes.SINGLETON);
        binder.bind(Configuration.class).toProvider(ConfigurationProvider.class).in(
                Scopes.SINGLETON);
        binder.bind(DataChannel.class).toProvider(DataChannelProvider.class).in(
                Scopes.SINGLETON);
    }
}
