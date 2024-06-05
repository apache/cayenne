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
package org.apache.cayenne.modeler.action;

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.dbimport.AddCatalogAction;
import org.apache.cayenne.modeler.action.dbimport.AddExcludeColumnAction;
import org.apache.cayenne.modeler.action.dbimport.AddExcludeProcedureAction;
import org.apache.cayenne.modeler.action.dbimport.AddExcludeTableAction;
import org.apache.cayenne.modeler.action.dbimport.AddIncludeColumnAction;
import org.apache.cayenne.modeler.action.dbimport.AddIncludeProcedureAction;
import org.apache.cayenne.modeler.action.dbimport.AddIncludeTableAction;
import org.apache.cayenne.modeler.action.dbimport.AddSchemaAction;
import org.apache.cayenne.modeler.action.dbimport.DeleteNodeAction;
import org.apache.cayenne.modeler.action.dbimport.EditNodeAction;
import org.apache.cayenne.modeler.action.dbimport.MoveImportNodeAction;
import org.apache.cayenne.modeler.action.dbimport.MoveInvertNodeAction;
import org.apache.cayenne.modeler.action.dbimport.ReverseEngineeringToolMenuAction;
import org.apache.cayenne.modeler.graph.action.ShowGraphEntityAction;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.ConfigurationNodeParentGetter;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Stores a map of modeler actions, and deals with activating/deactivating those actions
 * on state changes.
 */
public class DefaultActionManager implements ActionManager {

    private Collection<String> SPECIAL_ACTIONS;
    private Collection<String> PROJECT_ACTIONS;
    private Collection<String> DOMAIN_ACTIONS;
    private Collection<String> DATA_NODE_ACTIONS;
    private Collection<String> DATA_MAP_ACTIONS;
    private Collection<String> OBJ_ENTITY_ACTIONS;
    private Collection<String> DB_ENTITY_ACTIONS;
    private Collection<String> EMBEDDABLE_ACTIONS;
    private Collection<String> PROCEDURE_ACTIONS;
    private Collection<String> MULTIPLE_OBJECTS_ACTIONS;

    protected Map<String, Action> actionMap;

