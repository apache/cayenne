package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ItemEvent;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public abstract class AdditionalTabController {

    public static Logger logObj = LoggerFactory.getLogger(ErrorDebugDialog.class);
    public ProjectController projectController;
    public AdditionalTab view;

    public ConcurrentMap<DataMap, TabPanel> generatorsPanels;
    public Set<DataMap> selectedDataMaps;

    public String icon;

    public abstract void runGenerators(Set<DataMap> dataMap);

    public void createPanels(){
        Collection<DataMap> dataMaps = getDataMaps();
        generatorsPanels.clear();
        for(DataMap dataMap : dataMaps) {
            TabPanel cgenPanel = new TabPanel(dataMap, "icon-datamap.png");
            initListenersForPanel(cgenPanel);
            generatorsPanels.put(dataMap, cgenPanel);
        }
        selectedDataMaps.forEach(dataMap -> {
            if(generatorsPanels.get(dataMap) != null) {
                TabPanel currPanel = generatorsPanels.get(dataMap);
                currPanel.getCheckConfig().setSelected(true);
            }
        });
    }

    private void initListenersForPanel(TabPanel cgenPanel) {
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

    public abstract void showConfig(DataMap dataMap);

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

    public AdditionalTab getView() {
        return view;
    }

    public abstract CgenConfiguration createConfiguration(DataMap dataMap);

    public ProjectController getProjectController() {
        return projectController;
    }

    ConcurrentMap<DataMap, TabPanel> getGeneratorsPanels() {
        return generatorsPanels;
    }

    Set<DataMap> getSelectedDataMaps() {
        return selectedDataMaps;
    }
}
