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
package org.apache.cayenne.commitlog;

import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.commitlog.meta.AnnotationCommitLogEntityFactory;
import org.apache.cayenne.commitlog.meta.CommitLogEntity;
import org.apache.cayenne.commitlog.meta.CommitLogEntityFactory;
import org.apache.cayenne.commitlog.meta.IncludeAllCommitLogEntityFactory;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.tx.TransactionFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;

/**
 * A builder of a custom extensions module for {@link CommitLogModule} that customizes its services and installs
 * application-specific commit log listeners.
 *
 * @since 4.0
 */
public class CommitLogModuleExtender {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitLogModuleExtender.class);

    private Class<? extends CommitLogEntityFactory> entityFactoryType;
    private Collection<Class<? extends CommitLogListener>> listenerTypes;
    private Collection<CommitLogListener> listenerInstances;
    private boolean excludeFromTransaction;

    CommitLogModuleExtender() {
        entityFactory(IncludeAllCommitLogEntityFactory.class);
        this.listenerTypes = new HashSet<>();
        this.listenerInstances = new HashSet<>();
    }

    public CommitLogModuleExtender addListener(Class<? extends CommitLogListener> type) {
        this.listenerTypes.add(type);
        return this;
    }

    public CommitLogModuleExtender addListener(CommitLogListener instance) {
        this.listenerInstances.add(instance);
        return this;
    }

    /**
     * If called, events will be dispatched outside of the main commit
     * transaction. By default events are dispatched within the transaction, so
     * listeners can commit their code together with the main commit.
     */
    public CommitLogModuleExtender excludeFromTransaction() {
        this.excludeFromTransaction = true;
        return this;
    }

    /**
     * Installs entity filter that would only include entities annotated with
     * {@link CommitLog} on the callbacks. Also {@link CommitLog#confidential()}
     * properties will be obfuscated and {@link CommitLog#ignoredProperties()} -
     * excluded from the change collection.
     */
    public CommitLogModuleExtender commitLogAnnotationEntitiesOnly() {
        return entityFactory(AnnotationCommitLogEntityFactory.class);
    }

    /**
     * Installs a custom factory for {@link CommitLogEntity} objects that
     * allows implementors to use their own annotations, etc.
     */
    public CommitLogModuleExtender entityFactory(Class<? extends CommitLogEntityFactory> entityFactoryType) {
        this.entityFactoryType = entityFactoryType;
        return this;
    }

    /**
     * Creates a DI module that would install {@link CommitLogFilter} and its
     * listeners in Cayenne.
     */
    public Module module() {
        return binder -> {

            if (listenerTypes.isEmpty() && listenerInstances.isEmpty()) {
                LOGGER.info("No listeners configured. Skipping CommitLogFilter registration");
                return;
            }

            binder.bind(CommitLogEntityFactory.class).to(entityFactoryType);

            ListBuilder<CommitLogListener> listeners = CommitLogModule.contributeListeners(binder)
                    .addAll(listenerInstances);

            // types have to be added one-by-one
            for (Class<? extends CommitLogListener> type : listenerTypes) {
                listeners.add(type);
            }

            if (excludeFromTransaction) {
                ServerModule.contributeDomainSyncFilters(binder)
                        .insertBefore(createDiffInitFilter(), TransactionFilter.class)
                        .addAfter(CommitLogFilter.class, TransactionFilter.class);
            } else {
                ServerModule.contributeDomainSyncFilters(binder)
                        .insertBefore(CommitLogFilter.class, TransactionFilter.class);
            }
        };
    }

    /**
     * @return the filter that just initializes incoming Diff
     */
    private static DataChannelSyncFilter createDiffInitFilter() {
        GraphChangeHandler noopHandler = new GraphChangeHandler() {};
        return (originatingContext, changes, syncType, filterChain)
                -> {
            // see ObjectStoreGraphDiff.resolveDiff()
            changes.apply(noopHandler);
            return filterChain.onSync(originatingContext, changes, syncType);
        };
    }
}
