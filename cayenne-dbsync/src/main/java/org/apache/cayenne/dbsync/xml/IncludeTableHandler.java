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
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.PatternParam;
import org.xml.sax.Attributes;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @since 4.1
 */
class IncludeTableHandler extends PatternParamHandler {
    private IncludeTable includeTable;

    IncludeTableHandler(NamespaceAwareNestedTagHandler parentHandler, FilterContainer container) {
        super(parentHandler, container);
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) {
        isPinned = isPinned(attributes);
        switch (localName) {
            case DbImportTags.INCLUDE_TABLE_TAG:
                createIncludeTable();
                return true;
        }
        return false;
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case DbImportTags.NAME_TAG:
                createIncludeTableName(data);
                break;
            case DbImportTags.INCLUDE_COLUMN_TAG:
                createColumn(data, IncludeColumn::new, IncludeTable::addIncludeColumn);
                break;
            case DbImportTags.EXCLUDE_COLUMN_TAG:
                createColumn(data, ExcludeColumn::new, IncludeTable::addExcludeColumn);
                break;
        }
        return true;
    }

    private void createIncludeTableName(String includeTableName) {
        if (includeTableName.trim().isEmpty()) {
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
        includeTable.setPinned(isPinned);
        container.addIncludeTable(includeTable);
    }

    private   <T extends PatternParam> void createColumn(String pattern,
                                                               Function<String, T> paramConstructor,
                                                               BiConsumer<IncludeTable, T> addParam) {
        if (!pattern.trim().isEmpty() && includeTable != null) {
            T param = paramConstructor.apply(pattern);
            param.setPinned(isPinned);
            addParam.accept(includeTable, param);
        }
    }
}
