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
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.cache.NestedQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.configuration.ObjectStoreFactory;
import org.apache.cayenne.di.Inject;

/**
 * @since 3.1
 */
public class DataContextFactory implements ObjectContextFactory {

    @Inject
    protected DataRowStoreFactory dataRowStoreFactory;

    @Inject
    protected ObjectStoreFactory objectStoreFactory;

    @Inject
    protected QueryCache queryCache;

    @Override
    public ObjectContext createContext(DataChannel parent) {

        DataChannel c = parent;
        while (c != null && !(c instanceof DataDomain)) {
            c = c.getParent();
        }

        if (c == null) {
            throw new IllegalArgumentException("Parent DataChannel is not attached to a DataDomain: " + parent);
        }

        DataDomain domain = (DataDomain) c;

        // for new dataRowStores use the same name for all stores it makes it easier to track the event subject
        DataRowStore snapshotCache = domain.isSharedCacheEnabled()
                ? domain.getSharedSnapshotCache()
                : dataRowStoreFactory.createDataRowStore(domain.getName());

        DataContext context = newInstance(parent, objectStoreFactory.createObjectStore(snapshotCache));
        context.setValidatingObjectsOnCommit(domain.isValidatingObjectsOnCommit());
        context.setQueryCache(new NestedQueryCache(queryCache));
        return context;
    }

    @Override
    public ObjectContext createContext(ObjectContext parent) {
        if (!(parent instanceof DataContext dataContext)) {
            throw new IllegalArgumentException("Only a DataContext can be a parent of a nested context. "
                    + "Unsupported context type: " + parent);
        }

        // child ObjectStore should not have direct access to snapshot cache, so do not
        // pass it in constructor.
        ObjectStore objectStore = objectStoreFactory.createObjectStore(null);

        DataContext context = newInstance(new DataContextChannel(dataContext), objectStore);

        context.setValidatingObjectsOnCommit(dataContext.isValidatingObjectsOnCommit());
        context.setUsingSharedSnapshotCache(dataContext.isUsingSharedSnapshotCache());
        context.setQueryCache(new NestedQueryCache(queryCache));

        return context;
    }

    protected DataContext newInstance(DataChannel parent, ObjectStore objectStore) {
        return new DataContext(parent, objectStore);
    }
}
