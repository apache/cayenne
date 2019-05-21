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
import org.apache.cayenne.query.QueryMetadataProxy;

import java.util.List;

/**
 * A {@link QueryCache} wrapper that introduces a key namespace on top of a
 * delegate shared cache. This way multiple cache users can share the same
 * underlying cache without a possibility of key conflicts, yet refresh the
 * cache groups in a coordinated fashion.
 * 
 * @since 3.0
 */
public class NestedQueryCache implements QueryCache {

    // the idea is to be something short (to speed up comparisons), but clear
    // and unlikely to create a conflict with application cache keys...
    // fully-qualified class name that we used before was a bit too long
    private static final String NAMESPACE_PREXIX = "#nested-";
    private static volatile int currentId;

    protected QueryCache delegate;
    protected String namespace;

    private static final int nextInt() {
        if (currentId == Integer.MAX_VALUE) {
            currentId = 0;
        }

        return currentId++;
    }

    public NestedQueryCache(QueryCache delegate) {
        this.delegate = delegate;
        this.namespace = NAMESPACE_PREXIX + nextInt() + ":";
    }

    /**
     * Returns the actual implementation of the query cache that is wrapped by
     * this NestedQueryCache.
     */
    public QueryCache getDelegate() {
        return delegate;
    }

    /**
     * Clears the underlying shared cache.
     * @see QueryCache#clear()
     * @deprecated since 4.0
     */
    @Override
    @Deprecated
    public void clear() {
        // seems pretty evil - it clears the keys that do not belong to our subset of the cache
        delegate.clear();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List get(QueryMetadata metadata, QueryCacheEntryFactory factory) {
        return delegate.get(qualifiedMetadata(metadata), factory);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List get(QueryMetadata metadata) {
        return delegate.get(qualifiedMetadata(metadata));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void put(QueryMetadata metadata, List results) {
        delegate.put(qualifiedMetadata(metadata), results);
    }

    /**
     * Removes an entry for key in the current namespace.
     */
    @Override
    public void remove(String key) {
        delegate.remove(qualifiedKey(key));
    }

    /**
     * Invalidates a shared cache group.
     */
    @Override
    public void removeGroup(String groupKey) {
        delegate.removeGroup(groupKey);
    }

    @Override
    public void removeGroup(String groupKey, Class<?> keyType, Class<?> valueType) {
        delegate.removeGroup(groupKey, keyType, valueType);
    }

    private String qualifiedKey(String key) {
        return key != null ? namespace + key : null;
    }

    private QueryMetadata qualifiedMetadata(QueryMetadata md) {
        return new QueryMetadataProxy(md) {
            @Override
            public String getCacheKey() {
                return qualifiedKey(mdDelegate.getCacheKey());
            }
        };
    }
}
