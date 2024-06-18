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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.QueryDescriptorLoader;
import org.apache.cayenne.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import static org.apache.cayenne.util.Util.isBlank;

/**
 * @since 4.1
 */
public class QueryDescriptorHandler extends NamespaceAwareNestedTagHandler {

    private static final String QUERY_DESCRIPTOR_TAG = "query";
    private static final String QUERY_SQL_TAG = "sql";
    private static final String QUERY_EJBQL_TAG = "ejbql";
    private static final String QUERY_QUALIFIER_TAG = "qualifier";
    private static final String QUERY_ORDERING_TAG = "ordering";
    private static final String QUERY_PREFETCH_TAG = "prefetch";

    public static final String PROPERTY_TAG = "property";

    private DataMap map;

    private QueryDescriptorLoader queryBuilder;
    private QueryDescriptor descriptor;
    private boolean changed;

    private String sqlKey;
    private String descending;
    private String ignoreCase;

    private int semantics;

    public QueryDescriptorHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {

        switch (localName) {
            case QUERY_DESCRIPTOR_TAG:
                addQueryDescriptor(attributes);
                return true;

            case PROPERTY_TAG:
                addQueryDescriptorProperty(attributes);
                return true;

            case QUERY_SQL_TAG:
                this.sqlKey = attributes.getValue("adapter-class");
                return true;

            case QUERY_ORDERING_TAG:
                createQueryOrdering(attributes);
                return true;

            case QUERY_EJBQL_TAG:
            case QUERY_QUALIFIER_TAG:
            case QUERY_PREFETCH_TAG:
                createPrefetchSemantics(attributes);
                return true;
        }

        return false;
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case QUERY_SQL_TAG:
                queryBuilder.addSql(data, sqlKey);
                break;

            case QUERY_EJBQL_TAG:
                queryBuilder.setEjbql(data);
                break;

            case QUERY_QUALIFIER_TAG:
                createQualifier(data);
                break;

            case QUERY_ORDERING_TAG:
                addQueryOrdering(data);
                break;

            case QUERY_PREFETCH_TAG:
                addPrefetchWithSemantics(data);
                break;
        }
        return true;
    }

    @Override
    protected void beforeScopeEnd() {
        map.addQueryDescriptor(getQueryDescriptor());
    }

    private void addQueryDescriptor(Attributes attributes) throws SAXException {
        String name = attributes.getValue("name");
        if (null == name) {
            throw new SAXException("QueryDescriptorHandler::addQueryDescriptor() - no query name.");
        }

        queryBuilder = new QueryDescriptorLoader();
        queryBuilder.setName(name);

        String type = attributes.getValue("type");
        // Legacy format support (v7 and older)
        if(type == null) {
            queryBuilder.setLegacyFactory(attributes.getValue("factory"));
        } else {
            queryBuilder.setQueryType(type);
        }

        String rootName = attributes.getValue("root-name");
        queryBuilder.setRoot(map, attributes.getValue("root"), rootName);

        // TODO: Andrus, 2/13/2006 'result-type' is only used in ProcedureQuery
        // and is deprecated in 1.2
        String resultEntity = attributes.getValue("result-entity");
        if (!Util.isEmptyString(resultEntity)) {
            queryBuilder.setResultEntity(resultEntity);
        }

        changed = true;
    }

    private void addQueryDescriptorProperty(Attributes attributes) throws SAXException {
        String name = attributes.getValue("name");
        if (null == name) {
            throw new SAXException("QueryDescriptorHandler::addQueryDescriptorProperty() - no property name.");
        }

        String value = attributes.getValue("value");
        if (null == value) {
            throw new SAXException("QueryDescriptorHandler::addQueryDescriptorProperty() - no property value.");
        }

        queryBuilder.addProperty(name, value);
        changed = true;
    }

    private void createQualifier(String qualifier) {
        if (isBlank(qualifier)) {
            return;
        }

        queryBuilder.setQualifier(qualifier);
        changed = true;
    }

    private void createQueryOrdering(Attributes attributes) {
        descending = attributes.getValue("descending");
        ignoreCase = attributes.getValue("ignore-case");
    }

    private void addQueryOrdering(String path) {
        queryBuilder.addOrdering(path, descending, ignoreCase);
        changed = true;
    }

    private void createPrefetchSemantics(Attributes attributes) {
        semantics = convertPrefetchType(attributes.getValue("type"));
    }

    private void addPrefetchWithSemantics(String path) {
        queryBuilder.addPrefetch(path, semantics);
    }

    public QueryDescriptor getQueryDescriptor() {
        if(queryBuilder == null) {
            return null;
        }
        if(descriptor == null || changed) {
            descriptor = queryBuilder.buildQueryDescriptor();
            changed = false;
        }
        return descriptor;
    }

    private int convertPrefetchType(String type) {
        if (type != null) {
            switch (type) {
                case "joint":
                    return 1;
                case "disjoint":
                    return 2;
                case "disjointById":
                    return 3;
                default:
                    return 0;
            }
        }
        return 0;
    }
}
