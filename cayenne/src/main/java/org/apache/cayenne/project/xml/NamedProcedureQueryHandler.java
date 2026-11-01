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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.NamedProcedureQuery;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A handler of the "procedureQuery" tag of a DataMap.
 *
 * @since 5.0
 */
public class NamedProcedureQueryHandler extends NamedQueryHandler {

    static final String PROCEDURE_QUERY_TAG = "procedureQuery";

    private NamedProcedureQuery namedQuery;

    public NamedProcedureQueryHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler, map);
    }

    @Override
    public NamedProcedureQuery getNamedQuery() {
        return namedQuery;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return switch (localName) {
            case PROCEDURE_QUERY_TAG -> {
                namedQuery = new NamedProcedureQuery();
                loadQuerySettings(namedQuery, attributes);
                namedQuery.setRoot(resolveRoot(attributes));
                namedQuery.setResultEntityName(attributes.getValue("resultEntity"));
                namedQuery.setFetchLimit(intAttribute(attributes, "fetchLimit"));
                namedQuery.setFetchOffset(intAttribute(attributes, "fetchOffset"));
                namedQuery.setColumnNamesCapitalization(columnNameCapitalization(attributes));
                yield true;
            }
            case CACHE_GROUP_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        if (CACHE_GROUP_TAG.equals(localName)) {
            namedQuery.setCacheGroup(data);
        }
        return true;
    }
}
