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

package org.apache.cayenne.modeler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.apache.cayenne.modeler.action.AboutAction;
import org.apache.cayenne.modeler.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.action.CopyAction;
import org.apache.cayenne.modeler.action.CopyAttributeAction;
import org.apache.cayenne.modeler.action.CopyProcedureParameterAction;
import org.apache.cayenne.modeler.action.CopyRelationshipAction;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateCallbackMethodAction;
import org.apache.cayenne.modeler.action.CreateCallbackMethodForDataMapListenerAction;
import org.apache.cayenne.modeler.action.CreateCallbackMethodForListenerAction;
import org.apache.cayenne.modeler.action.CreateDataMapAction;
import org.apache.cayenne.modeler.action.CreateDataMapEntityListenerAction;
import org.apache.cayenne.modeler.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.action.CreateDomainAction;
import org.apache.cayenne.modeler.action.CreateEmbeddableAction;
import org.apache.cayenne.modeler.action.CreateNodeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateObjEntityListenerAction;
import org.apache.cayenne.modeler.action.CreateProcedureAction;
import org.apache.cayenne.modeler.action.CreateProcedureParameterAction;
import org.apache.cayenne.modeler.action.CreateQueryAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.CutAction;
import org.apache.cayenne.modeler.action.CutAttributeAction;
import org.apache.cayenne.modeler.action.CutProcedureParameterAction;
import org.apache.cayenne.modeler.action.CutRelationshipAction;
import org.apache.cayenne.modeler.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.action.DocumentationAction;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.action.FindAction;
import org.apache.cayenne.modeler.action.GenerateCodeAction;
import org.apache.cayenne.modeler.action.GenerateDBAction;
import org.apache.cayenne.modeler.action.ImportDBAction;
import org.apache.cayenne.modeler.action.ImportDataMapAction;
import org.apache.cayenne.modeler.action.ImportEOModelAction;
import org.apache.cayenne.modeler.action.InferRelationshipsAction;
import org.apache.cayenne.modeler.action.MigrateAction;
import org.apache.cayenne.modeler.action.NavigateBackwardAction;
import org.apache.cayenne.modeler.action.NavigateForwardAction;
import org.apache.cayenne.modeler.action.NewProjectAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.ProjectAction;
import org.apache.cayenne.modeler.action.RedoAction;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodForDataMapListenerAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodForListenerAction;
import org.apache.cayenne.modeler.action.RemoveEntityListenerAction;
import org.apache.cayenne.modeler.action.RemoveEntityListenerForDataMapAction;
import org.apache.cayenne.modeler.action.RemoveProcedureParameterAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.action.RevertAction;
import org.apache.cayenne.modeler.action.SaveAction;
import org.apache.cayenne.modeler.action.SaveAsAction;
import org.apache.cayenne.modeler.action.ShowLogConsoleAction;
import org.apache.cayenne.modeler.action.UndoAction;
import org.apache.cayenne.modeler.action.ValidateAction;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.project.ProjectPath;

/**
 * An object that manages CayenneModeler actions.
 * 
 */
public class ActionManager {

    static final Collection<String> SPECIAL_ACTIONS = Arrays.asList(SaveAction
            .getActionName(), UndoAction.getActionName(), RedoAction.getActionName());

    // search action added to project actions
    static final Collection<String> PROJECT_ACTIONS = Arrays.asList(RevertAction
            .getActionName(), CreateDomainAction.getActionName(), ProjectAction
            .getActionName(), ValidateAction.getActionName(), SaveAsAction
            .getActionName(), FindAction.getActionName());

    static final Collection<String> DOMAIN_ACTIONS = new HashSet<String>(PROJECT_ACTIONS);
    static {
        DOMAIN_ACTIONS.addAll(Arrays.asList(
                ImportDataMapAction.getActionName(),
                CreateDataMapAction.getActionName(),
                RemoveAction.getActionName(),
                CreateNodeAction.getActionName(),
                ImportDBAction.getActionName(),
                ImportEOModelAction.getActionName(),
                PasteAction.getActionName()));
    }
    
    static final Collection<String> DATA_MAP_ACTIONS = new HashSet<String>(DOMAIN_ACTIONS);
    static {
        DATA_MAP_ACTIONS.addAll(Arrays.asList(
                GenerateCodeAction.getActionName(),
                CreateEmbeddableAction.getActionName(),
                CreateObjEntityAction.getActionName(),
                CreateDbEntityAction.getActionName(),
                CreateQueryAction.getActionName(),
                CreateProcedureAction.getActionName(),
                GenerateDBAction.getActionName(),
                MigrateAction.getActionName(),
                InferRelationshipsAction.getActionName(),
                CutAction.getActionName(),
                CopyAction.getActionName()));
    }

