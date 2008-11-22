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

import org.apache.cayenne.map.DataMap;

/** 
 * An events describing a DataMap change.
 */
public class DataMapEvent extends MapEvent {
	protected DataMap dataMap;

	/** Creates a DataMap change event. */
	public DataMapEvent(Object src, DataMap dataMap) {
		super(src);
		this.dataMap = dataMap;
	}

	/** Creates a DataMap event of a specified type. */
	public DataMapEvent(Object src, DataMap dataMap, int id) {
		this(src, dataMap);
		setId(id);
	}

	/** Creates a DataMap name change event.*/
	public DataMapEvent(Object src, DataMap dataMap, String oldName) {
		this(src, dataMap);
		setOldName(oldName);
	}

	/** 
	 * Returns DataMap associated with this event. 
	 */
	public DataMap getDataMap() {
		return dataMap;
	}
	
	/**
	 * Sets DataMap associated with this event.
	 * 
	 * @param dataMap The dataMap to set
	 */
	public void setDataMap(DataMap dataMap) {
		this.dataMap = dataMap;
	}

    @Override
    public String getNewName() {
        return (dataMap != null) ? dataMap.getName() : null;
    }
}
