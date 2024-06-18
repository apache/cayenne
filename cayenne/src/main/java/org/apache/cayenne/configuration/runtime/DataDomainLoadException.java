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
package org.apache.cayenne.configuration.runtime;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;

/**
 * @since 3.1
 */
public class DataDomainLoadException extends ConfigurationException {

	private static final long serialVersionUID = 7969847819485380271L;
	
	private ConfigurationTree<DataChannelDescriptor> configurationTree;

	public DataDomainLoadException() {
	}

	public DataDomainLoadException(String messageFormat, Object... messageArgs) {
		super(messageFormat, messageArgs);
	}

	public DataDomainLoadException(ConfigurationTree<DataChannelDescriptor> configurationTree, String messageFormat,
			Object... messageArgs) {
		super(messageFormat, messageArgs);
		this.configurationTree = configurationTree;
	}

	public DataDomainLoadException(Throwable cause) {
		super(cause);
	}

	public DataDomainLoadException(String messageFormat, Throwable cause, Object... messageArgs) {
		super(messageFormat, cause, messageArgs);
	}

	public ConfigurationTree<DataChannelDescriptor> getConfigurationTree() {
		return configurationTree;
	}

}
