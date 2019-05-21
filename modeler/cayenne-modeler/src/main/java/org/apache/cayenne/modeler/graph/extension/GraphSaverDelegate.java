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

package org.apache.cayenne.modeler.graph.extension;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.modeler.graph.GraphMap;
import org.apache.cayenne.modeler.graph.GraphRegistry;
import org.apache.cayenne.project.extension.BaseSaverDelegate;

/**
 * @since 4.1
 */
class GraphSaverDelegate extends BaseSaverDelegate {

    DataChannelMetaData metaData;

    GraphSaverDelegate(DataChannelMetaData metaData) {
        this.metaData = metaData;
    }

    @Override
    public Void visitDataChannelDescriptor(DataChannelDescriptor channelDescriptor) {
        GraphRegistry registry = metaData.get(channelDescriptor, GraphRegistry.class);
        if (registry == null) {
            return null;
        }

        if(isStandalone()) {
            printGraphs(channelDescriptor);
        } else {
            printInclude(channelDescriptor);
        }

        return null;
    }

    private void printGraphs(DataChannelDescriptor channelDescriptor) {
        GraphRegistry registry = metaData.get(channelDescriptor, GraphRegistry.class);
        GraphMap map = registry.getGraphMap(channelDescriptor);
        encoder.start("graphs")
                .attribute("xmlns", GraphExtension.NAMESPACE)
                .nested(map, getParentDelegate())
                .end();
    }

    private void printInclude(DataChannelDescriptor channelDescriptor) {
        encoder.start("xi:include")
                .attribute("xmlns:xi", "http://www.w3.org/2001/XInclude")
                .attribute("href", channelDescriptor.getName() + GraphExtension.GRAPH_SUFFIX)
                .end();
    }


}