    static final Collection<String> OBJ_ENTITY_ACTIONS = new HashSet<String>(
            DATA_MAP_ACTIONS);
    static {
        OBJ_ENTITY_ACTIONS.addAll(Arrays.asList(
                ObjEntitySyncAction.getActionName(),
                CreateAttributeAction.getActionName(),
                CreateRelationshipAction.getActionName()));
    }

    static final Collection<String> DB_ENTITY_ACTIONS = new HashSet<String>(
            DATA_MAP_ACTIONS);
    static {
        DB_ENTITY_ACTIONS.addAll(Arrays.asList(
                CreateAttributeAction.getActionName(),
                CreateRelationshipAction.getActionName(),
                DbEntitySyncAction.getActionName()));
    }

    static final Collection<String> EMBEDDABLE_ACTIONS = new HashSet<String>(
            DATA_MAP_ACTIONS);
    static {
        EMBEDDABLE_ACTIONS.addAll(Arrays.asList(
                CreateAttributeAction.getActionName()));
    }
    
    static final Collection<String> PROCEDURE_ACTIONS = new HashSet<String>(
            DATA_MAP_ACTIONS);
    static {
        PROCEDURE_ACTIONS.addAll(Arrays.asList(CreateProcedureParameterAction
                .getActionName()));
    }
    
    static final Collection<String> MULTIPLE_OBJECTS_ACTIONS = new HashSet<String>(
            PROJECT_ACTIONS);
    static {
        MULTIPLE_OBJECTS_ACTIONS.addAll(Arrays.asList(
                RemoveAction.getActionName(),
                CutAction.getActionName(),
                CopyAction.getActionName(),
                PasteAction.getActionName()));
    }

    protected Map<String, Action> actionMap;

    public ActionManager(Application application) {
        this.actionMap = new HashMap<String, Action>(40);

        registerAction(new ProjectAction(application));
        registerAction(new NewProjectAction(application)).setAlwaysOn(true);
        registerAction(new OpenProjectAction(application)).setAlwaysOn(true);
        registerAction(new ImportDataMapAction(application));
        registerAction(new SaveAction(application));
        registerAction(new SaveAsAction(application));
        registerAction(new RevertAction(application));
        registerAction(new ValidateAction(application));
        registerAction(new RemoveAction(application));
        registerAction(new CreateDomainAction(application));
        registerAction(new CreateNodeAction(application));
        registerAction(new CreateDataMapAction(application));
        registerAction(new GenerateCodeAction(application));
        registerAction(new CreateObjEntityAction(application));
        registerAction(new CreateDbEntityAction(application));
        registerAction(new CreateProcedureAction(application));
        registerAction(new CreateProcedureParameterAction(application));
        registerAction(new RemoveProcedureParameterAction(application));
        registerAction(new CreateQueryAction(application));
        registerAction(new CreateAttributeAction(application));
        registerAction(new RemoveAttributeAction(application));
        registerAction(new CreateRelationshipAction(application));
        registerAction(new RemoveRelationshipAction(application));
        // start callback-related actions
        registerAction(new CreateCallbackMethodAction(application)).setAlwaysOn(true);
        registerAction(new CreateCallbackMethodForListenerAction(application));
        registerAction(new CreateCallbackMethodForDataMapListenerAction(application));
        registerAction(new RemoveCallbackMethodAction(application));
        registerAction(new RemoveCallbackMethodForListenerAction(application));
        registerAction(new RemoveCallbackMethodForDataMapListenerAction(application));
        registerAction(new CreateObjEntityListenerAction(application)).setAlwaysOn(true);
        registerAction(new CreateDataMapEntityListenerAction(application)).setAlwaysOn(
                true);
        registerAction(new RemoveEntityListenerAction(application));
        registerAction(new RemoveEntityListenerForDataMapAction(application));
        // end callback-related actions
        registerAction(new DbEntitySyncAction(application));
        registerAction(new ObjEntitySyncAction(application));
        registerAction(new ImportDBAction(application));
        registerAction(new InferRelationshipsAction(application));
        registerAction(new ImportEOModelAction(application));
        registerAction(new GenerateDBAction(application));
        registerAction(new MigrateAction(application));
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
        registerAction(new CutProcedureParameterAction(application));
        registerAction(new CopyAction(application));
        registerAction(new CopyAttributeAction(application));
        registerAction(new CopyRelationshipAction(application));
        registerAction(new CopyProcedureParameterAction(application));
        registerAction(new PasteAction(application));
        
        UndoAction undoAction = new UndoAction(application); 
        undoAction.setEnabled(false);
        registerAction(undoAction);
        
        RedoAction redoAction = new RedoAction(application);
        redoAction.setEnabled(false);
        registerAction(redoAction);
        
        registerAction(new CreateEmbeddableAction(application));
    }

