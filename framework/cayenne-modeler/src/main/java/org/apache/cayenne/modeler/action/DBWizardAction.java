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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataNodeDefaults;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.ModelerDbAdapter;
import org.apache.cayenne.project.ProjectDataSource;

/**
 */
public abstract class DBWizardAction extends CayenneAction {

    public DBWizardAction(String name, Application application) {
        super(name, application);
    }

    // ==== Guessing user preferences... *****

    protected DataNode getPreferredNode() {
        ProjectController projectController = getProjectController();
        DataNode node = projectController.getCurrentDataNode();

        // try a node that belongs to the current DataMap ...
        if (node == null) {
            DataMap map = projectController.getCurrentDataMap();
            if (map != null) {
                node = projectController.getCurrentDataDomain().lookupDataNode(map);
            }
        }

        return node;
    }

    protected String preferredDataSourceLabel(DBConnectionInfo nodeInfo) {
        if (nodeInfo == null || nodeInfo.getDomainPreference() == null) {

            // only driver nodes have meaningful connection info set
            DataNode node = getPreferredNode();
            return (node != null && DriverDataSourceFactory.class.getName().equals(
                    node.getDataSourceFactory())) ? "DataNode Connection Info" : null;
        }

        return nodeInfo.getKey();
    }

    /**
     * Determines the most reasonable default DataSource choice.
     */
    protected DBConnectionInfo preferredDataSource() {
        DataNode node = getPreferredNode();

        // no current node...
        if (node == null) {
            return null;
        }

        // if node has local DS set, use it
        DataNodeDefaults nodeDefaults = (DataNodeDefaults) getProjectController()
                .getPreferenceDomainForDataDomain()
                .getDetail(node.getName(), DataNodeDefaults.class, false);

        String key = (nodeDefaults != null) ? nodeDefaults.getLocalDataSource() : null;
        if (key != null) {
            DBConnectionInfo info = (DBConnectionInfo) getApplication()
                    .getPreferenceDomain()
                    .getDetail(key, DBConnectionInfo.class, false);
            if (info != null) {
                return info;
            }
        }

        // extract data from the node
        if (!DriverDataSourceFactory.class.getName().equals(node.getDataSourceFactory())) {
            return null;
        }

        // create transient object..
        DBConnectionInfo nodeInfo = new DBConnectionInfo();

        nodeInfo.copyFrom(((ProjectDataSource) node.getDataSource()).getDataSourceInfo());

        nodeInfo.setDbAdapter(null);
        if (node.getAdapter() instanceof ModelerDbAdapter) {
            nodeInfo.setDbAdapter(((ModelerDbAdapter) node.getAdapter())
                    .getAdapterClassName());
        }

        return nodeInfo;
    }

}
