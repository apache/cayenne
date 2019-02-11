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

package org.apache.cayenne.modeler.editor.cgen.domain;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.prefs.Preferences;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.modeler.editor.GeneratorsTabController;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.util.ModelerUtil;

/**
 * @since 4.1
 */
public class CgenTabController extends GeneratorsTabController {

    public CgenTabController(ProjectController projectController) {
        super(CgenConfiguration.class, projectController);
        this.view = new CgenTab(projectController, this);
    }

    public void runGenerators(Set<DataMap> dataMaps) {
        DataChannelMetaData metaData = Application.getInstance().getMetaData();
        if(dataMaps.isEmpty()) {
            view.showEmptyMessage();
            return;
        }
        boolean generationFail = false;
        for(DataMap dataMap : dataMaps) {
            try {
                CgenConfiguration cgenConfiguration = metaData.get(dataMap, CgenConfiguration.class);
                if(cgenConfiguration == null) {
                    cgenConfiguration = createConfiguration(dataMap);
                }
                ClassGenerationAction classGenerationAction = cgenConfiguration.isClient() ? new ClientClassGenerationAction(cgenConfiguration) :
                        new ClassGenerationAction(cgenConfiguration);
                classGenerationAction.prepareArtifacts();
                classGenerationAction.execute();
            } catch (Exception e) {
                logObj.error("Error generating classes", e);
                generationFail = true;
                ((CgenTab)view).showErrorMessage(e.getMessage());
            }
        }
        if(!generationFail) {
            ((CgenTab)view).showSuccessMessage();
        }
    }

    public CgenConfiguration createConfiguration(DataMap dataMap) {
        CgenConfiguration cgenConfiguration = new CgenConfiguration();
        Application.getInstance().getInjector().injectMembers(cgenConfiguration);
        cgenConfiguration.setDataMap(dataMap);
        Path basePath = Paths.get(ModelerUtil.initOutputFolder());

        // no destination folder
        if (basePath == null) {
            JOptionPane.showMessageDialog(this.getView(), "Select directory for source files.");
            return null;
        }

        // no such folder
        if (!Files.exists(basePath)) {
            try {
                Files.createDirectories(basePath);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this.getView(), "Can't create directory. " +
                        ". Select a different one.");
                return null;
            }
        }

        // not a directory
        if (!Files.isDirectory(basePath)) {
            JOptionPane.showMessageDialog(this.getView(), basePath + " is not a valid directory.");
            return null;
        }

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
            projectController.fireDataMapDisplayEvent(new DataMapDisplayEvent(this.getView(), dataMap, dataMap.getDataChannelDescriptor()));
        }
    }
}
