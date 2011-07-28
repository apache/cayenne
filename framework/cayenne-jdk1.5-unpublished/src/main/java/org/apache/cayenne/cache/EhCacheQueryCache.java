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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class EhCacheQueryCache implements QueryCache {
    
    /**
     * Default cache group name.
     */
    static final String DEFAULT_CACHE_NAME = "cayenne.default.cachegroup";
    
    private static Log logger = LogFactory.getLog(EhCacheQueryCache.class);
    
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
        }
        else {
            throw new CayenneRuntimeException("CacheManager cannot be null.");
        }
    }
    
    private void init() {
        cacheManager.addCacheIfAbsent(DEFAULT_CACHE_NAME);
    }

    public List get(QueryMetadata metadata) {
        String key = metadata.getCacheKey();
        if (key == null) {
            return null;
        }
        
        Element result = null;
        String[] groupNames = metadata.getCacheGroups();
        if (groupNames != null && groupNames.length > 0) {
            Ehcache cache = cacheManager.getCache(groupNames[0]);
            if (cache == null) {
                return null;
            }
            else {
                result = cache.get(key);
                if (result != null) {
                    return (List)result.getObjectValue();
                }
            }
            if (groupNames.length > 1) {
                logger.warn("multiple cache groups per key: " + key);
            }
        }
        else {
            result = getDefaultCache().get(key);
            if (result != null) {
                return (List)getDefaultCache().get(key).getObjectValue();
            }
        }
        return null;
    }

    public List get(QueryMetadata metadata, QueryCacheEntryFactory factory) {
        String key = metadata.getCacheKey();
        if (key == null) {
            return null;
        }
        
        Ehcache cache = null;
        Element result = null;
        String[] groupNames = metadata.getCacheGroups();
        if (groupNames != null && groupNames.length > 0) {
            cache = cacheManager.getCache(groupNames[0]);
            if (cache == null) {
                return null;
            }
            else {
                result = cache.get(key);
            }
            if (groupNames.length > 1) {
                logger.warn("multiple cache groups per key: " + key);
            }
        }
        else {
            cache = getDefaultCache();
            result = cache.get(key);
        }

        if (result != null) {
            return (List)result.getObjectValue();
        }

        // if no result in cache locking the key to write
        // and putting it to the cache
        cache.acquireWriteLockOnKey(key);
        try {
            
            // trying to read from cache again in case of
            // someone else put it to the cache before us
            List list = get(metadata);
            
            if (list == null) {
                
                // if not succeeded  in reading again putting
                // object to the cache ourselves
                Object noResult = factory.createObject();
                if (!(noResult instanceof List)) {
                    if (noResult == null) {
                        throw new CayenneRuntimeException("Null object created: "
                                + metadata.getCacheKey());
                    }
                    else {
                        throw new CayenneRuntimeException(
                                "Invalid query result, expected List, got "
                                + noResult.getClass().getName());
                    }
                }

                list = (List)noResult;
                put(metadata, list);
                return list;
            }
            else {
                return list;
            }
        }
        finally {
            cache.releaseWriteLockOnKey(key);
        }
    }

    public void put(QueryMetadata metadata, List results) {
        String key = metadata.getCacheKey();
        if (key != null) {
            String[] groupNames = metadata.getCacheGroups();
            if (groupNames != null && groupNames.length > 0) {
                Ehcache cache = cacheManager.addCacheIfAbsent(groupNames[0]);
                cache.put(new Element(key, results));
                if (groupNames.length > 1) {
                    logger.warn("multiple groups per key: " + key);
                }
            }
            else {
                getDefaultCache().put(new Element(key,results));
            }
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
        cacheManager.removeCache(groupKey);
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
     */
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