    private CayenneAction registerAction(CayenneAction action) {
        actionMap.put(action.getKey(), action);
        return action;
    }

    /**
     * Returns an action for key.
     * 
     * @param key action name
     * @return action
     */
    public CayenneAction getAction(String key) {
        return (CayenneAction) actionMap.get(key);
    }

    /**
     * Updates actions state to reflect an open project.
     */
    public void projectOpened() {
        processActionsState(PROJECT_ACTIONS);
        updateActions("");
    }

    public void projectClosed() {
        processActionsState(Collections.EMPTY_SET);
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
        processActionsState(DOMAIN_ACTIONS);
        updateActions("DataNode");
    }

    public void dataMapSelected() {
        processActionsState(DATA_MAP_ACTIONS);
        updateActions("DataMap");
        // reset
        // getAction(CreateAttributeAction.getActionName()).setName("Create Attribute");
    }

    public void objEntitySelected() {
        processActionsState(OBJ_ENTITY_ACTIONS);
        updateActions("ObjEntity");
    }

    public void dbEntitySelected() {
        processActionsState(DB_ENTITY_ACTIONS);
        updateActions("DbEntity");
    }

    public void derivedDbEntitySelected() {
        processActionsState(DB_ENTITY_ACTIONS);
        updateActions("Derived DbEntity");
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
    public void multipleObjectsSelected(ProjectPath[] paths) {
        processActionsState(MULTIPLE_OBJECTS_ACTIONS);
        
        updateActions("Selected Objects");
        
        CayenneAction cutAction = getAction(CutAction.getActionName());
        boolean canCopy = true; // cut/copy can be performed if selected objects are on the same level
        
        if (!cutAction.enableForPath(paths[0])) {
            canCopy = false;
        }
        else {
            Object parent = paths[0].getObjectParent();
            
            for (int i = 1; i < paths.length; i++) {
                if (paths[i].getObjectParent() != parent || !cutAction.enableForPath(paths[i])) {
                    canCopy = false;
                    break;
                }
            }
        }
        
        cutAction.setEnabled(canCopy);
        getAction(CopyAction.getActionName()).setEnabled(canCopy);
    }
    
    /**
     * Updates Remove, Cut and Copy actions' names
     */
    private void updateActions(String postfix) {
        if (postfix.length() > 0) {
            postfix = " " + postfix;
        }
        
        getAction(RemoveAction.getActionName()).setName("Remove" + postfix);
        getAction(CutAction.getActionName()).setName("Cut" + postfix);
        getAction(CopyAction.getActionName()).setName("Copy" + postfix);
        
        ((PasteAction) getAction(PasteAction.getActionName())).updateState();
    }

    /**
     * Sets the state of all controlled actions, flipping it to "enabled" for all actions
     * in provided collection and to "disabled" for the rest.
     * 
     * @param namesOfEnabled action names
     */
    protected void processActionsState(Collection<String> namesOfEnabled) {
        for (Map.Entry<String, Action> entry : actionMap.entrySet()) {

            if (!SPECIAL_ACTIONS.contains(entry.getKey())) {
                entry.getValue().setEnabled(namesOfEnabled.contains(entry.getKey()));
            }
        }
    }
    
    /**
     * Replaces standard Cut, Copy and Paste action maps, so that accelerators like 
     * Ctrl+X, Ctrl+C, Ctrl+V would work 
     */
    public void setupCCP(JComponent comp, String cutName, String copyName) {
        ActionMap map = comp.getActionMap();
        
        map.put(TransferHandler.getCutAction().getValue(Action.NAME), 
                getAction(cutName));
        map.put(TransferHandler.getCopyAction().getValue(Action.NAME),
                getAction(copyName));
        map.put(TransferHandler.getPasteAction().getValue(Action.NAME),
                getAction(PasteAction.getActionName()));
    }
}
