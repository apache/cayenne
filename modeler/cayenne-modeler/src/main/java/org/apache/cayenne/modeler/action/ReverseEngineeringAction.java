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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneAction;

import java.awt.event.ActionEvent;

/**
 * Action that imports database structure into a DataMap.
 */
public class ReverseEngineeringAction extends CayenneAction {

    public ReverseEngineeringAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName() {
        return "Reengineer Database Schema";
    }

    /**
     * Connects to DB and delegates processing to DbLoaderController, starting it asynchronously.
     */
    @Override
    public void performAction(ActionEvent event) {
        ProjectController projectController = getProjectController();
        DataMap dataMap = projectController.getCurrentDataMap();
        DataChannelDescriptor dataChannelDescriptor = projectController.getCurrentDataChanel();
        if (dataMap == null) {
            dataMap = new DataMap();
            dataMap.setName(NameBuilder
                    .builder(dataMap, dataChannelDescriptor)
                    .name());
            dataChannelDescriptor.getDataMaps().add(dataMap);
            getProjectController().fireDataMapEvent(new DataMapEvent(this, dataMap, MapEvent.ADD));
        }

        ((CayenneModelerController) projectController.getParent())
                .getEditorView()
                .getDataMapView()
                .setSelectedIndex(1);
    }
}
