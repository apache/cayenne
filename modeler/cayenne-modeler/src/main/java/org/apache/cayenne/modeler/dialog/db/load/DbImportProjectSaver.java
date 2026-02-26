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
package org.apache.cayenne.modeler.dialog.db.load;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.resource.Resource;

public class DbImportProjectSaver implements ProjectSaver {

    private ConfigurationNameMapper nameMapper;

    private ProjectController projectController;

    public DbImportProjectSaver(@Inject ProjectController projectController, @Inject ConfigurationNameMapper nameMapper) {
        this.projectController = projectController;
        this.nameMapper = nameMapper;
    }

    @Override
    public ProjectVersion getSupportedVersion() {
        // not important in the context of non-saving saver
        return null;
    }

    @Override
    public void save(Project project) {

        DataMap dataMap = (DataMap) project.getRootNode();

        if (projectController.getCurrentDataMap() != null) {
            projectController.fireDataMapEvent(new DataMapEvent(Application.getFrame(), dataMap, MapEvent.REMOVE));
            projectController.fireDataMapEvent(new DataMapEvent(Application.getFrame(), dataMap, MapEvent.ADD));
        } else {
            DataChannelDescriptor currentDomain = (DataChannelDescriptor) projectController.getProject().getRootNode();
            Resource baseResource = currentDomain.getConfigurationSource();
            // a new DataMap, so need to set configuration source for it
            if (baseResource != null) {
                Resource dataMapResource = baseResource.getRelativeResource(nameMapper.configurationLocation(dataMap));
                dataMap.setConfigurationSource(dataMapResource);
            }
            projectController.addDataMap(Application.getFrame(), dataMap);
        }
    }

    @Override
    public void saveAs(Project project, Resource baseDirectory) {
        save(project);
    }
}
