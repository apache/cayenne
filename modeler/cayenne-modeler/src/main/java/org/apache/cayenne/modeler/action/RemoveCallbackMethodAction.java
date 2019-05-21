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

import java.awt.event.ActionEvent;

import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.editor.CallbackType;
import org.apache.cayenne.modeler.editor.ObjCallbackMethod;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.undo.RemoveCallbackMethodUndoableEdit;


/**
 * Action class for removing callback methods from ObjEntity
 *
 * @version 1.0 Oct 30, 2007
 */
public class RemoveCallbackMethodAction extends RemoveAction {
    
    /**
     * unique action name
     */
    public final static String ACTION_NAME = "Remove Callback Method";
    
    /**
     * action name for multiple selection
     */
    private final static String ACTION_NAME_MULTIPLE = "Remove Callback Methods";

    /**
     * Constructor.
     *
     * @param application Application instance
     */
    public RemoveCallbackMethodAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * @return icon file name for button
     */
    @Override
    public String getIconName() {
        return "icon-trash.png";
    }
    
    /**
     * performs callback method removing
     * @param e event
     */
    public void performAction(ActionEvent e, boolean allowAsking) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);
        
        ObjCallbackMethod[] methods = getProjectController().getCurrentCallbackMethods();

        if ((methods.length == 1 && dialog.shouldDelete("callback method", methods[0].getName()))
        		|| (methods.length > 1 && dialog.shouldDelete("selected callback methods"))) {
        	removeCallbackMethods();
        }
    }

    /**
     * base logic for callback method removing
     */
    private void removeCallbackMethods() {
        ProjectController mediator = getProjectController();
        CallbackType callbackType = mediator.getCurrentCallbackType();

        ObjCallbackMethod[] callbackMethods = mediator.getCurrentCallbackMethods();

        for (ObjCallbackMethod callbackMethod : callbackMethods) {
            removeCallbackMethod(callbackType, callbackMethod.getName());
        }
        
        Application.getInstance().getUndoManager().addEdit( 
        		new RemoveCallbackMethodUndoableEdit(callbackType, callbackMethods));
    }
    
    public void removeCallbackMethod(CallbackType callbackType, String method) {
        ProjectController mediator = getProjectController();
        getCallbackMap().getCallbackDescriptor(callbackType.getType()).removeCallbackMethod(method);
        
        CallbackMethodEvent e = new CallbackMethodEvent(
                this,
                null,
                method,
                MapEvent.REMOVE);
        
        mediator.fireCallbackMethodEvent(e);
    }

    /**
     * @return unique action name
     */
    public static String getActionName() {
        return ACTION_NAME;
    }

    /**
     * @return CallbackMap fom which remove callback method
     */
    public CallbackMap getCallbackMap() {
        return getProjectController().getCurrentObjEntity().getCallbackMap();
    }

    public String getActionName(boolean multiple) {
        return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
    }
}

