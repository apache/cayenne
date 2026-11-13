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

import org.apache.cayenne.project.dbimport.ReverseEngineering;
import org.xml.sax.Attributes;

/**
 * Reads the "tableTypes" tag of the "dbImport" tag of a DataMap.
 */
final class DbImportTableTypesHandler extends NamespaceAwareNestedTagHandler {

    static final String TABLE_TYPES_TAG = "tableTypes";
    private static final String TABLE_TYPE_TAG = "tableType";

    private final ReverseEngineering reverseEngineering;

    DbImportTableTypesHandler(NamespaceAwareNestedTagHandler parentHandler, ReverseEngineering reverseEngineering) {
        super(parentHandler);
        this.reverseEngineering = reverseEngineering;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        return switch (localName) {
            case TABLE_TYPES_TAG, TABLE_TYPE_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (data.isBlank()) {
            return true;
        }

        if (TABLE_TYPE_TAG.equals(localName)) {
            reverseEngineering.addTableType(data);
        }
        return true;
    }
}
