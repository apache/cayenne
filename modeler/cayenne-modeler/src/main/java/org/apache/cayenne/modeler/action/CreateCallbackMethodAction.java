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
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.CallbackType;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.undo.CreateCallbackMethodUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.map.naming.NameConverter;

/**
 * Action class for creating callback methods on ObjEntity
 *
 * @version 1.0 Oct 30, 2007
 */
public class CreateCallbackMethodAction extends CayenneAction {
    
    /**
     * unique action name
     */
    public static final String ACTION_NAME = "Create callback method";

    /**
     * Constructor.
     * 
     * @param actionName unique action name
     * @param application Application instance
     */
    public CreateCallbackMethodAction(String actionName, Application application) {
        super(actionName, application);
    }

    /**
     * @return CallbackMap instance where to create a method
     */
    public CallbackMap getCallbackMap() {
        return getProjectController().getCurrentObjEntity().getCallbackMap();
    }
    
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
        String methodName = DefaultUniqueNameGenerator.generate(NameCheckers.ObjCallbackMethod, getProjectController().getCurrentObjEntity(), methodNamePrefix);

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
        return "on" + NameConverter.underscoredToJava(event.name(), true);
    }
    
    public static String getActionName() {
        return ACTION_NAME;
    }
    
    /**
     * Constructor.
     *
     * @param application Application instance
     */
    public CreateCallbackMethodAction(Application application) {
        super(ACTION_NAME, application);
    }
}

