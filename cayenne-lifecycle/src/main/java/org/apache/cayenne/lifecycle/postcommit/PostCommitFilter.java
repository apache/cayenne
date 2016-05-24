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
package org.apache.cayenne.lifecycle.postcommit;

import java.util.Collection;
import java.util.List;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.lifecycle.changemap.ChangeMap;
import org.apache.cayenne.lifecycle.changemap.MutableChangeMap;
import org.apache.cayenne.lifecycle.postcommit.meta.PostCommitEntityFactory;
import org.apache.cayenne.query.Query;

/**
 * A {@link DataChannelFilter} that captures commit changes, delegating their
 * processing to an underlying collection of listeners.
 * 
 * @since 4.0
 */
public class PostCommitFilter implements DataChannelFilter {

	static final String POST_COMMIT_LISTENERS_LIST = "cayenne.server.post_commit.listeners";

	private PostCommitEntityFactory entityFactory;
	private Collection<PostCommitListener> listeners;

	public PostCommitFilter(@Inject PostCommitEntityFactory entityFactory,
			@Inject(POST_COMMIT_LISTENERS_LIST) List<PostCommitListener> listeners) {
		this.entityFactory = entityFactory;
		this.listeners = listeners;
	}

	@Override
	public void init(DataChannel channel) {
		// do nothing...
	}

	@Override
	public QueryResponse onQuery(ObjectContext originatingContext, Query query, DataChannelFilterChain filterChain) {
		return filterChain.onQuery(originatingContext, query);
	}

	@Override
	public GraphDiff onSync(ObjectContext originatingContext, GraphDiff beforeDiff, int syncType,
			DataChannelFilterChain filterChain) {

		// process commits only; skip rollback
		if (syncType != DataChannel.FLUSH_CASCADE_SYNC && syncType != DataChannel.FLUSH_NOCASCADE_SYNC) {
			return filterChain.onSync(originatingContext, beforeDiff, syncType);
		}

		// don't collect changes if there are no listeners
		if (listeners.isEmpty()) {
			return filterChain.onSync(originatingContext, beforeDiff, syncType);
		}

		MutableChangeMap changes = new MutableChangeMap();

		// passing DataDomain, not ObjectContext to speed things up
		// and avoid capturing changed state when fetching snapshots
		DataChannel channel = originatingContext.getChannel();

		beforeCommit(changes, channel, beforeDiff);
		GraphDiff afterDiff = filterChain.onSync(originatingContext, beforeDiff, syncType);
		afterCommit(changes, channel, beforeDiff, afterDiff);
		notifyListeners(originatingContext, changes);

		return afterDiff;
	}

	private void beforeCommit(MutableChangeMap changes, DataChannel channel, GraphDiff contextDiff) {

		// capture snapshots of deleted objects before they are purged from
		// cache

		GraphChangeHandler handler = new DiffFilter(entityFactory,
				new DeletedDiffProcessor(changes, channel, entityFactory));
		contextDiff.apply(handler);
	}

	private void afterCommit(MutableChangeMap changes, DataChannel channel, GraphDiff contextDiff, GraphDiff dbDiff) {

		GraphChangeHandler handler = new DiffFilter(entityFactory,
				new DiffProcessor(changes, channel.getEntityResolver()));
		contextDiff.apply(handler);
		dbDiff.apply(handler);
	}

	private void notifyListeners(ObjectContext originatingContext, ChangeMap changes) {
		for (PostCommitListener l : listeners) {
			l.onPostCommit(originatingContext, changes);
		}
	}

}
