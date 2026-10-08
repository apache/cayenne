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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.ql.JavaCharStream;
import org.apache.cayenne.ql.QLParser;
import org.apache.cayenne.ql.QLParserTokenManager;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A handler of the "objectQuery" tag of a DataMap. The query is defined by a single String in the syntax of
 * {@link ObjectSelect#parse(String, Object...)}.
 *
 * @since 5.0
 */
public class SelectQueryDescriptorHandler extends QueryDescriptorHandler {

    static final String OBJECT_QUERY_TAG = "objectQuery";
    private static final String QL_TAG = "ql";

    private SelectQueryDescriptor descriptor;

    public SelectQueryDescriptorHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler, map);
    }

    @Override
    public SelectQueryDescriptor getQueryDescriptor() {
        return descriptor;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return switch (localName) {
            case OBJECT_QUERY_TAG -> {
                descriptor = new SelectQueryDescriptor();
                loadQuerySettings(descriptor, attributes);
                yield true;
            }
            case QL_TAG, CACHE_GROUP_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case QL_TAG -> setSelect(data);
            case CACHE_GROUP_TAG -> descriptor.setCacheGroup(data);
        }
        return true;
    }

    /**
     * Sets the query from its String form. The String defines the root entity, qualifier, orderings, prefetches,
     * limit, offset and "distinct" of the query. "$name" parameters in the String are preserved as named parameters,
     * to be bound when the query is executed.
     */
    private void setSelect(String select) {
        if (select.isBlank()) {
            return;
        }

        // TODO: the parsed query can be a ColumnSelect, have a "having" clause or be rooted in a DbEntity, none of
        //  which is representable in SelectQueryDescriptor, or editable in the Modeler. Both need to be reworked
        //  to hold a full query instead of its parts, and MappedSelect should stop assuming an ObjectSelect
        String name = descriptor.getName();
        FluentSelect<?, ?> parsed = parseSelect(select);
        if (!(parsed instanceof ObjectSelect<?> query)) {
            throw new ConfigurationException("Query '%s' selects columns, which is not supported in a mapped query: %s",
                    name, select);
        }
        if (query.getHaving() != null) {
            throw new ConfigurationException("Query '%s' has a 'having' clause, which is not supported in a mapped " +
                    "query: %s", name, select);
        }
        if (query.getEntityName() == null) {
            throw new ConfigurationException("Query '%s' must be rooted in an ObjEntity: %s", name, select);
        }

        // a select query is rooted in an ObjEntity, and never in a DataMap. If the entity is not in this DataMap, it is
        // kept by name
        ObjEntity rootEntity = map.getObjEntity(query.getEntityName());
        descriptor.setRoot(rootEntity != null ? rootEntity : query.getEntityName());
        descriptor.setQualifier(query.getWhere());
        descriptor.setOrderings(query.getOrderings() != null
                ? new ArrayList<>(query.getOrderings())
                : new ArrayList<>());

        HashMap<String, Integer> prefetchesMap = new HashMap<>();
        PrefetchTreeNode prefetches = query.getPrefetches();
        if (prefetches != null) {
            for (PrefetchTreeNode node : prefetches.nonPhantomNodes()) {
                // the root of the tree is not a prefetch
                if (node.getParent() != null) {
                    prefetchesMap.put(node.getPath().value(), node.getSemantics());
                }
            }
        }
        descriptor.setPrefetchesMap(prefetchesMap);

        descriptor.setFetchLimit(query.getLimit());
        descriptor.setFetchOffset(query.getOffset());
        descriptor.setDistinct(query.isDistinct());
    }

    /**
     * Parses a query String without binding its "$name" parameters, unlike
     * {@link ObjectSelect#parse(String, Object...)} that requires the values of all the parameters upfront.
     */
    private FluentSelect<?, ?> parseSelect(String select) {
        JavaCharStream stream = new JavaCharStream(new StringReader(select), 1, 1, select.length() + 1);
        QLParser parser = new QLParser(new QLParserTokenManager(stream));
        try {
            return parser.query();
        } catch (Throwable th) {
            throw new ConfigurationException("Query '%s' can't be parsed: %s", th, descriptor.getName(),
                    th.getMessage());
        }
    }
}