    public DefaultActionManager(@Inject Application application, @Inject ConfigurationNameMapper nameMapper) {
        initActions();
        this.actionMap = new HashMap<>(40);

        registerAction(new ProjectAction(application));
        registerAction(new NewProjectAction(application)).setAlwaysOn(true);
        registerAction(new OpenProjectAction(application)).setAlwaysOn(true);
        registerAction(new ImportDataMapAction(application, nameMapper));
        registerAction(new SaveAction(application));
        registerAction(new SaveAsAction(application));
        registerAction(new RevertAction(application));
        registerAction(new ValidateAction(application));
        registerAction(new RemoveAction(application));
        registerAction(new CreateNodeAction(application));
        registerAction(new CreateDataMapAction(application));
        registerAction(new GenerateCodeAction(application));
        registerAction(new CreateObjEntityAction(application));
        registerAction(new CreateObjEntityFromDbAction(application));
        registerAction(new CreateDbEntityAction(application));
        registerAction(new CreateProcedureAction(application));
        registerAction(new CreateProcedureParameterAction(application));
        registerAction(new RemoveProcedureParameterAction(application));
        registerAction(new CreateQueryAction(application));
        registerAction(new CreateAttributeAction(application));
        registerAction(new RemoveAttributeAction(application));
        registerAction(new CreateRelationshipAction(application));
        registerAction(new RemoveRelationshipAction(application));
        registerAction(new RemoveAttributeRelationshipAction(application));
        // start callback-related actions
        registerAction(new CreateCallbackMethodAction(application)).setAlwaysOn(true);
        registerAction(new RemoveCallbackMethodAction(application));
        // end callback-related actions
        registerAction(new DbEntitySyncAction(application));
        registerAction(new ObjEntitySyncAction(application));
        registerAction(new DbEntityCounterpartAction(application));
        registerAction(new ObjEntityCounterpartAction(application));
        registerAction(new ObjEntityToSuperEntityAction(application));
        registerAction(new ReverseEngineeringAction(application));
        registerAction(new InferRelationshipsAction(application));
        registerAction(new ReverseEngineeringToolMenuAction(application));
        registerAction(new ImportEOModelAction(application));
        registerAction(new GenerateDBAction(application));
        registerAction(new MigrateAction(application));
        registerAction(new AddSchemaAction(application)).setAlwaysOn(true);
        registerAction(new AddCatalogAction(application)).setAlwaysOn(true);
        registerAction(new AddIncludeTableAction(application)).setAlwaysOn(true);
        registerAction(new AddExcludeTableAction(application)).setAlwaysOn(true);
        registerAction(new AddIncludeColumnAction(application)).setAlwaysOn(true);
        registerAction(new AddExcludeColumnAction(application)).setAlwaysOn(true);
        registerAction(new AddIncludeProcedureAction(application)).setAlwaysOn(true);
        registerAction(new AddExcludeProcedureAction(application)).setAlwaysOn(true);
        registerAction(new GetDbConnectionAction(application)).setAlwaysOn(true);
        registerAction(new EditNodeAction(application)).setAlwaysOn(true);
        registerAction(new DeleteNodeAction(application)).setAlwaysOn(true);
        registerAction(new MoveImportNodeAction(application)).setAlwaysOn(true);
        registerAction(new LoadDbSchemaAction(application)).setAlwaysOn(true);
        registerAction(new MoveInvertNodeAction(application)).setAlwaysOn(true);
        registerAction(new AboutAction(application)).setAlwaysOn(true);
        registerAction(new DocumentationAction(application)).setAlwaysOn(true);
        registerAction(new ConfigurePreferencesAction(application)).setAlwaysOn(true);
        registerAction(new ExitAction(application)).setAlwaysOn(true);
        registerAction(new NavigateBackwardAction(application)).setAlwaysOn(true);
        registerAction(new NavigateForwardAction(application)).setAlwaysOn(true);
        // search action registered
        registerAction(new FindAction(application));

        registerAction(new ShowLogConsoleAction(application)).setAlwaysOn(true);

        registerAction(new CutAction(application));
        registerAction(new CutAttributeAction(application));
        registerAction(new CutRelationshipAction(application));
        registerAction(new CutAttributeRelationshipAction(application));
        registerAction(new CutProcedureParameterAction(application));
        registerAction(new CutCallbackMethodAction(application));
        registerAction(new CopyAction(application));
        registerAction(new CopyAttributeAction(application));
        registerAction(new CopyRelationshipAction(application));
        registerAction(new CopyAttributeRelationshipAction(application));
        registerAction(new CopyCallbackMethodAction(application));
        registerAction(new CopyProcedureParameterAction(application));
        registerAction(new PasteAction(application));

        UndoAction undoAction = new UndoAction(application);
        undoAction.setEnabled(false);
        registerAction(undoAction);

        RedoAction redoAction = new RedoAction(application);
        redoAction.setEnabled(false);
        registerAction(redoAction);

        registerAction(new CreateEmbeddableAction(application));
        registerAction(new ShowGraphEntityAction(application));

        registerAction(new CollapseTreeAction(application));
        registerAction(new FilterAction(application));

        registerAction(new LinkDataMapAction(application));
        registerAction(new LinkDataMapsAction(application));

        registerAction(new UpdateValidationConfigAction(application));
    }

