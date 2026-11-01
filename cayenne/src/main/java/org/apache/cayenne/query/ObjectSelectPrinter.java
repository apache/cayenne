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

package org.apache.cayenne.query;

import java.io.IOException;
import java.util.Collection;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.exp.property.Property;

/**
 * Prints a select query in the query language that {@link ObjectSelect#parse(String, Object...)} parses, the opposite
 * of the parser's builder. The implementation of {@link ObjectSelect#toQL()}, that reads the fields of the query
 * directly, so that the query needs no public getters for its clauses.
 *
 * @since 5.0
 */
final class ObjectSelectPrinter {

    private ObjectSelectPrinter() {
    }

    static void append(ObjectSelect<?> query, Appendable out) throws IOException {
        appendSelect(query, out);
        appendFrom(query, out);

        if (query.where != null) {
            out.append(" where ");
            query.where.appendAsString(out);
        }

        if (query.having != null) {
            out.append(" having ");
            query.having.appendAsString(out);
        }

        appendOrderings(query, out);

        if (query.fetchLimit > 0) {
            out.append(" limit ").append(String.valueOf(query.fetchLimit));
        }

        if (query.fetchOffset > 0) {
            out.append(" offset ").append(String.valueOf(query.fetchOffset));
        }

        appendPrefetches(query, out);
    }

    private static void appendSelect(ObjectSelect<?> query, Appendable out) throws IOException {
        Collection<Property<?>> columns = query.columns;
        boolean hasColumns = columns != null && !columns.isEmpty();

        // a query of the root objects needs no "select" clause, unless there's something to say about it
        if (!hasColumns && !query.distinct) {
            return;
        }

        out.append("select ");
        if (query.distinct) {
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

    private static void appendFrom(ObjectSelect<?> query, Appendable out) throws IOException {
        out.append("from ");
        if (query.entityName != null) {
            out.append(query.entityName);
        } else if (query.dbEntityName != null) {
            out.append(DbPathExp.DB_PREFIX).append(query.dbEntityName);
        } else if (query.entityType != null) {
            out.append(query.entityType.getSimpleName());
        }
    }

    private static void appendOrderings(ObjectSelect<?> query, Appendable out) throws IOException {
        Collection<Ordering> orderings = query.orderings;
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

    private static void appendPrefetches(ObjectSelect<?> query, Appendable out) throws IOException {
        PrefetchTreeNode prefetches = query.prefetches;
        if (prefetches == null) {
            return;
        }

        boolean first = true;
        for (PrefetchTreeNode node : prefetches.nonPhantomNodes()) {

            // the root of the tree is not a prefetch
            if (node.getParent() == null) {
                continue;
            }

            out.append(first ? " prefetch " : ", ");
            first = false;

            out.append(node.getPath().value());
            switch (node.getSemantics()) {
                case PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS -> out.append(" joint");
                case PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS -> out.append(" disjoint");
                case PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS -> out.append(" disjointById");
                default -> {
                    // undefined semantics is the default of the grammar
                }
            }
        }
    }
}
