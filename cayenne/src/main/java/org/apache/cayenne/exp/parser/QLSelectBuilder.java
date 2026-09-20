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

package org.apache.cayenne.exp.parser;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SortOrder;

/**
 * Collects the clauses of a select query as {@link QLParser} encounters them, and assembles either an ObjectSelect or
 * a ColumnSelect at the end. The assembled query is the same as one built with the fluent API. No mapping metadata
 * is needed, so the columns are created without a Java type, to be resolved against the model when the query is run.
 */
class QLSelectBuilder {

    private final List<Property<?>> columns = new ArrayList<>();
    private final List<Ordering> orderings = new ArrayList<>();
    private final List<String> prefetchPaths = new ArrayList<>();
    private final List<Integer> prefetchSemantics = new ArrayList<>();

    private String entityName;
    private boolean dbRoot;
    private boolean distinct;
    private boolean selectingRoot = true;
    private Expression where;
    private Expression having;
    private int limit;
    private int offset;

    void distinct() {
        this.distinct = true;
    }

    void column(Expression expression, String alias) {
        selectingRoot = columns.isEmpty() && alias == null && isSelf(expression);

        BaseProperty<Object> column = PropertyFactory.createBase(expression, null);
        columns.add(alias != null ? column.alias(alias) : column);
    }

    void fromObjEntity(String name) {
        this.entityName = name;
    }

    void fromDbEntity(String name) {
        this.entityName = name;
        this.dbRoot = true;
    }

    void where(Expression where) {
        this.where = where;
    }

    void having(Expression having) {
        this.having = having;
    }

    void ordering(Expression expression, boolean ascending, boolean insensitive) {
        SortOrder order;
        if (ascending) {
            order = insensitive ? SortOrder.ASCENDING_INSENSITIVE : SortOrder.ASCENDING;
        } else {
            order = insensitive ? SortOrder.DESCENDING_INSENSITIVE : SortOrder.DESCENDING;
        }
        orderings.add(new Ordering(expression, order));
    }

    void limit(int limit) {
        this.limit = limit;
    }

    void offset(int offset) {
        this.offset = offset;
    }

    void prefetch(String path, int semantics) {
        prefetchPaths.add(path);
        prefetchSemantics.add(semantics);
    }

    FluentSelect<?, ?> build() {
        ObjectSelect<?> objectSelect = dbRoot
                ? ObjectSelect.dbQuery(entityName)
                : ObjectSelect.query(Object.class, entityName);

        FluentSelect<?, ?> query = selectingRoot ? objectSelect : columnSelect(objectSelect);

        // "where" must be set before "having", as "having" switches the query to append to itself
        if (where != null) {
            query.where(rootPaths(where));
        }

        switch (query) {
            case ObjectSelect<?> select -> configure(select);
            case ColumnSelect<?> select -> configure(select);
            default -> throw new IllegalStateException("Unexpected query type: " + query.getClass().getName());
        }

        for (Ordering ordering : orderings) {
            query.orderBy(new Ordering(rootPaths(ordering.getSortSpec()), ordering.getSortOrder()));
        }
        if (limit > 0) {
            query.limit(limit);
        }
        if (offset > 0) {
            query.offset(offset);
        }
        for (int i = 0; i < prefetchPaths.size(); i++) {
            query.prefetch(prefetchPaths.get(i), prefetchSemantics.get(i));
        }
        return query;
    }

    private void configure(ObjectSelect<?> query) {
        if (distinct) {
            query.distinct();
        }
        if (having != null) {
            query.having(rootPaths(having));
        }
    }

    private void configure(ColumnSelect<?> query) {
        if (distinct) {
            query.distinct();
        }
        if (having != null) {
            query.having(rootPaths(having));
        }
    }

    private ColumnSelect<?> columnSelect(ObjectSelect<?> objectSelect) {
        if (columns.size() == 1) {
            return objectSelect.column(rootPaths(columns.get(0)));
        }

        Property<?>[] properties = new Property<?>[columns.size()];
        for (int i = 0; i < properties.length; i++) {
            properties[i] = rootPaths(columns.get(i));
        }
        return objectSelect.columns(properties);
    }

    private static boolean isSelf(Expression expression) {
        return expression.getType() == Expression.FULL_OBJECT && expression.getOperandCount() == 0;
    }

    private Property<?> rootPaths(Property<?> column) {
        if (!dbRoot) {
            return column;
        }
        BaseProperty<Object> dbColumn = PropertyFactory.createBase(rootPaths(column.getExpression()), null);
        return column.getAlias() != null ? dbColumn.alias(column.getAlias()) : dbColumn;
    }

    /**
     * In a query rooted in a DbEntity all the paths are DB paths, even when they are not prefixed with "db:".
     */
    private Expression rootPaths(Expression expression) {
        if (!dbRoot) {
            return expression;
        }
        return expression.transform(o -> o instanceof ASTObjPath objPath ? dbPath(objPath) : o);
    }

    private static ASTDbPath dbPath(ASTObjPath objPath) {
        ASTDbPath dbPath = new ASTDbPath(objPath.getPath());
        dbPath.setPathAliases(objPath.getPathAliases());
        return dbPath;
    }
}
