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
 * Action class for removing callback methods from DataMap's entity listener
 *
 * @version 1.0 Oct 30, 2007
 */
public class RemoveCallbackMethodForDataMapListenerAction extends AbstractRemoveCallbackMethodAction {
    /**
     * unique action name
     */
    public final static String ACTION_NAME = "Remove callback method for data map entity listener";
    
    /**
     * action name for multiple selection
     */
    private final static String ACTION_NAME_MULTIPLE = "Remove callback methods for data map entity listener";

    /**
     * Constructor.
     *
     * @param application Application instance
     */
    public RemoveCallbackMethodForDataMapListenerAction(Application application) {
        super(ACTION_NAME, application);
    }

    /**
     * @return CallbackMap fom which remove callback method
     */
    @Override
    public CallbackMap getCallbackMap() {
        String listenerClass = getProjectController().getCurrentListenerClass();
        return getProjectController().getCurrentDataMap().getDefaultEntityListener(listenerClass).getCallbackMap();
    }

    @Override
    public String getActionName(boolean multiple) {
        return multiple ? ACTION_NAME_MULTIPLE : ACTION_NAME;
    }
}

