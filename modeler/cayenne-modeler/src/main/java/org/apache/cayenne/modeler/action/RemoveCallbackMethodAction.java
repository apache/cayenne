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

import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.ui.project.editor.objentity.CallbackType;
import org.apache.cayenne.modeler.ui.project.editor.objentity.ObjCallbackMethod;
import org.apache.cayenne.modeler.event.model.CallbackMethodEvent;
import org.apache.cayenne.modeler.undo.RemoveCallbackMethodUndoableEdit;

import java.awt.event.ActionEvent;


/**
 * Action class for removing callback methods from ObjEntity
 */
public class RemoveCallbackMethodAction extends RemoveAction implements MultipleObjectsAction {

    private final static String ACTION_NAME = "Remove Callback Method";
    private final static String ACTION_NAME_MULTIPLE = "Remove Callback Methods";

    public RemoveCallbackMethodAction(Application application) {
        super(ACTION_NAME, application);
    }

    @Override
    public String getActionName(boolean multiple) {
        return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
    }

    @Override
    public void performAction(ActionEvent e, boolean allowAsking) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);

        ObjCallbackMethod[] methods = getProjectController().getSelectedCallbackMethods();

        if ((methods.length == 1 && dialog.shouldDelete("callback method", methods[0].getName()))
                || (methods.length > 1 && dialog.shouldDelete("selected callback methods"))) {
            removeCallbackMethods();
        }
    }

    private void removeCallbackMethods() {
        ProjectController mediator = getProjectController();
        CallbackType callbackType = mediator.getSelectedCallbackType();

        ObjCallbackMethod[] callbackMethods = mediator.getSelectedCallbackMethods();

        for (ObjCallbackMethod callbackMethod : callbackMethods) {
            removeCallbackMethod(callbackType, callbackMethod.getName());
        }

        Application.getInstance().getUndoManager().addEdit(
                new RemoveCallbackMethodUndoableEdit(callbackType, callbackMethods));
    }

    public void removeCallbackMethod(CallbackType callbackType, String method) {
        ProjectController controller = getProjectController();

        getProjectController().getSelectedObjEntity()
                .getCallbackMap()
                .getCallbackDescriptor(callbackType.getType())
                .removeCallbackMethod(method);

        CallbackMethodEvent e = new CallbackMethodEvent(
                this,
                null,
                method,
                MapEvent.REMOVE);

        controller.fireCallbackMethodEvent(e);
    }
}

