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

package org.apache.cayenne.dbsync.xml;

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;
import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * @since 4.1
 */
class CatalogHandler extends NamespaceAwareNestedTagHandler {

    private final ReverseEngineering configuration;
    private Catalog catalog;


    CatalogHandler(NamespaceAwareNestedTagHandler parentHandler, ReverseEngineering configuration) {
        super(parentHandler);
        this.configuration = configuration;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {
        if (namespaceURI.equals(targetNamespace)) {
            switch (localName) {
                case DbImportTags.SCHEMA_TAG:
                    return new SchemaHandler(this, catalog);
                case DbImportTags.INCLUDE_TABLE_TAG:
                    return new IncludeTableHandler(this, catalog);
                case DbImportTags.EXCLUDE_TABLE_TAG:
                case DbImportTags.INCLUDE_COLUMN_TAG:
                case DbImportTags.EXCLUDE_COLUMN_TAG:
                case DbImportTags.INCLUDE_PROCEDURE_TAG:
                case DbImportTags.EXCLUDE_PROCEDURE_TAG:
                    return new PatternParamHandler(this, catalog);
            }
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        switch (localName) {
            case DbImportTags.CATALOG_TAG:
                createCatalog();
                return true;
        }
        return false;
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case DbImportTags.NAME_TAG:
                createCatalogName(data);
        }
        return true;
    }

    private void createCatalogName(String catalogName) {
        if (!catalogName.trim().isEmpty() && catalog != null) {
            catalog.setName(catalogName);
        }
    }
    private void createCatalog() {
        catalog = new Catalog();
        configuration.addCatalog(catalog);
    }
}
