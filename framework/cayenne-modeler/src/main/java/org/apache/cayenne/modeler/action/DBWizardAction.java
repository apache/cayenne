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

import java.util.Collection;

import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.pref.DataNodeDefaults;
import org.apache.cayenne.modeler.util.CayenneAction;

/**
 */
public abstract class DBWizardAction extends CayenneAction {

    public DBWizardAction(String name, Application application) {
        super(name, application);
    }

    // ==== Guessing user preferences... *****

    protected DataNodeDescriptor getPreferredNode() {
        ProjectController projectController = getProjectController();
        DataNodeDescriptor node = projectController.getCurrentDataNode();

        // try a node that belongs to the current DataMap ...
        if (node == null) {
            DataMap map = projectController.getCurrentDataMap();
            if (map != null) {
                Collection<DataNodeDescriptor> nodes = ((DataChannelDescriptor)projectController.getProject().getRootNode()).getNodeDescriptors();
                for(DataNodeDescriptor n:nodes){
                    if(n.getDataMapNames().contains(map.getName())){
                        node = n;
                        break;
                    }
                }
            }
        }

        return node;
    }

    protected String preferredDataSourceLabel(DBConnectionInfo nodeInfo) {
        if (nodeInfo == null) {

            // only driver nodes have meaningful connection info set
            DataNodeDescriptor node = getPreferredNode();
            return (node != null && DriverDataSourceFactory.class.getName().equals(
                    node.getDataSourceFactoryType())) ? "DataNode Connection Info" : null;
        }

        return nodeInfo.getNodeName();
    }

    /**
     * Determines the most reasonable default DataSource choice.
     */
    protected DBConnectionInfo preferredDataSource() {
        DataNodeDescriptor node = getPreferredNode();

        // no current node...
        if (node == null) {
            return null;
        }

        // if node has local DS set, use it
        DataNodeDefaults nodeDefaults = (DataNodeDefaults) getApplication()
                .getCayenneProjectPreferences()
                .getProjectDetailObject(DataNodeDefaults.class,
                        getProjectController().getPreferenceForDataDomain().node(
                                "DataNode").node(node.getName()));

        String key = (nodeDefaults != null) ? nodeDefaults.getLocalDataSource() : null;
        if (key != null) {
            DBConnectionInfo info = (DBConnectionInfo) getApplication()
                    .getCayenneProjectPreferences()
                    .getDetailObject(DBConnectionInfo.class)
                    .getObject(key);

            if (info != null) {
                return info;
            }
        }

        // extract data from the node
        if (!DriverDataSourceFactory.class.getName().equals(node.getDataSourceFactoryType())) {
            return null;
        }

        // create transient object..
        DBConnectionInfo nodeInfo = new DBConnectionInfo();

        nodeInfo.copyFrom(node.getDataSourceDescriptor());

        nodeInfo.setDbAdapter(node.getAdapterType());

        return nodeInfo;
    }

}
