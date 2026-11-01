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
import org.apache.cayenne.map.NamedQuery;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A base of the handlers of the query tags of a DataMap. A subclass per query type reads its own tag into the
 * matching {@link NamedQuery}. The base reads the attributes common to all the query tags.
 *
 * @since 4.1
 */
public abstract class NamedQueryHandler extends NamespaceAwareNestedTagHandler {

    static final String CACHE_GROUP_TAG = "cacheGroup";

    protected final DataMap map;

    protected NamedQueryHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler);
        this.map = map;
    }

    @Override
    protected void beforeScopeEnd() {
        map.addNamedQuery(getNamedQuery());
    }

    /**
     * Returns the named query being read, or null before the query tag is processed.
     */
    public abstract NamedQuery getNamedQuery();

    /**
     * Reads the attributes common to all the query tags into a named query.
     */
    protected void loadQuerySettings(NamedQuery namedQuery, Attributes attributes) throws SAXException {
        String name = attributes.getValue("name");
        if (name == null) {
            throw new SAXException("Query has no name");
        }

        namedQuery.setName(name);
        namedQuery.setDataMap(map);

        String cacheStrategy = attributes.getValue("cacheStrategy");
        if (cacheStrategy != null) {
            namedQuery.setCacheStrategy(QueryCacheStrategy.safeValueOf(cacheStrategy));
        }

        namedQuery.setFetchingDataRows("true".equals(attributes.getValue("dataRows")));
        namedQuery.setPageSize(intAttribute(attributes, "pageSize"));
        namedQuery.setStatementFetchSize(intAttribute(attributes, "statementFetchSize"));
    }

    /**
     * Resolves the root of a query from its "root" and "rootName" attributes. Falls back to the DataMap when the root
     * type is not set, or the named root is not found.
     */
    protected Object resolveRoot(Attributes attributes) {
        String rootType = attributes.getValue("root");
        String rootName = attributes.getValue("rootName");
        if (rootType == null || rootName == null) {
            return map;
        }

        Object root = switch (rootType) {
            case NamedQuery.OBJ_ENTITY_ROOT -> map.getObjEntity(rootName);
            case NamedQuery.DB_ENTITY_ROOT -> map.getDbEntity(rootName);
            case NamedQuery.PROCEDURE_ROOT -> map.getProcedure(rootName);
            // the root is kept as an ObjEntity, since creating a Class requires the knowledge of the ClassLoader
            case NamedQuery.JAVA_CLASS_ROOT -> map.getObjEntityForJavaClass(rootName);
            default -> null;
        };

        return root != null ? root : map;
    }

    protected static int intAttribute(Attributes attributes, String name) {
        String value = attributes.getValue(name);
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * Reads the "columnNameCapitalization" attribute of a SQL or a procedure query, returning null if it is not set.
     */
    protected static CapsStrategy columnNameCapitalization(Attributes attributes) {
        String value = attributes.getValue("columnNameCapitalization");
        return value != null ? CapsStrategy.valueOf(value.toUpperCase()) : null;
    }
}
