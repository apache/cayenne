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

/**
 * @since 1.2
 */
public class ArcCreateOperation extends NodeDiff {

	private static final long serialVersionUID = 2497511574121718987L;
	
	protected Object targetNodeId;
	protected ArcId arcId;

	public ArcCreateOperation(Object nodeId, Object targetNodeId, ArcId arcId) {
		super(nodeId);
		this.targetNodeId = targetNodeId;
		this.arcId = arcId;
	}

	public ArcCreateOperation(Object nodeId, Object targetNodeId, ArcId arcId, int diffId) {
		super(nodeId, diffId);
		this.targetNodeId = targetNodeId;
		this.arcId = arcId;
	}

	@Override
	public void apply(GraphChangeHandler tracker) {
		tracker.arcCreated(nodeId, targetNodeId, arcId);
	}

	@Override
	public void undo(GraphChangeHandler tracker) {
		tracker.arcDeleted(nodeId, targetNodeId, arcId);
	}

	public Object getArcId() {
		return arcId;
	}

	public Object getTargetNodeId() {
		return targetNodeId;
	}
}
