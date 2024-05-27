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
package org.apache.cayenne.modeler.validation.extenstion;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.project.validation.Inspection;
import org.apache.cayenne.project.validation.ValidationConfig;
import org.xml.sax.Attributes;

import java.util.EnumSet;
import java.util.Set;

public class ValidationConfigHandler extends NamespaceAwareNestedTagHandler {

    static final String CONFIG_TAG = "validation";

    static final String EXCLUDE_TAG = "exclude";

    private final DataChannelMetaData metaData;
    private final Set<Inspection> disabledInspections;

    ValidationConfigHandler(NamespaceAwareNestedTagHandler parentHandler, DataChannelMetaData metaData) {
        super(parentHandler);
        this.metaData = metaData;
        targetNamespace = ValidationExtension.NAMESPACE;
        disabledInspections = EnumSet.noneOf(Inspection.class);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        if (CONFIG_TAG.equals(localName)) {
            disabledInspections.clear();
            return true;
        }
        return false;
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (localName.equals(EXCLUDE_TAG)) {
            disabledInspections.add(Inspection.valueOf(data));
            return true;
        }
        return false;
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) {
        if (CONFIG_TAG.equals(localName)) {
            createConfig();
        }
    }

    private void createConfig() {
        Set<Inspection> enabledInspections = EnumSet.allOf(Inspection.class);
        enabledInspections.removeAll(disabledInspections);
        loaderContext.addDataChannelListener(dataChannel -> {
            metaData.add(dataChannel, new ValidationConfig(enabledInspections));
        });
    }
}
