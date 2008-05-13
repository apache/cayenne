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

package org.apache.cayenne.map.event;

import org.apache.cayenne.access.DataNode;

/** 
 * Represents events resulted from DataNode changes 
 * in CayenneModeler.
 */
public class DataNodeEvent extends MapEvent {
	protected DataNode dataNode;

	/** Creates a node change event. */
	public DataNodeEvent(Object src, DataNode node) {
		super(src);
		setDataNode(node);
	}

	/** Creates a node event of a specified type. */
	public DataNodeEvent(Object src, DataNode node, int id) {
		this(src, node);
		setId(id);
	}

	/** Creates a node name change event.*/
	public DataNodeEvent(Object src, DataNode node, String oldName) {
		this(src, node);
		setOldName(oldName);
	}

	/** Returns node object associated with this event. */
	public DataNode getDataNode() {
		return dataNode;
	}

	/**
	 * Sets the dataNode.
	 * 
	 * @param dataNode The dataNode to set
	 */
	public void setDataNode(DataNode dataNode) {
		this.dataNode = dataNode;
	}
	
	@Override
    public String getNewName() {
		return (dataNode != null) ? dataNode.getName() : null;
	}
}
