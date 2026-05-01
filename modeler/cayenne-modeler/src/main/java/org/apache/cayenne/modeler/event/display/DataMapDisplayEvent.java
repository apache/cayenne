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

package org.apache.cayenne.modeler.event.display;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;

public class DataMapDisplayEvent extends DisplayEvent {

    private final DataChannelDescriptor domain;
    private final DataMap dataMap;
    private final DataNodeDescriptor dataNode;
    // True if the event should cause the editor to switch to the main DataMap tab
    private final boolean mainTabFocus;

    public DataMapDisplayEvent(Object src, DataChannelDescriptor domain, DataMap dataMap) {
        this(src, domain, dataMap, null, false);
    }

    public DataMapDisplayEvent(Object src, DataChannelDescriptor domain, DataMap dataMap, boolean mainTabFocus) {
        this(src, domain, dataMap, null, mainTabFocus);
    }

    public DataMapDisplayEvent(Object src,
                               DataChannelDescriptor domain,
                               DataMap dataMap,
                               DataNodeDescriptor dataNode) {
        this(src, domain, dataMap, dataNode, false);
    }

    public DataMapDisplayEvent(Object src,
                               DataChannelDescriptor domain,
                               DataMap dataMap,
                               DataNodeDescriptor dataNode,
                               boolean mainTabFocus) {
        super(src);
        this.domain = domain;
        this.dataMap = dataMap;
        this.dataNode = dataNode;
        this.mainTabFocus = mainTabFocus;
    }

    public DataChannelDescriptor getDomain() {
        return domain;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public DataNodeDescriptor getDataNode() {
        return dataNode;
    }

    public boolean isMainTabFocus() {
        return mainTabFocus;
    }
}
