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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.event.model.DataNodeEvent;
import org.apache.cayenne.modeler.undo.CreateDataMapUndoableEdit;

import java.awt.event.ActionEvent;

/**
 * Action that creates new DataMap in the project.
 */
public class CreateDataMapAction extends ModelerAbstractAction {
    
    public static void onMapCreated(Object src, ProjectController controller, DataMap map) {

        DataChannelDescriptor domain = (DataChannelDescriptor) controller.getProject().getRootNode();
        map.setDataChannelDescriptor(domain);
        domain.getDataMaps().add(map);

        DataNodeDescriptor node = controller.getSelectedDataNode();
        if (node != null && !node.getDataMapNames().contains(map.getName())) {
            node.getDataMapNames().add(map.getName());
            controller.fireDataNodeEvent(new DataNodeEvent(src, node));
        }

        controller.fireDataMapEvent(new DataMapEvent(src, map, MapEvent.ADD));

        DataMapDisplayEvent displayEvent = new DataMapDisplayEvent(src, map, domain, node);
        displayEvent.setMainTabFocus(true);
        controller.displayDataMap(displayEvent);
    }

    public CreateDataMapAction(Application application) {
        super("Create DataMap", application);
    }

    @Override
    public String getIconName() {
        return "icon-datamap.png";
    }

    public void performAction(ActionEvent e) {

        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) getProjectController()
                .getProject()
                .getRootNode();

        DataMap map = new DataMap();
        map.setName(NameBuilder.builder(map, dataChannelDescriptor).name());
        onMapCreated(this, getProjectController(), map);

        application.getUndoManager().addEdit(new CreateDataMapUndoableEdit(dataChannelDescriptor, map));
    }

    /**
     * Returns <code>true</code> if path contains a DataDomain object.
     */
    @Override
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        return ((DataNodeDescriptor) object).getDataChannelDescriptor() != null;
    }
}
