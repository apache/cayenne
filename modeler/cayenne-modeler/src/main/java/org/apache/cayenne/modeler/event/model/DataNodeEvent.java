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

import org.apache.cayenne.configuration.DataNodeDescriptor;

/**
 * Represents events resulted from DataNode changes in CayenneModeler.
 */
public class DataNodeEvent extends ModelEvent {

    private final DataNodeDescriptor dataNode;

    public static DataNodeEvent ofAdd(Object src, DataNodeDescriptor dataNode) {
        return new DataNodeEvent(src, dataNode, Type.ADD, null);
    }

    public static DataNodeEvent ofChange(Object src, DataNodeDescriptor dataNode) {
        return new DataNodeEvent(src, dataNode, Type.CHANGE, null);
    }

    public static DataNodeEvent ofChange(Object src, DataNodeDescriptor dataNode, String oldName) {
        return new DataNodeEvent(src, dataNode, Type.CHANGE, oldName);
    }

    public static DataNodeEvent ofRemove(Object src, DataNodeDescriptor dataNode) {
        return new DataNodeEvent(src, dataNode, Type.REMOVE, null);
    }

    private DataNodeEvent(Object src, DataNodeDescriptor dataNode, Type type, String oldName) {
        super(src, type, oldName);
        this.dataNode = dataNode;
    }

    public DataNodeDescriptor getDataNode() {
        return dataNode;
    }

    @Override
    public String getNewName() {
        return (dataNode != null) ? dataNode.getName() : null;
    }
}
