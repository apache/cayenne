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
package org.apache.cayenne.modeler.graph;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.DomainEvent;
import org.apache.cayenne.configuration.event.DomainListener;
import org.apache.cayenne.modeler.ProjectController;
import org.jgraph.JGraph;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for storing information about datadomain's builders.
 * Has methods for saving and loading graphs, as well as building graph for
 * specified data domain.
 * This class is center of all graph management in CM
 */
public class GraphRegistry implements DomainListener {

    /**
     * Main storage of graph builders
     */
    Map<DataChannelDescriptor, GraphMap> graphMaps;

    public GraphRegistry() {
        graphMaps = new HashMap<>();
    }

    /**
     * Builds graph with specified type, or returns existing one
     */
    public JGraph loadGraph(ProjectController mediator, DataChannelDescriptor domain, GraphType type) {
        GraphMap graphMap = graphMaps.get(domain);
        if (graphMap == null) {
            graphMap = new GraphMap(domain);
            graphMaps.put(domain, graphMap);
        }

        GraphBuilder builder = graphMap.get(type);
        if (builder == null) {
            builder = graphMap.createGraphBuilder(type, true);
            
            mediator.setDirty(true);
        }
        
        //marking this builder as default
        graphMap.setSelectedType(builder.getType());

        return builder.getGraph();
    }
    
    /**
     * Gets graph map for specified domain, creating it if needed
     */
    public GraphMap getGraphMap(DataChannelDescriptor domain) {
        GraphMap map = graphMaps.get(domain);
        if (map == null) {
            map = new GraphMap(domain);
            graphMaps.put(domain, map);
        }
        return map;
    }

    public void addGraphMap(DataChannelDescriptor domain, GraphMap map) {
        graphMaps.put(domain, map);
    }

    public void domainChanged(DomainEvent e) {
    }

    void unregisterDomain(DataChannelDescriptor dataChannelDescriptor) {
        GraphMap map = graphMaps.get(dataChannelDescriptor);
        if (map != null) {
            for (GraphBuilder builder : map.values()) {
                builder.destroy();
            }
            graphMaps.remove(dataChannelDescriptor);
        }
    }
    
    /**
     * Removes all listeners (and itself) from ProjectController
     */
    public void unregister(ProjectController mediator) {
        unregisterDomain((DataChannelDescriptor)mediator.getProject().getRootNode());
        mediator.removeDomainListener(this);
    }
}
