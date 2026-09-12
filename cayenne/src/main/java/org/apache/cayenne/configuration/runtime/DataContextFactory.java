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
package org.apache.cayenne.configuration.runtime;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataContextChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataRowStore;
import org.apache.cayenne.access.DataRowStoreFactory;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.cache.NestedQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;

/**
 * @since 3.1
 */
public class DataContextFactory implements ObjectContextFactory {

    @Inject
    protected DataRowStoreFactory dataRowStoreFactory;

    @Inject
    protected ObjectMapRetainStrategy retainStrategy;

    @Inject
    protected RuntimeProperties runtimeProperties;

    @Inject
    protected QueryCache queryCache;

    @Override
    public ObjectContext createContext(DataChannel parent) {

        DataDomain domain = parent.getDataDomain();
        if (domain == null) {
            throw new IllegalArgumentException("Parent DataChannel is not attached to a DataDomain: " + parent);
        }

        // for new dataRowStores use the same name for all stores it makes it easier to track the event subject
        DataRowStore snapshotCache = domain.isSharedCacheEnabled()
                ? domain.getSharedSnapshotCache()
                : dataRowStoreFactory.createDataRowStore(domain.getName());

        return newBuilder(parent)
                .snapshotCache(snapshotCache)
                .usingSharedSnapshotCache(domain.isSharedCacheEnabled())
                .validatingObjectsOnCommit(domain.isValidatingObjectsOnCommit())
                .build();
    }

    @Override
    public ObjectContext createContext(ObjectContext parent) {
        if (!(parent instanceof DataContext dataContext)) {
            throw new IllegalArgumentException("Only a DataContext can be a parent of a nested context. "
                    + "Unsupported context type: " + parent);
        }

        // child ObjectStore should not have direct access to snapshot cache, so do not pass it to the builder
        return newBuilder(new DataContextChannel(dataContext))
                .usingSharedSnapshotCache(dataContext.isUsingSharedSnapshotCache())
                .validatingObjectsOnCommit(dataContext.isValidatingObjectsOnCommit())
                .build();
    }

    protected DataContext.Builder newBuilder(DataChannel parent) {
        boolean sync = runtimeProperties.getBoolean(Constants.CONTEXTS_SYNC_PROPERTY, false);
        return DataContext.builder(parent)
                .objectMap(retainStrategy.createObjectMap())
                .syncWithSnapshotCache(sync)
                .queryCache(new NestedQueryCache(queryCache));
    }
}
