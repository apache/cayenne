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
import java.sql.Connection;

import javax.swing.SwingUtilities;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.ConnectionWizard;
import org.apache.cayenne.modeler.dialog.db.DbLoaderHelper;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.project.ProjectPath;

/**
 * Action that imports database structure into a DataMap.
 */
public class ImportDBAction extends DBWizardAction {

    public static String getActionName() {
        return "Reengineer Database Schema";
    }

    public ImportDBAction(Application application) {
        super(getActionName(), application);
    }

    /**
     * Connects to DB and delegates processing to DbLoaderController, starting it
     * asynchronously.
     */
    public void performAction(ActionEvent event) {

        // guess node connection
        DBConnectionInfo nodeInfo = preferredDataSource();
        String nodeKey = preferredDataSourceLabel(nodeInfo);

        // connect
        ConnectionWizard connectWizard = new ConnectionWizard(
                getProjectController(),
                "Reengineer DB Schema: Connect to Database",
                nodeKey,
                nodeInfo);

        if (!connectWizard.startupAction()) {
            // canceled
            return;
        }

        Connection connection = connectWizard.getConnection();
        DbAdapter adapter = connectWizard.getAdapter();
        DBConnectionInfo dataSourceInfo = connectWizard.getConnectionInfo();

        // from here pass control to DbLoaderHelper, running it from a thread separate
        // from EventDispatch

        final DbLoaderHelper helper = new DbLoaderHelper(
                getProjectController(),
                connection,
                adapter,
                dataSourceInfo.getUserName());
        Thread th = new Thread(new Runnable() {

            public void run() {
                helper.execute();

                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        application.getUndoManager().discardAllEdits();
                    }
                });
            }
        });
        
        th.start();
    }

    /**
     * Returns <code>true</code> if path contains a DataDomain object.
     */
    public boolean enableForPath(ProjectPath path) {
        if (path == null) {
            return false;
        }

        return path.firstInstanceOf(DataDomain.class) != null;
    }
}
