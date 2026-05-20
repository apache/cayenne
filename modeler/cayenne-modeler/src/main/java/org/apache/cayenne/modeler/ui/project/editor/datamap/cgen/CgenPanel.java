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

package org.apache.cayenne.modeler.ui.project.editor.datamap.cgen;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.gen.internal.Utils;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.event.model.DataMapListener;
import org.apache.cayenne.modeler.event.model.EmbeddableEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableListener;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityListener;
import org.apache.cayenne.modeler.event.model.ProjectAfterSaveEvent;
import org.apache.cayenne.modeler.pref.adapters.GeneralPrefs;
import org.apache.cayenne.modeler.project.CgenOps;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportResultDialog;
import org.apache.cayenne.tools.ToolsInjectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Editor panel for code generation. Top bar holds a configuration picker + Generate button;
 * the body splits into the per-class artefact selector (left) and the cgen options form (right).
 * Subscribes to project events to keep its model snapshot in sync with mapping edits.
 */
public class CgenPanel extends ProjectPanel
        implements ObjEntityListener, EmbeddableListener, DataMapListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CgenPanel.class);

    private final Set<ConfigurationNode> classes;
    private final SelectionModel selectionModel;
    private final CgenArtefactSelectorPanel classesSelector;
    private final CgenConfigPanel cgenConfigPanel;

    private final JButton generateButton;
    private final JComboBox<String> configurationsComboBox;
    private final JButton addConfigBtn;
    private final JButton editConfigBtn;
    private final JButton removeConfigBtn;

    private CgenConfigList cgenConfigList;
    private Object currentClass;
    private CgenConfiguration cgenConfiguration;

    private boolean initFromModel;

    public CgenPanel(ProjectSession session) {
        super(session);

        this.classes = new TreeSet<>(
                Comparator.comparing((ConfigurationNode o) -> o.acceptVisitor(TYPE_GETTER))
                        .thenComparing(o -> o.acceptVisitor(NAME_GETTER))
        );
        this.selectionModel = new SelectionModel();

        this.generateButton = new JButton("Generate");
        this.generateButton.setIcon(IconFactory.buildIcon("icon-gen_java.png"));
        this.generateButton.setEnabled(false);
        this.configurationsComboBox = new JComboBox<>();
        this.addConfigBtn = new JButton(IconFactory.buildIcon("icon-new.png"));
        this.addConfigBtn.setToolTipText("New configuration");
        this.editConfigBtn = new JButton(IconFactory.buildIcon("icon-edit.png"));
        this.editConfigBtn.setToolTipText("Rename configuration");
        this.removeConfigBtn = new JButton(IconFactory.buildIcon("icon-trash.png"));
        this.removeConfigBtn.setToolTipText("Remove configuration");

        this.cgenConfigPanel = new CgenConfigPanel(session, this);
        this.classesSelector = new CgenArtefactSelectorPanel(this);

        initLayout();
        initBindings();
        initListeners();

        session.addDataMapDisplayListener(e -> {
            DataMap map = e.getDataMap();
            if (map != null) {
                initFromModel(map);
            }
        });
    }

    public CgenConfiguration getCgenConfiguration() {
        return cgenConfiguration;
    }

    public CgenConfigPanel getStandardModeController() {
        return cgenConfigPanel;
    }

    public Set<?> getClasses() {
        return classes;
    }

    public boolean isSelected() {
        return selectionModel.isSelected(currentClass);
    }

    public boolean isSelected(Object item) {
        return selectionModel.isSelected(item);
    }

    public void setSelected(boolean selectedFlag) {
        if (currentClass instanceof DataMap) {
            updateArtifactGenerationMode(selectedFlag);
        }
        selectionModel.setSelected(currentClass, selectedFlag);
    }

    public void setSelected(Object item, boolean selectedFlag) {
        if (item instanceof DataMap) {
            updateArtifactGenerationMode(selectedFlag);
        }
        selectionModel.setSelected(item, selectedFlag);
    }

    public void setCurrentClass(Object currentClass) {
        this.currentClass = currentClass;
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

    public boolean isInitFromModel() {
        return initFromModel;
    }

    public void setInitFromModel(boolean initFromModel) {
        this.initFromModel = initFromModel;
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

    public void updateGenerateButton() {
        boolean isOutputPathValid = cgenConfigPanel.isDataValid();
        generateButton.setEnabled(!selectionModel.isModelEmpty() && isOutputPathValid);
    }

    public void updateSelectedEntities() {
        updateEntities();
        updateEmbeddables();
    }

    public void checkCgenConfigDirty() {
        if (initFromModel || cgenConfiguration == null) {
            return;
        }

        DataMap map = session.getSelectedDataMap();
        CgenConfigList existingConfigurations = app.getMetaData().get(map, CgenConfigList.class);
        if (existingConfigurations == null) {
            cgenConfigList.add(cgenConfiguration);
            app.getMetaData().add(map, cgenConfigList);
        }

        session.setDirty(true);
    }

    public void initFromModel(DataMap map) {
        initFromModel = true;
        prepareClasses(map);
        initCgenConfigurations(map);
        initConfigurationsComboBox();
        setConfiguration((String) configurationsComboBox.getSelectedItem());
        cgenConfigPanel.initForm(cgenConfiguration);
        classesSelector.startup();
        initFromModel = false;
        classesSelector.validate(classes);
    }

    /**
     * Builds a class generator for provided selections.
     */
    private void setConfiguration(String selectedConfig) {
        cgenConfiguration = cgenConfigList.getByName(selectedConfig);
        if (cgenConfiguration != null) {
            addToSelectedEntities(cgenConfiguration.getEntities());
            addToSelectedEmbeddables(cgenConfiguration.getEmbeddables());
            cgenConfiguration.setForce(true);
            return;
        }

        DataMap dataMap = session.getSelectedDataMap();
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

    private void initLayout() {
        setLayout(new BorderLayout());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        JScrollPane scrollPane = new JScrollPane(
                cgenConfigPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(150, 400));

        splitPane.setRightComponent(scrollPane);
        splitPane.setLeftComponent(classesSelector);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(buildConfigurationsPanel(), BorderLayout.WEST);
        topPanel.add(buildGeneratePanel(), BorderLayout.EAST);
        topPanel.setBorder(CgenConfigPanel.CGEN_PANEL_BORDER);

        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel buildConfigurationsPanel() {
        FormLayout layout = new FormLayout(
                "109dlu,3dlu,pref,3dlu,pref,3dlu,pref",
                "p");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(configurationsComboBox, cc.xy(1, 1));
        builder.add(addConfigBtn, cc.xy(3, 1));
        builder.add(editConfigBtn, cc.xy(5, 1));
        builder.add(removeConfigBtn, cc.xy(7, 1));
        return builder.getPanel();
    }

    private JPanel buildGeneratePanel() {
        FormLayout layout = new FormLayout("60dlu", "p");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(generateButton, cc.xy(1, 1));
        return builder.getPanel();
    }

    private void initConfigurationsComboBox() {
        configurationsComboBox.removeAllItems();
        cgenConfigList.getNames().forEach(configurationsComboBox::addItem);
    }

    private void initCgenConfigurations(DataMap dataMap) {
        cgenConfigList = app.getMetaData().get(dataMap, CgenConfigList.class);
        if (cgenConfigList == null) {
            cgenConfigList = new CgenConfigList();
            cgenConfigList.add(createDefaultCgenConfiguration(dataMap));
            app.getMetaData().add(dataMap, cgenConfigList);
        }
    }

    private void initListeners() {
        session.addObjEntityListener(this);
        session.addEmbeddableListener(this);
        session.addDataMapListener(this);
        session.addProjectSavedListener(this::onProjectSaved);
    }

    private void initBindings() {
        generateButton.addActionListener(e -> generateAction());
        addConfigBtn.addActionListener(e -> addConfigAction());
        editConfigBtn.addActionListener(e -> editConfigAction());
        removeConfigBtn.addActionListener(e -> removeConfigAction());
        configurationsComboBox.addActionListener(e -> {
            // ignore events fired while initFromModel() is rebuilding the combo box
            if (initFromModel) {
                return;
            }
            selectionModel.clearAll();
            setConfiguration((String) configurationsComboBox.getSelectedItem());
            cgenConfigPanel.initForm(cgenConfiguration);
            classesSelector.initBindings();
            classesSelector.validate(classes);
        });
        generatorSelectedAction();
    }

    private void generatorSelectedAction() {
        classesSelector.validate(classes);
        updateSelection(defaultPredicate);
        classesSelector.classSelectedAction();
    }

    private void generateAction() {
        ClassGenerationAction generator = new ToolsInjectorBuilder()
                .addModule(binder -> binder.bind(DataChannelMetaData.class).toInstance(app.getMetaData()))
                .create()
                .getInstance(ClassGenerationActionFactory.class)
                .createAction(cgenConfiguration);

        try {
            generator.prepareArtifacts();
            generator.execute();
            JOptionPane.showMessageDialog(this, "Class generation finished");
        } catch (CayenneRuntimeException e) {
            LOGGER.error("Error generating classes", e);
            JOptionPane.showMessageDialog(this, "Error generating classes - " + e.getUnlabeledMessage());
        } catch (Exception e) {
            LOGGER.error("Error generating classes", e);
            JOptionPane.showMessageDialog(this, "Error generating classes - " + e.getMessage());
        }
    }

    private void addConfigAction() {
        String name = JOptionPane.showInputDialog(
                this,
                "Type the name for new cgenConfiguration",
                configurationsComboBox.getSelectedItem());
        CgenConfiguration configuration = createDefaultCgenConfiguration(session.getSelectedDataMap());
        if (name != null) {
            if (!cgenConfigList.isExist(name) && !name.isEmpty()) {
                configuration.setName(name);
                cgenConfigList.add(configuration);
                configurationsComboBox.addItem(name);
                configurationsComboBox.setSelectedItem(name);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Can't create new configuration, same name is already exist or empty");
            }
        }
    }

    private void editConfigAction() {
        String name = JOptionPane.showInputDialog(
                this,
                "Type the new name for cgenConfiguration",
                configurationsComboBox.getSelectedItem());
        if (name != null) {
            if (!cgenConfigList.isExist(name) && !name.isEmpty()) {
                cgenConfiguration.setName(name);
                configurationsComboBox.removeItem(configurationsComboBox.getSelectedItem());
                configurationsComboBox.addItem(name);
                configurationsComboBox.setSelectedItem(name);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Can't rename configuration, name is already exist or empty");
            }
        }
    }

    private void removeConfigAction() {
        int result = JOptionPane.showConfirmDialog(this,
                "Configuration will be remove\n               Are you sure?",
                "Delete cgenConfiguration",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            if (configurationsComboBox.getItemCount() > 1) {
                cgenConfigList.removeByName(cgenConfiguration.getName());
                configurationsComboBox.removeItem(configurationsComboBox.getSelectedItem());
                configurationsComboBox.setSelectedIndex(0);
            } else {
                JOptionPane.showMessageDialog(this, "At least one configuration must exist");
            }
        }
    }

    private void prepareClasses(DataMap dataMap) {
        classes.clear();
        classes.add(dataMap);
        classes.addAll(dataMap.getObjEntities());
        classes.addAll(dataMap.getEmbeddables());
        selectionModel.initCollectionsForSelection(dataMap);
    }

    private CgenConfiguration createDefaultCgenConfiguration(DataMap map) {
        CgenConfiguration configuration = new CgenConfiguration();
        configuration.setName(CgenConfigList.DEFAULT_CONFIG_NAME);
        configuration.setForce(true);
        configuration.setDataMap(map);

        map.getObjEntities().forEach(configuration::loadEntity);
        map.getEmbeddables().forEach(configuration::loadEmbeddable);
        if (map.getLocation() != null) {
            Path basePath = CgenOps.baseDir(session);
            configuration.setRootPath(Utils.getRootPathForDataMap(map));
            configuration.updateOutputPath(basePath);
        }
        configuration.setEncoding(new GeneralPrefs(app.getPrefsLocator().appNode(GeneralPrefs.NODE)).getEncoding());
        return configuration;
    }

    private void updateArtifactGenerationMode(boolean selected) {
        cgenConfiguration.setArtifactsGenerationMode(selected ? "all" : "entity");
        checkCgenConfigDirty();
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

    private void addEntity(DataMap dataMap, ObjEntity objEntity) {
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

    @Override
    public void objEntityChanged(ObjEntityEvent e) {
        String oldName = e.getOldName();
        ObjEntity entity = e.getEntity();
        String newName = entity.getName();
        if (oldName == null || oldName.equals(newName)) {
            return;
        }
        selectionModel.renameSelectedEntity(entity.getDataMap(), oldName, newName);
        if (cgenConfiguration != null) {
            if (cgenConfiguration.getEntities().remove(oldName)) {
                cgenConfiguration.getEntities().add(newName);
            }
        }
        checkCgenConfigDirty();
    }

    @Override
    public void objEntityAdded(ObjEntityEvent e) {
        addEntity(e.getEntity().getDataMap(), e.getEntity());
    }

    @Override
    public void objEntityRemoved(ObjEntityEvent e) {
        selectionModel.removeFromSelectedEntities(e.getEntity());
        if (cgenConfiguration != null) {
            cgenConfiguration.getEntities().remove(e.getEntity().getName());
        }
        checkCgenConfigDirty();
    }

    @Override
    public void embeddableChanged(EmbeddableEvent e, DataMap map) {
        String oldClassName = e.getOldName();
        Embeddable embeddable = e.getEmbeddable();
        String newClassName = embeddable.getClassName();
        if (oldClassName == null || oldClassName.equals(newClassName)) {
            return;
        }
        selectionModel.renameSelectedEmbeddable(map, oldClassName, newClassName);
        if (cgenConfiguration != null) {
            if (cgenConfiguration.getEmbeddables().remove(oldClassName)) {
                cgenConfiguration.getEmbeddables().add(newClassName);
            }
        }
        checkCgenConfigDirty();
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
        if (e.getSource() instanceof DbImportResultDialog) {
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

    /**
     * Update cgen path on project save when no path is already set manually.
     */
    private void onProjectSaved(ProjectAfterSaveEvent e) {
        if (cgenConfigPanel != null && cgenConfiguration != null) {
            cgenConfigPanel.getOutputFolder()
                    .setText(cgenConfiguration.buildOutputPath().toString());
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

    private static final ConfigurationNodeVisitor<Integer> TYPE_GETTER = new BaseConfigurationNodeVisitor<>() {
        @Override
        public Integer visitDataMap(DataMap dataMap) { return 10; }
        @Override
        public Integer visitObjEntity(ObjEntity entity) { return 20; }
        @Override
        public Integer visitEmbeddable(Embeddable embeddable) { return 30; }
    };

    private static final ConfigurationNodeVisitor<String> NAME_GETTER = new BaseConfigurationNodeVisitor<>() {
        @Override
        public String visitDataMap(DataMap dataMap) { return dataMap.getName(); }
        @Override
        public String visitEmbeddable(Embeddable embeddable) { return embeddable.getClassName(); }
        @Override
        public String visitObjEntity(ObjEntity entity) { return entity.getName(); }
    };
}
