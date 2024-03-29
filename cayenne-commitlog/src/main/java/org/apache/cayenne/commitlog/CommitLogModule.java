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

import org.apache.cayenne.commitlog.meta.IncludeAllCommitLogEntityFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;

/**
 * Auto-loadable module that enables gathering of commit log information for Cayenne stack. To add custom listeners to
 * receive commit log events, implement {@link CommitLogListener} and register it using {@link CommitLogModule#extend(Binder)}
 *
 * @since 4.0
 */
public class CommitLogModule implements Module {

    /**
     * @deprecated use {@link #extend(Binder)} instead
     */
    @Deprecated(since = "5.0")
    static ListBuilder<CommitLogListener> contributeListeners(Binder binder) {
        return binder.bindList(CommitLogListener.class);
    }

    /**
     * Starts an extension module builder to add listeners and/or other customizations for {@link CommitLogModule}.
     *
     * @return a new builder of {@link CommitLogModule} extensions.
     * @since 5.0
     */
    public static CommitLogModuleExtender extend(Binder binder) {
        return new CommitLogModuleExtender(binder);
    }

    @Override
    public void configure(Binder binder) {

        extend(binder)
                .initAllExtensions()
                .entityFactory(IncludeAllCommitLogEntityFactory.class)
                .includeInTransaction();

        binder.bind(CommitLogFilter.class).to(CommitLogFilter.class);
    }
}
