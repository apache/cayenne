package org.apache.cayenne.modeler.event;
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


import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;

/**
 * Represents a display event of a DataMap.
 * 
 */
public class DataMapDisplayEvent extends DataNodeDisplayEvent {
	protected DataMap dataMap;

	/** True if different from current data map. */
	protected boolean dataMapChanged = true;

	public DataMapDisplayEvent(Object src, DataMap map, DataDomain domain) {
		this(src, map, domain, null);
	}

	public DataMapDisplayEvent(
		Object src,
		DataMap map,
		DataDomain domain,
		DataNode node) {

		super(src, domain, node);
		this.dataMap = map;
		setDataNodeChanged(false);
	}

	/** Get dataMap wrapper. */
	public DataMap getDataMap() {
		return dataMap;
	}
	
	/**
	 * Sets the dataMap.
	 * @param dataMap The dataMap to set
	 */
	public void setDataMap(DataMap dataMap) {
		this.dataMap = dataMap;
	}


	/** Returns true if data map is different from the current data map. */
	public boolean isDataMapChanged() {
		return dataMapChanged;
	}
	
	public void setDataMapChanged(boolean temp) {
		dataMapChanged = temp;
	}
}
