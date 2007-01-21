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
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.DBGeneratorOptions;
import org.apache.cayenne.modeler.dialog.db.DataSourceWizard;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.project.ProjectPath;

/**
 * Action that generates database tables from a DataMap.
 */
public class GenerateDBAction extends DBWizardAction {

    public static String getActionName() {
        return "Generate Database Schema";
    }

    public GenerateDBAction(Application application) {
        super(getActionName(), application);
    }

    public void performAction(ActionEvent e) {

        DBConnectionInfo nodeInfo = preferredDataSource();
        String nodeKey = preferredDataSourceLabel(nodeInfo);

        DataSourceWizard connectWizard = new DataSourceWizard(
                getProjectController(),
                "Generate DB Schema: Connect to Database",
                nodeKey,
                nodeInfo);

        if (!connectWizard.startupAction()) {
            // canceled
            return;
        }

        DataMap map = getProjectController().getCurrentDataMap();

        // sanity check
        if (map == null) {
            throw new IllegalStateException("No current DataMap selected.");
        }

        // ... show dialog...
        new DBGeneratorOptions(
                getProjectController(),
                "Generate DB Schema: Options",
                connectWizard.getConnectionInfo(),
                map).startupAction();
    }

    /**
     * Returns <code>true</code> if path contains a DataMap object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DataMap.class) != null;
    }
}
