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

import org.apache.cayenne.project.dbimport.Schema;
import org.apache.cayenne.project.dbimport.SchemaContainer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * Reads a "schema" tag of the "dbImport" tag of a DataMap, or of one of its "catalog" tags.
 */
final class DbImportSchemaHandler extends NamespaceAwareNestedTagHandler {

    static final String SCHEMA_TAG = "schema";

    private final SchemaContainer container;
    private Schema schema;

    DbImportSchemaHandler(NamespaceAwareNestedTagHandler parentHandler, SchemaContainer container) {
        super(parentHandler);
        this.container = container;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        return switch (localName) {
            case SCHEMA_TAG -> {
                schema = new Schema();
                container.addSchema(schema);
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
            schema.setName(data);
        } else {
            DbImportHandler.loadFilter(schema, localName, data);
        }
        return true;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {

        if (namespaceURI.equals(targetNamespace) && DbImportIncludeTableHandler.INCLUDE_TABLE_TAG.equals(localName)) {
            return new DbImportIncludeTableHandler(this, schema);
        }

        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }
}
