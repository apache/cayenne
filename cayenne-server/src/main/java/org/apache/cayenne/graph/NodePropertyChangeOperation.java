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

package org.apache.cayenne.graph;

import org.apache.cayenne.util.Util;

/**
 * @since 1.2
 */
public class NodePropertyChangeOperation extends NodeDiff {

	private static final long serialVersionUID = 3282063727025159961L;

	protected String property;
	protected Object oldValue;
	protected Object newValue;

	public NodePropertyChangeOperation(Object nodeId, String property, Object oldValue, Object newValue) {

		super(nodeId);
		this.property = property;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	public NodePropertyChangeOperation(Object nodeId, String property, Object oldValue, Object newValue, int diffId) {
		super(nodeId, diffId);

		this.property = property;
		this.oldValue = oldValue;
		this.newValue = newValue;
	}

	/**
	 * @since 3.0
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Changes the 'newValue'.
	 * 
	 * @since 3.0
	 */
	void setNewValue(Object newValue) {
		this.newValue = newValue;
	}

	/**
	 * Returns true if both old and new value are equal.
	 */
	@Override
	public boolean isNoop() {
		return Util.nullSafeEquals(oldValue, newValue);
	}

	@Override
	public void apply(GraphChangeHandler tracker) {
		tracker.nodePropertyChanged(nodeId, property, oldValue, newValue);
	}

	@Override
	public void undo(GraphChangeHandler tracker) {
		tracker.nodePropertyChanged(nodeId, property, newValue, oldValue);
	}
}
