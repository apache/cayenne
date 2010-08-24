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
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.CallbackType;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.undo.CreateCallbackMethodUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.util.NameConverter;

/**
 * Base class for creating callback methods
 * 
 * @version 1.0 Oct 28, 2007
 */
public abstract class AbstractCreateCallbackMethodAction extends CayenneAction {

    

    /**
     * Constructor.
     * 
     * @param actionName unique action name
     * @param application Application instance
     */
    public AbstractCreateCallbackMethodAction(String actionName, Application application) {
        super(actionName, application);
    }

    /**
     * @return CallbackMap instance where to create a method
     */
    public abstract CallbackMap getCallbackMap();

    /**
     * @return icon file name for button
     */
    public String getIconName() {
        return "icon-create-method.gif";
    }

    /**
     * performs adding new callback method
     * 
     * @param e event
     */
    public final void performAction(ActionEvent e) {
        CallbackType callbackType = getProjectController().getCurrentCallbackType();

        // generate methodName
        String methodNamePrefix = toMethodName(callbackType.getType());
        String methodName;
        // now that we're generating the method names based on the callback type, check to
        // see if the
        // raw prefix, no numbers, is taken.
        if (!getCallbackMap()
                .getCallbackDescriptor(callbackType.getType())
                .getCallbackMethods()
                .contains(methodNamePrefix)) {
            methodName = methodNamePrefix;
        }
        else {
            int counter = 1;
            do {
                methodName = methodNamePrefix + counter;
                counter++;
            } while (getCallbackMap()
                    .getCallbackDescriptor(callbackType.getType())
                    .getCallbackMethods()
                    .contains(methodName));
        }

        createCallbackMethod(getCallbackMap(), callbackType, methodName);
        application.getUndoManager().addEdit(
                new CreateCallbackMethodUndoableEdit(
                        getCallbackMap(),
                        callbackType,
                        methodName));
    }

    public void createCallbackMethod(
            CallbackMap map,
            CallbackType callbackType,
            String methodName) {
        map.getCallbackDescriptor(callbackType.getType()).addCallbackMethod(methodName);

        CallbackMethodEvent ce = new CallbackMethodEvent(
                this,
                null,
                methodName,
                MapEvent.ADD);

        getProjectController().fireCallbackMethodEvent(ce);
    }

    private String toMethodName(LifecycleEvent event) {
        return "on" + NameConverter.underscoredToJava(event.name(), true);
    }
}
