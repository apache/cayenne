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
package org.apache.cayenne.cache;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.util.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * A default implementation of the {@link QueryCache} interface that stores data in a
 * non-expiring LRUMap.
 * 
 * @since 3.0
 */
public class MapQueryCache implements QueryCache, Serializable {

    public static final int DEFAULT_CACHE_SIZE = 1000;

    static final String DEFAULT_CACHE_NAME = "cayenne.default.cache";

    protected final Map<String, Map<String, List<?>>> cacheGroups;

    private int maxSize;

    public MapQueryCache() {
        this(DEFAULT_CACHE_SIZE);
    }

    public MapQueryCache(int maxSize) {
        this.cacheGroups = new ConcurrentHashMap<>();
        this.maxSize = maxSize;
    }

    public List get(QueryMetadata metadata) {
        String key = metadata.getCacheKey();
        if (key == null) {
            return null;
        }
        Map<String, List<?>> map = createIfAbsent(metadata);
        synchronized (map) {
            return map.get(key);
        }
    }

    /**
     * Returns a non-null cached value. If it is not present in the cache, it is obtained
     * by calling {@link QueryCacheEntryFactory#createObject()} without blocking the cache. As
     * a result there is a potential of multiple threads to be updating cache in parallel -
     * this wouldn't lead to corruption of the cache, but can be suboptimal.
     */
    @SuppressWarnings("rawtypes")
    public List get(QueryMetadata metadata, QueryCacheEntryFactory factory) {
        List result = get(metadata);
        if (result == null) {
            List newObject = factory.createObject();
            if (newObject == null) {
                throw new CayenneRuntimeException("Null on cache rebuilding: %s", metadata.getCacheKey());
            }

            result = newObject;
            put(metadata, result);
        }

        return result;
    }

    public void put(QueryMetadata metadata, List results) {
        String key = metadata.getCacheKey();
        if (key == null) {
            return;
        }

        Map<String, List<?>> map = createIfAbsent(metadata);
        synchronized (map) {
            map.put(key, results);
        }
    }

    public void remove(String key) {
        if (key == null) {
            return;
        }

        for(Map<String, List<?>> map : cacheGroups.values()) {
            synchronized (map) {
                map.remove(key);
            }
        }
    }

    public void removeGroup(String groupKey) {
        if (groupKey != null) {
            cacheGroups.remove(groupKey);
        }
    }

    public void removeGroup(String groupKey, Class<?> keyType, Class<?> valueType) {
        removeGroup(groupKey);
    }

    public void clear() {
        cacheGroups.clear();
    }

    public int size() {
        int size = 0;
        for(Map<String, List<?>> map : cacheGroups.values()) {
            synchronized (map) {
                size += map.size();
            }
        }
        return size;
    }

    protected Map<String, List<?>> createIfAbsent(QueryMetadata metadata) {
        return createIfAbsent(cacheName(metadata));
    }

    protected Map<String, List<?>> createIfAbsent(String cacheName) {
        Map<String, List<?>> cache = getCache(cacheName);
        if (cache == null) {
            cache = createCache(cacheName);
        }

        return cache;
    }

    @SuppressWarnings("unchecked")
    protected synchronized Map<String, List<?>> createCache(String cacheName) {
        Map<String, List<?>> map = getCache(cacheName);
        if(map != null) {
            return map;
        }

        map = new ConcurrentLinkedHashMap.Builder<String, List<?>>().maximumWeightedCapacity(maxSize).build();
        cacheGroups.put(cacheName, map);
        return map;
    }

    protected Map<String, List<?>> getCache(String name) {
        return cacheGroups.get(name);
    }

    protected String cacheName(QueryMetadata metadata) {

        String cacheGroup = metadata.getCacheGroup();
        if (cacheGroup != null) {
            return cacheGroup;
        }

        // no explicit cache group
        return DEFAULT_CACHE_NAME;
    }
}
