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
package org.apache.cayenne.modeler.editor;

import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.1
 */
public abstract class GeneratorsTabController {

    public static Logger logObj = LoggerFactory.getLogger(ErrorDebugDialog.class);
    public ProjectController projectController;
    public GeneratorsTab view;
    private Class type;

    public ConcurrentMap<DataMap, GeneratorsPanel> generatorsPanels;
    public Set<DataMap> selectedDataMaps;

    public GeneratorsTabController(Class type, ProjectController projectController) {
        this.type = type;
        this.generatorsPanels = new ConcurrentHashMap<>();
        this.selectedDataMaps = new HashSet<>();
        this.projectController = projectController;
    }

    public String icon;

    public abstract void runGenerators(Set<DataMap> dataMaps);

    public void createPanels(){
        Collection<DataMap> dataMaps = getDataMaps();
        refreshSelectedMaps(dataMaps);
        generatorsPanels.clear();
        for(DataMap dataMap : dataMaps) {
            GeneratorsPanel generatorPanel = new GeneratorsPanel(dataMap, "icon-datamap.png", type);
            initListenersForPanel(generatorPanel);
            generatorsPanels.put(dataMap, generatorPanel);
        }
        selectedDataMaps.forEach(dataMap -> {
            if(generatorsPanels.get(dataMap) != null) {
                GeneratorsPanel currPanel = generatorsPanels.get(dataMap);
                currPanel.getCheckConfig().setSelected(true);
            }
        });
    }

    private void initListenersForPanel(GeneratorsPanel panel) {
        panel.getCheckConfig().addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                selectedDataMaps.add(panel.getDataMap());
            } else if(e.getStateChange() == ItemEvent.DESELECTED) {
                selectedDataMaps.remove(panel.getDataMap());
            }
            setGenerateButtonDisabled();
        });

        panel.getToConfigButton().addActionListener(action -> showConfig(panel.getDataMap()));

        view.getGenerationPanel().getSelectAll().addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                getGeneratorsPanels().forEach((key, value) -> {
                    if (value.getCheckConfig().isEnabled()) {
                        value.getCheckConfig().setSelected(true);
                    }
                });
            } else if(e.getStateChange() == ItemEvent.DESELECTED) {
                getGeneratorsPanels().forEach((key, value) -> {
                    if(value.getCheckConfig().isEnabled()) {
                        value.getCheckConfig().setSelected(false);
                    }
                });
            }
            setGenerateButtonDisabled();
        });
    }

    public abstract void showConfig(DataMap dataMap);

    private void setGenerateButtonDisabled() {
        if(selectedDataMaps.size() == 0) {
            view.getGenerationPanel().getGenerateAll().setEnabled(false);
        } else {
            view.getGenerationPanel().getGenerateAll().setEnabled(true);
        }
    }

    private Collection<DataMap> getDataMaps() {
        Project project = projectController.getProject();
        return  ((DataChannelDescriptor) project.getRootNode()).getDataMaps();
    }

    public GeneratorsTab getView() {
        return view;
    }

    public ProjectController getProjectController() {
        return projectController;
    }

    ConcurrentMap<DataMap, GeneratorsPanel> getGeneratorsPanels() {
        return generatorsPanels;
    }

    Set<DataMap> getSelectedDataMaps() {
        return selectedDataMaps;
    }

    private void refreshSelectedMaps(Collection<DataMap> dataMaps) {
        selectedDataMaps.removeIf(dataMap -> !dataMaps.contains(dataMap));
    }
}
