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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ObjectSnapshots;
import org.apache.cayenne.commitlog.meta.CommitLogEntity;
import org.apache.cayenne.commitlog.meta.CommitLogEntityFactory;
import org.apache.cayenne.commitlog.model.MutableChangeMap;
import org.apache.cayenne.commitlog.model.MutableObjectChange;
import org.apache.cayenne.commitlog.model.ObjectChangeType;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



class DeletedDiffProcessor implements GraphChangeHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeletedDiffProcessor.class);

	private CommitLogEntityFactory entityFactory;
	private MutableChangeMap changeSet;
	private ObjectContext context;

	DeletedDiffProcessor(MutableChangeMap changeSet, ObjectContext context, CommitLogEntityFactory entityFactory) {
		this.changeSet = changeSet;
		this.context = context;
		this.entityFactory = entityFactory;
	}

	@Override
	public void nodeRemoved(ObjectId id) {

		final MutableObjectChange objectChangeSet = changeSet.getOrCreate(id, ObjectChangeType.DELETE);

		// the committed state is in the snapshot cache (or the DB), never in the deleted object itself
		DataRow row = context.getObjectStore().getSnapshot(id);
		if (row == null) {
			LOGGER.warn("No DB snapshot for object to be deleted, no changes will be recorded. ID: {}", id);
			return;
		}

		ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(id.getEntityName());
		final CommitLogEntity entity = entityFactory.getEntity(id);

		for (PropertyDescriptor property : descriptor.getProperties()) {
			if (!entity.isIncluded(property.getName())) {
				continue;
			}

			switch (property) {
				case AttributeProperty attribute -> {
					Object value;
					if (entity.isConfidential(attribute.getName())) {
						value = Confidential.getInstance();
					} else {
						String key = attribute.getAttribute().getDbAttributeName();
						value = row.get(key);
					}

					if (value != null) {
						objectChangeSet.attributeChanged(attribute.getName(), value, null);
					}
				}
				case ToOneProperty toOne -> {
					// TODO: is there such a thing as "confidential" relationship that we need to hide?
					// a flattened to-one or an FK to a non-PK column can't be resolved to a target id from the row
					ObjRelationship relationship = toOne.getRelationship();
					if (relationship.isToPrimaryKey()) {
						ObjectId value = ObjectSnapshots.targetId(relationship, row);
						if (value != null) {
							objectChangeSet.toOneRelationshipDisconnected(toOne.getName(), value);
						}
					}
				}
				default -> {
				}
			}
		}
	}
}
