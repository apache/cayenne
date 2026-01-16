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

import java.util.HashMap;
import java.util.Map;

import javax.swing.undo.UndoableEdit;

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.graph.GraphBuilder;
import org.apache.cayenne.modeler.graph.GraphMap;
import org.apache.cayenne.modeler.graph.GraphRegistry;
import org.apache.cayenne.modeler.graph.GraphType;
import org.apache.cayenne.util.Util;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Class to load graph from XML
 */
class GraphHandler extends NamespaceAwareNestedTagHandler {

    static final String GRAPH_TAG = "graph";

    static final String ENTITY_TAG = "entity";

    Map<String, Map<String, ?>> propertiesMap;

    GraphType graphType;

    double scale;
            
    public GraphHandler(NamespaceAwareNestedTagHandler parent, final Application application) {
        super(parent);
        loaderContext.addDataChannelListener(dataChannelDescriptor -> {
            GraphRegistry registry = application.getMetaData().get(dataChannelDescriptor, GraphRegistry.class);
            if(registry == null) {
                registry = new GraphRegistry();
                application.getMetaData().add(dataChannelDescriptor, registry);
            }

            GraphMap map = registry.getGraphMap(dataChannelDescriptor);
            //apply changes
            GraphBuilder builder = map.createGraphBuilder(graphType, false);
            builder.getGraph().setScale(scale);

            // lookup
            Map<DefaultGraphCell, Map<String, ?>> propertiesMap = new HashMap<>();
            for (Map.Entry<String, Map<String, ?>> entry : this.propertiesMap.entrySet()) {
                DefaultGraphCell cell = builder.getEntityCell(entry.getKey());
                if (cell != null) {
                    // Prevent saved cell size from being reset on the next edit
                    GraphConstants.setResize(cell.getAttributes(), false);
                }
                propertiesMap.put(cell, entry.getValue());
            }

            builder.getGraph().getGraphLayoutCache().getModel().removeUndoableEditListener(builder);
            builder.getGraph().getGraphLayoutCache().edit(propertiesMap, null, null, new UndoableEdit[0]);
            builder.getGraph().getGraphLayoutCache().getModel().addUndoableEditListener(builder);
        });
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case GRAPH_TAG:
                String type = attributes.getValue("type");
                if (Util.isEmptyString(type)) {
                    throw new SAXException("Graph type not specified");
                }

                graphType = GraphType.valueOf(type);
                scale = Double.valueOf(attributes.getValue("scale"));
                propertiesMap = new HashMap<>();
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName, String qName, Attributes attributes) {
        switch (localName) {
            case ENTITY_TAG:
                return new EntityHandler(this);
        }
        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }
}
