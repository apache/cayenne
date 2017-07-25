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

import java.util.HashMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Map that stores graph builders <b>for a single domain</b> by their type 
 * and has additional methods to set currently selected graph and serialize to XML
 */
public class GraphMap extends HashMap<GraphType, GraphBuilder> implements XMLSerializable {
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

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.print("<graphs");
//        if (selectedType != null) {
//            encoder.print(" selected=\"" + selectedType + "\"");
//        }
        encoder.println(">");
        encoder.indent(1);
        
        for (GraphBuilder builder : values()) {
            builder.encodeAsXML(encoder, delegate);
        }
        
        encoder.indent(-1);
        encoder.println("</graphs>");
    }
    
    public GraphBuilder createGraphBuilder(GraphType type, boolean doLayout) {
        try {
            GraphBuilder builder = type.getBuilderClass().newInstance();
            builder.buildGraph(getProjectController(), domain, doLayout);
            put(type, builder);
            
            return builder;
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Could not instantiate GraphBuilder", e);
        }
    }
    
    //TODO do not use static context
    private ProjectController getProjectController() {
        return Application.getInstance().getFrameController().getProjectController();
    }
}
