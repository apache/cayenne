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

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * @since 4.1
 */
final class DataNodeChildrenHandler extends NamespaceAwareNestedTagHandler {

    static final String DATA_SOURCE_TAG = "data-source";
    static final String MAP_REF_TAG = "map-ref";

    private XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader;
    private DataNodeDescriptor nodeDescriptor;

    private DataSourceDescriptor dataSourceDescriptor;

    DataNodeChildrenHandler(XMLDataChannelDescriptorLoader xmlDataChannelDescriptorLoader, NamespaceAwareNestedTagHandler parentHandler, DataNodeDescriptor nodeDescriptor) {
        super(parentHandler);
        this.xmlDataChannelDescriptorLoader = xmlDataChannelDescriptorLoader;
        this.nodeDescriptor = nodeDescriptor;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        switch (localName) {
            case MAP_REF_TAG:
                nodeDescriptor.getDataMapNames().add(attributes.getValue("name"));
                return true;

            case DATA_SOURCE_TAG:
                nodeDescriptor.setDataSourceDescriptor(dataSourceDescriptor);
                return true;
        }

        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String name, Attributes attributes) {
        if (DATA_SOURCE_TAG.equals(localName)) {
            dataSourceDescriptor = new DataSourceDescriptor();
            return new DataSourceChildrenHandler(xmlDataChannelDescriptorLoader, this, dataSourceDescriptor);
        }

        return super.createChildTagHandler(namespaceURI, localName, name, attributes);
    }
}
