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

package org.apache.cayenne.modeler.editor.cgen;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.configuration.event.DataMapListener;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EmbeddableListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ErrorDebugDialog;
import org.apache.cayenne.modeler.dialog.pref.GeneralPreferences;
import org.apache.cayenne.modeler.editor.DbImportController;
import org.apache.cayenne.modeler.event.ProjectSavedEvent;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.tools.ToolsInjectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Main controller for the code generation UI.
 *
 * @since 4.1
 */
public class CgenController extends CayenneController implements ObjEntityListener, EmbeddableListener, DataMapListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorDebugDialog.class);
    protected final ProjectController projectController;
    protected final Set<ConfigurationNode> classes;
    protected final SelectionModel selectionModel;
    protected final CgenPane view;
    protected CgenArtefactSelectorController classesSelector;
    private final CgenConfigController cgenConfigController;
    private CgenConfigList cgenConfigList;
    private Object currentClass;
    private CgenConfiguration cgenConfiguration;

    private DataMap dataMap;
    private boolean initFromModel;

    public CgenController(ProjectController projectController) {
        super(projectController);
        this.cgenConfigController = new CgenConfigController(this);
        this.classesSelector = new CgenArtefactSelectorController(this);
        this.view = new CgenPane(cgenConfigController.getView(), classesSelector.getView());
        this.projectController = projectController;
        this.classes = new TreeSet<>(
                Comparator.comparing((ConfigurationNode o) -> o.acceptVisitor(TYPE_GETTER))
                        .thenComparing(o -> o.acceptVisitor(NAME_GETTER))
        );
        this.selectionModel = new SelectionModel();
        initBindings();
        initListeners();
    }

    private void initConfigurationsComboBox() {
        view.getConfigurationsComboBox().removeAllItems();
        cgenConfigList.getNames().forEach(n -> view.getConfigurationsComboBox().addItem(n));
    }

    public void initFromModel() {
        initFromModel = true;
        dataMap = projectController.getCurrentDataMap();
        prepareClasses(dataMap);
        initCgenConfigurations();
        initConfigurationsComboBox();
        setConfiguration((String) view.getConfigurationsComboBox().getSelectedItem());
        cgenConfigController.initForm(cgenConfiguration);
        addConfigurationComboBoxListener();
        classesSelector.startup();
        initFromModel = false;
        classesSelector.validate(classes);
    }

    private void addConfigurationComboBoxListener() {
        view.getConfigurationsComboBox().addActionListener(e -> {
            selectionModel.clearAll();
            setConfiguration((String) view.getConfigurationsComboBox().getSelectedItem());
            cgenConfigController.initForm(cgenConfiguration);
            classesSelector.initBindings();
            classesSelector.validate(classes);
        });
    }

    private void initCgenConfigurations() {
        cgenConfigList = projectController.getApplication().getMetaData().get(dataMap, CgenConfigList.class);
        if (cgenConfigList == null) {
            cgenConfigList = new CgenConfigList();
            cgenConfigList.add(createDefaultCgenConfiguration(dataMap));
            projectController.getApplication().getMetaData().add(dataMap, cgenConfigList);
        }
    }

    private void initListeners() {
        projectController.addObjEntityListener(this);
        projectController.addEmbeddableListener(this);
        projectController.addDataMapListener(this);
        projectController.addProjectSavedListener(this::onProjectSaved);
    }

    @Override
    public CgenPane getView() {
        return view;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);
        builder.bindToAction(view.getGenerateButton(), "generateAction()");
        builder.bindToAction(view.getAddConfigBtn(), "addConfigAction()");
        builder.bindToAction(view.getEditConfigBtn(), "editConfigAction()");
        builder.bindToAction(view.getRemoveConfigBtn(), "removeConfigAction()");
        generatorSelectedAction();
    }

    public void generatorSelectedAction() {
        classesSelector.validate(classes);
        updateSelection(defaultPredicate);
        classesSelector.classSelectedAction();
    }

    @SuppressWarnings("unused")
    public void generateAction() {
        ClassGenerationAction generator = new ToolsInjectorBuilder()
                .addModule(binder
                        -> binder.bind(DataChannelMetaData.class)
                        .toInstance(projectController.getApplication().getMetaData()))
                .create()
                .getInstance(ClassGenerationActionFactory.class)
                .createAction(cgenConfiguration);

        try {
            generator.prepareArtifacts();
            generator.execute();
            JOptionPane.showMessageDialog(
                    this.getView(),
                    "Class generation finished");
        } catch (CayenneRuntimeException e) {
            LOGGER.error("Error generating classes", e);
            JOptionPane.showMessageDialog(
                    this.getView(),
                    "Error generating classes - " + e.getUnlabeledMessage());
        } catch (Exception e) {
            LOGGER.error("Error generating classes", e);
            JOptionPane.showMessageDialog(
                    this.getView(),
                    "Error generating classes - " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public void addConfigAction() {
        String name = JOptionPane.showInputDialog(
                view,
                "Type the name for new cgenConfiguration",
                view.getConfigurationsComboBox().getSelectedItem()
        );
        CgenConfiguration configuration = createDefaultCgenConfiguration(dataMap);
        if (name != null) {
            if (configuration != null && !cgenConfigList.isExist(name) && !name.isEmpty()) {
                configuration.setName(name);
                cgenConfigList.add(configuration);
                view.getConfigurationsComboBox().addItem(name);
                view.getConfigurationsComboBox().setSelectedItem(name);
            } else {
                JOptionPane.showMessageDialog(
                        this.getView(),
                        "Can't create new configuration, same name is already exist or empty");
            }
        }
    }

    @SuppressWarnings("unused")
    public void editConfigAction() {
        String name = JOptionPane.showInputDialog(
                view,
                "Type the new name for cgenConfiguration",
                view.getConfigurationsComboBox().getSelectedItem()
        );
        if (name != null) {
            if (!cgenConfigList.isExist(name) && !name.isEmpty()) {
                cgenConfiguration.setName(name);
                view.getConfigurationsComboBox().removeItem(view.getConfigurationsComboBox().getSelectedItem());
                view.getConfigurationsComboBox().addItem(name);
                view.getConfigurationsComboBox().setSelectedItem(name);
            } else {
                JOptionPane.showMessageDialog(
                        this.getView(),
                        "Can't rename configuration, name is already exist or empty");
            }
        }
    }

    @SuppressWarnings("unused")
    public void removeConfigAction() {
        int result = JOptionPane.showConfirmDialog(
                view,
                "Configuration will be remove\n" +
                        "               Are you sure?",
                "Delete cgenConfiguration",
                JOptionPane.YES_NO_OPTION
        );
        if (result == JOptionPane.OK_OPTION) {
            if (view.getConfigurationsComboBox().getItemCount() > 1) {
                cgenConfigList.removeByName(cgenConfiguration.getName());
                view.getConfigurationsComboBox().removeItem(view.getConfigurationsComboBox().getSelectedItem());
                view.getConfigurationsComboBox().setSelectedIndex(0);
            } else {
                JOptionPane.showMessageDialog(
                        this.getView(),
                        "At least one configuration must exist");
            }
        }
    }

    public void updateGenerateButton(){
        boolean isOutputPathValid = cgenConfigController.getView().isDataValid();
        view.getGenerateButton().setEnabled(!selectionModel.isModelEmpty()&& isOutputPathValid);
    }

    private void prepareClasses(DataMap dataMap) {
        classes.clear();
        classes.add(dataMap);
        classes.addAll(dataMap.getObjEntities());
        classes.addAll(dataMap.getEmbeddables());
        selectionModel.initCollectionsForSelection(dataMap);
    }

    /**
     * Creates a class generator for provided selections.
     */
    public void setConfiguration(String selectedConfig) {
        cgenConfiguration = cgenConfigList.getByName(selectedConfig);
        if (cgenConfiguration != null) {
            addToSelectedEntities(cgenConfiguration.getEntities());
            addToSelectedEmbeddables(cgenConfiguration.getEmbeddables());
            cgenConfiguration.setForce(true);
            return;
        }

        cgenConfiguration = createDefaultCgenConfiguration(dataMap);
        addToSelectedEntities(dataMap.getObjEntities()
                .stream()
                .map(Entity::getName)
                .collect(Collectors.toList()));
        addToSelectedEmbeddables(dataMap.getEmbeddables()
                .stream()
                .map(Embeddable::getClassName)
                .collect(Collectors.toList()));
    }

    private CgenConfiguration createDefaultCgenConfiguration(DataMap map) {
        CgenConfiguration configuration = new CgenConfiguration();
        configuration.setName(CgenConfigList.DEFAULT_CONFIG_NAME);
        configuration.setForce(true);
        configuration.setDataMap(map);

        map.getObjEntities().forEach(configuration::loadEntity);
        map.getEmbeddables().forEach(configuration::loadEmbeddable);
        if (map.getLocation() != null) {
            Path basePath = Paths.get(ModelerUtil.initOutputFolder());
            configuration.updateOutputPath(basePath);
        }
        Preferences preferences = application.getPreferencesNode(GeneralPreferences.class, "");
        if (preferences != null) {
            configuration.setEncoding(preferences.get(GeneralPreferences.ENCODING_PREFERENCE, null));
        }
        return configuration;
    }

    public Set<?> getClasses() {
        return classes;
    }

    public boolean updateSelection(Predicate<ConfigurationNode> predicate) {
        boolean modified = selectionModel.updateSelection(predicate, classes);

        for (ConfigurationNode classObj : classes) {
            if (classObj instanceof DataMap) {
                boolean selected = predicate.test(classObj);
                updateArtifactGenerationMode(selected);
            }
        }

        return modified;
    }

    private void updateArtifactGenerationMode(boolean selected) {
        if (selected) {
            cgenConfiguration.setArtifactsGenerationMode("all");
        } else {
            cgenConfiguration.setArtifactsGenerationMode("entity");
        }
        checkCgenConfigDirty();
    }

    public boolean isSelected() {
        return selectionModel.isSelected(currentClass);
    }

    public void setSelected(boolean selectedFlag) {
        if (currentClass instanceof DataMap) {
            updateArtifactGenerationMode(selectedFlag);
        }
        selectionModel.setSelected(currentClass, selectedFlag);
    }

    public void setCurrentClass(Object currentClass) {
        this.currentClass = currentClass;
    }

    public void updateSelectedEntities() {
        updateEntities();
        updateEmbeddables();
    }

    public void checkCgenConfigDirty() {
        if (initFromModel || cgenConfiguration == null) {
            return;
        }

        DataMap map = projectController.getCurrentDataMap();
        CgenConfigList existingConfigurations = projectController.getApplication().getMetaData().get(map, CgenConfigList.class);
        if (existingConfigurations == null) {
            cgenConfigList.add(cgenConfiguration);
            getApplication().getMetaData().add(map, cgenConfigList);
        }

        projectController.setDirty(true);
    }

    private void updateEntities() {
        if (cgenConfiguration != null) {
            cgenConfiguration.getEntities().clear();
            for (ObjEntity entity : selectionModel.getSelectedEntities(classes)) {
                cgenConfiguration.loadEntity(entity);
            }
        }
        checkCgenConfigDirty();
    }

    private void updateEmbeddables() {
        if (cgenConfiguration != null) {
            cgenConfiguration.getEmbeddables().clear();
            for (Embeddable embeddable : selectionModel.getSelectedEmbeddables(classes)) {
                cgenConfiguration.loadEmbeddable(embeddable);
            }
        }
        checkCgenConfigDirty();
    }

    private void addToSelectedEntities(Collection<String> entities) {
        selectionModel.addSelectedEntities(entities);
        updateEntities();
    }

    void addEntity(DataMap dataMap, ObjEntity objEntity) {
        prepareClasses(dataMap);
        selectionModel.addSelectedEntity(objEntity.getName());
        if (cgenConfiguration != null) {
            cgenConfiguration.loadEntity(objEntity);
        }
        checkCgenConfigDirty();
    }

    private void addToSelectedEmbeddables(Collection<String> embeddables) {
        selectionModel.addSelectedEmbeddables(embeddables);
        updateEmbeddables();
    }

    public int getSelectedEntitiesSize() {
        return selectionModel.getSelectedEntitiesCount();
    }

    public boolean isEntitiesSelected() {
        return selectionModel.getSelectedEntitiesCount() > 0;
    }

    public boolean isEmbeddableSelected() {
        return selectionModel.getSelecetedEmbeddablesCount() > 0;
    }

    public int getSelectedEmbeddablesSize() {
        return selectionModel.getSelecetedEmbeddablesCount();
    }

    public boolean isDataMapSelected() {
        return selectionModel.getSelectedDataMapsCount() > 0;
    }

    public ProjectController getProjectController() {
        return projectController;
    }

    public boolean isInitFromModel() {
        return initFromModel;
    }

    public void setInitFromModel(boolean initFromModel) {
        this.initFromModel = initFromModel;
    }

    public CgenConfigController getStandardModeController() {
        return cgenConfigController;
    }

    @Override
    public void objEntityChanged(EntityEvent e) {
    }

    @Override
    public void objEntityAdded(EntityEvent e) {
        addEntity(e.getEntity().getDataMap(), (ObjEntity) e.getEntity());
    }

    @Override
    public void objEntityRemoved(EntityEvent e) {
        selectionModel.removeFromSelectedEntities((ObjEntity) e.getEntity());
        if (cgenConfiguration != null) {
            cgenConfiguration.getEntities().remove(e.getEntity().getName());
        }
        checkCgenConfigDirty();
    }

    @Override
    public void embeddableChanged(EmbeddableEvent e, DataMap map) {
    }

    @Override
    public void embeddableAdded(EmbeddableEvent e, DataMap map) {
        prepareClasses(map);
        Embeddable embeddable = e.getEmbeddable();
        selectionModel.addSelectedEmbeddable(embeddable.getClassName());
        if (cgenConfiguration != null) {
            cgenConfiguration.loadEmbeddable(embeddable);
        }
        checkCgenConfigDirty();
    }

    @Override
    public void embeddableRemoved(EmbeddableEvent e, DataMap map) {
        selectionModel.removeFromSelectedEmbeddables(e.getEmbeddable());
        if (cgenConfiguration != null) {
            cgenConfiguration.getEmbeddables().remove(e.getEmbeddable().getClassName());
        }
        checkCgenConfigDirty();
    }

    @Override
    public void dataMapChanged(DataMapEvent e) {
        if (e.getSource() instanceof DbImportController) {
            if (cgenConfiguration != null) {
                for (ObjEntity objEntity : e.getDataMap().getObjEntities()) {
                    if (!cgenConfiguration.getExcludedEntityArtifacts().contains(objEntity.getName())) {
                        addEntity(cgenConfiguration.getDataMap(), objEntity);
                    }
                }
            }
            checkCgenConfigDirty();
        }
    }

    @Override
    public void dataMapAdded(DataMapEvent e) {
    }

    @Override
    public void dataMapRemoved(DataMapEvent e) {
    }

    public CgenConfiguration getCgenConfiguration() {
        return cgenConfiguration;
    }

    /**
     * Update cgen path if project is saved and no path is already set manually
     *
     * @param e event we are processing
     */
    public void onProjectSaved(ProjectSavedEvent e) {
        // update path input
        if (getStandardModeController() != null
                && getStandardModeController().getView() != null
                && cgenConfiguration != null) {
            getStandardModeController().getView().getOutputFolder().setText(cgenConfiguration.buildOutputPath().toString());
        }
    }

    private final Predicate<ConfigurationNode> defaultPredicate = o -> o.acceptVisitor(new BaseConfigurationNodeVisitor<Boolean>() {
        @Override
        public Boolean visitDataMap(DataMap dataMap) {
            return false;
        }

        @Override
        public Boolean visitObjEntity(ObjEntity entity) {
            return classesSelector.getProblem(entity.getName()) == null;
        }

        @Override
        public Boolean visitEmbeddable(Embeddable embeddable) {
            return classesSelector.getProblem(embeddable.getClassName()) == null;
        }
    });

    private static final ConfigurationNodeVisitor<Integer> TYPE_GETTER = new BaseConfigurationNodeVisitor<Integer>() {
        @Override
        public Integer visitDataMap(DataMap dataMap) {
            return 10;
        }

        @Override
        public Integer visitObjEntity(ObjEntity entity) {
            return 20;
        }

        @Override
        public Integer visitEmbeddable(Embeddable embeddable) {
            return 30;
        }
    };

    private static final ConfigurationNodeVisitor<String> NAME_GETTER = new BaseConfigurationNodeVisitor<String>() {
        @Override
        public String visitDataMap(DataMap dataMap) {
            return dataMap.getName();
        }

        @Override
        public String visitEmbeddable(Embeddable embeddable) {
            return embeddable.getClassName();
        }

        @Override
        public String visitObjEntity(ObjEntity entity) {
            return entity.getName();
        }
    };
}
