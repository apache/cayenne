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

import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.QueryMetadata;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 4.0
 */
public class JCacheQueryCache implements QueryCache {

    @Inject
    protected CacheManager cacheManager;

    @Inject
    protected JCacheConfigurationFactory configurationFactory;

    private Set<String> seenCacheNames = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    @Override
    public List get(QueryMetadata metadata) {
        String key = Objects.requireNonNull(metadata.getCacheKey());
        Cache<String, List> cache = createIfAbsent(metadata);

        return cache.get(key);
    }

    @Override
    public List get(QueryMetadata metadata, QueryCacheEntryFactory factory) {
        String key = Objects.requireNonNull(metadata.getCacheKey());
        Cache<String, List> cache = createIfAbsent(metadata);

        List<?> result = cache.get(key);
        return result != null
                ? result
                : cache.invoke(key, new JCacheEntryLoader(factory));
    }

    @Override
    public void put(QueryMetadata metadata, List results) {
        String key = Objects.requireNonNull(metadata.getCacheKey());
        Cache<String, List> cache = createIfAbsent(metadata);

        cache.put(key, results);
    }

    @Override
    public void remove(String key) {
        if (key != null) {
            for (String cache : cacheManager.getCacheNames()) {
                getCache(cache).remove(key);
            }
        }
    }

    @Override
    public void removeGroup(String groupKey) {
        Cache<String, List> cache = getCache(groupKey);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public void removeGroup(String groupKey, Class<?> keyType, Class<?> valueType) {
        Cache cache = cacheManager.getCache(groupKey, keyType, valueType);
        if (cache != null) {
            cache.clear();
        }
    }

    @Override
    public void clear() {
        for (String name : seenCacheNames) {
            getCache(name).clear();
        }
    }

    protected Cache<String, List> createIfAbsent(QueryMetadata metadata) {
        return createIfAbsent(cacheName(metadata));
    }

    @SuppressWarnings("unchecked")
    protected Cache<String, List> createIfAbsent(String cacheName) {

        Cache<String, List> cache = getCache(cacheName);
        if (cache == null) {

            try {
                cache = createCache(cacheName);
            } catch (CacheException e) {
                // someone else just created this cache?
                cache = getCache(cacheName);
                if (cache == null) {
                    // giving up... the error was about something else...
                    throw e;
                }
            }

            seenCacheNames.add(cacheName);
        }

        return cache;
    }

    protected Cache createCache(String cacheName) {
        return cacheManager.createCache(cacheName, configurationFactory.create(cacheName));
    }

    protected Cache<String, List> getCache(String name) {
        return cacheManager.getCache(name);
    }

    protected String cacheName(QueryMetadata metadata) {

        String cacheGroup = metadata.getCacheGroup();
        if (cacheGroup != null) {
            return cacheGroup;
        }

        // no explicit cache group
        return JCacheConstants.DEFAULT_CACHE_NAME;
    }

    @BeforeScopeEnd
    public void shutdown() {
        cacheManager.close();
    }
}
