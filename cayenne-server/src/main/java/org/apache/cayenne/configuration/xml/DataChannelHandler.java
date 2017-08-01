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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.util.LocalizedStringsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
final class DataChannelHandler extends NamespaceAwareNestedTagHandler {

    private static Logger logger = LoggerFactory.getLogger(XMLDataChannelDescriptorLoader.class);

    static final String DOMAIN_TAG = "domain";

    private XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader;
    DataChannelDescriptor descriptor;

    DataChannelHandler(XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader, DataChannelDescriptor dataChannelDescriptor, LoaderContext loaderContext) {
        super(loaderContext);
        this.xmlDataChannelDescriptorLoader = xmlDataChannelDescriptorLoader;
        this.descriptor = dataChannelDescriptor;
        setTargetNamespace(DataChannelDescriptor.SCHEMA_XSD);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case DOMAIN_TAG:
                validateVersion(attributes);
                return true;
        }
        return false;
    }

    protected void validateVersion(Attributes attributes) {
        String version = attributes.getValue("project-version");
        if(!XMLDataChannelDescriptorLoader.CURRENT_PROJECT_VERSION.equals(version)) {
            throw new CayenneRuntimeException("Unsupported project version: %s, please upgrade project using Modeler v%s",
                    version, LocalizedStringsHandler.getString("cayenne.version"));
        }
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String name, Attributes attributes) {

        if (localName.equals(DOMAIN_TAG)) {
            return new DataChannelChildrenHandler(xmlDataChannelDescriptorLoader, this);
        }

        logger.info(unexpectedTagMessage(localName, DOMAIN_TAG));
        return super.createChildTagHandler(namespaceURI, localName, name, attributes);
    }
}