    private void initActions() {
        SPECIAL_ACTIONS = new HashSet<>();

        SPECIAL_ACTIONS.addAll(Arrays.asList(
                SaveAction.class.getName(),
                UndoAction.class.getName(),
                RedoAction.class.getName()));

        PROJECT_ACTIONS = new HashSet<>();
        PROJECT_ACTIONS.addAll(Arrays.asList(
                RevertAction.class.getName(),
                ProjectAction.class.getName(),
                ValidateAction.class.getName(),
                SaveAsAction.class.getName(),
                FindAction.class.getName()));

        DOMAIN_ACTIONS = new HashSet<>(PROJECT_ACTIONS);
        DOMAIN_ACTIONS.addAll(Arrays.asList(
                ImportDataMapAction.class.getName(),
                CreateDataMapAction.class.getName(),
                CreateNodeAction.class.getName(),
                ReverseEngineeringAction.class.getName(),
                ImportEOModelAction.class.getName(),
                GenerateCodeAction.class.getName(),
                GenerateDBAction.class.getName(),
                PasteAction.class.getName(),
                ReverseEngineeringToolMenuAction.class.getName()));

        DATA_NODE_ACTIONS = new HashSet<>(DOMAIN_ACTIONS);
        DATA_NODE_ACTIONS.addAll(Arrays.asList(
                LinkDataMapsAction.class.getName(),
                RemoveAction.class.getName()));

        DATA_MAP_ACTIONS = new HashSet<>(DOMAIN_ACTIONS);
        DATA_MAP_ACTIONS.addAll(Arrays.asList(
                CreateEmbeddableAction.class.getName(),
                CreateObjEntityAction.class.getName(),
                CreateDbEntityAction.class.getName(),
                CreateQueryAction.class.getName(),
                CreateProcedureAction.class.getName(),
                MigrateAction.class.getName(),
                RemoveAction.class.getName(),
                InferRelationshipsAction.class.getName(),
                CutAction.class.getName(),
                CopyAction.class.getName()));

        OBJ_ENTITY_ACTIONS = new HashSet<>(DATA_MAP_ACTIONS);

        OBJ_ENTITY_ACTIONS.addAll(Arrays.asList(
                ObjEntitySyncAction.class.getName(),
                CreateAttributeAction.class.getName(),
                CreateRelationshipAction.class.getName(),
                ObjEntityCounterpartAction.class.getName(),
                ObjEntityToSuperEntityAction.class.getName(),
                ShowGraphEntityAction.class.getName()));

        DB_ENTITY_ACTIONS = new HashSet<>(DATA_MAP_ACTIONS);

        DB_ENTITY_ACTIONS.addAll(Arrays.asList(
                CreateAttributeAction.class.getName(),
                CreateRelationshipAction.class.getName(),
                DbEntitySyncAction.class.getName(),
                DbEntityCounterpartAction.class.getName(),
                ShowGraphEntityAction.class.getName(),
                CreateObjEntityFromDbAction.class.getName()));

        EMBEDDABLE_ACTIONS = new HashSet<>(DATA_MAP_ACTIONS);

        EMBEDDABLE_ACTIONS.addAll(Collections.singletonList(CreateAttributeAction.class.getName()));

        PROCEDURE_ACTIONS = new HashSet<>(DATA_MAP_ACTIONS);

        PROCEDURE_ACTIONS.addAll(Collections.singletonList(CreateProcedureParameterAction.class
                .getName()));

        MULTIPLE_OBJECTS_ACTIONS = new HashSet<>(PROJECT_ACTIONS);

        MULTIPLE_OBJECTS_ACTIONS.addAll(Arrays.asList(
                RemoveAction.class.getName(),
                CutAction.class.getName(),
                CopyAction.class.getName(),
                PasteAction.class.getName()));

    }

    protected CayenneAction registerAction(CayenneAction action) {
        Action oldAction = actionMap.put(action.getClass().getName(), action);
        if (oldAction != null && oldAction != action) {

            actionMap.put(action.getClass().getName(), oldAction);
            throw new IllegalArgumentException("There is already an action of type "
                    + action.getClass().getName()
                    + ", attempt to register a second instance.");
        }

        return action;
    }

    public void addProjectAction(String actionName) {
        if (!PROJECT_ACTIONS.contains(actionName)) {
            PROJECT_ACTIONS.add(actionName);
        }
    }

