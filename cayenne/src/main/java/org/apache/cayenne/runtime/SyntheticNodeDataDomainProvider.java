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
package org.apache.cayenne.runtime;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.DataDomainProvider;
import org.apache.cayenne.map.DataMap;

/**
 * @since 4.0
 */
class SyntheticNodeDataDomainProvider extends DataDomainProvider {

	@Override
	protected DataDomain createAndInitDataDomain() throws Exception {

		DataDomain dataDomain = super.createAndInitDataDomain();

		// no nodes... add a synthetic node... it will become the default
		if (dataDomain.getDataNodes().isEmpty()) {

			DataChannelDescriptor channelDescriptor = new DataChannelDescriptor();

			DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor(createSyntheticDataNodeName(dataDomain));

			for (DataMap map : dataDomain.getDataMaps()) {
				nodeDescriptor.getDataMapNames().add(map.getName());
			}

			nodeDescriptor.setDataChannelDescriptor(channelDescriptor);

			DataNode node = addDataNode(dataDomain, nodeDescriptor);
			dataDomain.setDefaultNode(node);
		}
		return dataDomain;
	}

	protected String createSyntheticDataNodeName(DataDomain domain) {

		// using Domain's name for the node name..
		// distinguishing nodes by name may be useful in case of multiple stacks used in the same transaction...

		return domain.getName() != null ? domain.getName() : CayenneRuntimeBuilder.DEFAULT_NAME;
	}

}
