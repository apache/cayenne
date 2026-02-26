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
package org.apache.cayenne.project;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.xml.ProjectVersion;
import org.apache.cayenne.resource.Resource;

/**
 * A model of a Cayenne mapping project. A project consists of descriptors for
 * DataChannel, DataNodes and DataMaps and associated filesystem files they are
 * loaded from and saved to.
 * 
 * @since 3.1
 */
public class Project {

	/**
	 * Current version of Cayenne project.
	 * Used by different parsers and savers of project's XML files.
	 *
	 * @since 4.1
	 * @deprecated since 5.0 use {@link ProjectVersion#getCurrent()} instead
	 */
	@Deprecated(since = "5.0", forRemoval = true)
	public static final int VERSION = (int) ProjectVersion.getCurrent().getAsDouble();

	protected boolean modified;

	protected ConfigurationTree<?> configurationTree;
	private ConfigurationNodeVisitor<Resource> configurationSourceGetter;
	private Collection<URL> unusedResources;

	public Project(ConfigurationTree<?> configurationTree) {
		this.configurationTree = configurationTree;
		this.configurationSourceGetter = new ConfigurationSourceGetter();
		this.unusedResources = new HashSet<>();
	}

	public ConfigurationTree<?> getConfigurationTree() {
		return configurationTree;
	}

	public ConfigurationNode getRootNode() {
		return configurationTree.getRootNode();
	}

	/**
	 * Returns <code>true</code> if the project is modified.
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * Updates "modified" state of the project.
	 */
	public void setModified(boolean modified) {
		this.modified = modified;
	}

	public Resource getConfigurationResource(ConfigurationNode configNode) {
		return configNode.acceptVisitor(configurationSourceGetter);
	}

	public Resource getConfigurationResource() {
		return configurationTree.getRootNode().acceptVisitor(configurationSourceGetter);
	}

	public Collection<URL> getUnusedResources() {
		return unusedResources;
	}
}
