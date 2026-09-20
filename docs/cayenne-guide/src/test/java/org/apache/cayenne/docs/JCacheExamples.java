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
package org.apache.cayenne.docs;

import org.apache.cayenne.jcache.JCacheModule;
import org.apache.cayenne.runtime.CayenneRuntime;

import javax.cache.CacheManager;

/**
 * Examples that are only compiled, but not run, as they refer to the cache configuration files that are not a part of
 * the docs test project.
 */
public class JCacheExamples {

    private CacheManager customCacheManager;

    public void providerConfig() {
        // tag::providerConfig[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addModule(binder -> JCacheModule.extend(binder)
                        .setJCacheProviderConfig("cache-config.xml"))
                .build();
        // end::providerConfig[]
    }

    public void cacheManager() {
        // tag::cacheManager[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addModule(binder ->
                        binder.bind(CacheManager.class).toInstance(customCacheManager))
                .build();
        // end::cacheManager[]
    }

    public void ehcacheConfig() {
        // tag::ehcacheConfig[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addModule(binder -> JCacheModule.extend(binder)
                        .setJCacheProviderConfig("file:/ehcache.xml"))
                .build();
        // end::ehcacheConfig[]
    }
}
