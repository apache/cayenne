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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

/**
 * Handles a tag that has no dedicated handler: a leaf tag read by the parent handler, a tool-only tag in the runtime
 * mode, or an unknown tag. Skips its nested tags, leaving its text to the parent handler.
 */
final class SkippedTagHandler extends NamespaceAwareNestedTagHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLProjectLoader.class);

    SkippedTagHandler(NamespaceAwareNestedTagHandler parentHandler, String targetNamespace) {
        super(parentHandler, targetNamespace);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        LOGGER.debug("Skipping tag <{}:{}>", namespaceURI, localName);
        return true;
    }
}
