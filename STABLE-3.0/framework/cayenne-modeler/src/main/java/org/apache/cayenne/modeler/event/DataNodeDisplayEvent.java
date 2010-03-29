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

package org.apache.cayenne.modeler.event;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;

/**
 * Represents a display event of a DataNode.
 * 
 */
public class DataNodeDisplayEvent extends DomainDisplayEvent {

	protected DataNode dataNode;
	
	/** True if data node is different from the current data node. */
	protected boolean dataNodeChanged = true;

	/** Current DataNode changed. */
	public DataNodeDisplayEvent(Object src, DataDomain domain, DataNode node) {
		super(src, domain);
		this.dataNode = node;
		setDomainChanged(false);
	}

	/** Get data node (data source) associated with this data map. */
	public DataNode getDataNode() {
		return dataNode;
	}

	/** Returns true if data node is different from the current data node. */
	public boolean isDataNodeChanged() {
		return dataNodeChanged;
	}
	
	public void setDataNodeChanged(boolean temp) {
		dataNodeChanged = temp;
	}
	/**
	 * Sets the dataNode.
	 * @param dataNode The dataNode to set
	 */
	public void setDataNode(DataNode dataNode) {
		this.dataNode = dataNode;
	}


}
