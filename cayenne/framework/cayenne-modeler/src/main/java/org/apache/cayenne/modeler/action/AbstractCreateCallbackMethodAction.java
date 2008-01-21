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
import org.apache.cayenne.modeler.editor.CallbackType;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.util.CayenneAction;

/**
 * Base class for creating callback methods
 *
 * @author Vasil Tarasevich
 * @version 1.0 Oct 28, 2007
 */
public abstract class AbstractCreateCallbackMethodAction extends CayenneAction {
    /**
     * default name for new callback method
     */
    private static final String NEW_CALLBACK_METHOD = "untitled";

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
     * @param e event
     */
    public final void performAction(ActionEvent e) {
        CallbackType callbackType = getProjectController().getCurrentCallbackType();

        //generate methodName
        int counter = 1;
        String methodName;
        do {
            methodName = NEW_CALLBACK_METHOD + counter;
            counter++;
        } while(getCallbackMap().getCallbackDescriptor(callbackType.getType()).getCallbackMethods().contains(methodName));

        getCallbackMap().getCallbackDescriptor(callbackType.getType()).addCallbackMethod(methodName);

        CallbackMethodEvent ce = new CallbackMethodEvent(
                e.getSource(),
                null,
                methodName,
                MapEvent.ADD
        );

        getProjectController().fireCallbackMethodEvent(ce);
    }
}

