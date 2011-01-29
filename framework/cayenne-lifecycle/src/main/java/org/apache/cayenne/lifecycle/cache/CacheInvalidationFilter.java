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
import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreRemove;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;

/**
 * A {@link DataChannelFilter} that invalidates cache groups defined for mapped entities
 * via {@link CacheGroups} annotations.
 * 
 * @since 3.1
 */
public class CacheInvalidationFilter implements DataChannelFilter {

    private final ThreadLocal<Set<String>> groups = new ThreadLocal<Set<String>>();

    public void init(DataChannel channel) {
        // noop
    }

    public QueryResponse onQuery(
            ObjectContext originatingContext,
            Query query,
            DataChannelFilterChain filterChain) {
        return filterChain.onQuery(originatingContext, query);
    }

    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType,
            DataChannelFilterChain filterChain) {

        try {
            GraphDiff result = filterChain.onSync(originatingContext, changes, syncType);

            // no exceptions, flush...

            Collection<String> groupSet = groups.get();
            if (groupSet != null && !groupSet.isEmpty()) {

                // TODO: replace this with QueryCache injection once CAY-1445 is done
                QueryCache cache = ((DataContext) originatingContext).getQueryCache();

                for (String group : groupSet) {
                    cache.removeGroup(group);
                }
            }

            return result;
        }
        finally {
            groups.set(null);
        }
    }

    /**
     * A callback method that records cache group to flush at the end of the commit.
     */
    @PrePersist(entityAnnotations = CacheGroups.class)
    @PreRemove(entityAnnotations = CacheGroups.class)
    @PreUpdate(entityAnnotations = CacheGroups.class)
    protected void preCommit(Object object) {

        Set<String> groupSet = groups.get();
        if (groupSet == null) {
            groupSet = new HashSet<String>();
            groups.set(groupSet);
        }

        addCacheGroups(groupSet, object);
    }

    /**
     * A method that builds a list of cache groups for a given object and adds them to the
     * invalidation group set. This implementation adds all groups defined via
     * {@link CacheGroups} annotation for a given class. Subclasses may override this
     * method to provide more fine-grained filtering of cache groups to invalidate, based
     * on the state of the object.
     */
    protected void addCacheGroups(Set<String> groupSet, Object object) {
        CacheGroups a = object.getClass().getAnnotation(CacheGroups.class);
        for (String group : a.value()) {
            groupSet.add(group);
        }
    }
}
