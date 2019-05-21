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
package org.apache.cayenne.modeler.graph;

import java.util.HashMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;

/**
 * Map that stores graph builders <b>for a single domain</b> by their type 
 * and has additional methods to set currently selected graph and serialize to XML
 */
public class GraphMap extends HashMap<GraphType, GraphBuilder> {
    /**
     * type that is currently selected
     */
    GraphType selectedType;
    
    /**
     * Domain
     */
    DataChannelDescriptor domain;
    
    public GraphMap(DataChannelDescriptor domain) {
        this.domain = domain;
    }
     
    /**
     * Returns domain
     */
    public DataChannelDescriptor getDomain() {
        return domain;
    }
    
    /**
     * Returns type that is currently selected
     */
    public GraphType getSelectedType() {
        return selectedType;
    }
    
    /**
     * Sets type that is currently selected
     */
    public void setSelectedType(GraphType selectedType) {
        this.selectedType = selectedType;
    }
    
    public GraphBuilder createGraphBuilder(GraphType type, boolean doLayout) {
        try {
            GraphBuilder builder = type.getBuilderClass().newInstance();
            builder.buildGraph(getProjectController(), domain, doLayout);
            put(type, builder);
            
            return builder;
        } catch (Exception e) {
            throw new CayenneRuntimeException("Could not instantiate GraphBuilder", e);
        }
    }

    private ProjectController getProjectController() {
        return Application.getInstance().getFrameController().getProjectController();
    }
}
