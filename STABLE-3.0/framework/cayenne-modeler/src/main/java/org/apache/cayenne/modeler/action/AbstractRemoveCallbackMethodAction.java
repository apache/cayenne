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
package org.apache.cayenne.modeler.action;

import java.awt.event.ActionEvent;

import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.editor.CallbackType;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;

/**
 * Base class for removing callback methofs
 *
 * @version 1.0 Oct 28, 2007
 */
public abstract class AbstractRemoveCallbackMethodAction extends RemoveAction {

    

    /**
     * Constructor.
     *
     * @param actionName unique action name
     * @param application Application instance
     */
    public AbstractRemoveCallbackMethodAction(String actionName, Application application) {
        super(actionName, application);
    }

    /**
     * @return CallbackMap instance where to create a method
     */
    public abstract CallbackMap getCallbackMap();

    /**
     * @return icon file name for button
     */
    @Override
    public String getIconName() {
        return "icon-remove-method.gif";
    }

    /**
     * performs callback method removing
     * @param e event
     */
    @Override
    public final void performAction(ActionEvent e, boolean allowAsking) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);
        
        String[] methods = getProjectController().getCurrentCallbackMethods();

        if (methods.length > 0) {
            if ((methods.length == 1 && dialog.shouldDelete("callback method", methods[0]))
                    || (methods.length > 1 && dialog.shouldDelete("selected callback methods"))) {
                removeCallbackMethods(e);
            }
        }
    }

    /**
     * base logic for callback method removing
     * @param actionEvent event
     */
    private void removeCallbackMethods(ActionEvent actionEvent) {
        ProjectController mediator = getProjectController();
        CallbackType callbackType = mediator.getCurrentCallbackType();

        String[] callbackMethods = mediator.getCurrentCallbackMethods();

        for (String callbackMethod : callbackMethods) {
            removeCallbackMethod(getCallbackMap(), callbackType, callbackMethod);
        }
    }
    
    public void removeCallbackMethod(CallbackMap map, CallbackType callbackType, String method) {
        ProjectController mediator = getProjectController();
        map.getCallbackDescriptor(callbackType.getType()).removeCallbackMethod(method);
        
        CallbackMethodEvent e = new CallbackMethodEvent(
                this,
                null,
                method,
                MapEvent.REMOVE);
        
        mediator.fireCallbackMethodEvent(e);
    }
    
    /**
     * Returns action name, depending on count of selected rows
     */
    public abstract String getActionName(boolean multiple);
}

