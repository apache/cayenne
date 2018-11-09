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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClientClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.prefs.Preferences;

public class CgenTabController {

    private static Logger logObj = LoggerFactory.getLogger(ErrorDebugDialog.class);

    private ProjectController projectController;
    private CgenTab view;

    private ConcurrentMap<DataMap, CgenPanel> generatorsPanels;
    private Set<DataMap> selectedDataMaps;

    public CgenTabController(ProjectController projectController) {
        this.projectController = projectController;
        this.view = new CgenTab(projectController, this);
        this.generatorsPanels = new ConcurrentHashMap<>();
        this.selectedDataMaps = new HashSet<>();
    }

    void createPanels() {
        Collection<DataMap> dataMaps = getDataMaps();
        generatorsPanels.clear();
        for(DataMap dataMap : dataMaps) {
            CgenPanel cgenPanel = new CgenPanel(dataMap);
            initListenersForPanel(cgenPanel);
            generatorsPanels.put(dataMap, cgenPanel);
        }
        selectedDataMaps.forEach(dataMap -> {
            if(generatorsPanels.get(dataMap) != null) {
                CgenPanel currPanel = generatorsPanels.get(dataMap);
                currPanel.getCheckConfig().setSelected(true);
            }
        });
    }

    private void initListenersForPanel(CgenPanel cgenPanel) {
        cgenPanel.getCheckConfig().addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                selectedDataMaps.add(cgenPanel.getDataMap());
            } else if(e.getStateChange() == ItemEvent.DESELECTED) {
                selectedDataMaps.remove(cgenPanel.getDataMap());
            }
            setGenerateButtonDisabled();
        });

        cgenPanel.getToConfigButton().addActionListener(action -> showConfig(cgenPanel.getDataMap()));

        view.getSelectAll().addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                getGeneratorsPanels().forEach((key, value) -> value.getCheckConfig().setSelected(true));
            } else if(e.getStateChange() == ItemEvent.DESELECTED) {
                getGeneratorsPanels().forEach((key, value) -> value.getCheckConfig().setSelected(false));
            }
            setGenerateButtonDisabled();
        });
    }

    private void setGenerateButtonDisabled() {
        if(selectedDataMaps.size() == 0) {
            view.getGenerateAll().setEnabled(false);
        } else {
            view.getGenerateAll().setEnabled(true);
        }
    }

    private Collection<DataMap> getDataMaps() {
        Project project = projectController.getProject();
        return  ((DataChannelDescriptor) project.getRootNode()).getDataMaps();
    }

    public CgenTab getView() {
        return view;
    }

    void runGenerators(Set<DataMap> dataMaps) {
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
                view.showErrorMessage(e.getMessage());
            }
        }
        if(!generationFail) {
            view.showSuccessMessage();
        }
    }

    private CgenConfiguration createConfiguration(DataMap dataMap) {
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

    private void showConfig(DataMap dataMap) {
        if (dataMap != null) {
            projectController.fireDataMapDisplayEvent(new DataMapDisplayEvent(this.getView(), dataMap, dataMap.getDataChannelDescriptor()));
        }
    }

    public ProjectController getProjectController() {
        return projectController;
    }

    ConcurrentMap<DataMap, CgenPanel> getGeneratorsPanels() {
        return generatorsPanels;
    }

    public Set<DataMap> getSelectedDataMaps() {
        return selectedDataMaps;
    }
}
