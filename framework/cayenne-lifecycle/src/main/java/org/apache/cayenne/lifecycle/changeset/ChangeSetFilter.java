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
package org.apache.cayenne.lifecycle.changeset;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;

/**
 * A {@link DataChannelFilter} that provides interested parties with a thread-local access
 * to the current commit changeset. It will only return a non-null change set when commit
 * within the scope of the filter
 * {@link #onSync(ObjectContext, GraphDiff, int, DataChannelFilterChain)} method. The
 * filter is intended to be used by pre-commit and post-commit listeners.
 * 
 * @since 3.1
 */
public class ChangeSetFilter implements DataChannelFilter {

    private static final ThreadLocal<ChangeSet> PRE_COMMIT_CHANGE_SET = new ThreadLocal<ChangeSet>();

    public static ChangeSet preCommitChangeSet() {
        return PRE_COMMIT_CHANGE_SET.get();
    }

    public void init(DataChannel channel) {
        // noop..
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
            PRE_COMMIT_CHANGE_SET.set(new GenericChangeSet(changes));
            return filterChain.onSync(originatingContext, changes, syncType);
        }
        finally {
            PRE_COMMIT_CHANGE_SET.set(null);
        }
    }

}
