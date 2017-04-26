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
package org.apache.cayenne.cache;

import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.query.QueryMetadata;
import org.slf4j.Logger;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.slf4j.LoggerFactory;

/**
 * @deprecated since 4.0 please use JCacheQueryCache (provided by "cayenne-jcache" module)
 */
@Deprecated
public class EhCacheQueryCache implements QueryCache {

    /**
     * Default cache group name.
     */
    private static final String DEFAULT_CACHE_NAME = "cayenne.default.cachegroup";

    private static final Logger logger = LoggerFactory.getLogger(EhCacheQueryCache.class);

    protected CacheManager cacheManager;

    public EhCacheQueryCache() {
        cacheManager = new CacheManager();
        init();
    }

    public EhCacheQueryCache(String configFile) {
        cacheManager = new CacheManager(configFile);
        init();
    }

    public EhCacheQueryCache(CacheManager cacheManager) {
        if (cacheManager != null) {
            this.cacheManager = cacheManager;
            init();
        } else {
            throw new CayenneRuntimeException("CacheManager cannot be null.");
        }
    }

    private void init() {
        cacheManager.addCacheIfAbsent(DEFAULT_CACHE_NAME);
    }

    @SuppressWarnings("rawtypes")
    public List get(QueryMetadata metadata) {
        String key = metadata.getCacheKey();
        if (key == null) {
            return null;
        }

        String cacheName = cacheName(metadata);
        Ehcache cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            return null;
        }

        Element result = cache.get(key);
        return result != null ? (List) result.getObjectValue() : null;
    }

    @SuppressWarnings("rawtypes")
    public List get(QueryMetadata metadata, QueryCacheEntryFactory factory) {

        String key = metadata.getCacheKey();
        if (key == null) {
            // TODO: we should really throw here... The method declares that it
            // never returns null
            return null;
        }

        String cacheName = cacheName(metadata);

        // create empty cache for cache group here, as we have a factory to
        // create an object, and should never ever return null from this
        // method.
        Ehcache cache = cacheManager.addCacheIfAbsent(cacheName);
        Element result = cache.get(key);

        if (result != null) {
            return (List) result.getObjectValue();
        }

        // if no result in cache locking the key to write
        // and putting it to the cache
        cache.acquireWriteLockOnKey(key);
        try {

            // now that we locked the key, let's reread the cache again, in case
            // an object appeared there already
            result = cache.get(key);

            if (result != null) {
                return (List) result.getObjectValue();
            }

            // if not succeeded in reading again putting
            // object to the cache ourselves
            List object = factory.createObject();
            if (object == null) {
                throw new CayenneRuntimeException("Null object created: %s", metadata.getCacheKey());
            }

            cache.put(new Element(key, object));
            return object;

        } finally {
            cache.releaseWriteLockOnKey(key);
        }
    }

    /**
     * @since 4.0
     */
	protected String cacheName(QueryMetadata metadata) {
		if (metadata.getCacheGroup() != null) {
			return metadata.getCacheGroup();
		}

		return DEFAULT_CACHE_NAME;
	}

    @SuppressWarnings("rawtypes")
    public void put(QueryMetadata metadata, List results) {
        String key = metadata.getCacheKey();
        if (key != null) {
            String cacheName = cacheName(metadata);
            Ehcache cache = cacheManager.addCacheIfAbsent(cacheName);
            cache.put(new Element(key, results));
        }
    }

    public void remove(String key) {
        if (key != null) {
            for (String cache : cacheManager.getCacheNames()) {
                cacheManager.getCache(cache).remove(key);
            }
        }
    }

    public void removeGroup(String groupKey) {
        Ehcache cache = cacheManager.getEhcache(groupKey);
        if(cache != null) {
            cache.removeAll();
        }
    }

    public void removeGroup(String groupKey, Class<?> keyType, Class<?> valueType) {
        removeGroup(groupKey);
    }

    public void clear() {
        cacheManager.removalAll();
    }

    public int size() {
        int size = 0;
        for (String cache : cacheManager.getCacheNames()) {
            size += cacheManager.getCache(cache).getSize();
        }
        return size;
    }

    /**
     * Returns default cache group.
     * 
     * @deprecated since 4.0 - this method is no longer in use. If you are
     *             overriding it, override {@link #cacheName(QueryMetadata)}
     *             instead.
     */
    @Deprecated
    public Ehcache getDefaultCache() {
        return cacheManager.getCache(DEFAULT_CACHE_NAME);
    }

    /**
     * Shuts down EhCache CacheManager
     */
    @BeforeScopeEnd
    public void shutdown() {
        cacheManager.shutdown();
    }
}
