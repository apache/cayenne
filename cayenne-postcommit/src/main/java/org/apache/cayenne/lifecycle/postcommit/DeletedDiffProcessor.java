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

import java.util.List;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.lifecycle.changemap.MutableChangeMap;
import org.apache.cayenne.lifecycle.changemap.MutableObjectChange;
import org.apache.cayenne.lifecycle.changemap.ObjectChangeType;
import org.apache.cayenne.lifecycle.postcommit.meta.PostCommitEntity;
import org.apache.cayenne.lifecycle.postcommit.meta.PostCommitEntityFactory;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DeletedDiffProcessor implements GraphChangeHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeletedDiffProcessor.class);

	private PostCommitEntityFactory entityFactory;
	private MutableChangeMap changeSet;
	private DataChannel channel;

	DeletedDiffProcessor(MutableChangeMap changeSet, DataChannel channel, PostCommitEntityFactory entityFactory) {
		this.changeSet = changeSet;
		this.channel = channel;
		this.entityFactory = entityFactory;
	}

	@Override
	public void nodeRemoved(Object nodeId) {
		ObjectId id = (ObjectId) nodeId;

		final MutableObjectChange objectChangeSet = changeSet.getOrCreate(id, ObjectChangeType.DELETE);

		// TODO: rewrite with SelectById query after Cayenne upgrade
		ObjectIdQuery query = new ObjectIdQuery(id, true, ObjectIdQuery.CACHE);
		QueryResponse result = channel.onQuery(null, query);

		@SuppressWarnings("unchecked")
		List<DataRow> rows = result.firstList();

		if (rows.isEmpty()) {
			LOGGER.warn("No DB snapshot for object to be deleted, no changes will be recorded. ID: " + id);
			return;
		}

		final DataRow row = rows.get(0);

		ClassDescriptor descriptor = channel.getEntityResolver().getClassDescriptor(id.getEntityName());
		final PostCommitEntity entity = entityFactory.getEntity(id);

		descriptor.visitProperties(new PropertyVisitor() {

			@Override
			public boolean visitAttribute(AttributeProperty property) {

				if (!entity.isIncluded(property.getName())) {
					return true;
				}

				Object value;
				if (entity.isConfidential(property.getName())) {
					value = Confidential.getInstance();
				} else {
					String key = property.getAttribute().getDbAttributeName();
					value = row.get(key);
				}

				if (value != null) {
					objectChangeSet.attributeChanged(property.getName(), value, null);
				}
				return true;
			}

			@Override
			public boolean visitToOne(ToOneProperty property) {
				// TODO record FK changes?
				return true;
			}

			@Override
			public boolean visitToMany(ToManyProperty property) {
				return true;
			}

		});
	}

	@Override
	public void nodeIdChanged(Object nodeId, Object newId) {
		// do nothing
	}

	@Override
	public void nodeCreated(Object nodeId) {
		// do nothing
	}

	@Override
	public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
		// do nothing
	}

	@Override
	public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
		// do nothing
	}

	@Override
	public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
		// do nothing
	}
}
