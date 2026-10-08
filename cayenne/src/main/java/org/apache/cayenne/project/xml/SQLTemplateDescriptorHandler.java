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
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A handler of the "sqlQuery" tag of a DataMap.
 *
 * @since 5.0
 */
public class SQLTemplateDescriptorHandler extends QueryDescriptorHandler {

    static final String SQL_QUERY_TAG = "sqlQuery";
    private static final String SQL_TAG = "sql";
    private static final String PREFETCH_TAG = "prefetch";

    private SQLTemplateDescriptor descriptor;
    private String sqlAdapterClass;
    private int prefetchSemantics;

    public SQLTemplateDescriptorHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler, map);
    }

    @Override
    public SQLTemplateDescriptor getQueryDescriptor() {
        return descriptor;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return switch (localName) {
            case SQL_QUERY_TAG -> {
                descriptor = new SQLTemplateDescriptor();
                loadQuerySettings(descriptor, attributes);
                descriptor.setRoot(resolveRoot(attributes));
                descriptor.setColumnNamesCapitalization(columnNameCapitalization(attributes));
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

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case SQL_TAG -> addSql(data);
            case PREFETCH_TAG -> addPrefetch(data);
            case CACHE_GROUP_TAG -> descriptor.setCacheGroup(data);
        }
        return true;
    }

    /**
     * Adds a SQL String. If the "sql" tag has an "adapterClass", the SQL is specific to that adapter. Otherwise it is
     * the default SQL of the query.
     */
    private void addSql(String sql) {
        if (sqlAdapterClass == null) {
            descriptor.setSql(sql);
        } else {
            descriptor.getAdapterSql().put(sqlAdapterClass, sql);
        }
    }

    private void addPrefetch(String path) {
        if (!path.isBlank()) {
            descriptor.addPrefetch(path.trim(), prefetchSemantics);
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
