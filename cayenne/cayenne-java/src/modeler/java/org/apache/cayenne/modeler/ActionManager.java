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
import java.util.Iterator;
import java.util.Map;

import javax.swing.Action;

import org.apache.cayenne.modeler.action.AboutAction;
import org.apache.cayenne.modeler.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateDataMapAction;
import org.apache.cayenne.modeler.action.CreateDbEntityAction;
import org.apache.cayenne.modeler.action.CreateDerivedDbEntityAction;
import org.apache.cayenne.modeler.action.CreateDomainAction;
import org.apache.cayenne.modeler.action.CreateNodeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateProcedureAction;
import org.apache.cayenne.modeler.action.CreateProcedureParameterAction;
import org.apache.cayenne.modeler.action.CreateQueryAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.action.DerivedEntitySyncAction;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.action.GenerateCodeAction;
import org.apache.cayenne.modeler.action.GenerateDBAction;
import org.apache.cayenne.modeler.action.ImportDBAction;
import org.apache.cayenne.modeler.action.ImportDataMapAction;
import org.apache.cayenne.modeler.action.ImportEOModelAction;
import org.apache.cayenne.modeler.action.NavigateBackwardAction;
import org.apache.cayenne.modeler.action.NavigateForwardAction;
import org.apache.cayenne.modeler.action.NewProjectAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.OpenProjectAction;
import org.apache.cayenne.modeler.action.ProjectAction;
import org.apache.cayenne.modeler.action.RemoveAction;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.action.RemoveProcedureParameterAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.action.RevertAction;
import org.apache.cayenne.modeler.action.SaveAction;
import org.apache.cayenne.modeler.action.SaveAsAction;
import org.apache.cayenne.modeler.action.ValidateAction;
import org.apache.cayenne.modeler.util.CayenneAction;

/**
 * An object that manages CayenneModeler actions.
 * 
 * @author Andrus Adamchik
 */
public class ActionManager {

    static final Collection SPECIAL_ACTIONS = new HashSet(Arrays.asList(new String[] {
            SaveAction.getActionName(), RevertAction.getActionName()
    }));

    static final Collection PROJECT_ACTIONS = Arrays.asList(new String[] {
            CreateDomainAction.getActionName(), ProjectAction.getActionName(),
            ValidateAction.getActionName(), SaveAsAction.getActionName()
    });

    static final Collection DOMAIN_ACTIONS = new HashSet(PROJECT_ACTIONS);
    static {
        DOMAIN_ACTIONS.addAll(Arrays.asList(new String[] {
                ImportDataMapAction.getActionName(), CreateDataMapAction.getActionName(),
                RemoveAction.getActionName(), CreateNodeAction.getActionName(),
                ImportDBAction.getActionName(), ImportEOModelAction.getActionName()
        }));
    }

    static final Collection DATA_MAP_ACTIONS = new HashSet(DOMAIN_ACTIONS);
    static {
        DATA_MAP_ACTIONS.addAll(Arrays.asList(new String[] {
                GenerateCodeAction.getActionName(),
                CreateObjEntityAction.getActionName(),
                CreateDbEntityAction.getActionName(),
                CreateDerivedDbEntityAction.getActionName(),
                CreateQueryAction.getActionName(), CreateProcedureAction.getActionName(),
                GenerateDBAction.getActionName()
        }));
    }

    static final Collection OBJ_ENTITY_ACTIONS = new HashSet(DATA_MAP_ACTIONS);
    static {
        OBJ_ENTITY_ACTIONS.addAll(Arrays.asList(new String[] {
                ObjEntitySyncAction.getActionName(),
                CreateAttributeAction.getActionName(),
                CreateRelationshipAction.getActionName()
        }));
    }

    static final Collection DB_ENTITY_ACTIONS = new HashSet(DATA_MAP_ACTIONS);
    static {
        DB_ENTITY_ACTIONS.addAll(Arrays.asList(new String[] {
                CreateAttributeAction.getActionName(),
                CreateRelationshipAction.getActionName(),
                DbEntitySyncAction.getActionName()
        }));
    }

