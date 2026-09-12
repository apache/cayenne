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
import org.apache.cayenne.commitlog.model.MutableChangeMap;
import org.apache.cayenne.commitlog.model.MutableObjectChange;
import org.apache.cayenne.commitlog.model.ObjectChangeType;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Records changes in a given transaction to a {@link MutableChangeMap} object.
 * 
 * @since 4.0
 */
class DiffProcessor implements GraphChangeHandler {

	private EntityResolver entityResolver;
	private MutableChangeMap changeSet;

	DiffProcessor(MutableChangeMap changeSet, EntityResolver entityResolver) {
		this.changeSet = changeSet;
		this.entityResolver = entityResolver;
	}

	@Override
	public void nodeRemoved(ObjectId id) {
		// do nothing... deletes are processed pre-commit
	}

	@Override
	public void nodePropertyChanged(ObjectId id, String property, Object oldValue, Object newValue) {
		changeSet.getOrCreate(id, ObjectChangeType.UPDATE).attributeChanged(property, oldValue,
				newValue);
	}

	@Override
	public void nodeIdChanged(ObjectId id, ObjectId newId) {
		changeSet.aliasId(id, newId);
	}

	@Override
	public void nodeCreated(ObjectId id) {
		changeSet.getOrCreate(id, ObjectChangeType.INSERT);
	}

	@Override
	public void arcDeleted(ObjectId id, ObjectId targetId, ArcId arcId) {
		String relationshipName = arcId.toString();

		ObjEntity entity = entityResolver.getObjEntity(id.getEntityName());
		ObjRelationship relationship = entity.getRelationship(relationshipName);

		MutableObjectChange c = changeSet.getOrCreate(id, ObjectChangeType.UPDATE);

		if (relationship.isToMany()) {
			c.toManyRelationshipDisconnected(relationshipName, targetId);
		} else {
			c.toOneRelationshipDisconnected(relationshipName, targetId);
		}
	}

	@Override
	public void arcCreated(ObjectId id, ObjectId targetId, ArcId arcId) {

		String relationshipName = arcId.toString();

		ObjEntity entity = entityResolver.getObjEntity(id.getEntityName());
		ObjRelationship relationship = entity.getRelationship(relationshipName);

		MutableObjectChange c = changeSet.getOrCreate(id, ObjectChangeType.UPDATE);

		if (relationship.isToMany()) {
			c.toManyRelationshipConnected(relationshipName, targetId);
		} else {
			c.toOneRelationshipConnected(relationshipName, targetId);
		}
	}
}
