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

package org.apache.cayenne.modeler.editor.cgen.domain;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.prefs.Preferences;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.modeler.editor.GeneratorsTabController;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.tools.ToolsInjectorBuilder;

/**
 * @since 4.1
 */
public class CgenTabController extends GeneratorsTabController<CgenConfiguration> {

    public CgenTabController(ProjectController projectController) {
        super(projectController, CgenConfiguration.class, true);
        this.view = new CgenTab(projectController, this);
    }

    public void runGenerators(Set<DataMap> dataMaps) {
        DataChannelMetaData metaData = Application.getInstance().getMetaData();
        if (dataMaps.isEmpty()) {
            view.showEmptyMessage();
            return;
        }
        boolean generationFail = false;
        ClassGenerationActionFactory actionFactory = new ToolsInjectorBuilder()
                .addModule(binder
                        -> binder.bind(DataChannelMetaData.class).toInstance(metaData))
                .create()
                .getInstance(ClassGenerationActionFactory.class);

        for (DataMap dataMap : dataMaps) {
            try {
                CgenConfigList cgenConfigList = metaData.get(dataMap, CgenConfigList.class);
                if (cgenConfigList == null) {
                    cgenConfigList = new CgenConfigList();
                    cgenConfigList.add(createConfiguration(dataMap));
                }
                for (CgenConfiguration cgenConfiguration : cgenConfigList.getAll()) {
                    cgenConfiguration.setForce(true);
                    ClassGenerationAction action = actionFactory.createAction(cgenConfiguration);
                    action.prepareArtifacts();
                    action.execute();
                }

            } catch (Exception e) {
                LOGGER.error("Error generating classes", e);
                generationFail = true;
                ((CgenTab) view).showErrorMessage(e.getMessage());
            }
        }
        if (!generationFail) {
            ((CgenTab) view).showSuccessMessage();
        }
    }

    public CgenConfiguration createConfiguration(DataMap dataMap) {
        CgenConfiguration cgenConfiguration = new CgenConfiguration();
        cgenConfiguration.setDataMap(dataMap);
        Path basePath = Paths.get(ModelerUtil.initOutputFolder());
        cgenConfiguration.setRootPath(basePath);
        Preferences preferences = Application.getInstance().getPreferencesNode(GeneralPreferences.class, "");
        if (preferences != null) {
            cgenConfiguration.setEncoding(preferences.get(GeneralPreferences.ENCODING_PREFERENCE, null));
        }

        cgenConfiguration.resolveExcludeEntities();
        cgenConfiguration.resolveExcludeEmbeddables();
        return cgenConfiguration;
    }

    public void showConfig(DataMap dataMap) {
        if (dataMap != null) {
            DataMapDisplayEvent event = new DataMapDisplayEvent(getView(), dataMap, dataMap.getDataChannelDescriptor());
            getProjectController().fireDataMapDisplayEvent(event);
        }
    }
}
