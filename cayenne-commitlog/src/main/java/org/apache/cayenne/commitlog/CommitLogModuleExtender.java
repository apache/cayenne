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

import org.apache.cayenne.commitlog.meta.CommitLogEntityFactory;
import org.apache.cayenne.configuration.runtime.CoreModuleExtender;

/**
 * @deprecated use {@link CoreModuleExtender} commitlog methods directly via {@link org.apache.cayenne.configuration.runtime.CoreModule#extend(org.apache.cayenne.di.Binder)}.
 * @since 4.0
 */
@Deprecated(since = "5.0")
public class CommitLogModuleExtender {

    private final CoreModuleExtender coreExtender;

    CommitLogModuleExtender(CoreModuleExtender coreExtender) {
        this.coreExtender = coreExtender;
    }

    /**
     * @deprecated use {@link CoreModuleExtender#addCommitLogListener(CommitLogListener)}
     */
    @Deprecated(since = "5.0")
    public CommitLogModuleExtender addListener(CommitLogListener instance) {
        coreExtender.addCommitLogListener(instance);
        return this;
    }

    /**
     * @deprecated use {@link CoreModuleExtender#addCommitLogListener(Class)}
     */
    @Deprecated(since = "5.0")
    public CommitLogModuleExtender addListener(Class<? extends CommitLogListener> type) {
        coreExtender.addCommitLogListener(type);
        return this;
    }

    /**
     * @deprecated use {@link CoreModuleExtender#excludeCommitLogFromTransaction()}
     */
    @Deprecated(since = "5.0")
    public CommitLogModuleExtender excludeFromTransaction() {
        coreExtender.excludeCommitLogFromTransaction();
        return this;
    }

    /**
     * No-op. Commit log listeners are now included in the transaction by default.
     *
     * @deprecated the default behavior is now to include in the transaction
     */
    @Deprecated(since = "5.0")
    public CommitLogModuleExtender includeInTransaction() {
        return this;
    }

    /**
     * @deprecated use {@link CoreModuleExtender#commitLogAnnotationEntitiesOnly()}
     */
    @Deprecated(since = "5.0")
    public CommitLogModuleExtender commitLogAnnotationEntitiesOnly() {
        coreExtender.commitLogAnnotationEntitiesOnly();
        return this;
    }

    /**
     * @deprecated use {@link CoreModuleExtender#commitLogEntityFactory(Class)}
     */
    @Deprecated(since = "5.0")
    public CommitLogModuleExtender entityFactory(Class<? extends CommitLogEntityFactory> entityFactoryType) {
        coreExtender.commitLogEntityFactory(entityFactoryType);
        return this;
    }
}
