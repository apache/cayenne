/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Action;

import org.objectstyle.cayenne.modeler.action.AboutAction;
import org.objectstyle.cayenne.modeler.action.ConfigurePreferencesAction;
import org.objectstyle.cayenne.modeler.action.CreateAttributeAction;
import org.objectstyle.cayenne.modeler.action.CreateDataMapAction;
import org.objectstyle.cayenne.modeler.action.CreateDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDerivedDbEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateDomainAction;
import org.objectstyle.cayenne.modeler.action.CreateNodeAction;
import org.objectstyle.cayenne.modeler.action.CreateObjEntityAction;
import org.objectstyle.cayenne.modeler.action.CreateProcedureAction;
import org.objectstyle.cayenne.modeler.action.CreateProcedureParameterAction;
import org.objectstyle.cayenne.modeler.action.CreateQueryAction;
import org.objectstyle.cayenne.modeler.action.CreateRelationshipAction;
import org.objectstyle.cayenne.modeler.action.DbEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.DerivedEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.ExitAction;
import org.objectstyle.cayenne.modeler.action.GenerateCodeAction;
import org.objectstyle.cayenne.modeler.action.GenerateDBAction;
import org.objectstyle.cayenne.modeler.action.ImportDBAction;
import org.objectstyle.cayenne.modeler.action.ImportDataMapAction;
import org.objectstyle.cayenne.modeler.action.ImportEOModelAction;
import org.objectstyle.cayenne.modeler.action.NavigateBackwardAction;
import org.objectstyle.cayenne.modeler.action.NavigateForwardAction;
import org.objectstyle.cayenne.modeler.action.NewProjectAction;
import org.objectstyle.cayenne.modeler.action.ObjEntitySyncAction;
import org.objectstyle.cayenne.modeler.action.OpenProjectAction;
import org.objectstyle.cayenne.modeler.action.ProjectAction;
import org.objectstyle.cayenne.modeler.action.RemoveAction;
import org.objectstyle.cayenne.modeler.action.RemoveAttributeAction;
import org.objectstyle.cayenne.modeler.action.RemoveProcedureParameterAction;
import org.objectstyle.cayenne.modeler.action.RemoveRelationshipAction;
import org.objectstyle.cayenne.modeler.action.RevertAction;
import org.objectstyle.cayenne.modeler.action.SaveAction;
import org.objectstyle.cayenne.modeler.action.SaveAsAction;
import org.objectstyle.cayenne.modeler.action.ValidateAction;
import org.objectstyle.cayenne.modeler.util.CayenneAction;

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