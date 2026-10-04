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
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.QueryDescriptorLoader;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.PrefetchTreeNode;
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

    static final String OBJECT_QUERY_TAG = "objectQuery";
    static final String SQL_QUERY_TAG = "sqlQuery";
    static final String PROCEDURE_QUERY_TAG = "procedureQuery";
    private static final String QUERY_SQL_TAG = "sql";
    private static final String QUERY_QL_TAG = "ql";
    private static final String QUERY_PREFETCH_TAG = "prefetch";
    private static final String QUERY_CACHE_GROUP_TAG = "cacheGroup";

    private DataMap map;

    private QueryDescriptorLoader queryBuilder;
    private QueryDescriptor descriptor;

    private String sqlKey;
    private int semantics;

    public QueryDescriptorHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {

        return switch (localName) {
            case OBJECT_QUERY_TAG -> {
                addQueryDescriptor(SelectQueryDescriptor::new, attributes);
                yield true;
            }
            case SQL_QUERY_TAG -> {
                addQueryDescriptor(SQLTemplateDescriptor::new, attributes);
                yield true;
            }
            case PROCEDURE_QUERY_TAG -> {
                addQueryDescriptor(ProcedureQueryDescriptor::new, attributes);
                yield true;
            }
            case QUERY_SQL_TAG -> {
                this.sqlKey = attributes.getValue("adapterClass");
                yield true;
            }
            case QUERY_PREFETCH_TAG -> {
                createPrefetchSemantics(attributes);
                yield true;
            }
            case QUERY_QL_TAG, QUERY_CACHE_GROUP_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case QUERY_SQL_TAG -> queryBuilder.addSql(data, sqlKey);
            case QUERY_QL_TAG -> queryBuilder.setSelect(data);
            case QUERY_PREFETCH_TAG -> addPrefetchWithSemantics(data);
            case QUERY_CACHE_GROUP_TAG -> queryBuilder.setCacheGroup(data);
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

        String rootName = attributes.getValue("rootName");
        queryBuilder.setRoot(map, attributes.getValue("root"), rootName);

        // TODO: Andrus, 2/13/2006 'result-type' is only used in ProcedureQuery
        // and is deprecated in 1.2
        String resultEntity = attributes.getValue("resultEntity");
        if (!Util.isEmptyString(resultEntity)) {
            queryBuilder.setResultEntity(resultEntity);
        }

        String cacheStrategy = attributes.getValue("cacheStrategy");
        if (cacheStrategy != null) {
            queryBuilder.setCacheStrategy(QueryCacheStrategy.safeValueOf(cacheStrategy));
        }

        String columnNameCapitalization = attributes.getValue("columnNameCapitalization");
        if (columnNameCapitalization != null) {
            queryBuilder.setColumnNameCapitalization(CapsStrategy.valueOf(columnNameCapitalization.toUpperCase()));
        }

        queryBuilder.setFetchingDataRows("true".equals(attributes.getValue("dataRows")));
        queryBuilder.setFetchLimit(intAttribute(attributes, "fetchLimit"));
        queryBuilder.setFetchOffset(intAttribute(attributes, "fetchOffset"));
        queryBuilder.setPageSize(intAttribute(attributes, "pageSize"));
        queryBuilder.setStatementFetchSize(intAttribute(attributes, "statementFetchSize"));
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
        if(descriptor == null) {
            descriptor = queryBuilder.buildQueryDescriptor();
        }
        return descriptor;
    }

    private int convertPrefetchType(String type) {
        return switch (type) {
            case "joint" -> PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS;
            case "disjoint" -> PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS;
            case "disjointById" -> PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS;
            case null, default -> PrefetchTreeNode.UNDEFINED_SEMANTICS;
        };
    }
}
