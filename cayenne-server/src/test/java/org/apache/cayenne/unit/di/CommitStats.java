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

package org.apache.cayenne.unit.di;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.DataChannelSyncFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.graph.GraphDiff;
import org.junit.rules.ExternalResource;

import java.util.function.Supplier;

public class CommitStats implements DataChannelSyncFilter {

    private int commitCount;
    private Supplier<DataDomain> dataDomain;

    public CommitStats(Supplier<DataDomain> dataDomain) {
        this.dataDomain = dataDomain;
    }

    public void before() {
        dataDomain.get().addSyncFilter(this);
        commitCount = 0;
    }

    public void after() {
        dataDomain.get().removeSyncFilter(this);
    }

    @Override
    public GraphDiff onSync(
            ObjectContext originatingContext,
            GraphDiff changes,
            int syncType,
            DataChannelSyncFilterChain filterChain) {

        switch (syncType) {
            case DataChannel.FLUSH_CASCADE_SYNC:
                commitCount++;
                break;
        }

        return filterChain.onSync(originatingContext, changes, syncType);
    }

    public int getCommitCount() {
        return commitCount;
    }
}
