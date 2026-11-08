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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.EmbeddableObject;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.exp.AggregateFunctionCallExp;
import org.apache.cayenne.exp.ConcatExp;
import org.apache.cayenne.exp.CountExp;
import org.apache.cayenne.exp.DistinctExp;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.ExtractExp;
import org.apache.cayenne.exp.FullObjectExp;
import org.apache.cayenne.exp.LengthExp;
import org.apache.cayenne.exp.LocateExp;
import org.apache.cayenne.exp.LowerExp;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.exp.PathExp;
import org.apache.cayenne.exp.SubstringExp;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.TrimExp;
import org.apache.cayenne.exp.UpperExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.compiled.CompiledPropertyPath;
import org.apache.cayenne.map.compiled.CompiledDataMap;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledEmbeddedAttribute;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.ResultSegment;
import org.apache.cayenne.query.ResultSegments;
import org.apache.cayenne.query.ScalarResultSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A resolved select of objects, data rows or columns of an entity, produced by an {@link ObjectSelect}. Besides the
 * facts common to all queries, it carries the translation inputs (qualifier, orderings, columns, aliases) and the
 * selects that fetch its disjoint prefetches, as {@link #prefetches() children}. A child select is distinguished by a
 * non-null {@link #prefetchPath()}; it may select extra {@link #resultPaths() result paths} needed to attach its rows
 * to the parent objects.
 * <p>
 * Instances are created with a {@link Builder}, which derives the entity, the path aliases and the column result
 * mapping from the query state.
 *
 * @since 6.0
 */
public record ResolvedSelect(
        Query<?> source,
        CompiledEntity entity,
        CompiledTable table,
        CompiledDataMap dataMap,
        CompiledMapping mapping,
        Expression where,
        Expression having,
        List<Ordering> orderings,
        List<Property<?>> columns,
        boolean distinct,
        boolean suppressingDistinct,
        boolean fetchingDataRows,
        int fetchLimit,
        int fetchOffset,
        int pageSize,
        int statementFetchSize,
        int queryTimeout,
        QueryCacheStrategy cacheStrategy,
        String cacheGroup,
        PrefetchTreeNode prefetchTree,
        Map<String, String> pathSplitAliases,
        ResultSegments resultSegments,
        CayennePath prefetchPath,
        List<PathExp> resultPaths,
        List<ResolvedSelect> prefetches) implements ResolvedQuery {

    /**
     * A select never writes, so it can run in a read-only transaction.
     */
    @Override
    public boolean readOnly() {
        return true;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the cache key of a caching select, or null if the select does not cache. The key is a function of the
     * entity, the columns, the qualifier, the orderings, the fetch range and the prefetches.
     */
    @Override
    public String cacheKey(ValueObjectTypeRegistry valueObjectTypes) {
        return cacheStrategy != QueryCacheStrategy.NO_CACHE ? cacheKey(this, valueObjectTypes) : null;
    }

    /**
     * Computes the cache key of a select regardless of its cache strategy (a subquery is keyed whether or not it
     * caches).
     */
    private static String cacheKey(ResolvedSelect select, ValueObjectTypeRegistry valueObjectTypes) {
        StringBuilder key = new StringBuilder();
        CompiledMapping mapping = select.mapping;

        // handler to create string out of expressions
        TraversalHandler traversalHandler = new ToCacheKeyTraversalHandler(valueObjectTypes, key,
                subquery -> cacheKey(subquery.resolve(mapping), valueObjectTypes));

        if (select.entity != null) {
            key.append(select.entity.name());
        } else if (select.table != null) {
            key.append("db:").append(select.table.name());
        }

        for (Property<?> property : select.columns) {
            key.append("/c:");
            property.getExpression().traverse(traversalHandler);
        }

        if (select.where != null) {
            key.append('/');
            select.where.traverse(traversalHandler);
        }

        for (Ordering o : select.orderings) {
            key.append('/').append(o.getSortSpecString());
            if (!o.isAscending()) {
                key.append(":d");
            }
            if (o.isCaseInsensitive()) {
                key.append(":i");
            }
        }

        if (select.having != null) {
            key.append('/');
            select.having.traverse(traversalHandler);
        }

        if (select.fetchLimit > 0 || select.fetchOffset > 0) {
            key.append('/');
            if (select.fetchOffset > 0) {
                key.append('o').append(select.fetchOffset);
            }
            if (select.fetchLimit > 0) {
                key.append('l').append(select.fetchLimit);
            }
        }

        // add prefetch to cache key per CAY-2349
        if (select.prefetchTree != null) {
            select.prefetchTree.traverse(new ToCacheKeyPrefetchProcessor(key));
        }

        return key.toString();
    }

    /**
     * Collects the state of a select and derives a {@link ResolvedSelect} from it against a {@link CompiledMapping}.
     */
    public static final class Builder {

        private Query<?> source;
        private Class<?> entityType;
        private String entityName;
        private String dbEntityName;
        private Expression where;
        private Expression having;
        private Collection<Ordering> orderings;
        private Collection<Property<?>> columns;
        private boolean singleColumn = true;
        private boolean distinct;
        private boolean suppressingDistinct;
        private boolean fetchingDataRows;
        private int fetchLimit = FETCH_LIMIT_DEFAULT;
        private int fetchOffset = FETCH_OFFSET_DEFAULT;
        private int pageSize = PAGE_SIZE_DEFAULT;
        private int statementFetchSize = STATEMENT_FETCH_SIZE_DEFAULT;
        private int queryTimeout = QUERY_TIMEOUT_DEFAULT;
        private QueryCacheStrategy cacheStrategy = QueryCacheStrategy.getDefaultStrategy();
        private String cacheGroup;
        private PrefetchTreeNode prefetchTree;
        private CayennePath prefetchPath;
        private Collection<PathExp> resultPaths;

        private Builder() {
        }

        /**
         * Sets the query the select is resolved from. For a prefetch child this is the root user query.
         */
        public Builder source(Query<?> source) {
            this.source = source;
            return this;
        }

        /**
         * Sets the root of the select to a persistent class, clearing any other root.
         */
        public Builder entityType(Class<?> entityType) {
            return root(entityType, null, null);
        }

        /**
         * Sets the root of the select to a named ObjEntity, clearing any other root.
         */
        public Builder entityName(String entityName) {
            return root(null, entityName, null);
        }

        /**
         * Sets the root of the select to a named DbEntity, clearing any other root.
         */
        public Builder dbEntityName(String dbEntityName) {
            return root(null, null, dbEntityName);
        }

        private Builder root(Class<?> entityType, String entityName, String dbEntityName) {
            this.entityType = entityType;
            this.entityName = entityName;
            this.dbEntityName = dbEntityName;
            return this;
        }

        public Builder where(Expression where) {
            this.where = where;
            return this;
        }

        public Builder having(Expression having) {
            this.having = having;
            return this;
        }

        public Builder orderings(Collection<Ordering> orderings) {
            this.orderings = orderings;
            return this;
        }

        /**
         * Sets the columns of a column select. A single-column select returns the column values as is, anything else
         * returns an Object[] per row.
         */
        public Builder columns(Collection<Property<?>> columns, boolean singleColumn) {
            this.columns = columns;
            this.singleColumn = singleColumn;
            return this;
        }

        public Builder distinct(boolean distinct) {
            this.distinct = distinct;
            return this;
        }

        public Builder suppressingDistinct(boolean suppressingDistinct) {
            this.suppressingDistinct = suppressingDistinct;
            return this;
        }

        public Builder fetchingDataRows(boolean fetchingDataRows) {
            this.fetchingDataRows = fetchingDataRows;
            return this;
        }

        public Builder fetchLimit(int fetchLimit) {
            this.fetchLimit = fetchLimit;
            return this;
        }

        public Builder fetchOffset(int fetchOffset) {
            this.fetchOffset = fetchOffset;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder statementFetchSize(int statementFetchSize) {
            this.statementFetchSize = statementFetchSize;
            return this;
        }

        public Builder queryTimeout(int queryTimeout) {
            this.queryTimeout = queryTimeout;
            return this;
        }

        public Builder cacheStrategy(QueryCacheStrategy cacheStrategy) {
            this.cacheStrategy = cacheStrategy != null ? cacheStrategy : QueryCacheStrategy.getDefaultStrategy();
            return this;
        }

        public Builder cacheGroup(String cacheGroup) {
            this.cacheGroup = cacheGroup;
            return this;
        }

        /**
         * Sets the prefetch tree. The resolved select gets a deep copy of it.
         */
        public Builder prefetchTree(PrefetchTreeNode prefetchTree) {
            this.prefetchTree = prefetchTree;
            return this;
        }

        /**
         * Marks the select as the one fetching the disjoint prefetch at the given path of its parent, with the extra db
         * paths to select so that the rows can be attached to the parent objects. A prefetch select does not resolve
         * disjoint prefetches of its own.
         */
        public Builder prefetch(CayennePath prefetchPath, Collection<PathExp> resultPaths) {
            this.prefetchPath = prefetchPath;
            this.resultPaths = resultPaths;
            return this;
        }

        public ResolvedSelect build(CompiledMapping mapping) {
            Root root = resolveRoot(mapping);
            List<Ordering> orderings = this.orderings != null ? new ArrayList<>(this.orderings) : List.of();
            List<Property<?>> columns = this.columns != null ? new ArrayList<>(this.columns) : List.of();
            PrefetchTreeNode prefetchTree = this.prefetchTree != null ? this.prefetchTree.deepClone() : null;

            Map<String, String> aliases = resolveAliases(columns, orderings);
            ResultSegments resultSegments = columns.isEmpty() ? null : resultSegments(columns, root, mapping);

            // prefetches are suppressed for paginated queries, as they are resolved per page, and for data row
            // queries, as there are no objects to attach the related objects to
            List<ResolvedSelect> prefetches = prefetchPath == null
                    && prefetchTree != null
                    && !fetchingDataRows
                    && pageSize <= 0
                    && root.entity != null
                    ? prefetches(root.entity, prefetchTree, mapping)
                    : List.of();

            return new ResolvedSelect(source, root.entity, root.table, root.dataMap, mapping,
                    where, having, orderings, columns, distinct, suppressingDistinct, fetchingDataRows, fetchLimit,
                    fetchOffset, pageSize, statementFetchSize, queryTimeout, cacheStrategy, cacheGroup,
                    prefetchTree, aliases, resultSegments, prefetchPath,
                    resultPaths != null ? new ArrayList<>(resultPaths) : List.of(), prefetches);
        }

        private record Root(CompiledEntity entity, CompiledTable table, CompiledDataMap dataMap) {
        }

        private Root resolveRoot(CompiledMapping mapping) {
            CompiledEntity entity;
            if (entityType != null) {
                entity = mapping.entity(entityType);
                if (entity == null) {
                    throw new CayenneRuntimeException("Unrecognized entity type: %s", entityType.getName());
                }
            } else if (entityName != null) {
                entity = mapping.entity(entityName);
                if (entity == null) {
                    throw new CayenneRuntimeException("Unrecognized ObjEntity name: %s", entityName);
                }
            } else if (dbEntityName != null) {
                CompiledTable table = mapping.schema().table(dbEntityName);
                if (table == null) {
                    throw new CayenneRuntimeException("Unrecognized DbEntity name: %s", dbEntityName);
                }
                return new Root(null, table, table.map());
            } else {
                throw new CayenneRuntimeException("Undefined root entity of the query");
            }

            return new Root(entity, entity.table(), entity.map());
        }

        // auto aliases

        private Map<String, String> resolveAliases(List<Property<?>> columns, List<Ordering> orderings) {
            Map<String, String> aliases = new HashMap<>();
            if (where != null) {
                resolveAliases(where, aliases);
            }
            for (Ordering ordering : orderings) {
                if (ordering.getSortSpec() != null) {
                    resolveAliases(ordering.getSortSpec(), aliases);
                }
            }
            if (having != null) {
                resolveAliases(having, aliases);
            }
            for (Property<?> column : columns) {
                if (column.getExpression() != null) {
                    resolveAliases(column.getExpression(), aliases);
                }
            }
            return aliases;
        }

        private static void resolveAliases(Expression expression, Map<String, String> aliases) {
            for (Map.Entry<String, String> entry : expression.getPathAliases().entrySet()) {
                aliases.compute(entry.getKey(), (key, value) -> {
                    if (value != null && !value.equals(entry.getValue())) {
                        throw new CayenneRuntimeException("Can't add the same alias to different path segments.");
                    }
                    return entry.getValue();
                });
            }

            int len = expression.getOperandCount();
            for (int i = 0; i < len; i++) {
                if (expression.getOperand(i) instanceof Expression operand) {
                    resolveAliases(operand, aliases);
                }
            }
        }

        // column result mapping

        private ResultSegments resultSegments(List<Property<?>> columns, Root root, CompiledMapping mapping) {
            List<ResultSegment> segments = new ArrayList<>(columns.size());
            for (Property<?> column : columns) {
                segments.add(column.getType() != null
                        ? typedSegment(column, root.entity, mapping)
                        : untypedSegment(column, root.entity, mapping));
            }
            return new ResultSegments(segments, !singleColumn);
        }

        /**
         * Resolves a segment of a column with a declared Java type.
         */
        private static ResultSegment typedSegment(Property<?> column, CompiledEntity entity,
                                                  CompiledMapping mapping) {
            Expression exp = column.getExpression();
            Class<?> type = column.getType();

            // a to-many property is declared as a List or a Map, but a row holds a single related object, same as
            // with "flat()". The entity can't be resolved from the declared type, so taking it from the model.
            if (exp instanceof ObjPathExp
                    && (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))
                    && entity != null
                    && exp.evaluate(entity) instanceof CompiledRelationship relationship) {
                return entitySegment(relationship.target());
            }

            // "self", "flat()" or a to-one path
            if (exp instanceof FullObjectExp
                    || (exp instanceof ObjPathExp && Persistent.class.isAssignableFrom(type))) {
                CompiledEntity columnEntity = mapping.entity(type);
                if (columnEntity == null) {
                    throw new CayenneRuntimeException("No entity mapped for column '%s' of type %s", exp, type);
                }
                return entitySegment(columnEntity);
            }

            if (EmbeddableObject.class.isAssignableFrom(type)) {
                return embeddableSegment(exp, entity);
            }

            return new ScalarResultSegment(columnName(column), type, -1);
        }

        /**
         * Resolves a segment of a column without a declared type, inferring the type from the model or the expression.
         */
        private static ResultSegment untypedSegment(Property<?> column, CompiledEntity entity,
                                                    CompiledMapping mapping) {
            Expression exp = column.getExpression();

            // "self"
            if (exp instanceof FullObjectExp) {
                if (entity == null) {
                    throw new CayenneRuntimeException("Can't resolve an entity for column '%s'", exp);
                }
                return entitySegment(entity);
            }
            if (exp instanceof ObjPathExp && entity != null) {
                return switch (exp.evaluate(entity)) {
                    case CompiledEmbeddedAttribute ignored -> embeddableSegment(exp, entity);
                    // An entity segment means that a row holds a single object of the relationship target entity:
                    //  * to-one: the one related object
                    //  * to-many: a row is repeated for each related object, same as "flat()" does for a typed
                    //    property
                    // In both cases it is an outer join, so a row with no related objects is preserved and holds a
                    // null. Unlike a typed to-many property, there is no declared List or Map type here that a
                    // single object would contradict, so both kinds of relationships are resolved the same way.
                    case CompiledRelationship relationship -> entitySegment(relationship.target());
                    case CompiledAttribute attribute ->
                            new ScalarResultSegment(columnName(column), attribute.valueClass(), -1);
                    default -> new ScalarResultSegment(columnName(column), null, -1);
                };
            }

            return new ScalarResultSegment(columnName(column), inferType(exp, entity), -1);
        }

        private static ResultSegment entitySegment(CompiledEntity entity) {
            return new EntityResultSegment(entity, Collections.emptyMap(), -1);
        }

        private static ResultSegment embeddableSegment(Expression exp, CompiledEntity entity) {
            Object o = exp.evaluate(entity);
            if (!(o instanceof CompiledEmbeddedAttribute attribute)) {
                throw new CayenneRuntimeException("EmbeddedAttribute expected, %s found", o);
            }
            return new EmbeddableResultSegment(attribute.embeddable(), Collections.emptyMap(), -1);
        }

        /**
         * Infers the Java type of a function result, or returns null when it can't be told from the expression.
         */
        private static Class<?> inferType(Expression exp, CompiledEntity entity) {
            return switch (exp) {
                case CountExp ignored -> Long.class;
                case AggregateFunctionCallExp aggregate -> operandType(aggregate, entity);
                case UpperExp ignored -> String.class;
                case LowerExp ignored -> String.class;
                case ConcatExp ignored -> String.class;
                case SubstringExp ignored -> String.class;
                case TrimExp ignored -> String.class;
                case LengthExp ignored -> Integer.class;
                case LocateExp ignored -> Integer.class;
                case ExtractExp ignored -> Integer.class;
                default -> null;
            };
        }

        private static Class<?> operandType(Expression function, CompiledEntity entity) {
            if (function.getOperandCount() == 0 || !(function.getOperand(0) instanceof Expression operand)) {
                return null;
            }
            if (operand instanceof DistinctExp) {
                return operandType(operand, entity);
            }
            if (operand instanceof ObjPathExp
                    && entity != null
                    && operand.evaluate(entity) instanceof CompiledAttribute attribute) {
                return attribute.valueClass();
            }
            return null;
        }

        private static String columnName(Property<?> column) {
            return column.getName() == null ? column.getExpression().expName() : column.getName();
        }

        // disjoint prefetches

        /**
         * Resolves a child select for each disjoint (or unknown-semantics) prefetch node of the tree, at any depth. A
         * child is rooted in the entity at the end of the prefetch path, qualified by the parent qualifier translated
         * to that entity, and carries the node subtree so that its own joint prefetches work. Its nested disjoint
         * prefetches are children of this select as well, not of the child.
         */
        private List<ResolvedSelect> prefetches(CompiledEntity entity, PrefetchTreeNode tree,
                                                CompiledMapping mapping) {
            List<PrefetchTreeNode> disjointNodes = new ArrayList<>();
            tree.traverse(new PrefetchProcessor() {
                @Override
                public boolean startPhantomPrefetch(PrefetchTreeNode node) {
                    return true;
                }

                @Override
                public boolean startDisjointPrefetch(PrefetchTreeNode node) {
                    // don't do anything to root
                    if (node != tree) {
                        disjointNodes.add(node);
                    }
                    return true;
                }

                @Override
                public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
                    // resolved during object conversion, once the parent rows are known
                    return true;
                }

                @Override
                public boolean startJointPrefetch(PrefetchTreeNode node) {
                    // resolved by the join of the parent select
                    return true;
                }

                @Override
                public boolean startUnknownPrefetch(PrefetchTreeNode node) {
                    // route unknown as disjoint...
                    return startDisjointPrefetch(node);
                }

                @Override
                public void finishPrefetch(PrefetchTreeNode node) {
                }
            });

            if (disjointNodes.isEmpty()) {
                return List.of();
            }

            // chain query and entity qualifiers
            Expression qualifier = where;
            Expression entityQualifier = entity.qualifier();
            if (entityQualifier != null) {
                qualifier = qualifier != null ? qualifier.andExp(entityQualifier) : entityQualifier;
            }

            List<ResolvedSelect> prefetches = new ArrayList<>(disjointNodes.size());
            for (PrefetchTreeNode node : disjointNodes) {
                CayennePath prefetchPath = node.getPath();

                // find last relationship
                CompiledPropertyPath resolved = entity.resolvePath(prefetchPath);
                if (!resolved.endsInRelationship()) {
                    throw new CayenneRuntimeException("Invalid prefetch '%s' for entity '%s'", prefetchPath,
                            entity.name());
                }
                CompiledRelationship relationship = resolved.relationships().getLast();

                // extra result columns to be able to relate result rows to the parent result objects
                List<PathExp> resultPaths = relationship.isForeignKey()
                        ? List.of()
                        : List.of((PathExp) ExpressionFactory.dbPathExp(relationship.reverseDbPath()));

                prefetches.add(builder()
                        .source(source)
                        .entityName(relationship.target().name())
                        .where(entity.translateToRelatedTable(qualifier, prefetchPath))
                        .statementFetchSize(statementFetchSize)
                        // pass prefetch subtree to enable joint prefetches...
                        .prefetchTree(node)
                        .prefetch(prefetchPath, resultPaths)
                        .build(mapping));
            }

            return prefetches;
        }
    }
}
