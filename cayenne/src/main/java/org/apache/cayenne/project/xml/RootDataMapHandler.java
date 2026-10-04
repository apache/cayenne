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
import org.apache.cayenne.resource.Resource;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
public class RootDataMapHandler extends VersionAwareHandler {

    private final Resource configurationSource;
    private DataMapHandler dataMapHandler;

    public RootDataMapHandler(LoaderContext loaderContext, Resource configurationSource) {
        super(loaderContext, DataMapHandler.DATA_MAP_TAG);
        setTargetNamespace(DataMap.SCHEMA_XSD);
        this.configurationSource = configurationSource;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        // the name of the root tag before version 14
        if ("data-map".equals(localName)) {
            validateVersion(namespaceURI, attributes);
        }
        return super.processElement(namespaceURI, localName, attributes);
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName, String qName,
                                                   Attributes attributes) {
        if (targetNamespace.equals(namespaceURI) && DataMapHandler.DATA_MAP_TAG.equals(localName)) {
            dataMapHandler = new DataMapHandler(this, configurationSource);
            return dataMapHandler;
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    /**
     * Returns the DataMap loaded by this handler, or null if there was none in the document.
     */
    public DataMap getDataMap() {
        return dataMapHandler != null ? dataMapHandler.getDataMap() : null;
    }
}
