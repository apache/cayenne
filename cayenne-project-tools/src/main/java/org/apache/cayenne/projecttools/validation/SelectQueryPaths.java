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
package org.apache.cayenne.projecttools.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.exp.EnclosingObjectExp;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.exp.PathExp;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;

/**
 * Checks the entity names and paths of a parsed {@link SelectQueryDescriptor} against the mapping. Used by the project
 * validator, and by the Modeler to mark the problems in the query String. The walk of the query that resolves the
 * paths is available on its own, see {@link #walk(DataMap, FluentSelect, PathVisitor)}.
 *
 * @since 5.0
 */
public final class SelectQueryPaths {

    /**
     * A clause of a query that a problem was found in.
     */
    public enum Clause {
        FROM, SELECT, WHERE, HAVING, ORDER_BY, PREFETCH
    }

    /**
     * A name of an entity or a path that can't be resolved.
     */
    public record Problem(String name, Clause clause, String message) {
    }

    /**
     * An occurrence of a path in a query or its subquery. A column alias used in place of a path is reported with the
     * components of the path of its column, and is not reported if its column is not a resolvable path.
     */
    public record PathRef(Clause clause, Entity<?, ?, ?> entity, PathExp path, String name,
                          List<Object> components, Expression parent) {
    }

    /**
     * A callback of {@link #walk(DataMap, FluentSelect, PathVisitor)}.
     */
    public interface PathVisitor {

        void visitPath(PathRef path);

        default void visitUnknownRoot(FluentSelect<?, ?> query) {
        }
    }

    private final DataMap dataMap;
    private final PathVisitor visitor;
    private final Entity<?, ?, ?> entity;

    // the aliases of the columns can be used in place of the paths elsewhere in the query, each maps to the
    // components of the path of its column, or to null if the column is not a resolvable path
    private final Map<String, List<Object>> aliases = new HashMap<>();

    private SelectQueryPaths(DataMap dataMap, PathVisitor visitor, Entity<?, ?, ?> entity) {
        this.dataMap = dataMap;
        this.visitor = visitor;
        this.entity = entity;
    }

    /**
     * Returns the problems with the entity names and paths of a query, empty if there are none, or if the query
     * String can't be parsed, or the query is not in a DataMap.
     */
    public static List<Problem> check(SelectQueryDescriptor query) {
        if (query.getDataMap() == null || !query.isValid()) {
            return List.of();
        }

        List<Problem> problems = new ArrayList<>();
        FluentSelect<?, ?> parsed = query.getParsedQuery();
        Entity<?, ?, ?> root = walk(query.getDataMap(), parsed, new PathVisitor() {

            @Override
            public void visitPath(PathRef path) {
                if (path.components() == null) {
                    problems.add(new Problem(path.name(), path.clause(),
                            "Can't resolve path '" + path.name() + "' in " + path.entity().getName()));
                }
            }

            @Override
            public void visitUnknownRoot(FluentSelect<?, ?> unknown) {
                problems.add(unknown.getDbEntityName() != null
                        ? new Problem(unknown.getDbEntityName(), Clause.FROM,
                                "Unknown DbEntity '" + unknown.getDbEntityName() + "'")
                        : new Problem(unknown.getEntityName(), Clause.FROM,
                                "Unknown ObjEntity '" + unknown.getEntityName() + "'"));
            }
        });

        if (root != null && parsed.getPrefetches() != null) {
            for (PrefetchTreeNode node : parsed.getPrefetches().nonPhantomNodes()) {
                if (node.getParent() != null) {
                    checkPrefetch(root, node.getPath(), problems);
                }
            }
        }
        return problems;
    }

