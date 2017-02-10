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

package org.apache.cayenne.jcache;

import java.net.URI;
import java.net.URISyntaxException;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;

/**
 * @since 4.0
 */
public class JCacheManagerProvider implements Provider<CacheManager> {

    @Inject
    private RuntimeProperties properties;

    @Override
    public CacheManager get() throws DIRuntimeException {
        CachingProvider provider;
        try {
            provider = Caching.getCachingProvider();
        } catch (CacheException e) {
            throw new RuntimeException("'cayenne-jcache' doesn't bundle any JCache providers. " +
                    "You must place a JCache 1.0 provider on classpath explicitly.", e);
        }

        CacheManager manager;
        URI jcacheConfig = getConfig();

        if(jcacheConfig == null) {
            manager = provider.getCacheManager();
        } else {
            manager = provider.getCacheManager(jcacheConfig, null);
        }

        return manager;
    }

    private URI getConfig() {
        String config = properties.get(JCacheConstants.JCACHE_PROVIDER_CONFIG);
        if(config == null) {
            return null;
        } else {
            try {
                return new URI(config);
            } catch (URISyntaxException ex) {
                throw new RuntimeException("Wrong value for JCache provider config property", ex);
            }
        }
    }
}
