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

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.commitlog.meta.CommitLogEntity;
import org.apache.cayenne.commitlog.meta.CommitLogEntityFactory;
import org.apache.cayenne.commitlog.model.MutableChangeMap;
import org.apache.cayenne.commitlog.model.MutableObjectChange;
import org.apache.cayenne.commitlog.model.ObjectChangeType;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.reflect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class DeletedDiffProcessor implements GraphChangeHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeletedDiffProcessor.class);

	private CommitLogEntityFactory entityFactory;
	private MutableChangeMap changeSet;
	private DataChannel channel;

	DeletedDiffProcessor(MutableChangeMap changeSet, DataChannel channel, CommitLogEntityFactory entityFactory) {
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
		final CommitLogEntity entity = entityFactory.getEntity(id);

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
                if (!entity.isIncluded(property.getName())) {
                    return true;
                }

                // TODO: is there such a thing as "confidential" relationship that we need to hide?

                DbRelationship dbRelationship = property.getRelationship().getDbRelationships().get(0);

                ObjectId value = row.createTargetObjectId(
                        property.getTargetDescriptor().getEntity().getName(),
                        dbRelationship);

                if (value != null) {
                    objectChangeSet.toOneRelationshipDisconnected(property.getName(), value);
                }
				return true;
			}

			@Override
			public boolean visitToMany(ToManyProperty property) {
				return true;
			}

		});
	}
}
