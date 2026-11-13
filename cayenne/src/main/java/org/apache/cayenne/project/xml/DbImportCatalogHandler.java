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

import org.apache.cayenne.project.dbimport.Catalog;
import org.apache.cayenne.project.dbimport.ReverseEngineering;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * Reads a "catalog" tag of the "dbImport" tag of a DataMap.
 */
final class DbImportCatalogHandler extends NamespaceAwareNestedTagHandler {

    static final String CATALOG_TAG = "catalog";

    private final ReverseEngineering reverseEngineering;
    private Catalog catalog;

    DbImportCatalogHandler(NamespaceAwareNestedTagHandler parentHandler, ReverseEngineering reverseEngineering) {
        super(parentHandler);
        this.reverseEngineering = reverseEngineering;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        return switch (localName) {
            case CATALOG_TAG -> {
                catalog = new Catalog();
                reverseEngineering.addCatalog(catalog);
                yield true;
            }
            case DbImportHandler.NAME_TAG -> true;
            default -> DbImportHandler.isFilterTag(localName);
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (data.isBlank()) {
            return true;
        }

        if (DbImportHandler.NAME_TAG.equals(localName)) {
            catalog.setName(data);
        } else {
            DbImportHandler.loadFilter(catalog, localName, data);
        }
        return true;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {

        if (namespaceURI.equals(targetNamespace)) {
            return switch (localName) {
                case DbImportSchemaHandler.SCHEMA_TAG -> new DbImportSchemaHandler(this, catalog);
                case DbImportIncludeTableHandler.INCLUDE_TABLE_TAG -> new DbImportIncludeTableHandler(this, catalog);
                default -> super.createChildTagHandler(namespaceURI, localName, qName, attributes);
            };
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }
}
