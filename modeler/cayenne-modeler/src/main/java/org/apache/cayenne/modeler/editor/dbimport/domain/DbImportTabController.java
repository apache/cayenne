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
package org.apache.cayenne.modeler.editor.dbimport.domain;

import java.util.Set;

import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ReverseEngineeringAction;
import org.apache.cayenne.modeler.editor.GeneratorsTabController;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;

/**
 * @since 4.1
 */
public class DbImportTabController extends GeneratorsTabController<ReverseEngineering> {

    public DbImportTabController(ProjectController projectController) {
        super(projectController, ReverseEngineering.class, false);
        this.view = new DbImportTab(projectController, this);
    }

    @Override
    public void runGenerators(Set<DataMap> dataMaps) {
        if(dataMaps.isEmpty()) {
            view.showEmptyMessage();
            return;
        }
        Application.getInstance().getFrameController().getDbImportController().setGlobalImport(true);
        ReverseEngineeringAction reverseEngineeringAction = Application.getInstance().getActionManager().getAction(ReverseEngineeringAction.class);
        reverseEngineeringAction.performAction(dataMaps);
    }

    @Override
    public void showConfig(DataMap dataMap) {
        if (dataMap != null) {
            DataMapDisplayEvent event = new DataMapDisplayEvent(getView(), dataMap, dataMap.getDataChannelDescriptor());
            getProjectController().fireDataMapDisplayEvent(event);
        }
    }
}
