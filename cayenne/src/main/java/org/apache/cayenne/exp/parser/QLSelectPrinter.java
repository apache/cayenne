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

import java.io.IOException;
import java.util.Collection;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Ordering;

/**
 * Prints a select query as a String that {@link QLParser} can parse back, the opposite of {@link QLSelectBuilder}.
 * Used to print the nested selects of an expression.
 * <p>
 * Just like the expressions that it consists of, a printed query may not be parseable if it contains values that have
 * no String representation in the grammar (dates, persistent objects, etc.) Also there is no access to the mapping
 * here, so a root defined as a Java class is printed as a simple name of that class, that is the name of the entity
 * by convention, but doesn't have to be.
 */
class QLSelectPrinter {

    static void append(FluentSelect<?, ?> query, Appendable out) throws IOException {
        appendSelect(query, out);
        appendFrom(query, out);

        if (query.getWhere() != null) {
            out.append(" where ");
            query.getWhere().appendAsString(out);
        }

        if (query.getHaving() != null) {
            out.append(" having ");
            query.getHaving().appendAsString(out);
        }

        appendOrderings(query, out);

        if (query.getLimit() > 0) {
            out.append(" limit ").append(String.valueOf(query.getLimit()));

            // there's no "offset" without a "limit" in the grammar
            if (query.getOffset() > 0) {
                out.append(" offset ").append(String.valueOf(query.getOffset()));
            }
        }
    }

    private static void appendSelect(FluentSelect<?, ?> query, Appendable out) throws IOException {
        Collection<Property<?>> columns = query.getColumns();
        boolean hasColumns = columns != null && !columns.isEmpty();

        // a query of the root objects needs no "select" clause, unless there's something to say about it
        if (!hasColumns && !query.isDistinct()) {
            return;
        }

        out.append("select ");
        if (query.isDistinct()) {
            out.append("distinct ");
        }

        if (!hasColumns) {
            out.append("self ");
            return;
        }

        boolean first = true;
        for (Property<?> column : columns) {
            if (!first) {
                out.append(", ");
            }
            first = false;

            column.getExpression().appendAsString(out);
            if (column.getAlias() != null) {
                out.append(" as ").append(column.getAlias());
            }
        }
        out.append(' ');
    }

    private static void appendFrom(FluentSelect<?, ?> query, Appendable out) throws IOException {
        out.append("from ");
        if (query.getEntityName() != null) {
            out.append(query.getEntityName());
        } else if (query.getDbEntityName() != null) {
            out.append(ASTDbPath.DB_PREFIX).append(query.getDbEntityName());
        } else if (query.getEntityType() != null) {
            out.append(query.getEntityType().getSimpleName());
        }
    }

    private static void appendOrderings(FluentSelect<?, ?> query, Appendable out) throws IOException {
        Collection<Ordering> orderings = query.getOrderings();
        if (orderings == null || orderings.isEmpty()) {
            return;
        }

        out.append(" order by ");
        boolean first = true;
        for (Ordering ordering : orderings) {
            if (!first) {
                out.append(", ");
            }
            first = false;

            Expression sortSpec = ordering.getSortSpec();
            sortSpec.appendAsString(out);
            if (ordering.isDescending()) {
                out.append(" desc");
            }
            if (ordering.isCaseInsensitive()) {
                out.append(" insensitive");
            }
        }
    }
}
