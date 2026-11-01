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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.NamedSQLQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A handler of the "sqlQuery" tag of a DataMap.
 *
 * @since 5.0
 */
public class NamedSQLQueryHandler extends NamedQueryHandler {

    static final String SQL_QUERY_TAG = "sqlQuery";
    private static final String SQL_TAG = "sql";
    private static final String PREFETCH_TAG = "prefetch";

    private NamedSQLQuery namedQuery;
    private String sqlAdapterClass;
    private int prefetchSemantics;

    public NamedSQLQueryHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler, map);
    }

    @Override
    public NamedSQLQuery getNamedQuery() {
        return namedQuery;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return switch (localName) {
            case SQL_QUERY_TAG -> {
                namedQuery = new NamedSQLQuery();
                loadQuerySettings(namedQuery, attributes);
                namedQuery.setRoot(sqlQueryRoot(attributes));
                namedQuery.setColumnNamesCapitalization(columnNameCapitalization(attributes));
                yield true;
            }
            case SQL_TAG -> {
                sqlAdapterClass = attributes.getValue("adapterClass");
                yield true;
            }
            case PREFETCH_TAG -> {
                prefetchSemantics = prefetchSemantics(attributes.getValue("type"));
                yield true;
            }
            case CACHE_GROUP_TAG -> true;
            default -> false;
        };
    }

    // a SQL query root is an ObjEntity or the DataMap; the "dbEntity" and "procedure" roots of the older formats only
    // routed the query to the DataNode of the map, so they are loaded as the DataMap
    private Object sqlQueryRoot(Attributes attributes) {
        Object root = resolveRoot(attributes);
        return root instanceof ObjEntity ? root : map;
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case SQL_TAG -> addSql(data);
            case PREFETCH_TAG -> addPrefetch(data);
            case CACHE_GROUP_TAG -> namedQuery.setCacheGroup(data);
        }
        return true;
    }

    /**
     * Adds a SQL String. If the "sql" tag has an "adapterClass", the SQL is specific to that adapter. Otherwise it is
     * the default SQL of the query.
     */
    private void addSql(String sql) {
        if (sqlAdapterClass == null) {
            namedQuery.setSql(sql);
        } else {
            namedQuery.getAdapterSql().put(sqlAdapterClass, sql);
        }
    }

    private void addPrefetch(String path) {
        if (!path.isBlank()) {
            namedQuery.addPrefetch(path.trim(), prefetchSemantics);
        }
    }

    private static int prefetchSemantics(String type) {
        return switch (type) {
            case "joint" -> PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS;
            case "disjoint" -> PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS;
            case "disjointById" -> PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS;
            case null, default -> PrefetchTreeNode.UNDEFINED_SEMANTICS;
        };
    }
}
