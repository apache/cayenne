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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

/**
 * @since 4.1
 */
public class DefaultHandlerFactory implements HandlerFactory {

    private static final Logger logger = LoggerFactory.getLogger(XMLDataChannelDescriptorLoader.class);

    @Override
    public NamespaceAwareNestedTagHandler createHandler(String namespace, String localName, NamespaceAwareNestedTagHandler parent) {
        return new NamespaceAwareNestedTagHandler(parent, namespace) {
            @Override
            protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
                logger.debug("Skipping unknown tag <{}:{}>", namespaceURI, localName);
                return true;
            }
        };
    }

}
