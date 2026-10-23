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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.exp.EnclosingObjectExp;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;

/**
 * Checks the entity names and paths of a parsed {@link SelectQueryDescriptor} against the mapping. Used by the project
 * validator, and by the Modeler to mark the problems in the query String.
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
     *
     * @param name    the entity name or the path, as written in the query String
     * @param clause  the clause of the query (or its subquery) that contains the name
     * @param message a description of the problem
     */
    public record Problem(String name, Clause clause, String message) {
    }

    private final DataMap dataMap;
    private final List<Problem> problems = new ArrayList<>();

    private SelectQueryPaths(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    /**
     * Returns the problems with the entity names and paths of a query, empty if there are none, or if the query
     * String can't be parsed, or the query is not in a DataMap.
     */
    public static List<Problem> check(SelectQueryDescriptor query) {
        if (query.getDataMap() == null || !query.isValid()) {
            return List.of();
        }

        SelectQueryPaths paths = new SelectQueryPaths(query.getDataMap());
        paths.checkSelect(query.getParsedQuery(), true);
        return paths.problems;
    }

    private void checkSelect(FluentSelect<?, ?> query, boolean root) {
        Entity<?, ?, ?> entity = resolveRoot(query);
        if (entity == null) {
            return;
        }

        // the aliases of the columns can be used in place of the paths elsewhere in the query
        Set<String> aliases = new HashSet<>();
        if (query.getColumns() != null) {
            for (Property<?> column : query.getColumns()) {
                checkExpression(entity, column.getExpression(), Clause.SELECT, aliases);
                if (column.getAlias() != null) {
                    aliases.add(column.getAlias());
                }
            }
        }

        checkExpression(entity, query.getWhere(), Clause.WHERE, aliases);
        checkExpression(entity, query.getHaving(), Clause.HAVING, aliases);

        if (query.getOrderings() != null) {
            for (Ordering ordering : query.getOrderings()) {
                checkExpression(entity, ordering.getSortSpec(), Clause.ORDER_BY, aliases);
            }
        }

        if (root && query.getPrefetches() != null) {
            for (PrefetchTreeNode node : query.getPrefetches().nonPhantomNodes()) {
                if (node.getParent() != null) {
                    checkPrefetch(entity, node.getPath().value());
                }
            }
        }
    }

    private Entity<?, ?, ?> resolveRoot(FluentSelect<?, ?> query) {
        if (query.getDbEntityName() != null) {
            DbEntity entity = dataMap.getDbEntity(query.getDbEntityName());
            if (entity == null) {
                problems.add(new Problem(query.getDbEntityName(), Clause.FROM,
                        "Unknown DbEntity '" + query.getDbEntityName() + "'"));
            }
            return entity;
        }

        ObjEntity entity = dataMap.getObjEntity(query.getEntityName());
        if (entity == null) {
            problems.add(new Problem(query.getEntityName(), Clause.FROM,
                    "Unknown ObjEntity '" + query.getEntityName() + "'"));
        }
        return entity;
    }

    private void checkExpression(Entity<?, ?, ?> entity, Expression expression, Clause clause, Set<String> aliases) {
        if (expression == null) {
            return;
        }

        switch (expression) {
            // the path of an enclosing object is relative to the outer query, that is checked on its own
            case EnclosingObjectExp ignored -> {
            }
            case SubqueryExp subquery -> {
                if (subquery.getQuery() != null) {
                    checkSelect(subquery.getQuery(), false);
                }
            }
            // there are no ObjPathExp in a query rooted in a DbEntity, all its paths are DB paths
            case ObjPathExp path when !aliases.contains(path.getPath().value()) ->
                    checkPath(entity, path.getExpandedPath().value(), path.getPath().value(), clause);
            case DbPathExp path -> {
                // a DB path needs no "db:" prefix in a query rooted in a DbEntity
                if (entity instanceof ObjEntity objEntity) {
                    if (objEntity.getDbEntity() != null) {
                        checkPath(objEntity.getDbEntity(), path.getExpandedPath().value(),
                                "db:" + path.getPath().value(), clause);
                    }
                } else if (!aliases.contains(path.getPath().value())) {
                    checkPath(entity, path.getExpandedPath().value(), path.getPath().value(), clause);
                }
            }
            default -> {
                for (int i = 0; i < expression.getOperandCount(); i++) {
                    if (expression.getOperand(i) instanceof Expression operand) {
                        checkExpression(entity, operand, clause, aliases);
                    }
                }
            }
        }
    }

    private void checkPath(Entity<?, ?, ?> entity, String path, String name, Clause clause) {
        try {
            entity.resolvePath(path);
        } catch (ExpressionException e) {
            problems.add(new Problem(name, clause, "Can't resolve path '" + name + "' in " + entity.getName()));
        }
    }

    private void checkPrefetch(Entity<?, ?, ?> entity, String path) {
        List<Object> components;
        try {
            components = entity.resolvePath(path);
        } catch (ExpressionException e) {
            problems.add(new Problem(path, Clause.PREFETCH,
                    "Can't resolve prefetch path '" + path + "' in " + entity.getName()));
            return;
        }

        if (!(components.getLast() instanceof Relationship<?, ?, ?>)) {
            problems.add(new Problem(path, Clause.PREFETCH,
                    "Prefetch path '" + path + "' doesn't end with a relationship"));
        }
    }
}
