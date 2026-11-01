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
package org.apache.cayenne.ql;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An ObjectSelect subclass produced by parsing Cayenne QL. Extends super with methods to inspect its contents.
 *
 * @since 6.0
 */
// TODO: is it such a horrible idea for ObjectSelect to keep the getters?
public final class ParsedObjectSelect extends ObjectSelect<Object> {

    /**
     * Parses a query String for inspection, keeping its "$name" parameters in the parsed clauses.
     *
     * @throws ExpressionException if the String can't be parsed
     */
    public static ParsedObjectSelect parse(String ql) {
        return parse(ql, (Map<String, ?>) null);
    }

    /**
     * Parses a query String, binding the values of its "$name" parameters by name, and pruning the conditions whose
     * parameters have no value, as {@link ObjectSelect#parse(String, Map)} does.
     *
     * @param params the values of the parameters by name, or null to keep the parameters in the parsed clauses
     * @throws ExpressionException if the String can't be parsed
     */
    public static ParsedObjectSelect parse(String ql, Map<String, ?> params) {
        return QLParser.parse(ql, parser -> {
            if (params != null) {
                parser.setParameters(params);
            }
            return parser.query();
        });
    }

    ParsedObjectSelect(String entityName, boolean dbRoot) {
        if (dbRoot) {
            fetchDataRows();
            dbEntityName(entityName);
        } else {
            entityName(entityName);
        }
    }

    /**
     * Returns the name of the root ObjEntity, or null if the query is rooted in a DbEntity.
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Returns the name of the root DbEntity, or null if the query is rooted in an ObjEntity.
     */
    public String getDbEntityName() {
        return dbEntityName;
    }

    /**
     * Returns whether the query is rooted in a DbEntity, i.e. is "from db:NAME".
     */
    public boolean isDbQuery() {
        return dbEntityName != null;
    }

    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Returns the columns of the "select" clause, or null for a query of the root objects.
     */
    public Collection<Property<?>> getColumns() {
        return columns;
    }

    /**
     * Returns whether the query selects columns rather than the root objects.
     */
    public boolean isColumnQuery() {
        return columns != null;
    }

    /**
     * Returns whether the query selects a single column, so that each result row is the value of that column rather
     * than an {@code Object[]}. Always false for a query of the root objects.
     */
    public boolean isSingleColumn() {
        return columns != null && singleColumn;
    }

    /**
     * Returns the "where" clause, or null if there is none.
     */
    public Expression getWhere() {
        return where;
    }

    /**
     * Returns the "having" clause, or null if there is none.
     */
    public Expression getHaving() {
        return having;
    }

    /**
     * Returns the orderings of the "order by" clause, empty if there is none.
     */
    public Collection<Ordering> getOrderings() {
        return orderings != null ? orderings : List.of();
    }

    /**
     * Returns the "limit" clause, or zero if there is none.
     */
    public int getLimit() {
        return fetchLimit;
    }

    /**
     * Returns the "offset" clause, or zero if there is none.
     */
    public int getOffset() {
        return fetchOffset;
    }

    /**
     * Returns the tree of the "prefetch" clause, or null if there is none.
     */
    public PrefetchTreeNode getPrefetches() {
        return prefetches;
    }
}
