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

import javax.swing.Icon;
import javax.swing.JLabel;
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
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.spi.ModuleLoader;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.gen.ClientClassGenerationAction;
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
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.tools.CayenneToolsModuleProvider;
import org.apache.cayenne.validation.ValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.1
 * A controller for the class generator dialog.
 */
public class CodeGeneratorController extends CayenneController implements ObjEntityListener, EmbeddableListener, DataMapListener {
    /**
     * Logger to print stack traces
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorDebugDialog.class);
    private static final Icon ERROR_ICON = ModelerUtil.buildIcon("icon-error.png");
    public static final String SELECTED_PROPERTY = "selected";

    protected ProjectController projectController;
    protected ValidationResult lastValidationResult;

    protected List<Object> classes;
    protected SelectionModel selectionModel;
    protected Object currentClass;

    protected boolean initFromModel;

    protected CodeGeneratorPane view;

    protected ClassesTabController classesSelector;
    protected GeneratorTabController generatorSelector;
    private ConcurrentMap<DataMap, GeneratorController> prevGeneratorController;

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
        prepareClasses(projectController.getCurrentDataMap());
        DataMap dataMap = projectController.getCurrentDataMap();
        classesSelectedAction();
        CgenConfiguration cgenConfiguration = createConfiguration();
        GeneratorController modeController = prevGeneratorController.get(dataMap) != null
                        ? prevGeneratorController.get(dataMap)
                        : isDefaultConfig(cgenConfiguration)
                            ? cgenConfiguration.isClient()
                                ? generatorSelector.getClientGeneratorController()
                                : generatorSelector.getStandartController()
                            : generatorSelector.getCustomModeController();

        prevGeneratorController.put(dataMap, modeController);
        generatorSelector.setSelectedController(modeController);
        classesSelector.startup();
        initFromModel = false;
        validate(modeController);
    }

    private boolean isDefaultConfig(CgenConfiguration cgenConfiguration) {
        return cgenConfiguration.isMakePairs() && cgenConfiguration.isUsePkgPath() &&
                !cgenConfiguration.isOverwrite() && !cgenConfiguration.isCreatePKProperties() &&
                !cgenConfiguration.isCreatePropertyNames() && cgenConfiguration.getOutputPattern().equals("*.java") &&
                (cgenConfiguration.getTemplate().equals(ClassGenerationAction.SUBCLASS_TEMPLATE) ||
                        cgenConfiguration.getTemplate().equals(ClientClassGenerationAction.SUBCLASS_TEMPLATE)) &&
                (cgenConfiguration.getSuperTemplate().equals(ClassGenerationAction.SUPERCLASS_TEMPLATE) ||
                        cgenConfiguration.getSuperTemplate().equals(ClientClassGenerationAction.SUPERCLASS_TEMPLATE)) &&
                (cgenConfiguration.getSuperPkg() == null || cgenConfiguration.getSuperPkg().isEmpty());

    }

    private void initListeners(){
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
        builder.bindToAction(this, "classesSelectedAction()", SELECTED_PROPERTY);
        builder.bindToAction(generatorSelector, "generatorSelectedAction()",
                GeneratorTabController.GENERATOR_PROPERTY);

        generatorSelectedAction();
    }

    public void generatorSelectedAction() {
        GeneratorController controller = generatorSelector.getGeneratorController();
        validate(controller);

        Predicate<Object> predicate = controller != null
                ? controller.getDefaultClassFilter()
                : o -> false;

        updateSelection(predicate);
        classesSelector.classSelectedAction();
    }

    public void classesSelectedAction() {
        if(!isInitFromModel()) {
            getProjectController().setDirty(true);
        }
    }

    public void generateAction() {
        CgenConfiguration cgenConfiguration = createConfiguration();
        ClassGenerationAction generator = DIBootstrap
                .createInjector(new ModuleLoader()
                        .load(CayenneToolsModuleProvider.class))
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
    public CgenConfiguration createConfiguration() {
        DataMap map = projectController.getCurrentDataMap();
        CgenConfiguration cgenConfiguration = projectController.getApplication().getMetaData().get(map, CgenConfiguration.class);
        if(cgenConfiguration != null){
            addToSelectedEntities(cgenConfiguration.getEntities());
            addToSelectedEmbeddables(cgenConfiguration.getEmbeddables());
            cgenConfiguration.setForce(true);
            return cgenConfiguration;
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
                return null;
            }
        }
        // not a directory
        if (!Files.isDirectory(basePath)) {
            JOptionPane.showMessageDialog(this.getView(), basePath + " is not a valid directory.");
            return null;
        }

        cgenConfiguration.setRootPath(basePath);
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
        getApplication().getMetaData().add(map, cgenConfiguration);
        projectController.setDirty(true);

        return cgenConfiguration;
    }

    public List<Object> getClasses() {
        return classes;
    }

    public void validate(GeneratorController validator) {
        ValidationResult validationResult = new ValidationResult();
        if (validator != null) {
            for (Object classObj : classes) {
                if (classObj instanceof ObjEntity) {
                    validator.validateEntity(validationResult, (ObjEntity) classObj, false);
                } else if (classObj instanceof Embeddable) {
                    validator.validateEmbeddable(validationResult, (Embeddable) classObj);
                }
            }
        }
        this.lastValidationResult = validationResult;
    }

    public boolean updateSelection(Predicate<Object> predicate) {
        boolean modified = selectionModel.updateSelection(predicate, classes);

        for (Object classObj : classes) {
            if(classObj instanceof DataMap) {
                boolean select = predicate.test(classObj);
                updateArtifactGenerationMode(classObj, select);
            }
        }

        if (modified) {
            firePropertyChange(SELECTED_PROPERTY, null, null);
        }

        return modified;
    }

    private void updateArtifactGenerationMode(Object classObj, boolean selected) {
        DataMap dataMap = (DataMap) classObj;
        CgenConfiguration cgenConfiguration = projectController.getApplication().getMetaData().get(dataMap, CgenConfiguration.class);
        if(selected) {
            cgenConfiguration.setArtifactsGenerationMode("all");
        } else {
            cgenConfiguration.setArtifactsGenerationMode("entity");
        }
    }

    public boolean isSelected() {
        return selectionModel.isSelected(currentClass);
    }

    public void setSelected(boolean selectedFlag) {
        if (currentClass instanceof DataMap) {
            updateArtifactGenerationMode(currentClass, selectedFlag);
        }
        if (selectionModel.setSelected(currentClass, selectedFlag)) {
            firePropertyChange(SELECTED_PROPERTY, null, null);
        }
    }

    /**
     * Returns the first encountered validation problem for an antity matching the name or
     * null if the entity is valid or the entity is not present.
     */
    public JLabel getProblem(Object obj) {
        String name = null;
        if (obj instanceof ObjEntity) {
            name = ((ObjEntity) obj).getName();
        } else if (obj instanceof Embeddable) {
            name = ((Embeddable) obj).getClassName();
        }

        ValidationFailure validationFailure = null;
        if (lastValidationResult != null) {
            List<ValidationFailure> failures = lastValidationResult.getFailures(name);
            if (!failures.isEmpty()) {
                validationFailure = failures.get(0);
            }
        }

        JLabel labelIcon = new JLabel();
        labelIcon.setVisible(true);
        if(validationFailure != null) {
            labelIcon.setIcon(ERROR_ICON);
            labelIcon.setToolTipText(validationFailure.getDescription());
        }
        return labelIcon;
    }

