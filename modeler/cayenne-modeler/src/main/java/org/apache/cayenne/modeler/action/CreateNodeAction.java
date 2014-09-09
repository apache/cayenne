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

import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.configuration.server.XMLPoolingDataSourceFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.naming.DefaultUniqueNameGenerator;
import org.apache.cayenne.map.naming.NameCheckers;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.undo.CreateNodeUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;

public class CreateNodeAction extends CayenneAction {

	public static String getActionName() {
		return "Create DataNode";
	}

	/**
	 * Constructor for CreateNodeAction.
	 * 
	 * @param application
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
		DataNodeDescriptor node = buildDataNode();
		createDataNode(node);
		application.getUndoManager().addEdit(new CreateNodeUndoableEdit(application, node));
	}

	public void createDataNode(DataNodeDescriptor node) {
		DataChannelDescriptor domain = (DataChannelDescriptor) getProjectController().getProject().getRootNode();
		domain.getNodeDescriptors().add(node);
		getProjectController().fireDataNodeEvent(new DataNodeEvent(this, node, MapEvent.ADD));
		getProjectController().fireDataNodeDisplayEvent(new DataNodeDisplayEvent(this, domain, node));
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
		ProjectController mediator = getProjectController();
		DataChannelDescriptor domain = (DataChannelDescriptor) mediator.getProject().getRootNode();

		DataNodeDescriptor node = buildDataNode(domain);

		DataSourceInfo src = new DataSourceInfo();
		node.setDataSourceDescriptor(src);

		// by default create JDBC Node
		node.setDataSourceFactoryType(XMLPoolingDataSourceFactory.class.getName());
		node.setSchemaUpdateStrategyType(SkipSchemaUpdateStrategy.class.getName());

		return node;
	}

	/**
	 * A factory method that makes a new DataNode.
	 */
	DataNodeDescriptor buildDataNode(DataChannelDescriptor domain) {
		DataNodeDescriptor node = new DataNodeDescriptor(DefaultUniqueNameGenerator.generate(
				NameCheckers.dataNodeDescriptor, domain));
		node.setDataChannelDescriptor(domain);

		return node;
	}
}
