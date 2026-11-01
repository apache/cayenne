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
package org.apache.cayenne.projecttools;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.NamedSelectQuery;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.ql.ParsedObjectSelect;
import org.apache.cayenne.query.Ordering;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What the tools need to know about a mapped select query, derived by parsing its String: whether it can be parsed,
 * its root entity, the kind of its result and its parameters. An instance is a snapshot of the String at the time of
 * parsing, so a tool that keeps it has to parse again when the String changes.
 *
 * @since 6.0
 */
public final class SelectQueryInfo {

    /**
     * The kind of the objects that a select query fetches, see {@link #resultKind()}.
     */
    public enum ResultKind {

        /**
         * The persistent objects of the root ObjEntity.
         */
        ENTITY,

        /**
         * DataRows, for a query rooted in a DbEntity or fetching DataRows.
         */
        DATA_ROW,

        /**
         * The values of the single column of a column query.
         */
        SCALAR,

        /**
         * The arrays of the values of the multiple columns of a column query.
         */
        ARRAY
    }

    /**
     * Parses the String of a query. Never throws: a String that can't be parsed makes an invalid info, that holds
     * the parse error.
     */
    public static SelectQueryInfo parse(NamedSelectQuery query) {
        String ql = query.getQl();
        if (ql == null || ql.isBlank()) {
            return new SelectQueryInfo(query, null, new ExpressionException("The query is empty"));
        }

        try {
            return new SelectQueryInfo(query, ParsedObjectSelect.parse(ql), null);
        } catch (ExpressionException e) {
            return new SelectQueryInfo(query, null, e);
        }
    }

    private final NamedSelectQuery query;
    private final ParsedObjectSelect parsed;
    private final ExpressionException error;

    private SelectQueryInfo(NamedSelectQuery query, ParsedObjectSelect parsed, ExpressionException error) {
        this.query = query;
        this.parsed = parsed;
        this.error = error;
    }

    public NamedSelectQuery query() {
        return query;
    }

    /**
     * Returns whether the query String is empty.
     */
    public boolean isEmpty() {
        return query.getQl() == null || query.getQl().isBlank();
    }

    /**
     * Returns whether the query String could be parsed.
     */
    public boolean isValid() {
        return parsed != null;
    }

    /**
     * Returns the error of parsing the query String, or null if it could be parsed.
     */
    public ExpressionException error() {
        return error;
    }

    /**
     * Returns the parsed String, to inspect its clauses.
     *
     * @throws ExpressionException if the String could not be parsed
     */
    public ParsedObjectSelect parsed() {
        if (parsed == null) {
            throw error;
        }
        return parsed;
    }

    /**
     * Returns the name of the entity of the "from" clause, or null if the String could not be parsed.
     */
    public String rootName() {
        if (parsed == null) {
            return null;
        }
        return parsed.isDbQuery() ? parsed.getDbEntityName() : parsed.getEntityName();
    }

    /**
     * Returns whether the query is rooted in a DbEntity, i.e. is "from db:NAME".
     */
    public boolean isDbQuery() {
        return parsed != null && parsed.isDbQuery();
    }

    /**
     * Returns whether the query selects columns, and not the root objects.
     */
    public boolean isColumnQuery() {
        return parsed != null && parsed.isColumnQuery();
    }

    /**
     * Returns the number of the columns of a column query, or zero for a query of the root objects.
     */
    public int columnCount() {
        return isColumnQuery() ? parsed.getColumns().size() : 0;
    }

    /**
     * Returns the entity of the "from" clause resolved in the DataMap of the query, or null if the String could not
     * be parsed, the query is not in a DataMap, or no such entity is mapped.
     */
    public Entity<?, ?, ?> root() {
        DataMap dataMap = query.getDataMap();
        if (parsed == null || dataMap == null) {
            return null;
        }
        return parsed.isDbQuery()
                ? dataMap.getDbEntity(parsed.getDbEntityName())
                : dataMap.getObjEntity(parsed.getEntityName());
    }

    /**
     * Returns the kind of the objects that the query fetches, or null if the String could not be parsed, or the
     * query is rooted in an entity that is not mapped.
     */
    public ResultKind resultKind() {
        if (parsed == null) {
            return null;
        }
        if (parsed.isColumnQuery()) {
            return parsed.isSingleColumn() ? ResultKind.SCALAR : ResultKind.ARRAY;
        }

        return switch (root()) {
            case ObjEntity ignored when !query.isFetchingDataRows() -> ResultKind.ENTITY;
            case ObjEntity ignored -> ResultKind.DATA_ROW;
            case DbEntity ignored -> ResultKind.DATA_ROW;
            case null, default -> null;
        };
    }

    /**
     * Returns the names of the "$name" parameters of the query, in the order of their first occurrence, empty if the
     * String could not be parsed.
     */
    public Set<String> parameterNames() {
        Set<String> names = new LinkedHashSet<>();
        if (parsed != null) {
            collectParameterNames(parsed, names);
        }
        return names;
    }

    private static void collectParameterNames(ParsedObjectSelect query, Set<String> names) {
        ParameterCollector collector = new ParameterCollector(names);
        if (query.getColumns() != null) {
            for (Property<?> column : query.getColumns()) {
                collector.collect(column.getExpression());
            }
        }
        collector.collect(query.getWhere());
        collector.collect(query.getHaving());
        for (Ordering ordering : query.getOrderings()) {
            collector.collect(ordering.getSortSpec());
        }
    }

    // collects the names of the unbound "$name" parameters of the expressions, including those of the subqueries
    private record ParameterCollector(Set<String> names) implements TraversalHandler {

        void collect(Expression expression) {
            if (expression != null) {
                expression.traverse(this);
            }
        }

        @Override
        public void startNode(Expression node, Expression parentNode) {
            // a subquery keeps its select out of the expression tree
            if (node instanceof SubqueryExp subquery && subquery.getQuery() instanceof ParsedObjectSelect nested) {
                collectParameterNames(nested, names);
            }
        }

        @Override
        public void objectNode(Object leaf, Expression parentNode) {
            switch (leaf) {
                case ExpressionParameter parameter -> names.add(parameter.getName());
                // a list of values is stored as a single array operand
                case Object[] values -> {
                    for (Object value : values) {
                        objectNode(value, parentNode);
                    }
                }
                case null, default -> {
                }
            }
        }
    }
}