    public JLabel getItemName(Object obj) {
        String className;
        Icon icon;
        if (obj instanceof Embeddable) {
            className = ((Embeddable) obj).getClassName();
            icon = CellRenderers.iconForObject(new Embeddable());
        } else if(obj instanceof ObjEntity) {
            className = ((ObjEntity) obj).getName();
            icon = CellRenderers.iconForObject(new ObjEntity());
        } else {
            className = ((DataMap) obj).getName();
            icon = CellRenderers.iconForObject(new DataMap());
        }
        JLabel labelIcon = new JLabel();
        labelIcon.setIcon(icon);
        labelIcon.setVisible(true);
        labelIcon.setText(className);
        return labelIcon;
    }

    public Object getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(Object currentClass) {
        this.currentClass = currentClass;
    }

    public void updateSelectedEntities(){
        updateEntities();
        updateEmbeddables();
    }

    CgenConfiguration getCurrentConfiguration() {
        DataMap map = getProjectController().getCurrentDataMap();
        return projectController.getApplication().getMetaData().get(map, CgenConfiguration.class);
    }

    private void updateEntities() {
        CgenConfiguration cgenConfiguration = getCurrentConfiguration();
        if(cgenConfiguration != null) {
            cgenConfiguration.getEntities().clear();
            for(ObjEntity entity: selectionModel.getSelectedEntities(classes)) {
                cgenConfiguration.loadEntity(entity);
            }
        }
    }

