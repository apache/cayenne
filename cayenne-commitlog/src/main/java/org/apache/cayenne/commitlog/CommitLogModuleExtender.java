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
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.graph.GraphChangeHandler;

/**
 * A builder of a custom extensions module for {@link CommitLogModule} that customizes its services and installs
 * application-specific commit log listeners.
 *
 * @since 4.0
 */
public class CommitLogModuleExtender {

    private final Binder binder;
    private ListBuilder<CommitLogListener> commitLogListeners;

    protected CommitLogModuleExtender(Binder binder) {
        this.binder = binder;
    }

    protected CommitLogModuleExtender initAllExtensions() {
        contributeCommitLogListeners();
        return this;
    }

    public CommitLogModuleExtender addListener(Class<? extends CommitLogListener> type) {
        contributeCommitLogListeners().add(type);
        return this;
    }

    public CommitLogModuleExtender addListener(CommitLogListener instance) {
        contributeCommitLogListeners().add(instance);
        return this;
    }

    /**
     * If called, events will be dispatched outside the main commit transaction. By default, events are dispatched
     * within the transaction, so listeners can commit their code together with the main commit.
     */
    public CommitLogModuleExtender excludeFromTransaction() {
        CoreModule.extend(binder).addSyncFilter(createDiffInitFilter(), true);
        return registerFilter(false);
    }

    /**
     * @since 5.0
     */
    public CommitLogModuleExtender includeInTransaction() {
        return registerFilter(true);
    }

    protected CommitLogModuleExtender registerFilter(boolean inTx) {
        CoreModule.extend(binder).addSyncFilter(CommitLogFilter.class, inTx);
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
     * Installs a custom factory for {@link CommitLogEntity} objects that allows implementors to use their own
     * annotations, etc.
     */
    public CommitLogModuleExtender entityFactory(Class<? extends CommitLogEntityFactory> entityFactoryType) {
        binder.bind(CommitLogEntityFactory.class).to(entityFactoryType);
        return this;
    }

    private ListBuilder<CommitLogListener> contributeCommitLogListeners() {
        if (commitLogListeners == null) {
            commitLogListeners = binder.bindList(CommitLogListener.class);
        }
        return commitLogListeners;
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
