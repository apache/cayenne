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
package org.apache.cayenne.unit.di.runtime;

import org.apache.cayenne.access.UnitTestDomain;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.DataChannelSyncStats;
import org.apache.cayenne.unit.di.UnitTestClosure;

public class RuntimeCaseDataChannelInterceptor implements DataChannelInterceptor {

    @Inject
    // injecting provider to make this provider independent from scoping of CayenneRuntime
    protected Provider<CayenneRuntime> runtimeProvider;

    public void runWithQueriesBlocked(UnitTestClosure closure) {

        UnitTestDomain channel = (UnitTestDomain) runtimeProvider
                .get()
                .getChannel();

        channel.setBlockingQueries(true);
        try {
            closure.execute();
        }
        finally {
            channel.setBlockingQueries(false);
        }
    }

    public int runWithQueryCounter(UnitTestClosure closure) {
        UnitTestDomain channel = (UnitTestDomain) runtimeProvider.get().getChannel();
        RuntimeCaseDataNode node = (RuntimeCaseDataNode)channel.getDataNodes().iterator().next();

        int start = node.getQueriesCount();
        int end;
        try {
            closure.execute();
        } finally {
            end = node.getQueriesCount();
        }
        return end - start;
    }

    public DataChannelSyncStats runWithSyncStatsCollection(UnitTestClosure closure) {
        throw new UnsupportedOperationException("TODO... so far unused");
    }
}
