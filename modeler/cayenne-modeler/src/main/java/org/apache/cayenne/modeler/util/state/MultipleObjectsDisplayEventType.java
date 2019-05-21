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

package org.apache.cayenne.modeler.util.state;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.MultipleObjectsDisplayEvent;

import java.util.ArrayList;
import java.util.List;

class MultipleObjectsDisplayEventType extends DisplayEventType {

    public MultipleObjectsDisplayEventType(ProjectController controller) {
        super(controller);
    }

    @Override
    public void fireLastDisplayEvent() {
        DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();

        String parentObjectName = preferences.getParentObject();
        ConfigurationNode parentObject;
        ConfigurationNode[] multipleObjects;

        if (dataChannel.getDataMap(parentObjectName) != null) {
            DataMap dataMap = dataChannel.getDataMap(parentObjectName);
            parentObject = dataMap;
            multipleObjects = getLastMultipleObjects(dataMap);
        } else if (dataChannel.getNodeDescriptor(parentObjectName) != null) {
            DataNodeDescriptor dataNode = dataChannel.getNodeDescriptor(parentObjectName);
            parentObject = dataNode;
            multipleObjects = getLastMultipleObjects(dataNode);
        } else {
            parentObject = dataChannel;
            multipleObjects = getLastMultipleObjects(dataChannel);
        }

        MultipleObjectsDisplayEvent multipleDisplayEvent = new MultipleObjectsDisplayEvent(this, multipleObjects, parentObject);
        controller.fireMultipleObjectsDisplayEvent(multipleDisplayEvent);
    }

    @Override
    public void saveLastDisplayEvent() {
        preferences.setEvent(MultipleObjectsDisplayEvent.class.getSimpleName());
        preferences.setParentObject(getObjectName(controller.getCurrentParentPath()));

        ConfigurationNode[] multipleObjects = controller.getCurrentPaths();
        if (multipleObjects == null) {
            preferences.setMultipleObjects("");
        } else {
            StringBuilder sb = new StringBuilder();
            for (ConfigurationNode object : multipleObjects) {
                String objectName = getObjectName(object);
                if (!objectName.isEmpty()) {
                    sb.append(objectName).append(",");
                }
            }
            preferences.setMultipleObjects(sb.toString());
        }
    }

    protected ConfigurationNode[] getLastMultipleObjects(DataChannelDescriptor dataChannel) {
        List<ConfigurationNode> configurationNodeList = new ArrayList<>();
        ConfigurationNode[] nodes = new ConfigurationNode[0];

        String multipleObjects = preferences.getMultipleObjects();
        if (multipleObjects.isEmpty()) {
            return configurationNodeList.toArray(nodes);
        }

        for (String objectName : multipleObjects.split(",")) {
            ConfigurationNode configNode = getConfigNode(dataChannel, objectName);
            if (configNode != null) {
                configurationNodeList.add(configNode);
            }
        }

        return configurationNodeList.toArray(nodes);
    }

    protected ConfigurationNode[] getLastMultipleObjects(DataNodeDescriptor dataNode) {
        List<ConfigurationNode> configurationNodeList = new ArrayList<>();
        ConfigurationNode[] nodes = new ConfigurationNode[0];

        String multipleObjects = preferences.getMultipleObjects();
        if (multipleObjects.isEmpty()) {
            return configurationNodeList.toArray(nodes);
        }

        for (String objectName : multipleObjects.split(",")) {
            if (dataNode.getDataMapNames().contains(objectName)) {
                configurationNodeList.add(dataNode.getDataChannelDescriptor().getDataMap(objectName));
            }
        }

        return configurationNodeList.toArray(nodes);
    }

    protected ConfigurationNode[] getLastMultipleObjects(DataMap dataMap) {
        List<ConfigurationNode> configurationNodeList = new ArrayList<>();
        ConfigurationNode[] nodes = new ConfigurationNode[0];

        String multipleObjects = preferences.getMultipleObjects();
        if (multipleObjects.isEmpty()) {
            return configurationNodeList.toArray(nodes);
        }

        for (String objectName : multipleObjects.split(",")) {
            if (dataMap.getObjEntity(objectName) != null) {
                configurationNodeList.add(dataMap.getObjEntity(objectName));
            } else if (dataMap.getDbEntity(objectName) != null) {
                configurationNodeList.add(dataMap.getDbEntity(objectName));
            } else if (dataMap.getEmbeddable(objectName) != null) {
                configurationNodeList.add(dataMap.getEmbeddable(objectName));
            } else if (dataMap.getProcedure(objectName) != null) {
                configurationNodeList.add(dataMap.getProcedure(objectName));
            } else if (dataMap.getQueryDescriptor(objectName) != null) {
                configurationNodeList.add(dataMap.getQueryDescriptor(objectName));
            }
        }

        return configurationNodeList.toArray(nodes);
    }

    private ConfigurationNode getConfigNode(DataChannelDescriptor dataChannel, String objectName) {
        if (dataChannel.getName().equals(objectName)) {
            return dataChannel;
        }

        for (DataNodeDescriptor dataNode : dataChannel.getNodeDescriptors()) {
            if (dataNode.getName().equals(objectName)) {
                return dataNode;
            }
        }

        for (DataMap dataMap : dataChannel.getDataMaps()) {
            if (dataMap.getName().equals(objectName)) {
                return dataMap;
            }
        }

        return null;
    }

}