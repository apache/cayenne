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

import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.undo.UndoableEdit;

import org.apache.cayenne.util.Util;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class to load graph from XML
 */
public class GraphLoader extends DefaultHandler {
    static final String GRAPH_TAG = "graph";
    
    static final String ENTITY_TAG = "entity";
    
    /**
     * Map graphs will be loaded into
     */
    GraphMap map;
    
    /**
     * Current builder
     */
    GraphBuilder builder;
    
    /**
     * Changed properties for every builder
     */
    Map<DefaultGraphCell, Map<String, ?>> propertiesMap;
            
    public GraphLoader(GraphMap map) {
        this.map = map;
    }
    
    @Override
    public void startElement(
            String uri,
            String localName,
            String qName,
            Attributes attributes) throws SAXException {
        if (GRAPH_TAG.equalsIgnoreCase(localName)) {
            String type = attributes.getValue("", "type");
            if (Util.isEmptyString(type)) {
                throw new SAXException("Graph type not specified");
            }
            
            GraphType graphType = GraphType.valueOf(type);
            if (graphType == null) {
                throw new SAXException("Graph type " + type + " not supported");
            }
            
            builder = map.createGraphBuilder(graphType, false);
            builder.getGraph().setScale(getAsDouble(attributes, "scale"));
            
            propertiesMap = new Hashtable<DefaultGraphCell, Map<String,?>>();
        }
        else if (ENTITY_TAG.equalsIgnoreCase(localName)) {
            String name = attributes.getValue("", "name");
            DefaultGraphCell cell = builder.getEntityCell(name);
            if (cell != null) {
                Map<String, Object> props = new Hashtable<String, Object>();
                GraphConstants.setBounds(props, 
                        new Rectangle2D.Double(
                            getAsDouble(attributes, "x"),
                            getAsDouble(attributes, "y"),
                            getAsDouble(attributes, "width"),
                            getAsDouble(attributes, "height")
                        ));
                propertiesMap.put(cell, props);
            }
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (GRAPH_TAG.equalsIgnoreCase(localName)) {
            //apply changes
            builder.getGraph().getGraphLayoutCache().getModel().removeUndoableEditListener(builder);
            builder.getGraph().getGraphLayoutCache().edit(propertiesMap, null, null, new UndoableEdit[0]);
            builder.getGraph().getGraphLayoutCache().getModel().addUndoableEditListener(builder);
        }
    }
    
    private double getAsDouble(Attributes atts, String key) {
        return Double.valueOf(atts.getValue("", key));
    }
}