    public void removeProjectaction(String actionName) {
        if (PROJECT_ACTIONS.contains(actionName)) {
            PROJECT_ACTIONS.remove(actionName);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Action> T getAction(Class<T> actionClass) {
        return (T) actionMap.get(actionClass.getName());
    }

    /**
     * Updates actions state to reflect an open project.
     */
    public void projectOpened() {
        processActionsState(PROJECT_ACTIONS);
        updateActions("");
    }

    public void projectClosed() {
        processActionsState(Collections.emptySet());
        updateActions("");
    }

    /**
     * Updates actions state to reflect DataDomain selection.
     */
    public void domainSelected() {
        processActionsState(DOMAIN_ACTIONS);
        updateActions("DataDomain");
    }

    public void dataNodeSelected() {
        processActionsState(DATA_NODE_ACTIONS);
        updateActions("DataNode");
    }

    public void dataMapSelected() {
        processActionsState(DATA_MAP_ACTIONS);
        updateActions("DataMap");
    }

    public void objEntitySelected() {
        processActionsState(OBJ_ENTITY_ACTIONS);
        updateActions("ObjEntity");
    }

    public void dbEntitySelected() {
        processActionsState(DB_ENTITY_ACTIONS);
        updateActions("DbEntity");
    }

    public void procedureSelected() {
        processActionsState(PROCEDURE_ACTIONS);
        updateActions("Procedure");
    }

    public void querySelected() {
        processActionsState(DATA_MAP_ACTIONS);
        updateActions("Query");
    }

    public void embeddableSelected() {
        processActionsState(EMBEDDABLE_ACTIONS);
        updateActions("Embeddable");
    }

    /**
     * Invoked when several objects were selected in ProjectTree at time
     */
    public void multipleObjectsSelected(
            ConfigurationNode[] objects,
            Application application) {
        processActionsState(MULTIPLE_OBJECTS_ACTIONS);

        updateActions("Selected Objects");

        CayenneAction cutAction = getAction(CutAction.class);
        boolean canCopy = true; // cut/copy can be performed if selected objects are on
        // the same level

        if (!cutAction.enableForPath(objects[0])) {
            canCopy = false;
        }
        else {
            ConfigurationNodeParentGetter parentGetter = application
                    .getInjector()
                    .getInstance(ConfigurationNodeParentGetter.class);
            Object parent = parentGetter.getParent(objects[0]);

            for (int i = 1; i < objects.length; i++) {
                if (parentGetter.getParent(objects[i]) != parent
                        || !cutAction.enableForPath(objects[i])) {
                    canCopy = false;
                    break;
                }
            }
        }

        cutAction.setEnabled(canCopy);
        getAction(CopyAction.class).setEnabled(canCopy);
    }

    /**
     * Updates Remove, Cut and Copy actions' names
     */
    private void updateActions(String postfix) {
        if (postfix.length() > 0) {
            postfix = " " + postfix;
        }

        getAction(RemoveAction.class).setName("Remove" + postfix);
        getAction(CutAction.class).setName("Cut" + postfix);
        getAction(CopyAction.class).setName("Copy" + postfix);

        getAction(PasteAction.class).updateState();
    }

    /**
     * Sets the state of all controlled actions, flipping it to "enabled" for all actions
     * in provided collection and to "disabled" for the rest.
     */
    protected void processActionsState(Collection<String> namesOfEnabled) {
        for (Map.Entry<String, Action> entry : actionMap.entrySet()) {

            if (!SPECIAL_ACTIONS.contains(entry.getKey())) {
                entry.getValue().setEnabled(namesOfEnabled.contains(entry.getKey()));
            }
        }
    }

    public void setupCutCopyPaste(
            JComponent comp,
            Class<? extends Action> cutActionType,
            Class<? extends Action> copyActionType) {

        ActionMap map = comp.getActionMap();

        map.put(
                TransferHandler.getCutAction().getValue(Action.NAME),
                getAction(cutActionType));
        map.put(
                TransferHandler.getCopyAction().getValue(Action.NAME),
                getAction(copyActionType));
        map.put(
                TransferHandler.getPasteAction().getValue(Action.NAME),
                getAction(PasteAction.class));
    }

}