    static final Collection PROCEDURE_ACTIONS = new HashSet(DATA_MAP_ACTIONS);
    static {
        PROCEDURE_ACTIONS.addAll(Arrays.asList(new String[] {
            CreateProcedureParameterAction.getActionName()
        }));
    }

    protected Map actionMap;

    public ActionManager(Application application) {
        this.actionMap = new HashMap();

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
        registerAction(new CreateDerivedDbEntityAction(application));
        registerAction(new CreateProcedureAction(application));
        registerAction(new CreateProcedureParameterAction(application));
        registerAction(new RemoveProcedureParameterAction(application));
        registerAction(new CreateQueryAction(application));
        registerAction(new CreateAttributeAction(application));
        registerAction(new RemoveAttributeAction(application));
        registerAction(new CreateRelationshipAction(application));
        registerAction(new RemoveRelationshipAction(application));
        registerAction(new DbEntitySyncAction(application));
        registerAction(new ObjEntitySyncAction(application));
        registerAction(new DerivedEntitySyncAction(application));
        registerAction(new ImportDBAction(application));
        registerAction(new ImportEOModelAction(application));
        registerAction(new GenerateDBAction(application));
        registerAction(new AboutAction(application)).setAlwaysOn(true);
        registerAction(new ConfigurePreferencesAction(application)).setAlwaysOn(true);
        registerAction(new ExitAction(application)).setAlwaysOn(true);
        registerAction(new NavigateBackwardAction(application)).setAlwaysOn(true);
        registerAction(new NavigateForwardAction(application)).setAlwaysOn(true);
    }

    private CayenneAction registerAction(CayenneAction action) {
        actionMap.put(action.getKey(), action);
        return action;
    }

    /**
     * Returns an action for key.
     */
    public CayenneAction getAction(String key) {
        return (CayenneAction) actionMap.get(key);
    }

    /**
     * Updates actions state to reflect an open project.
     */
    public void projectOpened() {
        processActionsState(PROJECT_ACTIONS);
        getAction(RemoveAction.getActionName()).setName("Remove");
    }

    public void projectClosed() {
        processActionsState(Collections.EMPTY_SET);
        getAction(RemoveAction.getActionName()).setName("Remove");
    }

    /**
     * Updates actions state to reflect DataDomain selecttion.
     */
    public void domainSelected() {
        processActionsState(DOMAIN_ACTIONS);
    }

    public void dataNodeSelected() {
        processActionsState(DOMAIN_ACTIONS);
        getAction(RemoveAction.getActionName()).setName("Remove DataNode");
    }

    public void dataMapSelected() {
        processActionsState(DATA_MAP_ACTIONS);
        getAction(RemoveAction.getActionName()).setName("Remove DataMap");
        // reset
        // getAction(CreateAttributeAction.getActionName()).setName("Create Attribute");
    }

    public void objEntitySelected() {
        processActionsState(OBJ_ENTITY_ACTIONS);
        getAction(RemoveAction.getActionName()).setName("Remove ObjEntity");
    }

    public void dbEntitySelected() {
        processActionsState(DB_ENTITY_ACTIONS);
        getAction(RemoveAction.getActionName()).setName("Remove DbEntity");
    }
    
    public void derivedDbEntitySelected() {
        processActionsState(DB_ENTITY_ACTIONS);
        getAction(RemoveAction.getActionName()).setName("Remove Derived DbEntity");
    }

    public void procedureSelected() {
        processActionsState(PROCEDURE_ACTIONS);
        getAction(RemoveAction.getActionName()).setName("Remove Procedure");
    }

    public void querySelected() {
        processActionsState(DATA_MAP_ACTIONS);
        getAction(RemoveAction.getActionName()).setName("Remove Query");
    }

    /**
     * Sets the state of all controlled actions, flipping it to "enabled" for all actions
     * in provided collection and to "disabled" for the rest.
     */
    protected void processActionsState(Collection namesOfEnabled) {
        Iterator it = actionMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            if (!SPECIAL_ACTIONS.contains(entry.getKey())) {
                ((Action) entry.getValue()).setEnabled(namesOfEnabled.contains(entry
                        .getKey()));
            }
        }
    }
}
