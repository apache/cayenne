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

import java.util.List;

import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.map.EntityListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.AbstractRemoveCallbackMethodAction;
import org.apache.cayenne.modeler.action.CreateCallbackMethodForListenerAction;
import org.apache.cayenne.modeler.action.CreateObjEntityListenerAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodForListenerAction;
import org.apache.cayenne.modeler.action.RemoveEntityListenerAction;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.util.CayenneAction;

/**
 * Tab for editing entity listeners of an ObjEntity
 *
 * @version 1.0 Oct 28, 2007
 */
public class ObjEntityCallbackListenersTab extends AbstractCallbackListenersTab {

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
     * init listener
     */
    protected void initController() {
        super.initController();

        mediator.addObjEntityDisplayListener(
                new ObjEntityDisplayListener() {
                    public void currentObjEntityChanged(EntityDisplayEvent e) {
                        if (ObjEntityCallbackListenersTab.this.isVisible()) {
                            rebuildListenerClassCombo(null);
                            updateCallbackTypeCounters();
                            mediator.setCurrentCallbackType((CallbackType)callbackTypeCombo.getSelectedItem());
                            rebuildTable();
                        }
                    }
                }
        );
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

    protected EntityListener getEntityListener(String listenerClass) {
        return mediator.getCurrentObjEntity().getEntityListener(listenerClass);
    }
}

