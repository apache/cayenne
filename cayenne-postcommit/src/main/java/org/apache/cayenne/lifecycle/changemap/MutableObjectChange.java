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
	private Map<String, MutableAttributeChange> attributeChanges;
	private Map<String, MutableToManyRelationshipChange> toManyRelationshipChanges;
	private Map<String, MutableToOneRelationshipChange> toOneRelationshipChanges;

	private ObjectChangeType type;

	public MutableObjectChange(ObjectId preCommitId) {
		this.preCommitId = preCommitId;
	}

	@Override
	public Map<String, ? extends PropertyChange> getChanges() {
		Map<String, PropertyChange> allChanges = new HashMap<>();

		if (attributeChanges != null) {
			allChanges.putAll(attributeChanges);
		}

		if (toOneRelationshipChanges != null) {
			allChanges.putAll(toOneRelationshipChanges);
		}

		if (toManyRelationshipChanges != null) {
			allChanges.putAll(toManyRelationshipChanges);
		}

		return allChanges;
	}

	@Override
	public Map<String, ? extends AttributeChange> getAttributeChanges() {
		return attributeChanges != null ? attributeChanges : Collections.<String, AttributeChange> emptyMap();
	}

	@Override
	public Map<String, ? extends ToManyRelationshipChange> getToManyRelationshipChanges() {
		return toManyRelationshipChanges != null ? toManyRelationshipChanges
				: Collections.<String, ToManyRelationshipChange> emptyMap();
	}

	@Override
	public Map<String, ? extends ToOneRelationshipChange> getToOneRelationshipChanges() {
		return toOneRelationshipChanges != null ? toOneRelationshipChanges
				: Collections.<String, ToOneRelationshipChange> emptyMap();
	}

	@Override
	public ObjectChangeType getType() {
		return type;
	}

	@Override
	public ObjectId getPreCommitId() {
		return preCommitId;
	}

	@Override
	public ObjectId getPostCommitId() {
		return postCommitId != null ? postCommitId : preCommitId;
	}

	public void setPostCommitId(ObjectId postCommitId) {
		this.postCommitId = postCommitId;
	}

	public void setType(ObjectChangeType changeType) {
		if (this.type == null || TYPE_PRECEDENCE[changeType.ordinal()] > TYPE_PRECEDENCE[this.type.ordinal()]) {
			this.type = changeType;
		}
	}

	public void toManyRelationshipConnected(String property, ObjectId value) {
		getOrCreateToManyChange(property).connected(value);
	}

	public void toManyRelationshipDisconnected(String property, ObjectId value) {
		getOrCreateToManyChange(property).disconnected(value);
	}

	public void toOneRelationshipConnected(String property, ObjectId value) {
		getOrCreateToOneChange(property).connected(value);
	}

	public void toOneRelationshipDisconnected(String property, ObjectId value) {
		getOrCreateToOneChange(property).disconnected(value);
	}

	public void attributeChanged(String property, Object oldValue, Object newValue) {

		if (type == null) {
			throw new IllegalStateException("Null op");
		}

		MutableAttributeChange c = getOrCreateAttributeChange(property);
		c.setNewValue(newValue);
		c.setOldValue(oldValue);
	}

	private MutableAttributeChange getOrCreateAttributeChange(String property) {
		MutableAttributeChange pChange = attributeChanges != null ? attributeChanges.get(property) : null;

		if (pChange == null) {

			if (attributeChanges == null) {
				attributeChanges = new HashMap<>();
			}

			pChange = new MutableAttributeChange();
			attributeChanges.put(property, pChange);
		}

		return pChange;
	}

	private MutableToOneRelationshipChange getOrCreateToOneChange(String property) {
		MutableToOneRelationshipChange pChange = toOneRelationshipChanges != null
				? toOneRelationshipChanges.get(property) : null;

		if (pChange == null) {

			if (toOneRelationshipChanges == null) {
				toOneRelationshipChanges = new HashMap<>();
			}

			pChange = new MutableToOneRelationshipChange();
			toOneRelationshipChanges.put(property, pChange);
		}

		return pChange;
	}

	private MutableToManyRelationshipChange getOrCreateToManyChange(String property) {
		MutableToManyRelationshipChange pChange = toManyRelationshipChanges != null
				? toManyRelationshipChanges.get(property) : null;

		if (pChange == null) {

			if (toManyRelationshipChanges == null) {
				toManyRelationshipChanges = new HashMap<>();
			}

			pChange = new MutableToManyRelationshipChange();
			toManyRelationshipChanges.put(property, pChange);
		}

		return pChange;
	}
}
