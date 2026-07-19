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

package org.apache.cayenne.modeler.ui.action;

import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.event.model.DataNodeEvent;
import org.apache.cayenne.configuration.runtime.XMLPoolingDataSourceFactory;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.event.display.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.toolkit.AppAction;
import org.apache.cayenne.modeler.undo.CreateNodeUndoableEdit;

import java.awt.event.ActionEvent;

public class CreateNodeAction extends AppAction {

    public static void createDataNode(Object src, ProjectSession session, DataNodeDescriptor node) {
        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();
        domain.getNodeDescriptors().add(node);
        session.fireDataNodeEvent(DataNodeEvent.ofAdd(src, node));
        session.displayDataNode(new DataNodeDisplayEvent(src, domain, node));
    }

    public CreateNodeAction(Application application) {
        super(application, "Create DataNode");
    }

    @Override
    public String getIconName() {
        return "icon-node.png";
    }

    @Override
    public void performAction(ActionEvent e) {
        DataNodeDescriptor node = buildDataNode();
        createDataNode(this, getProjectSession(), node);
        app.getUndoManager().addEdit(new CreateNodeUndoableEdit(getProjectSession(), node));
    }


    /**
     * Returns <code>true</code> if path contains a DataDomain object.
     */
    public boolean enableForPath(ConfigurationNode object) {
        return object != null && ((DataNodeDescriptor) object).getDataChannelDescriptor() != null;

    }

    /**
     * Creates a new DataNode, adding to the current domain, but doesn't send
     * any events.
     */
    public DataNodeDescriptor buildDataNode() {
        DataChannelDescriptor domain = (DataChannelDescriptor) getProjectSession().project().getRootNode();

        DataNodeDescriptor node = buildDataNode(domain);
        node.setDataSourceDescriptor(new DataSourceDescriptor());

        // by default create JDBC Node
        node.setDataSourceFactoryType(XMLPoolingDataSourceFactory.class.getName());
        node.setSchemaUpdateStrategyType(SkipSchemaUpdateStrategy.class.getName());

        return node;
    }

    /**
     * A factory method that makes a new DataNode.
     */
    DataNodeDescriptor buildDataNode(DataChannelDescriptor dataChannelDescriptor) {
        DataNodeDescriptor node = new DataNodeDescriptor();
        node.setName(NameBuilder.of(node, dataChannelDescriptor).build());
        node.setDataChannelDescriptor(dataChannelDescriptor);

        return node;
    }
}
