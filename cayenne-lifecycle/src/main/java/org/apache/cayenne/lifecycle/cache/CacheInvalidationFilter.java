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

package org.apache.cayenne.lifecycle.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreRemove;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;

/**
 * <p>
 * A {@link DataChannelFilter} that invalidates cache groups.
 * Use custom rules for invalidation provided via DI.
 * </p>
 * <p>
 * Default rule is based on entities' {@link CacheGroups} annotation.
 * </p>
 * <p>
 *     To add default filter: <pre>
 *         ServerRuntime.builder("cayenne-project.xml")
 *              .addModule(CacheInvalidationModuleBuilder.builder().build());
 *     </pre>
 * </p>
 *
 * @since 3.1
 * @see InvalidationHandler
 * @see CacheInvalidationModuleBuilder
 */
public class CacheInvalidationFilter implements DataChannelFilter {

    @Inject
    private Provider<QueryCache> cacheProvider;

    @Inject(CacheInvalidationModuleBuilder.INVALIDATION_HANDLERS_LIST)
    private List<InvalidationHandler> handlers;

    private final Map<Class<? extends Persistent>, InvalidationFunction> mappedHandlers;

    private final InvalidationFunction skipHandler;

    private final ThreadLocal<Set<String>> groups;

    public CacheInvalidationFilter() {
        mappedHandlers = new ConcurrentHashMap<>();
        skipHandler = new InvalidationFunction() {
            @Override
            public Collection<String> apply(Persistent p) {
                return Collections.emptyList();
            }
        };
        groups = new ThreadLocal<>();
    }

    public void init(DataChannel channel) {
        // noop
    }

    public QueryResponse onQuery(ObjectContext originatingContext, Query query, DataChannelFilterChain filterChain) {
        return filterChain.onQuery(originatingContext, query);
    }

    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes,
                            int syncType, DataChannelFilterChain filterChain) {
        try {
            GraphDiff result = filterChain.onSync(originatingContext, changes, syncType);
            // no exceptions, flush...
            Collection<String> groupSet = groups.get();
            if (groupSet != null && !groupSet.isEmpty()) {
                QueryCache cache = cacheProvider.get();
                for (String group : groupSet) {
                    cache.removeGroup(group);
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

        InvalidationFunction invalidationFunction = mappedHandlers.get(p.getClass());
        if(invalidationFunction == null) {
            invalidationFunction = skipHandler;
            for (InvalidationHandler handler : handlers) {
                InvalidationFunction function = handler.canHandle(p.getClass());
                if (function != null) {
                    invalidationFunction = function;
                    break;
                }
            }
            mappedHandlers.put(p.getClass(), invalidationFunction);
        }

        Collection<String> objectGroups = invalidationFunction.apply(p);
        if (!objectGroups.isEmpty()) {
            getOrCreateTxGroups().addAll(objectGroups);
        }
    }


    protected Set<String> getOrCreateTxGroups() {
        Set<String> txGroups = groups.get();
        if (txGroups == null) {
            txGroups = new HashSet<>();
            groups.set(txGroups);
        }

        return txGroups;
    }
}
