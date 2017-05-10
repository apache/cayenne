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

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.undo.CreateDataMapUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;

import java.awt.event.ActionEvent;

/**
 * Action that creates new DataMap in the project.
 */
public class CreateDataMapAction extends CayenneAction {

    public static String getActionName() {
        return "Create DataMap";
    }

    public CreateDataMapAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-datamap.png";
    }

    /** Calls addDataMap() or creates new data map if no data node selected. */
    public void createDataMap(DataMap map) {
        ProjectController mediator = getProjectController();
        mediator.addDataMap(this, map);
    }

    public void performAction(ActionEvent e) {
        ProjectController mediator = getProjectController();

        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) mediator
                .getProject()
                .getRootNode();

        DataMap map = new DataMap();
        map.setName(NameBuilder.builder(map, dataChannelDescriptor).name());
        createDataMap(map);

        application.getUndoManager().addEdit(new CreateDataMapUndoableEdit(dataChannelDescriptor, map));
    }

    /**
     * Returns <code>true</code> if path contains a DataDomain object.
     */
    public boolean enableForPath(ConfigurationNode object) {
        if (object == null) {
            return false;
        }

        return ((DataNodeDescriptor) object).getDataChannelDescriptor() != null;
    }
}
