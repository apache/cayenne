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
package org.apache.cayenne.configuration.server;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;

/**
 * @since 3.2
 */
class SyntheticNodeDataDomainProvider extends DataDomainProvider {

    @Override
    protected DataDomain createAndInitDataDomain() throws Exception {

        DataDomain dataDomain = super.createAndInitDataDomain();

        // no nodes... add a synthetic node... it will become the default
        if (dataDomain.getDataNodes().isEmpty()) {

            DataChannelDescriptor channelDescriptor = new DataChannelDescriptor();
            channelDescriptor.setName(DEFAULT_NAME);

            DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor(DEFAULT_NAME);
            nodeDescriptor.setDataChannelDescriptor(channelDescriptor);

            DataNode node = addDataNode(dataDomain, nodeDescriptor);
            dataDomain.setDefaultNode(node);
        }
        return dataDomain;
    }

}
