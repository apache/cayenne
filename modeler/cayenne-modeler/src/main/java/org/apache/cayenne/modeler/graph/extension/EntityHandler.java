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

import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.jgraph.graph.GraphConstants;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
class EntityHandler extends NamespaceAwareNestedTagHandler {

    static final String ENTITY_TAG = "entity";

    GraphHandler graphHandler;

    public EntityHandler(GraphHandler parentHandler) {
        super(parentHandler);
        this.graphHandler = parentHandler;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case ENTITY_TAG:
                String name = attributes.getValue("name");
                Map<String, Object> props = new Hashtable<>();
                GraphConstants.setBounds(props,
                        new Rectangle2D.Double(
                                Double.valueOf(attributes.getValue("x")),
                                Double.valueOf(attributes.getValue("y")),
                                Double.valueOf(attributes.getValue("width")),
                                Double.valueOf(attributes.getValue("height"))
                        ));
                graphHandler.propertiesMap.put(name, props);
                return true;
        }
        return false;
    }
}
