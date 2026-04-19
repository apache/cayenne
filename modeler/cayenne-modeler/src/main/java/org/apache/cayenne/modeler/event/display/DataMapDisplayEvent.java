package org.apache.cayenne.modeler.event.display;
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


import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;

public class DataMapDisplayEvent extends DataNodeDisplayEvent {

    protected final DataMap dataMap;

    // True if the event should cause the editor to switch to the main DataMap tab
    protected boolean mainTabFocus;

    public DataMapDisplayEvent(Object src, DataMap map, DataChannelDescriptor dataChannelDescriptor) {
        this(src, map, dataChannelDescriptor, null);
    }

    public DataMapDisplayEvent(
            Object src,
            DataMap map,
            DataChannelDescriptor dataChannelDescriptor,
            DataNodeDescriptor node) {

        super(src, dataChannelDescriptor, node);
        this.dataMap = map;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public boolean isMainTabFocus() {
        return mainTabFocus;
    }

    public void setMainTabFocus(boolean mainTabFocus) {
        this.mainTabFocus = mainTabFocus;
    }
}
