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
package org.apache.cayenne.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.configuration.BaseConfigurationNodeVisitor;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;

/**
 * @since 3.1
 */
class SaveableNodesGetter extends
        BaseConfigurationNodeVisitor<Collection<ConfigurationNode>> {

    @Override
    public Collection<ConfigurationNode> visitDataChannelDescriptor(
            DataChannelDescriptor descriptor) {

        Collection<ConfigurationNode> nodes = new ArrayList<ConfigurationNode>();
        nodes.add(descriptor);

        for (DataMap map : descriptor.getDataMaps()) {
            nodes.add(map);
        }

        return nodes;
    }

    @Override
    public Collection<ConfigurationNode> visitDataMap(DataMap dataMap) {
        return Collections.<ConfigurationNode> singletonList(dataMap);
    }
}
