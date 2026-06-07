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

package org.apache.cayenne.jcache;

import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;

import javax.cache.CacheManager;

/**
 * <p>
 * JCache Module
 * </p>
 *
 * @since 4.0
 */
public class JCacheModule implements Module {

    public static JCacheModuleExtender extend(Binder binder) {
        return new JCacheModuleExtender(binder);
    }

    /**
     * @deprecated in favor of {@link #extend(Binder)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public static void contributeJCacheProviderConfig(Binder binder, String providerConfigURI) {
        extend(binder).setJCacheProviderConfig(providerConfigURI);
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(CacheManager.class).toProvider(JCacheManagerProvider.class);
        binder.bind(JCacheConfigurationFactory.class).to(JCacheDefaultConfigurationFactory.class);
        binder.bind(QueryCache.class).to(JCacheQueryCache.class);
    }

}