    /**
     * Walks the paths of the "select", "where", "having" and "order by" clauses of a query and of its subqueries,
     * resolving each of them against the root of the (sub)query that contains it. The paths of the enclosing
     * objects of a subquery are relative to the outer query, and are not walked.
     *
     * @return the root entity of the query, or null if it is not mapped
     */
    public static Entity<?, ?, ?> walk(DataMap dataMap, FluentSelect<?, ?> query, PathVisitor visitor) {
        Entity<?, ?, ?> entity = query.getDbEntityName() != null
                ? dataMap.getDbEntity(query.getDbEntityName())
                : dataMap.getObjEntity(query.getEntityName());
        if (entity == null) {
            visitor.visitUnknownRoot(query);
            return null;
        }

        new SelectQueryPaths(dataMap, visitor, entity).walkClauses(query);
        return entity;
    }

    private void walkClauses(FluentSelect<?, ?> query) {
        if (query.getColumns() != null) {
            for (Property<?> column : query.getColumns()) {
                PathRef path = walkExpression(column.getExpression(), null, Clause.SELECT);
                if (column.getAlias() != null) {
                    aliases.put(column.getAlias(), path != null ? path.components() : null);
                }
            }
        }

        walkExpression(query.getWhere(), null, Clause.WHERE);
        walkExpression(query.getHaving(), null, Clause.HAVING);

        if (query.getOrderings() != null) {
            for (Ordering ordering : query.getOrderings()) {
                walkExpression(ordering.getSortSpec(), null, Clause.ORDER_BY);
            }
        }
    }

    // returns the visited path if the expression is a path itself
    private PathRef walkExpression(Expression expression, Expression parent, Clause clause) {
        if (expression == null) {
            return null;
        }

        PathRef path = switch (expression) {
            // the path of an enclosing object is relative to the outer query, that is walked on its own
            case EnclosingObjectExp ignored -> null;
            case SubqueryExp subquery -> {
                if (subquery.getQuery() != null) {
                    walk(dataMap, subquery.getQuery(), visitor);
                }
                yield null;
            }
            // there are no ObjPathExp in a query rooted in a DbEntity, all its paths are DB paths
            case ObjPathExp objPath -> pathRef(entity, objPath, objPath.getPath().value(), parent, clause);
            // a DB path of a query rooted in an ObjEntity is resolved against its DbEntity, with a "db:" prefix
            case DbPathExp dbPath when entity instanceof ObjEntity objEntity -> objEntity.getDbEntity() != null
                    ? pathRef(objEntity.getDbEntity(), dbPath, "db:" + dbPath.getPath().value(), parent, clause)
                    : null;
            // a DB path needs no "db:" prefix in a query rooted in a DbEntity
            case DbPathExp dbPath -> pathRef(entity, dbPath, dbPath.getPath().value(), parent, clause);
            default -> {
                for (int i = 0; i < expression.getOperandCount(); i++) {
                    if (expression.getOperand(i) instanceof Expression operand) {
                        walkExpression(operand, expression, clause);
                    }
                }
                yield null;
            }
        };

        if (path != null) {
            visitor.visitPath(path);
        }
        return path;
    }

    // an alias is never a "db:" path, as it is a simple name
    private PathRef pathRef(Entity<?, ?, ?> target, PathExp path, String name, Expression parent, Clause clause) {
        if (!aliases.containsKey(name)) {
            return new PathRef(clause, target, path, name, resolve(target, path.getExpandedPath()), parent);
        }

        // an alias of a column that is not a resolvable path is not a path either
        List<Object> components = aliases.get(name);
        return components != null ? new PathRef(clause, target, path, name, components, parent) : null;
    }

    private static List<Object> resolve(Entity<?, ?, ?> entity, CayennePath path) {
        try {
            return entity.resolvePath(path);
        } catch (ExpressionException e) {
            return null;
        }
    }

    private static void checkPrefetch(Entity<?, ?, ?> entity, CayennePath path, List<Problem> problems) {
        List<Object> components = resolve(entity, path);
        if (components == null) {
            problems.add(new Problem(path.value(), Clause.PREFETCH,
                    "Can't resolve prefetch path '" + path.value() + "' in " + entity.getName()));
        } else if (!(components.getLast() instanceof Relationship<?, ?, ?>)) {
            problems.add(new Problem(path.value(), Clause.PREFETCH,
                    "Prefetch path '" + path.value() + "' doesn't end with a relationship"));
        }
    }
}
