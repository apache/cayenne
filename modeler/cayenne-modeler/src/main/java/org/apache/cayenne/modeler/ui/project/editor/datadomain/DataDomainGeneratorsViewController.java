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
package org.apache.cayenne.modeler.ui.project.editor.datadomain;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.event.model.DataMapListener;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class DataDomainGeneratorsViewController<T> implements DataMapListener {

    protected final static Logger LOGGER = LoggerFactory.getLogger(DataDomainGeneratorsViewController.class);

    protected final ProjectController controller;
    private final ConcurrentMap<DataMap, DataDomainGeneratorsPanel> generatorsPanels;
    private final Set<DataMap> selectedDataMaps;
    private final Class<T> type;
    private final boolean selectAllByDefault;

    protected DataDomainGeneratorsView view;

    public DataDomainGeneratorsViewController(ProjectController controller, Class<T> type, boolean selectAllByDefault) {
        this.generatorsPanels = new ConcurrentHashMap<>();
        this.selectedDataMaps = new HashSet<>();
        this.type = type;
        this.selectAllByDefault = selectAllByDefault;
        this.controller = controller;
        this.controller.addDataMapListener(this);
    }

    private boolean isSelectAllChecked() {
        return view.getGenerationPanel().getSelectAll().isSelected();
    }

    public abstract void runGenerators(Set<DataMap> dataMaps);

    void createPanels(){
        Collection<DataMap> dataMaps = getDataMaps();
        refreshSelectedMaps(dataMaps);
        generatorsPanels.clear();
        for(DataMap dataMap : dataMaps) {
            DataDomainGeneratorsPanel generatorPanel = new DataDomainGeneratorsPanel(controller.getApplication(),dataMap, "icon-datamap.png", type);
            initListenersForPanel(generatorPanel);
            generatorsPanels.put(dataMap, generatorPanel);
        }
        selectedDataMaps.forEach(dataMap -> {
            if(generatorsPanels.get(dataMap) != null) {
                DataDomainGeneratorsPanel currPanel = generatorsPanels.get(dataMap);
                currPanel.getCheckConfig().setSelected(true);
            }
        });
        if(selectedDataMaps.isEmpty() && selectAllByDefault) {
            DataDomainGeneratorsView.TopGeneratorPanel topGeneratorPanel = view.getGenerationPanel();
            topGeneratorPanel.getSelectAll().setSelected(true);
            topGeneratorPanel.getGenerateAll().setEnabled(true);
            for (Map.Entry<DataMap, DataDomainGeneratorsPanel> entry : generatorsPanels.entrySet()) {
                entry.getValue().getCheckConfig().setSelected(true);
            }
        }
    }

    private void initListenersForPanel(DataDomainGeneratorsPanel panel) {
        panel.getCheckConfig().addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                selectedDataMaps.add(panel.getDataMap());
                if(selectedDataMaps.size() == generatorsPanels.size()) {
                    view.getGenerationPanel().getSelectAll().setSelected(true);
                }
            } else if(e.getStateChange() == ItemEvent.DESELECTED) {
                selectedDataMaps.remove(panel.getDataMap());
                view.getGenerationPanel().getSelectAll().setSelected(false);
            }
            setGenerateButtonDisabled();
        });

        panel.getToConfigButton().addActionListener(action -> showConfig(panel.getDataMap()));

        view.getGenerationPanel().getSelectAll().addActionListener(e -> {
            boolean isSelected = view.getGenerationPanel().getSelectAll().isSelected();
            if(isSelected) {
                getGeneratorsPanels().forEach((key, value) -> {
                    if (value.getCheckConfig().isEnabled()) {
                        value.getCheckConfig().setSelected(true);
                    }
                });
            } else {
                getGeneratorsPanels().forEach((key, value) -> {
                    if (value.getCheckConfig().isEnabled()) {
                        value.getCheckConfig().setSelected(false);
                    }
                });
            }
            setGenerateButtonDisabled();
        });
    }

    public abstract void showConfig(DataMap dataMap);

    private void setGenerateButtonDisabled() {
        if(selectedDataMaps.isEmpty()) {
            view.getGenerationPanel().getGenerateAll().setEnabled(false);
        } else {
            view.getGenerationPanel().getGenerateAll().setEnabled(true);
        }
    }

    private Collection<DataMap> getDataMaps() {
        Project project = controller.getProject();
        return  ((DataChannelDescriptor) project.getRootNode()).getDataMaps();
    }

    public DataDomainGeneratorsView getView() {
        return view;
    }

    public ProjectController getController() {
        return controller;
    }

    ConcurrentMap<DataMap, DataDomainGeneratorsPanel> getGeneratorsPanels() {
        return generatorsPanels;
    }

    Set<DataMap> getSelectedDataMaps() {
        return selectedDataMaps;
    }

    private void refreshSelectedMaps(Collection<DataMap> dataMaps) {
        selectedDataMaps.removeIf(dataMap -> !dataMaps.contains(dataMap));
    }

    @Override
    public void dataMapAdded(DataMapEvent e) {
        DataDomainGeneratorsPanel generatorPanel = new DataDomainGeneratorsPanel(controller.getApplication(),e.getDataMap(), "icon-datamap.png", type);
        initListenersForPanel(generatorPanel);
        generatorsPanels.put(e.getDataMap(), generatorPanel);
        if(isSelectAllChecked()) {
            generatorPanel.getCheckConfig().setSelected(true);
            selectedDataMaps.add(e.getDataMap());
        }
    }

    @Override
    public void dataMapRemoved(DataMapEvent e) {
        selectedDataMaps.remove(e.getDataMap());
        generatorsPanels.remove(e.getDataMap());
    }

    @Override
    public void dataMapChanged(DataMapEvent e) {
    }
}
