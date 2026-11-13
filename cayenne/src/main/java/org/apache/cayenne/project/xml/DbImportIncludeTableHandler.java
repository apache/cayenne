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

import org.apache.cayenne.project.dbimport.ExcludeColumn;
import org.apache.cayenne.project.dbimport.FilterContainer;
import org.apache.cayenne.project.dbimport.IncludeColumn;
import org.apache.cayenne.project.dbimport.IncludeTable;
import org.xml.sax.Attributes;

/**
 * Reads an "includeTable" tag of the "dbImport" tag of a DataMap, or of one of its "catalog" or "schema" tags.
 */
final class DbImportIncludeTableHandler extends NamespaceAwareNestedTagHandler {

    static final String INCLUDE_TABLE_TAG = "includeTable";

    private final FilterContainer container;
    private IncludeTable includeTable;

    DbImportIncludeTableHandler(NamespaceAwareNestedTagHandler parentHandler, FilterContainer container) {
        super(parentHandler);
        this.container = container;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        return switch (localName) {
            case INCLUDE_TABLE_TAG, DbImportHandler.NAME_TAG, DbImportHandler.INCLUDE_COLUMN_TAG,
                 DbImportHandler.EXCLUDE_COLUMN_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (data.isBlank()) {
            return true;
        }

        switch (localName) {
            case DbImportHandler.NAME_TAG -> loadName(data);
            case DbImportHandler.INCLUDE_COLUMN_TAG -> {
                if (includeTable != null) {
                    includeTable.addIncludeColumn(new IncludeColumn(data));
                }
            }
            case DbImportHandler.EXCLUDE_COLUMN_TAG -> {
                if (includeTable != null) {
                    includeTable.addExcludeColumn(new ExcludeColumn(data));
                }
            }
        }
        return true;
    }

    // a table with no name is skipped
    private void loadName(String name) {
        if (includeTable == null) {
            includeTable = new IncludeTable();
            container.addIncludeTable(includeTable);
        }
        includeTable.setName(name);
    }
}
