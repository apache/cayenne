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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;

import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 4.0
 */
public class JCacheQueryCache implements QueryCache {

    private static final Log LOGGER = LogFactory.getLog(JCacheQueryCache.class);

    @Inject
    private CacheManager cacheManager;

    @Inject
    private JCacheConfigurationFactory configurationFactory;

    private ConcurrentMap<String, Object> seenCacheNames = new ConcurrentHashMap<>();

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
    public void clear() {
        for(String name : seenCacheNames.keySet()) {
            getCache(name).clear();
        }
    }

    /**
     * Returns -1 to indicate that we can't calculate the size. JCache and EhCache can potentially have a complex topology
     * that can not be meaningfully described by a single int. Use other means (like provider-specific JMX) to monitor cache.
     *
     * @deprecated since 4.0
     * @return -1
     */
    @Override
    @Deprecated
    public int size() {
        return -1;
    }

    protected Cache<String, List> createIfAbsent(QueryMetadata metadata) {
        return createIfAbsent(cacheName(metadata));
    }

    protected Cache<String, List> createIfAbsent(String cacheName) {

        Cache<String, List> cache = getCache(cacheName);
        if (cache == null) {

            try {
                cache = cacheManager.createCache(cacheName, configurationFactory.create(cacheName));
            } catch (CacheException e) {
                // someone else just created this cache?
                cache = getCache(cacheName);
                if (cache == null) {
                    // giving up... the error was about something else...
                    throw e;
                }
            }

            seenCacheNames.put(cacheName, 1);
        }

        return cache;
    }

    protected Cache<String, List> getCache(String name) {
        return cacheManager.getCache(name, String.class, List.class);
    }

    protected String cacheName(QueryMetadata metadata) {

        String[] cacheGroups = metadata.getCacheGroups();

        if (cacheGroups != null && cacheGroups.length > 0) {

            if (cacheGroups.length > 1) {
                if (LOGGER.isWarnEnabled()) {
                    List<String> ignored = Arrays.asList(cacheGroups).subList(1, cacheGroups.length);
                    LOGGER.warn("multiple cache groups per key '" + metadata.getCacheKey() + "', using the first one: "
                            + cacheGroups[0] + ". Ignoring others: " + ignored);
                }
            }

            return cacheGroups[0];
        }

        // no explicit cache groups
        return JCacheConstants.DEFAULT_CACHE_NAME;
    }

    /**
     * Shuts down CacheManager
     */
    @BeforeScopeEnd
    public void shutdown() {
        cacheManager.close();
    }
}
