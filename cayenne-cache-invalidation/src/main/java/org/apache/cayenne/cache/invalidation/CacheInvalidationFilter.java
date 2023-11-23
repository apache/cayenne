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

package org.apache.cayenne.cache.invalidation;

import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.DataChannelSyncFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreRemove;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.graph.GraphDiff;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * <p>
 * A {@link DataChannelSyncFilter} that invalidates cache groups. Use custom rules for invalidation provided via DI.
 * Default rule is based on entities' {@link CacheGroups} annotation.
 * </p>
 * <p>
 * To enable the invalidation filter, just include "cayenne-cache-invalidation" module in your project. To add
 * custom invalidation handlers, use CacheInvalidationModule "extender" API:
 * <pre>
 *  CayenneRuntime.builder("cayenne-project.xml")
 *     .addModule(b -> CacheInvalidationModule.extend(b).addHandler(MyHandler.class).build());
 * </pre>
 * </p>
 *
 * @see CacheInvalidationModuleExtender
 * @see InvalidationHandler
 * @since 4.0 enhanced to support custom handlers.
 */
public class CacheInvalidationFilter implements DataChannelSyncFilter {

    private final Provider<QueryCache> cacheProvider;
    private final List<InvalidationHandler> handlers;
    private final Map<Class<? extends Persistent>, Function<Persistent, Collection<CacheGroupDescriptor>>> mappedHandlers;
    private final Function<Persistent, Collection<CacheGroupDescriptor>> skipHandler;
    private final ThreadLocal<Set<CacheGroupDescriptor>> groups;

    public CacheInvalidationFilter(@Inject Provider<QueryCache> cacheProvider, @Inject List<InvalidationHandler> handlers) {
        this.mappedHandlers = new ConcurrentHashMap<>();
        this.skipHandler = p -> Collections.emptyList();
        this.groups = new ThreadLocal<>();
        this.cacheProvider = cacheProvider;
        this.handlers = handlers;
    }

    @Override
    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes,
                            int syncType, DataChannelSyncFilterChain filterChain) {
        try {
            GraphDiff result = filterChain.onSync(originatingContext, changes, syncType);
            // no exceptions, flush...
            Collection<CacheGroupDescriptor> groupSet = groups.get();
            if (groupSet != null && !groupSet.isEmpty()) {
                QueryCache cache = cacheProvider.get();
                for (CacheGroupDescriptor group : groupSet) {
                    if (group.getKeyType() != Void.class) {
                        cache.removeGroup(group.getCacheGroupName(), group.getKeyType(), group.getValueType());
                    } else {
                        cache.removeGroup(group.getCacheGroupName());
                    }
                }
            }
            return result;
        } finally {
            groups.set(null);
        }
    }

    /**
     * A callback method that records cache group to flush at the end of the commit.
     */
    @PrePersist
    @PreRemove
    @PreUpdate
    protected void preCommit(Object object) {
        // TODO: for some reason we can't use Persistent as the argument type... (is it fixed in Cayenne 4.0.M4?)
        Persistent p = (Persistent) object;

        Function<Persistent, Collection<CacheGroupDescriptor>> invalidationFunction = mappedHandlers
                .computeIfAbsent(p.getClass(), cl -> {
                    for (InvalidationHandler handler : handlers) {
                        Function<Persistent, Collection<CacheGroupDescriptor>> function = handler.canHandle(cl);
                        if (function != null) {
                            return function;
                        }
                    }
                    return skipHandler;
                });

        Collection<CacheGroupDescriptor> objectGroups = invalidationFunction.apply(p);
        if (!objectGroups.isEmpty()) {
            getOrCreateTxGroups().addAll(objectGroups);
        }
    }

    protected Set<CacheGroupDescriptor> getOrCreateTxGroups() {
        Set<CacheGroupDescriptor> txGroups = groups.get();
        if (txGroups == null) {
            txGroups = new HashSet<>();
            groups.set(txGroups);
        }

        return txGroups;
    }
}
