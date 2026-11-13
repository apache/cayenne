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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class EmbeddableHandler extends NamespaceAwareNestedTagHandler {

    static final String EMBEDDABLE_TAG = "embeddable";
    private static final String EMBEDDABLE_ATTRIBUTE_TAG = "embeddableAttribute";

    private DataMap map;

    private Embeddable embeddable;

    private EmbeddableAttribute lastAttribute;

    public EmbeddableHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return switch (localName) {
            case EMBEDDABLE_TAG -> {
                createEmbeddable(attributes);
                yield true;
            }
            case EMBEDDABLE_ATTRIBUTE_TAG -> {
                createEmbeddableAttribute(attributes);
                yield true;
            }
            case CommentHandler.COMMENT_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (CommentHandler.COMMENT_TAG.equals(localName)) {
            CommentHandler.loadComment(loaderContext, embeddable, data);
            return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {

        if (namespaceURI.equals(targetNamespace) && EMBEDDABLE_ATTRIBUTE_TAG.equals(localName)) {
            return new CommentHandler(this, () -> lastAttribute);
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    private void createEmbeddable(Attributes attributes) {
        embeddable = new Embeddable(attributes.getValue("className"));
        map.addEmbeddable(embeddable);
    }

    private void createEmbeddableAttribute(Attributes attributes) {
        lastAttribute = new EmbeddableAttribute(attributes.getValue("name"));
        lastAttribute.setType(attributes.getValue("type"));
        lastAttribute.setDbAttributeName(attributes.getValue("dbAttributeName"));
        embeddable.addAttribute(lastAttribute);
    }
}
