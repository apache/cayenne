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
package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.*;
import org.apache.cayenne.modeler.event.*;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.CallbackMap;

import java.util.List;


/**
 * Tab for editing entity listeners of an ObjEntity
 *
 * @author Vasil Tarasevich
 * @version 1.0 Oct 28, 2007
 */
public class ObjEntityCallbackListenersTab extends AbstractCallbackListenersTab
    implements ObjEntityDisplayListener {

    /**
     * constructor
     * @param mediator mediator instance
     */
    public ObjEntityCallbackListenersTab(ProjectController mediator) {
        super(mediator);
    }

    /**
     * @return CallbackMap with callback methods
     */
    protected CallbackMap getCallbackMap() {
        String listenerClass = (String) listenerClassCombo.getSelectedItem();
        if (mediator.getCurrentObjEntity() != null &&
                mediator.getCurrentObjEntity().getEntityListener(listenerClass) != null) {
            return mediator.getCurrentObjEntity().getEntityListener(listenerClass).getCallbackMap();
        }
        return null;
    }


    /**
     * Current obj entity used as a model has changed.
     *
     * @param e event
     */
    public void currentObjEntityChanged(EntityDisplayEvent e) {
        if (e.getSource() == this) {
            return;
        }

        ObjEntity entity = (ObjEntity) e.getEntity();
        // Important: process event even if this is the same entity,
        // since the inheritance structure might have changed
        if (entity != null) {
            rebuildListenerClassCombo();

            mediator.fireCallbackTypeSelectionEvent(
                new CallbackTypeSelectionEvent(
                    e.getSource(),
                    (CallbackType)callbackTypeCombo.getItemAt(0))
            );

            rebuildTable();
        }

        // if an entity was selected on a tree,
        // unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }

    /**
     * init listener
     */
    protected void initController() {
        super.initController();
        mediator.addObjEntityDisplayListener(this);
    }

    /**
     * @return returns entity listeners list
     */
    protected List getEntityListeners() {
        return mediator.getCurrentObjEntity() == null ? null :
               mediator.getCurrentObjEntity().getEntityListeners();
    }

    /**
     * @return create callback method action
     */
    protected CayenneAction getCreateCallbackMethodAction() {
        Application app = Application.getInstance();
        return app.getAction(CreateCallbackMethodForListenerAction.ACTION_NAME);
    }


    /**
     * @return remove callback method action
     */
    protected AbstractRemoveCallbackMethodAction getRemoveCallbackMethodAction() {
        Application app = Application.getInstance();
        return (AbstractRemoveCallbackMethodAction)app.getAction(RemoveCallbackMethodForListenerAction.ACTION_NAME);
    }

    /**
     * @return action for removing entity listeners
     */
    protected CayenneAction getRemoveEntityListenerAction() {
        return Application.getInstance().getAction(RemoveEntityListenerAction.getActionName());
    }

    /**
     * @return action for creating entity listeners
     */
    public CayenneAction getCreateEntityListenerAction() {
        return Application.getInstance().getAction(CreateObjEntityListenerAction.getActionName());
    }

    /**
     * @return action for changing entity listeners
     */
    public CayenneAction getChangeEntityListenerAction() {
        return Application.getInstance().getAction(ChangeObjEntityListenerClassAction.getActionName());
    }
}

