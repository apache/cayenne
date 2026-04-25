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

package org.apache.cayenne.modeler.pref.project;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;

class EntityDisplayEventType extends DisplayEventType {

    EntityDisplayEventType(ProjectController controller) {
        super(controller);
    }

    @Override
    public void fireLastDisplayEvent() {
        DataChannelDescriptor dataChannel = (DataChannelDescriptor) controller.getProject().getRootNode();
        if (!dataChannel.getName().equals(preferences.getDomain())) {
            return;
        }

        DataNodeDescriptor dataNode = dataChannel.getNodeDescriptor(preferences.getNode());
        DataMap dataMap = dataChannel.getDataMap(preferences.getDataMap());
        if (dataMap == null) {
            return;
        }

        Entity<?, ?, ?> entity = getLastEntity(dataMap);
        if (entity == null) {
            return;
        }

        EntityDisplayEvent entityDisplayEvent = new EntityDisplayEvent(this, entity, dataMap, dataNode, dataChannel);
        if (entity instanceof ObjEntity) {
            controller.displayObjEntity(entityDisplayEvent);
        } else if (entity instanceof DbEntity) {
            controller.displayDbEntity(entityDisplayEvent);
        }
    }

    @Override
    public void saveLastDisplayEvent() {

        preferences.setEvent(EntityDisplayEvent.class.getSimpleName());

        DataChannelDescriptor domain = controller.getSelectedDataDomain();
        DataNodeDescriptor node = controller.getSelectedDataNode();
        DataMap dataMap = controller.getSelectedDataMap();
        DbEntity dbEntity = controller.getSelectedDbEntity();
        ObjEntity objEntity = controller.getSelectedObjEntity();

        if (domain != null) {
            preferences.setDomain(domain.getName());
            preferences.setNode(node != null ? node.getName() : "");

            if (dataMap != null) {

                preferences.setDataMap(dataMap.getName());

                if (objEntity != null) {
                    preferences.setObjEntity(objEntity.getName());
                    preferences.setDbEntity(null);
                } else if (dbEntity != null) {
                    preferences.setDbEntity(dbEntity.getName());
                    preferences.setObjEntity(null);
                }
            }
        }
    }

    Entity<?, ?, ?> getLastEntity(DataMap dataMap) {
        return !preferences.getObjEntity().isEmpty()
                ? dataMap.getObjEntity(preferences.getObjEntity())
                : dataMap.getDbEntity(preferences.getDbEntity());
    }
}
