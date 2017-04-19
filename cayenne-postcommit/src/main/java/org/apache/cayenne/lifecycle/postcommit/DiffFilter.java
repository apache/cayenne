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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.lifecycle.postcommit.meta.PostCommitEntity;
import org.apache.cayenne.lifecycle.postcommit.meta.PostCommitEntityFactory;

/**
 * Filters changes passing only auditable object changes to the underlying
 * delegate.
 */
class DiffFilter implements GraphChangeHandler {

	private PostCommitEntityFactory entityFactory;
	private GraphChangeHandler delegate;

	DiffFilter(PostCommitEntityFactory entityFactory, GraphChangeHandler delegate) {
		this.entityFactory = entityFactory;
		this.delegate = delegate;
	}

	@Override
	public void nodeIdChanged(Object nodeId, Object newId) {
		if (entityFactory.getEntity((ObjectId) nodeId).isIncluded()) {
			delegate.nodeIdChanged(nodeId, newId);
		}
	}

	@Override
	public void nodeCreated(Object nodeId) {
		if (entityFactory.getEntity((ObjectId) nodeId).isIncluded()) {
			delegate.nodeCreated(nodeId);
		}
	}

	@Override
	public void nodeRemoved(Object nodeId) {
		if (entityFactory.getEntity((ObjectId) nodeId).isIncluded()) {
			delegate.nodeRemoved(nodeId);
		}
	}

	@Override
	public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
		PostCommitEntity entity = entityFactory.getEntity((ObjectId) nodeId);
		if (entity.isIncluded(property)) {

			if (entity.isConfidential(property)) {
				oldValue = Confidential.getInstance();
				newValue = Confidential.getInstance();
			}

			delegate.nodePropertyChanged(nodeId, property, oldValue, newValue);
		}
	}

	@Override
	public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
		if (entityFactory.getEntity((ObjectId) nodeId).isIncluded(arcId.toString())) {
			delegate.arcCreated(nodeId, targetNodeId, arcId);
		}
	}

	@Override
	public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
		if (entityFactory.getEntity((ObjectId) nodeId).isIncluded(arcId.toString())) {
			delegate.arcDeleted(nodeId, targetNodeId, arcId);
		}
	}
}
