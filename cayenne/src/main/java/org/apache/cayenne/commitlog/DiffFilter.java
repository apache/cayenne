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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.commitlog.meta.CommitLogEntity;
import org.apache.cayenne.commitlog.meta.CommitLogEntityFactory;

/**
 * Filters changes passing only auditable object changes to the underlying
 * delegate.
 */
class DiffFilter implements GraphChangeHandler {

	private CommitLogEntityFactory entityFactory;
	private GraphChangeHandler delegate;

	DiffFilter(CommitLogEntityFactory entityFactory, GraphChangeHandler delegate) {
		this.entityFactory = entityFactory;
		this.delegate = delegate;
	}

	@Override
	public void nodeIdChanged(ObjectId id, ObjectId newId) {
		if (entityFactory.getEntity(id).isIncluded()) {
			delegate.nodeIdChanged(id, newId);
		}
	}

	@Override
	public void nodeCreated(ObjectId id) {
		if (entityFactory.getEntity(id).isIncluded()) {
			delegate.nodeCreated(id);
		}
	}

	@Override
	public void nodeRemoved(ObjectId id) {
		if (entityFactory.getEntity(id).isIncluded()) {
			delegate.nodeRemoved(id);
		}
	}

	@Override
	public void nodePropertyChanged(ObjectId id, String property, Object oldValue, Object newValue) {
		CommitLogEntity entity = entityFactory.getEntity(id);
		if (entity.isIncluded(property)) {

			if (entity.isConfidential(property)) {
				oldValue = Confidential.getInstance();
				newValue = Confidential.getInstance();
			}

			delegate.nodePropertyChanged(id, property, oldValue, newValue);
		}
	}

	@Override
	public void arcCreated(ObjectId id, ObjectId targetId, ArcId arcId) {
		if (entityFactory.getEntity(id).isIncluded(arcId.toString())) {
			delegate.arcCreated(id, targetId, arcId);
		}
	}

	@Override
	public void arcDeleted(ObjectId id, ObjectId targetId, ArcId arcId) {
		if (entityFactory.getEntity(id).isIncluded(arcId.toString())) {
			delegate.arcDeleted(id, targetId, arcId);
		}
	}
}
