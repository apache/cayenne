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

import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.modeler.Application;


/**
 * Action class for creating callback methods on DataMap entity listener
 *
 * @version 1.0 Oct 30, 2007
 */
public class CreateCallbackMethodForDataMapListenerAction extends AbstractCreateCallbackMethodAction {
    /**
     * unique action name
     */
    public static final String ACTION_NAME = "Create callback method for data map entity listener";

    /**
     * Constructor.
     *
     * @param application Application instance
     */
    public CreateCallbackMethodForDataMapListenerAction(Application application) {
        super(ACTION_NAME, application);
    }

    /**
     * @return CallbackMap instance where to create a method
     */
    public CallbackMap getCallbackMap() {
        String listenerClass = getProjectController().getCurrentListenerClass();
        return getProjectController().getCurrentDataMap().getDefaultEntityListener(listenerClass).getCallbackMap();
    }
}

