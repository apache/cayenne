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
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.PatternParam;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @since 5.0
 */
class PatternParamHandler extends NamespaceAwareNestedTagHandler {

    protected boolean isPinned;

    protected final FilterContainer container;

    PatternParamHandler(NamespaceAwareNestedTagHandler parentHandler, FilterContainer container) {
        super(parentHandler);
        this.container = container;
        this.isPinned = false;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        isPinned = isPinned(attributes);
        return true;
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case DbImportTags.EXCLUDE_TABLE_TAG:
                createPatternParam(data, ExcludeTable::new, FilterContainer::addExcludeTable);
                break;
            case DbImportTags.INCLUDE_COLUMN_TAG:
                createPatternParam(data, IncludeColumn::new, FilterContainer::addIncludeColumn);
                break;
            case DbImportTags.EXCLUDE_COLUMN_TAG:
                createPatternParam(data, ExcludeColumn::new, FilterContainer::addExcludeColumn);
                break;
            case DbImportTags.INCLUDE_PROCEDURE_TAG:
                createPatternParam(data, IncludeProcedure::new, FilterContainer::addIncludeProcedure);
                break;
            case DbImportTags.EXCLUDE_PROCEDURE_TAG:
                createPatternParam(data, ExcludeProcedure::new, FilterContainer::addExcludeProcedure);
                break;
        }
        return true;
    }

    protected <T extends PatternParam> void createPatternParam(String pattern,
                                                               Function<String, T> paramConstructor,
                                                               BiConsumer<FilterContainer, T> addParam) {
        if (!pattern.trim().isEmpty() && container != null) {
            T param = paramConstructor.apply(pattern);
            param.setPinned(isPinned);
            addParam.accept(container, param);
        }
    }

    protected boolean isPinned(Attributes attributes) {
        return Objects.equals(attributes.getValue(DbImportTags.PINNED), "true");
    }

}
