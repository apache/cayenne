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

package org.apache.cayenne.modeler.event.model;

import org.apache.cayenne.map.DataMap;

/**
 * An event describing a DataMap change.
 */
public class DataMapEvent extends ModelEvent {

    private final DataMap dataMap;

    public static DataMapEvent ofAdd(Object src, DataMap dataMap) {
        return new DataMapEvent(src, dataMap, Type.ADD, null);
    }

    public static DataMapEvent ofChange(Object src, DataMap dataMap) {
        return new DataMapEvent(src, dataMap, Type.CHANGE, null);
    }

    public static DataMapEvent ofChange(Object src, DataMap dataMap, String oldName) {
        return new DataMapEvent(src, dataMap, Type.CHANGE, oldName);
    }

    public static DataMapEvent ofRemove(Object src, DataMap dataMap) {
        return new DataMapEvent(src, dataMap, Type.REMOVE, null);
    }

    private DataMapEvent(Object src, DataMap dataMap, Type type, String oldName) {
        super(src, type, oldName);
        this.dataMap = dataMap;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    @Override
    public String getNewName() {
        return (dataMap != null) ? dataMap.getName() : null;
    }
}
