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
package org.apache.cayenne.configuration;

import org.apache.cayenne.resource.Resource;

/**
 * A service that maps the names of configuration objects to the resource names.
 */
public interface ConfigurationNameMapper {

	/**
	 * Returns the name of a configuration resource based on a naming convention
	 * for a given node type.
	 */
	String configurationLocation(ConfigurationNode node);

	/**
	 * Returns the name of a configuration resource based on a naming convention
	 * for a given node type.
	 */
	String configurationLocation(Class<? extends ConfigurationNode> type, String nodeName);

	/**
	 * Returns a node name for a given configuration type and a configuration
	 * resource. This operation is the opposite of the
	 * {@link #configurationLocation(Class, String)} . May return null if the
	 * resource name is not following the expected naming format.
	 */
	String configurationNodeName(Class<? extends ConfigurationNode> type, Resource resource);
}
