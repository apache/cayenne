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
package org.apache.cayenne.map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionParameter;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.project.ProjectNodeVisitor;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.util.XMLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A mapped select query, defined by a String in the syntax of {@link ObjectSelect#parse(String, Object...)}.
 * The root of the query is defined by its "from" clause, so it is null while the String can't be parsed.
 * The settings that the String has no syntax for (caching, page size, etc.) are the properties of the
 * descriptor.
 *
 * @since 4.0
 */
public class SelectQueryDescriptor extends QueryDescriptor {

    // a single or double quoted string literal of the query grammar, with its backslash escapes
    private static final String QUOTED_STRING = "'(?:[^'\\\\]|\\\\.)*'|\"(?:[^\"\\\\]|\\\\.)*\"";

    private String ql;

    // the parsed "ql", or the error of parsing it, both lazily set. Inspected, but never run, as it is not a copy
    private FluentSelect<?, ?> parsed;
    private ExpressionException parseError;

    /**
     * Returns the query String, exactly as it was set.
     *
     * @since 5.0
     */
    public String getQl() {
        return ql;
    }

    /**
     * Sets the query String. The String is stored as is, and is not validated until the query is parsed.
     *
     * @since 5.0
     */
    public void setQl(String ql) {
        this.ql = ql;
        this.parsed = null;
        this.parseError = null;
    }

    /**
     * Returns whether the query String can be parsed.
     *
     * @since 5.0
     */
    public boolean isValid() {
        return parsedOrNull() != null;
    }

    /**
     * Returns the parsed query String with its "$name" parameters unbound. The returned query is shared and must not
     * be modified or run, use {@link #buildQuery(Map)} to get a query to run.
     *
     * @throws ExpressionException if the String is empty or can't be parsed
     * @since 5.0
     */
    public FluentSelect<?, ?> getParsedQuery() {
        FluentSelect<?, ?> query = parsedOrNull();
        if (query == null) {
            throw parseError;
        }
        return query;
    }

    private FluentSelect<?, ?> parsedOrNull() {
        if (parsed == null && parseError == null) {
            if (ql == null || ql.isBlank()) {
                parseError = new ExpressionException("The query is empty");
            } else {
                try {
                    parsed = ObjectSelect.parse(ql);
                } catch (ExpressionException e) {
                    parseError = e;
                }
            }
        }
        return parsed;
    }

    /**
     * Returns whether this query selects columns, and not the root objects.
     *
     * @since 5.0
     */
    public boolean isColumnQuery() {
        return parsedOrNull() instanceof ColumnSelect;
    }

    /**
     * Returns whether this query is rooted in a DbEntity.
     *
     * @since 5.0
     */
    public boolean isDbQuery() {
        FluentSelect<?, ?> query = parsedOrNull();
        return query != null && query.getDbEntityName() != null;
    }

    /**
     * Returns the names of the "$name" parameters of the query, in the order of their first occurrence.
     *
     * @throws ExpressionException if the query String can't be parsed
     * @since 5.0
     */
    public Set<String> getParameterNames() {
        Set<String> names = new LinkedHashSet<>();
        collectParameterNames(getParsedQuery(), names);
        return names;
    }

    private static void collectParameterNames(FluentSelect<?, ?> query, Set<String> names) {
        ParameterCollector collector = new ParameterCollector(names);
        if (query.getColumns() != null) {
            for (Property<?> column : query.getColumns()) {
                collector.collect(column.getExpression());
            }
        }
        collector.collect(query.getWhere());
        collector.collect(query.getHaving());
        if (query.getOrderings() != null) {
            for (Ordering ordering : query.getOrderings()) {
                collector.collect(ordering.getSortSpec());
            }
        }
    }

    /**
     * Renames an entity in the "from" clauses of the query String, those of the subqueries included. The rest of the
     * String, its formatting and string literals included, is kept as is. The String is not parsed, so it may be one
     * that can't be.
     *
     * @param db whether the entity is a DbEntity, i.e. is referenced as "from db:NAME"
     */
    void renameEntity(String oldName, String newName, boolean db) {
        if (ql == null || oldName.equals(newName)) {
            return;
        }

        // A scan of the String is exact for any String that parses: the grammar has no comments, and "from" followed
        // by a simple name is a "from" clause anywhere but inside a string literal, which is why the literals are
        // matched (and kept) too. The keyword is case-sensitive, and the name must be a whole token.
        Pattern reference = Pattern.compile(QUOTED_STRING + "|(\\bfrom\\s+" + (db ? "db:\\s*" : "") + ")"
                + Pattern.quote(oldName) + "(?![\\w$.])", Pattern.DOTALL);
        String renamed = reference.matcher(ql)
                .replaceAll(m -> Matcher.quoteReplacement(m.group(1) == null ? m.group() : m.group(1) + newName));
        if (!renamed.equals(ql)) {
            setQl(renamed);
        }
    }

    /**
     * Returns the root of the query defined by its "from" clause: an ObjEntity, a DbEntity or, if no such entity is
     * mapped, its name. Returns null if the query String can't be parsed.
     */
    @Override
    public Object getRoot() {
        FluentSelect<?, ?> query = parsedOrNull();
        if (query == null) {
            return null;
        }

        boolean dbRoot = query.getDbEntityName() != null;
        String rootName = dbRoot ? query.getDbEntityName() : query.getEntityName();
        if (dataMap == null) {
            return rootName;
        }

        Object entity = dbRoot ? dataMap.getDbEntity(rootName) : dataMap.getObjEntity(rootName);
        return entity != null ? entity : rootName;
    }

    /**
     * Not supported, as the root is defined by the "from" clause of the query String.
     */
    @Override
    public void setRoot(Object root) {
        throw new UnsupportedOperationException("The root of a select query is defined by its 'from' clause");
    }

    /**
     * @deprecated since 5.0, as the query is defined by a String, see {@link #getParsedQuery()}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public boolean isDistinct() {
        FluentSelect<?, ?> query = parsedOrNull();
        return query != null && query.isDistinct();
    }

    /**
     * Returns the "where" clause of the query, or null if it has none or can't be parsed.
     *
     * @deprecated since 5.0, as the query is defined by a String, see {@link #getParsedQuery()}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Expression getQualifier() {
        FluentSelect<?, ?> query = parsedOrNull();
        return query != null ? query.getWhere() : null;
    }

    /**
     * Returns a list of the orderings of the query, empty if it has none or can't be parsed.
     *
     * @deprecated since 5.0, as the query is defined by a String, see {@link #getParsedQuery()}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public List<Ordering> getOrderings() {
        FluentSelect<?, ?> query = parsedOrNull();
        return query != null && query.getOrderings() != null
                ? new ArrayList<>(query.getOrderings())
                : new ArrayList<>();
    }

    /**
     * Returns a map of the prefetch paths of the query with their semantics, empty if it has none or can't be parsed.
     *
     * @since 4.1
     * @deprecated since 5.0, as the query is defined by a String, see {@link #getParsedQuery()}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public Map<String, Integer> getPrefetchesMap() {
        Map<String, Integer> prefetches = new HashMap<>();
        FluentSelect<?, ?> query = parsedOrNull();
        if (query != null && query.getPrefetches() != null) {
            for (PrefetchTreeNode node : query.getPrefetches().nonPhantomNodes()) {
                // the root of the tree is not a prefetch
                if (node.getParent() != null) {
                    prefetches.put(node.getPath().value(), node.getSemantics());
                }
            }
        }
        return prefetches;
    }

    /**
     * Returns the query with its "$name" parameters unbound.
     */
    @Override
    public FluentSelect<?, ?> buildQuery() {
        return configure(parse(null));
    }

    /**
     * Returns the query with its "$name" parameters bound to the values from the map. The smallest condition that
     * contains a parameter missing from the map is pruned.
     *
     * @since 5.0
     */
    @Override
    public FluentSelect<?, ?> buildQuery(Map<String, ?> parameters) {
        return configure(parse(parameters != null ? parameters : Map.of()));
    }

    // each call parses the String anew, so that the query can be modified by the caller
    private FluentSelect<?, ?> parse(Map<String, ?> parameters) {
        if (ql == null || ql.isBlank()) {
            throw new CayenneRuntimeException("Query '%s' is empty", name);
        }

        try {
            return ObjectSelect.parse(ql, parameters);
        } catch (ExpressionException e) {
            throw new CayenneRuntimeException("Query '%s' can't be parsed: %s", e, name, e.getUnlabeledMessage());
        }
    }

    private FluentSelect<?, ?> configure(FluentSelect<?, ?> query) {
        query.pageSize(pageSize)
                .statementFetchSize(statementFetchSize)
                .cacheStrategy(cacheStrategy, cacheGroup);

        // a query rooted in a DbEntity fetches DataRows anyway, and a column query can't fetch them
        if (fetchingDataRows && query instanceof ObjectSelect<?> select) {
            select.fetchDataRows();
        }

        return query;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ProjectNodeVisitor<?> delegate) {
        // the String defines the root and all the clauses of the query, the rest of the properties are stored
        // separately
        encoder.start("objectQuery")
                .attribute("name", getName())
                .attribute("cacheStrategy", cacheStrategy != QueryCacheStrategy.NO_CACHE ? cacheStrategy.name() : null)
                .attribute("dataRows", fetchingDataRows)
                .attribute("pageSize", pageSize)
                .attribute("statementFetchSize", statementFetchSize);

        if (ql != null && !ql.isBlank()) {
            encoder.start("ql").cdata(ql, true).end();
        }

        encodeCacheGroup(encoder);

        delegate.visitQuery(this);
        encoder.end();
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
            if (node instanceof SubqueryExp subquery && subquery.getQuery() != null) {
                collectParameterNames(subquery.getQuery(), names);
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
