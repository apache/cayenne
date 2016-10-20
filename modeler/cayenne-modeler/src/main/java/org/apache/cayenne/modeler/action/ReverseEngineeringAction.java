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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.db.ConnectionWizard;
import org.apache.cayenne.modeler.dialog.db.DbLoaderHelper;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.CayenneAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;

/**
 * Action that imports database structure into a DataMap.
 */
public class ReverseEngineeringAction extends DBWizardAction {

    public ReverseEngineeringAction(Application application) {
        super(getActionName(), application);
    }

    public static String getActionName() {
        return "Reengineer Database Schema";
    }

    /**
     * Connects to DB and delegates processing to DbLoaderController, starting it asynchronously.
     */
    @Override
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
        ReverseEngineering reverseEngineering = new ReverseEngineering();

        final DbLoaderHelper helper = new DbLoaderHelper(
                getProjectController(),
                connection,
                adapter,
                dataSourceInfo,
                reverseEngineering);
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
}
