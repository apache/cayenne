/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @since 4.1
 */
class IncludeTableHandler extends NamespaceAwareNestedTagHandler {

    private static final String INCLUDE_TABLE_TAG = "includeTable";
    private static final String INCLUDE_TABLE_NAME_TAG = "name";
    private static final String INCLUDE_COLUMN_TAG = "includeColumn";
    private static final String EXCLUDE_COLUMN_TAG = "excludeColumn";

    private IncludeTable includeTable;

    private FilterContainer entity;

    IncludeTableHandler(NamespaceAwareNestedTagHandler parentHandler,FilterContainer entity) {
        super(parentHandler);
        this.entity = entity;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        switch (localName) {
            case INCLUDE_TABLE_TAG:
                return true;
        }

        return false;
    }

    @Override
    protected void processCharData(String localName, String data) {
        switch (localName) {
            case INCLUDE_TABLE_NAME_TAG:
                createIncludeTableName(data);
                break;
            case INCLUDE_COLUMN_TAG:
                createIncludeColumn(data);
                break;
            case EXCLUDE_COLUMN_TAG:
                createExcludeColumn(data);
                break;
        }
    }

    private void createExcludeColumn(String excludeColumn) {
        if (excludeColumn.trim().length() == 0) {
            return;
        }

        if (includeTable != null) {
            includeTable.addExcludeColumn(new ExcludeColumn(excludeColumn));
        }
    }

    private void createIncludeColumn(String includeColumn) {
        if (includeColumn.trim().length() == 0) {
            return;
        }

        if (includeTable != null) {
            includeTable.addIncludeColumn(new IncludeColumn(includeColumn));
        }
    }

    private void createIncludeTableName(String includeTableName) {
        if (includeTableName.trim().length() == 0) {
            return;
        }

        if (includeTable == null) {
            createIncludeTable();
        }

        if (includeTable != null) {
            includeTable.setName(includeTableName);
        }
    }

    private void createIncludeTable() {
        includeTable = new IncludeTable();
        entity.addIncludeTable(includeTable);
    }
}
