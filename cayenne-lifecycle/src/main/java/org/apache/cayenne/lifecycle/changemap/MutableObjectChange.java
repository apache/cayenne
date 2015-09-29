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
package org.apache.cayenne.lifecycle.changemap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ObjectId;

/**
 * A mutable implementation of {@link ObjectChange}.
 * 
 * @since 4.0
 */
public class MutableObjectChange implements ObjectChange {

	private static final int[] TYPE_PRECEDENCE;

	static {
		TYPE_PRECEDENCE = new int[ObjectChangeType.values().length];

		// decreasing precedence of operations when recording audits is DELETE,
		// INSERT, UPDATE
		TYPE_PRECEDENCE[ObjectChangeType.DELETE.ordinal()] = 3;
		TYPE_PRECEDENCE[ObjectChangeType.INSERT.ordinal()] = 2;
		TYPE_PRECEDENCE[ObjectChangeType.UPDATE.ordinal()] = 1;
	}

	// note that we are tracking DB-level changes for clarity

	private ObjectId preCommitId;
	private ObjectId postCommitId;
	private Map<String, ObjectPropertyChange> changes;
	private Map<String, Object> snapshot;
	private ObjectChangeType changeType;

	public MutableObjectChange(ObjectId preCommitId) {
		this.preCommitId = preCommitId;
	}

	@Override
	public Map<String, ObjectPropertyChange> getChanges() {
		return changes != null ? changes : Collections.<String, ObjectPropertyChange> emptyMap();
	}

	@Override
	public Map<String, Object> getSnapshot() {
		return snapshot != null ? snapshot : Collections.<String, Object> emptyMap();
	}

	@Override
	public ObjectChangeType getChangeType() {
		return changeType;
	}

	@Override
	public ObjectId getPreCommitId() {
		return preCommitId;
	}

	@Override
	public ObjectId getFinalId() {
		return postCommitId != null ? postCommitId : preCommitId;
	}

	public void setPostCommitId(ObjectId postCommitId) {
		this.postCommitId = postCommitId;
	}

	public void setChangeType(ObjectChangeType changeType) {
		if (this.changeType == null
				|| TYPE_PRECEDENCE[changeType.ordinal()] > TYPE_PRECEDENCE[this.changeType.ordinal()]) {
			this.changeType = changeType;
		}
	}

	public void setSnapshotProperty(String property, Object value) {
		if (snapshot == null) {
			snapshot = new HashMap<>();
		}

		snapshot.put(property, value);
	}

	public void setPropertyChange(String property, Object oldValue, Object newValue) {

		if (changeType == null) {
			throw new IllegalStateException("Null op");
		}

		switch (changeType) {
		case INSERT:
			setSnapshotProperty(property, newValue);
			break;
		case UPDATE:
			getOrCreate(property, oldValue).setNewValue(newValue);
			break;

		// this 'case' may be redundant; deletes are tracked by directly
		// calling 'propertySnapshot'.
		case DELETE:
			setSnapshotProperty(property, oldValue);
			break;
		default:
			throw new IllegalStateException("Unexpected op: " + changeType);
		}
	}

	private ObjectPropertyChange getOrCreate(String property, Object oldValue) {
		ObjectPropertyChange pChange = changes != null ? changes.get(property) : null;

		if (pChange == null) {

			if (changes == null) {
				changes = new HashMap<>();
			}

			pChange = new ObjectPropertyChange(oldValue);
			changes.put(property, pChange);
		}

		return pChange;
	}
}
