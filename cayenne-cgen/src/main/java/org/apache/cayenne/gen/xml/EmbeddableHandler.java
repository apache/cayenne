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
package org.apache.cayenne.gen.xml;

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class EmbeddableHandler extends NamespaceAwareNestedTagHandler {

    private static final String EMBEDDABLE_TAG = "embeddable";
    private static final String EMBEDDABLE_NAME_TAG = "name";

    private ClassGenerationAction configuration;

    EmbeddableHandler(NamespaceAwareNestedTagHandler parentHandler, ClassGenerationAction configuration) {
        super(parentHandler);
        this.configuration = configuration;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case EMBEDDABLE_TAG:
                return true;
        }

        return false;
    }

    @Override
    protected void processCharData(String localName, String data) {
        switch (localName) {
            case EMBEDDABLE_NAME_TAG:
                createEmbeddableEntity(data);
                break;
        }
    }

    private void createEmbeddableEntity(String data) {
        if(data.trim().length() == 0) {
            return;
        }

        if(configuration != null) {
            configuration.loadEmbeddable(data);
        }
    }
}

