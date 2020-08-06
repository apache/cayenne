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

package org.apache.cayenne.modeler.util.state;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.pref.ProjectStatePreferences;

import java.util.EventObject;
import java.util.prefs.BackingStoreException;

public final class ProjectStateUtil {

    public ProjectStateUtil() {
    }

    public void saveLastState(ProjectController controller) {
        EventObject displayEvent = controller.getLastDisplayEvent();
        ConfigurationNode[] multiplyObjects = controller.getCurrentPaths();

        if (displayEvent == null && multiplyObjects == null) {
            return;
        }

        ProjectStatePreferences preferences = controller.getProjectStatePreferences();
        if (preferences.getCurrentPreference() == null) {
            return;
        }

        try {
            preferences.getCurrentPreference().clear();
        } catch (BackingStoreException e) {
            // ignore exception
        }

        if (displayEvent != null) {
            DisplayEventTypes.valueOf(displayEvent.getClass().getSimpleName())
                    .createDisplayEventType(controller)
                    .saveLastDisplayEvent();
        } else if (multiplyObjects.length != 0) {
            new MultipleObjectsDisplayEventType(controller).saveLastDisplayEvent();
        }
    }

    public void fireLastState(ProjectController controller) {
        ProjectStatePreferences preferences = controller.getProjectStatePreferences();

        String displayEventName = preferences.getEvent();
        if (!displayEventName.isEmpty()) {
            DisplayEventTypes.valueOf(displayEventName)
                    .createDisplayEventType(controller)
                    .fireLastDisplayEvent();
        }
    }

}
