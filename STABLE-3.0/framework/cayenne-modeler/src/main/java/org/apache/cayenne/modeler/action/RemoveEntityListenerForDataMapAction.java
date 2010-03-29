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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.ConfirmRemoveDialog;
import org.apache.cayenne.modeler.event.EntityListenerEvent;


/**
 * Action class for removing entity listeners from a DamaMap
 *
 * @version 1.0 Oct 30, 2007
 */
public class RemoveEntityListenerForDataMapAction extends RemoveAction {
    
    
    /**
     * unique action name
     */
    private static final String ACTION_NAME = "Remove entity listener for data map";

    /**
     * Constructor.
     *
     * @param application Application instance
     */
    public RemoveEntityListenerForDataMapAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * @return unique action name
     */
    public static String getActionName() {
        return ACTION_NAME;
    }

    /**
     * @return icon file name for button
     */
    public String getIconName() {
        return "icon-remove-listener.gif";
    }
    
    /**
     * base entity listener removing logic
     * @param e event
     */
    public void performAction(ActionEvent e, boolean allowAsking) {
        ConfirmRemoveDialog dialog = getConfirmDeleteDialog(allowAsking);

        if (getProjectController().getCurrentListenerClass() != null) {
            if (dialog.shouldDelete("entity listener", getProjectController()
                    .getCurrentListenerClass())) {
                
                String listenerClass = getProjectController().getCurrentListenerClass();
                removeEntityListener(getProjectController().getCurrentDataMap(), listenerClass);
            }
        }
    }

    public void removeEntityListener(DataMap map, String listenerClass) {
        if (listenerClass != null) {
            map.removeDefaultEntityListener(listenerClass);

            getProjectController().fireEntityListenerEvent(
                    new EntityListenerEvent(
                            RemoveEntityListenerForDataMapAction.this,
                            listenerClass,
                            listenerClass,
                            MapEvent.REMOVE
                    )
            );
        }
    }
}

