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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.project.Project;
import org.xml.sax.Attributes;

/**
 * Reads the "validation" tag of the project.
 */
final class ValidationHandler extends NamespaceAwareNestedTagHandler {

    static final String VALIDATION_TAG = "validation";
    private static final String EXCLUDE_TAG = "exclude";

    private final Project project;
    private final List<String> disabledInspections;

    ValidationHandler(NamespaceAwareNestedTagHandler parentHandler, Project project) {
        super(parentHandler);
        this.project = project;
        this.disabledInspections = new ArrayList<>();
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        return switch (localName) {
            case VALIDATION_TAG, EXCLUDE_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (EXCLUDE_TAG.equals(localName)) {
            // normalized by the Project
            disabledInspections.add(data);
            return true;
        }

        return false;
    }

    @Override
    protected void beforeScopeEnd() {
        project.setDisabledInspections(disabledInspections);
    }
}
