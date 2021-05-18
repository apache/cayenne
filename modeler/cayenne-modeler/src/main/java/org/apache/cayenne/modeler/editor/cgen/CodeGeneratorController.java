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

import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.configuration.event.DataMapListener;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.ModuleLoader;
import org.apache.cayenne.gen.CgenConfiguration;
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
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.tools.ToolsInjectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.1
 * A controller for the class generator dialog.
 */
public class CodeGeneratorController extends CayenneController implements ObjEntityListener, EmbeddableListener, DataMapListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorDebugDialog.class);

    protected final ProjectController projectController;
    protected final List<Object> classes;
    protected final SelectionModel selectionModel;
    protected final CodeGeneratorPane view;
    protected final ClassesTabController classesSelector;
    protected final GeneratorTabController generatorSelector;
    protected final ConcurrentMap<DataMap, GeneratorController> prevGeneratorController;

    private Object currentClass;
    private CgenConfiguration cgenConfiguration;
    private boolean initFromModel;

    public CodeGeneratorController(ProjectController projectController) {
        super(projectController);
        this.classesSelector = new ClassesTabController(this);
        this.generatorSelector = new GeneratorTabController(this);
        this.view = new CodeGeneratorPane(generatorSelector.getView(), classesSelector.getView());
        this.prevGeneratorController = new ConcurrentHashMap<>();
        this.projectController = projectController;
        this.classes = new ArrayList<>();
        this.selectionModel = new SelectionModel();
        initBindings();
        initListeners();
    }

    public void initFromModel() {
        initFromModel = true;
        DataMap dataMap = projectController.getCurrentDataMap();

        prepareClasses(dataMap);
        createConfiguration(dataMap);

        GeneratorController modeController = prevGeneratorController.get(dataMap);
        if (modeController == null) {
            if (cgenConfiguration.isDefault()) {
                modeController = cgenConfiguration.isClient()
                        ? generatorSelector.getClientGeneratorController()
                        : generatorSelector.getStandartController();
            } else {
                modeController = generatorSelector.getCustomModeController();
            }
        }

        prevGeneratorController.put(dataMap, modeController);
        generatorSelector.setSelectedController(modeController);
        classesSelector.startup();
        initFromModel = false;
        classesSelector.validate(classes);
    }

    private void initListeners() {
        projectController.addObjEntityListener(this);
        projectController.addEmbeddableListener(this);
        projectController.addDataMapListener(this);
    }

    @Override
    public CodeGeneratorPane getView() {
        return view;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);

        builder.bindToAction(view.getGenerateButton(), "generateAction()");
        builder.bindToAction(generatorSelector, "generatorSelectedAction()",
                GeneratorTabController.GENERATOR_PROPERTY);

        generatorSelectedAction();
    }

    public void generatorSelectedAction() {
        GeneratorController controller = generatorSelector.getGeneratorController();
        classesSelector.validate(classes);

        Predicate<Object> defaultPredicate = object -> {
            if (object instanceof ObjEntity) {
                return classesSelector.getProblem(((ObjEntity) object).getName()) == null;
            }

            if (object instanceof Embeddable) {
                return classesSelector.getProblem(((Embeddable) object).getClassName()) == null;
            }
            return false;
        };

        Predicate<Object> predicate = controller != null
                ? defaultPredicate
                : o -> false;

        updateSelection(predicate);
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
        } catch (Exception e) {
            LOGGER.error("Error generating classes", e);
            JOptionPane.showMessageDialog(
                    this.getView(),
                    "Error generating classes - " + e.getMessage());
        }
    }

    public ConcurrentMap<DataMap, GeneratorController> getPrevGeneratorController() {
        return prevGeneratorController;
    }

    public void enableGenerateButton(boolean enable) {
        view.getGenerateButton().setEnabled(enable);
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
    public void createConfiguration(DataMap map) {
        cgenConfiguration = projectController.getApplication().getMetaData().get(map, CgenConfiguration.class);
        if (cgenConfiguration != null) {
            addToSelectedEntities(cgenConfiguration.getEntities());
            addToSelectedEmbeddables(cgenConfiguration.getEmbeddables());
            cgenConfiguration.setForce(true);
            return;
        }

        cgenConfiguration = new CgenConfiguration(false);
        cgenConfiguration.setForce(true);
        cgenConfiguration.setDataMap(map);

        Path basePath = Paths.get(ModelerUtil.initOutputFolder());

        // TODO: this should be done in actual generation, not here
        // no such folder
        if (!Files.exists(basePath)) {
            try {
                Files.createDirectories(basePath);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(getView(), "Can't create directory. Select a different one.");
                return;
            }
        }
        // not a directory
        if (!Files.isDirectory(basePath)) {
            JOptionPane.showMessageDialog(this.getView(), basePath + " is not a valid directory.");
            return;
        }

        if (map.getLocation() != null) {
            cgenConfiguration.setRootPath(basePath);
        }
        Preferences preferences = application.getPreferencesNode(GeneralPreferences.class, "");
        if (preferences != null) {
            cgenConfiguration.setEncoding(preferences.get(GeneralPreferences.ENCODING_PREFERENCE, null));
        }

        addToSelectedEntities(map.getObjEntities()
                .stream()
                .map(Entity::getName)
                .collect(Collectors.toList()));
        addToSelectedEmbeddables(map.getEmbeddables()
                .stream()
                .map(Embeddable::getClassName)
                .collect(Collectors.toList()));
    }

    public List<Object> getClasses() {
        return classes;
    }

    public boolean updateSelection(Predicate<Object> predicate) {
        boolean modified = selectionModel.updateSelection(predicate, classes);

        for (Object classObj : classes) {
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
        CgenConfiguration existingConfig = projectController.getApplication().getMetaData().get(map, CgenConfiguration.class);
        if (existingConfig == null) {
            getApplication().getMetaData().add(map, cgenConfiguration);
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
                cgenConfiguration.loadEmbeddable(embeddable.getClassName());
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
        String embeddableClassName = e.getEmbeddable().getClassName();
        selectionModel.addSelectedEmbeddable(embeddableClassName);
        if (cgenConfiguration != null) {
            cgenConfiguration.loadEmbeddable(embeddableClassName);
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
                    if (!cgenConfiguration.getExcludeEntityArtifacts().contains(objEntity.getName())) {
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
}
