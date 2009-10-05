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

import javax.sql.DataSource;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateNodeUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.ModelerDbAdapter;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.project.ProjectDataSource;
import org.apache.cayenne.project.ProjectPath;

/**
 */
public class CreateNodeAction extends CayenneAction {

    

    public static String getActionName() {
        return "Create DataNode";
    }

    /**
     * Constructor for CreateNodeAction.
     * 
     * @param name
     */
    public CreateNodeAction(Application application) {
        super(getActionName(), application);
    }

    public String getIconName() {
        return "icon-node.gif";
    }

    /**
     * @see org.apache.cayenne.modeler.util.CayenneAction#performAction(ActionEvent)
     */
    public void performAction(ActionEvent e) {
        DataNode node = buildDataNode();
        createDataNode(getProjectController().getCurrentDataDomain(), node);
        application.getUndoManager().addEdit(
                new CreateNodeUndoableEdit(application, getProjectController()
                        .getCurrentDataDomain(), node));
    }

    public void createDataNode(DataDomain domain, DataNode node) {
        domain.addNode(node);
        getProjectController().fireDataNodeEvent(
                new DataNodeEvent(this, node, MapEvent.ADD));
        getProjectController().fireDataNodeDisplayEvent(
                new DataNodeDisplayEvent(this, domain, node));
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

    /**
     * Creates a new DataNode, adding to the current domain, but doesn't send any events.
     */
    public DataNode buildDataNode() {
        ProjectController mediator = getProjectController();
        DataDomain domain = mediator.getCurrentDataDomain();

        // use domain name as DataNode base, as node names must be unique across the
        // project...
        DataNode node = buildDataNode(domain);

        ProjectDataSource src = new ProjectDataSource(new DataSourceInfo());
        node.setDataSource(src);
        node.setAdapter(new ModelerDbAdapter(src));

        // by default create JDBC Node
        node.setDataSourceFactory(DriverDataSourceFactory.class.getName());
        node.setSchemaUpdateStrategyName(SkipSchemaUpdateStrategy.class.getName());

        return node;
    }

    /**
     * A factory method that makes a new DataNode.
     */
    DataNode buildDataNode(DataDomain domain) {
        String name = NamedObjectFactory.createName(DataNode.class, domain, domain
                .getName()
                + "Node");

        // ensure that DataNode exposes DataSource directly, so that UI widgets could work
        // with it.
        return new DataNode(name) {

            public DataSource getDataSource() {
                return dataSource;
            }
        };
    }
}
