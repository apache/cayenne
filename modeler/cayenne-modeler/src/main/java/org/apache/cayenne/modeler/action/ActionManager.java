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

import javax.swing.Action;
import javax.swing.JComponent;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.modeler.Application;

/**
 * Stores a map of modeler actions, and deals with activating/deactivating those actions
 * on state changes.
 */
public interface ActionManager {

    /**
     * Returns an action for key.
     */
    <T extends Action> T getAction(Class<T> actionClass);

    /**
     * Updates actions state to reflect an open project.
     */
    void projectOpened();

    void projectClosed();

    /**
     * Updates actions state to reflect DataDomain selection.
     */
    void domainSelected();

    void dataNodeSelected();

    void dataMapSelected();

    void objEntitySelected();

    void dbEntitySelected();

    void procedureSelected();

    void querySelected();

    void embeddableSelected();

    /**
     * Invoked when several objects were selected in ProjectTree at time
     */
    void multipleObjectsSelected(ConfigurationNode[] objects, Application application);

    /**
     * Replaces standard Cut, Copy and Paste action maps, so that accelerators like
     * Ctrl+X, Ctrl+C, Ctrl+V would work.
     */
    void setupCutCopyPaste(
            JComponent comp,
            Class<? extends Action> cutActionType,
            Class<? extends Action> copyActionType);
}
