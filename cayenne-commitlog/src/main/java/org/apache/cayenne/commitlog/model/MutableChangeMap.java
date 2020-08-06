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
package org.apache.cayenne.commitlog.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.cayenne.ObjectId;

/**
 * A mutable implementation of {@link ChangeMap}.
 * 
 * @since 4.0
 */
public class MutableChangeMap implements ChangeMap {

	private Map<ObjectId, MutableObjectChange> changes;

	public MutableObjectChange getOrCreate(ObjectId id, ObjectChangeType type) {
		MutableObjectChange changeSet = getOrCreate(id);
		changeSet.setType(type);
		return changeSet;
	}

	private MutableObjectChange getOrCreate(ObjectId id) {

		MutableObjectChange objectChange = changes != null ? changes.get(id) : null;

		if (objectChange == null) {

			if (changes == null) {
				changes = new HashMap<>();
			}

			objectChange = new MutableObjectChange(id);
			changes.put(id, objectChange);
		}

		return objectChange;
	}

	public MutableObjectChange aliasId(ObjectId preCommitId, ObjectId postCommitId) {
		MutableObjectChange changeSet = getOrCreate(preCommitId);
		changeSet.setPostCommitId(postCommitId);
		changes.put(postCommitId, changeSet);
		return changeSet;
	}

	@Override
	public Collection<? extends ObjectChange> getUniqueChanges() {
		// ensure distinct change set
		return changes == null ? Collections.<ObjectChange> emptySet() : new HashSet<>(changes.values());
	}

	@Override
	public Map<ObjectId, ? extends ObjectChange> getChanges() {
		return changes == null ? Collections.<ObjectId, ObjectChange> emptyMap() : changes;
	}
}
