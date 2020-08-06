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

import org.apache.cayenne.query.QueryMetadata;

import java.util.List;

/**
 * Defines API of a cache that stores query results.
 *
 * @since 3.0
 */
public interface QueryCache {

    /**
     * Returns a cached query result for the given QueryMetadata or null if the result is
     * not cached or is expired.
     */
    @SuppressWarnings("rawtypes")
    List get(QueryMetadata metadata);

    /**
     * Returns a cached query result for the given QueryMetadata. If the result is not
     * cached or is expired, cache will use provided factory to rebuild the value and
     * store it in the cache. A corollary is that this method never returns null.
     * <p>
     * Compared to {@link #get(QueryMetadata)}, this method allows the cache to do
     * appropriate synchronization when refreshing the entry, preventing multiple threads
     * from running the same query when a missing entry is requested by multiple threads
     * simultaneously.
     */
    @SuppressWarnings("rawtypes")
    List get(QueryMetadata metadata, QueryCacheEntryFactory factory);

    @SuppressWarnings("rawtypes")
    void put(QueryMetadata metadata, List results);

    /**
     * Removes a single entry from cache.
     */
    void remove(String key);

    /**
     * Removes a group of entries identified by group key. Note that depending on
     * implementation this method may either actively remove the entries belonging to the
     * group or just mark them as expired, so that they are refreshed on the next access.
     * In the former case the cache size would shrink, but in the later the cache size
     * will not change after calling this method.
     */
    void removeGroup(String groupKey);

    /**
     * Removes a group of entries identified by group key.
     * Can be used if cache provider supports strictly typed caches.
     * @see QueryCache#removeGroup(String)
     *
     * @since 4.0
     */
    void removeGroup(String groupKey, Class<?> keyType, Class<?> valueType);

    /**
     * Clears all cache entries.
     *
     * @deprecated since 4.0. Please use implementation specific methods to perform cache management tasks.
     */
    @Deprecated
    void clear();
}
