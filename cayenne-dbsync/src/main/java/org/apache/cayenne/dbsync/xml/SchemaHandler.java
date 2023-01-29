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
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.dbsync.reverse.dbimport.SchemaContainer;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

/**
 * @since 4.1
 */
class SchemaHandler extends NamespaceAwareNestedTagHandler {
    private final SchemaContainer entity;

    private Schema schema;

    SchemaHandler(NamespaceAwareNestedTagHandler parentHandler, SchemaContainer entity) {
        super(parentHandler);
        this.entity = entity;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        switch (localName) {
            case DbImportTags.SCHEMA_TAG:
                createSchema();
                return true;
        }
        return false;
    }

    @Override
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {
        if (namespaceURI.equals(targetNamespace)) {
            switch (localName) {
                case DbImportTags.INCLUDE_TABLE_TAG:
                    return new IncludeTableHandler(this, schema);
                case DbImportTags.EXCLUDE_TABLE_TAG:
                case DbImportTags.INCLUDE_COLUMN_TAG:
                case DbImportTags.EXCLUDE_COLUMN_TAG:
                case DbImportTags.INCLUDE_PROCEDURE_TAG:
                case DbImportTags.EXCLUDE_PROCEDURE_TAG:
                    return new PatternParamHandler(this, schema);
            }
        }
        return super.createChildTagHandler(namespaceURI, localName, qName, attributes);
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case DbImportTags.NAME_TAG:
                createSchemaName(data);
                break;
        }
        return true;
    }

    private void createSchemaName(String schemaName) {
        if (!schemaName.trim().isEmpty() && schema != null) {
            schema.setName(schemaName);
        }
    }

    private void createSchema() {
        schema = new Schema();
        entity.addSchema(schema);
    }
}
