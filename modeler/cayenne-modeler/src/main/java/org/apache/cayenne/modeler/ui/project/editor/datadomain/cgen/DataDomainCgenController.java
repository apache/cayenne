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

package org.apache.cayenne.modeler.ui.project.editor.datadomain.cgen;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.event.display.DataMapDisplayEvent;
import org.apache.cayenne.modeler.project.CgenOps;
import org.apache.cayenne.modeler.ui.preferences.general.GeneralPreferencesController;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.project.editor.datadomain.DataDomainGeneratorsViewController;
import org.apache.cayenne.tools.ToolsInjectorBuilder;

import java.util.Set;
import java.util.prefs.Preferences;

public class DataDomainCgenController extends DataDomainGeneratorsViewController<CgenConfiguration> {

    public DataDomainCgenController(ProjectController projectController) {
        super(projectController, CgenConfiguration.class, true);
        this.view = new DataDomainCgenView(projectController, this);
    }

    public void runGenerators(Set<DataMap> dataMaps) {
        DataChannelMetaData metaData = controller.getApplication().getMetaData();
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
            } catch (CayenneRuntimeException e) {
                LOGGER.error("Error generating classes", e);
                generationFail = true;
                ((DataDomainCgenView) view).showErrorMessage(e.getUnlabeledMessage());
            } catch (Exception e) {
                LOGGER.error("Error generating classes", e);
                generationFail = true;
                ((DataDomainCgenView) view).showErrorMessage(e.getMessage());
            }
        }
        if (!generationFail) {
            ((DataDomainCgenView) view).showSuccessMessage();
        }
    }

    public CgenConfiguration createConfiguration(DataMap dataMap) {
        CgenConfiguration cgenConfiguration = new CgenConfiguration();
        cgenConfiguration.setDataMap(dataMap);
        cgenConfiguration.updateOutputPath(CgenOps.baseDir(controller.getApplication()));
        Preferences preferences = controller.getApplication().getPreferencesNode(GeneralPreferencesController.class, "");
        if (preferences != null) {
            cgenConfiguration.setEncoding(preferences.get(GeneralPreferencesController.ENCODING_PREFERENCE, null));
        }

        cgenConfiguration.resolveExcludedEntities();
        cgenConfiguration.resolveExcludedEmbeddables();
        return cgenConfiguration;
    }

    public void showConfig(DataMap dataMap) {
        if (dataMap != null) {
            DataMapDisplayEvent event = new DataMapDisplayEvent(getView(), dataMap, dataMap.getDataChannelDescriptor());
            getController().displayDataMap(event);
        }
    }
}
