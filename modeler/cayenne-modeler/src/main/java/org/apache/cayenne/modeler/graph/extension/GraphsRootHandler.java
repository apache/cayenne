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

package org.apache.cayenne.modeler.graph.extension;

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.modeler.Application;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
class GraphsRootHandler extends NamespaceAwareNestedTagHandler {

    static final String GRAPHS_TAG = "graphs";

    Application application;

    public GraphsRootHandler(NamespaceAwareNestedTagHandler parentHandler, Application application) {
        super(parentHandler);
        setTargetNamespace(GraphExtension.NAMESPACE);
        this.application = application;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return GRAPHS_TAG.equals(localName);
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName, String qName, Attributes attributes) {
        if(GraphHandler.GRAPH_TAG.equals(localName)) {
            return new GraphHandler(this, application);
        }
        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }
}
