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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class EmbeddableHandler extends NamespaceAwareNestedTagHandler {

    private static final String EMBEDDABLE_TAG = "embeddable";
    private static final String EMBEDDABLE_ATTRIBUTE_TAG = "embeddable-attribute";

    private DataMap map;

    private Embeddable embeddable;

    public EmbeddableHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case EMBEDDABLE_TAG:
                createEmbeddable(attributes);
                return true;

            case EMBEDDABLE_ATTRIBUTE_TAG:
                createEmbeddableAttribute(attributes);
                return true;
        }

        return false;
    }

    private void createEmbeddable(Attributes attributes) {
        embeddable = new Embeddable(attributes.getValue("className"));
        map.addEmbeddable(embeddable);
    }

    private void createEmbeddableAttribute(Attributes attributes) {
        EmbeddableAttribute ea = new EmbeddableAttribute(attributes.getValue("name"));
        ea.setType(attributes.getValue("type"));
        ea.setDbAttributeName(attributes.getValue("db-attribute-name"));
        embeddable.addAttribute(ea);
    }

    public Embeddable getEmbeddable() {
        return embeddable;
    }
}
