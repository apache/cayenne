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

import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks.CallbackType;
import org.apache.cayenne.modeler.event.model.CallbackMethodEvent;
import org.apache.cayenne.modeler.undo.CreateCallbackMethodUndoableEdit;
import org.apache.cayenne.util.Util;

import java.awt.event.ActionEvent;

/**
 * Action class for creating callback methods on ObjEntity
 */
public class CreateCallbackMethodAction extends ModelerAbstractAction {

    public CreateCallbackMethodAction(Application application) {
        super("Create callback method", application);
    }

    /**
     * @return CallbackMap instance where to create a method
     */
    public CallbackMap getCallbackMap() {
        return getProjectController().getSelectedObjEntity().getCallbackMap();
    }

    @Override
    public String getIconName() {
        return "icon-create-method.png";
    }

    @Override
    public final void performAction(ActionEvent e) {
        CallbackType callbackType = getProjectController().getSelectedCallbackType();

        String methodName = NameBuilder
                .builderForCallbackMethod(getProjectController().getSelectedObjEntity())
                .baseName(toMethodName(callbackType.getType()))
                .name();

        createCallbackMethod(callbackType, methodName);
        application.getUndoManager().addEdit(
                new CreateCallbackMethodUndoableEdit(
                        callbackType,
                        methodName));
    }

    public void createCallbackMethod(
            CallbackType callbackType,
            String methodName) {
        getCallbackMap().getCallbackDescriptor(callbackType.getType()).addCallbackMethod(methodName);

        CallbackMethodEvent ce = new CallbackMethodEvent(
                this,
                null,
                methodName,
                MapEvent.ADD);

        getProjectController().fireCallbackMethodEvent(ce);
    }

    private String toMethodName(LifecycleEvent event) {
        return "on" + Util.underscoredToJava(event.name(), true);
    }
}