    private void updateEmbeddables() {
        CgenConfiguration cgenConfiguration = getCurrentConfiguration();
        if(cgenConfiguration != null) {
            cgenConfiguration.getEmbeddables().clear();
            for(Embeddable embeddable : selectionModel.getSelectedEmbeddables(classes)) {
                cgenConfiguration.loadEmbeddable(embeddable.getClassName());
            }
        }
    }

    private void addToSelectedEntities(Collection<String> entities) {
        selectionModel.addSelectedEntities(entities);
        updateEntities();
    }

    void addEntity(DataMap dataMap, ObjEntity objEntity) {
        prepareClasses(dataMap);
        selectionModel.addSelectedEntity(objEntity.getName());
        CgenConfiguration cgenConfiguration = getCurrentConfiguration();
        if(cgenConfiguration != null) {
            cgenConfiguration.loadEntity(objEntity);
        }
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
    public void objEntityChanged(EntityEvent e) {}

    @Override
    public void objEntityAdded(EntityEvent e) {
        addEntity(e.getEntity().getDataMap(), (ObjEntity) e.getEntity());
    }

    @Override
    public void objEntityRemoved(EntityEvent e) {
        selectionModel.removeFromSelectedEntities((ObjEntity) e.getEntity());
        DataMap map = e.getEntity().getDataMap();
        CgenConfiguration cgenConfiguration = projectController.getApplication().getMetaData().get(map, CgenConfiguration.class);
        if(cgenConfiguration != null) {
            cgenConfiguration.getEntities().remove(e.getEntity().getName());
        }
    }

    @Override
    public void embeddableChanged(EmbeddableEvent e, DataMap map) {}

    @Override
    public void embeddableAdded(EmbeddableEvent e, DataMap map) {
        prepareClasses(map);
        String embeddableClassName = e.getEmbeddable().getClassName();
        selectionModel.addSelectedEmbeddable(embeddableClassName);
        CgenConfiguration cgenConfiguration = getCurrentConfiguration();
        if(cgenConfiguration != null) {
            cgenConfiguration.loadEmbeddable(embeddableClassName);
        }
    }

    @Override
    public void embeddableRemoved(EmbeddableEvent e, DataMap map) {
        selectionModel.removeFromSelectedEmbeddables(e.getEmbeddable());
        CgenConfiguration cgenConfiguration = projectController.getApplication().getMetaData().get(map, CgenConfiguration.class);
        if(cgenConfiguration != null) {
            cgenConfiguration.getEmbeddables().remove(e.getEmbeddable().getClassName());
        }
    }

    @Override
    public void dataMapChanged(DataMapEvent e) {
        if(e.getSource() instanceof DbImportController) {
            CgenConfiguration cgenConfiguration = getCurrentConfiguration();
            if(cgenConfiguration != null) {
                for(ObjEntity objEntity : e.getDataMap().getObjEntities()) {
                    if(!cgenConfiguration.getExcludeEntityArtifacts().contains(objEntity.getName())) {
                        addEntity(cgenConfiguration.getDataMap(), objEntity);
                    }
                }
            }
        }
    }

    @Override
    public void dataMapAdded(DataMapEvent e) {}

    @Override
    public void dataMapRemoved(DataMapEvent e) {}
}
