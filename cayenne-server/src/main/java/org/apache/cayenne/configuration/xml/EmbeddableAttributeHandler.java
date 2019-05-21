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

import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class EmbeddableAttributeHandler extends NamespaceAwareNestedTagHandler {

    private static final String EMBEDDED_ATTRIBUTE_TAG = "embedded-attribute";
    private static final String EMBEDDABLE_ATTRIBUTE_OVERRIDE_TAG = "embeddable-attribute-override";

    private ObjEntity entity;

    private EmbeddedAttribute embeddedAttribute;

    public EmbeddableAttributeHandler(NamespaceAwareNestedTagHandler parentHandler, ObjEntity entity) {
        super(parentHandler);
        this.entity = entity;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case EMBEDDED_ATTRIBUTE_TAG:
                createEmbeddableAttribute(attributes);
                return true;

            case EMBEDDABLE_ATTRIBUTE_OVERRIDE_TAG:
                createEmbeddableAttributeOverride(attributes);
                return true;
        }

        return false;
    }

    private void createEmbeddableAttribute(Attributes attributes) {
        embeddedAttribute = new EmbeddedAttribute(attributes.getValue("name"));
        embeddedAttribute.setType(attributes.getValue("type"));
        entity.addAttribute(embeddedAttribute);
    }

    private void createEmbeddableAttributeOverride(Attributes attributes) {
        embeddedAttribute.addAttributeOverride(attributes.getValue("name"),
                attributes.getValue("db-attribute-path"));
    }
}
