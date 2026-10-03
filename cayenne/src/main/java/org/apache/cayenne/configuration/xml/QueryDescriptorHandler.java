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
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.map.ProcedureQueryDescriptor;
import java.util.function.Supplier;


/**
 * @since 4.1
 */
public class QueryDescriptorHandler extends NamespaceAwareNestedTagHandler {

    private static final String OBJECT_QUERY_TAG = "object-query";
    private static final String SQL_QUERY_TAG = "sql-query";
    private static final String PROCEDURE_QUERY_TAG = "procedure-query";
    private static final String QUERY_SQL_TAG = "sql";
    private static final String QUERY_QL_TAG = "ql";
    private static final String QUERY_PREFETCH_TAG = "prefetch";
    private static final String QUERY_CACHE_GROUP_TAG = "cache-group";

    private DataMap map;

    private QueryDescriptorLoader queryBuilder;
    private QueryDescriptor descriptor;
    private boolean changed;

    private String sqlKey;
    private int semantics;

    public QueryDescriptorHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {

        switch (localName) {
            case OBJECT_QUERY_TAG:
                addQueryDescriptor(SelectQueryDescriptor::new, attributes);
                return true;

            case SQL_QUERY_TAG:
                addQueryDescriptor(SQLTemplateDescriptor::new, attributes);
                return true;

            case PROCEDURE_QUERY_TAG:
                addQueryDescriptor(ProcedureQueryDescriptor::new, attributes);
                return true;

            case QUERY_CACHE_GROUP_TAG:
                return true;

            case QUERY_SQL_TAG:
                this.sqlKey = attributes.getValue("adapter-class");
                return true;

            case QUERY_QL_TAG:
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

            case QUERY_QL_TAG:
                queryBuilder.setSelect(data);
                changed = true;
                break;

            case QUERY_PREFETCH_TAG:
                addPrefetchWithSemantics(data);
                break;

            case QUERY_CACHE_GROUP_TAG:
                queryBuilder.setCacheGroup(data);
                changed = true;
                break;
        }
        return true;
    }

    @Override
    protected void beforeScopeEnd() {
        map.addQueryDescriptor(getQueryDescriptor());
    }

    private void addQueryDescriptor(Supplier<? extends QueryDescriptor> type, Attributes attributes) throws SAXException {
        String name = attributes.getValue("name");
        if (null == name) {
            throw new SAXException("QueryDescriptorHandler::addQueryDescriptor() - no query name.");
        }

        queryBuilder = new QueryDescriptorLoader();
        queryBuilder.setName(name);

        queryBuilder.setQueryType(type);

        String rootName = attributes.getValue("root-name");
        queryBuilder.setRoot(map, attributes.getValue("root"), rootName);

        // TODO: Andrus, 2/13/2006 'result-type' is only used in ProcedureQuery
        // and is deprecated in 1.2
        String resultEntity = attributes.getValue("result-entity");
        if (!Util.isEmptyString(resultEntity)) {
            queryBuilder.setResultEntity(resultEntity);
        }

        String cacheStrategy = attributes.getValue("cache-strategy");
        if (cacheStrategy != null) {
            queryBuilder.setCacheStrategy(QueryCacheStrategy.safeValueOf(cacheStrategy));
        }

        String columnNameCapitalization = attributes.getValue("column-name-capitalization");
        if (columnNameCapitalization != null) {
            queryBuilder.setColumnNameCapitalization(CapsStrategy.valueOf(columnNameCapitalization.toUpperCase()));
        }

        queryBuilder.setFetchingDataRows(Boolean.parseBoolean(attributes.getValue("data-rows")));
        queryBuilder.setFetchLimit(intAttribute(attributes, "fetch-limit"));
        queryBuilder.setFetchOffset(intAttribute(attributes, "fetch-offset"));
        queryBuilder.setPageSize(intAttribute(attributes, "page-size"));
        queryBuilder.setStatementFetchSize(intAttribute(attributes, "statement-fetch-size"));

        changed = true;
    }

    private int intAttribute(Attributes attributes, String name) {
        String value = attributes.getValue(name);
        return value != null ? Integer.parseInt(value) : 0;
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
