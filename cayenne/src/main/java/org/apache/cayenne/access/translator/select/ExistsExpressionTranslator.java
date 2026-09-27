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
package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.exp.NotExistsExp;
import org.apache.cayenne.exp.SubqueryExp;
import org.apache.cayenne.exp.AggregateConditionExp;
import org.apache.cayenne.exp.ConditionExp;
import org.apache.cayenne.exp.BaseExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 5.0
 */
class ExistsExpressionTranslator {

    private final SelectTranslatorContext context;
    private final Expression expressionToTranslate;
    private final boolean not;

    ExistsExpressionTranslator(SelectTranslatorContext context, BaseExp exists) {
        this.context = context;
        this.expressionToTranslate = exists;
        this.not = exists instanceof NotExistsExp;
    }

    Expression translate() {
        Object child = expressionToTranslate.getOperand(0);
        if(child instanceof SubqueryExp) {
            return expressionToTranslate;
        }

        Expression translatedExpression;
        if(child instanceof Expression expression) {
            translatedExpression = expression;
        } else {
            throw new IllegalArgumentException("Expected expression as a child, got " + child);
        }

        DbEntity entity = context.getRootDbEntity();
        ObjEntity objEntity = context.getMetadata().getObjEntity();
        if (objEntity != null) {
            // unwrap all paths to DB
            translatedExpression = objEntity.translateToDbPath(translatedExpression);
        }

        // the relationship each path starts with, keyed by the node holding the rest of that path. A path that
        // doesn't start with a relationship of the root entity is not in the map.
        Map<DbPathExp, DbRelationship> relationships = new IdentityHashMap<>();

        // 0. quick path for a simple case - exists query for a single path expression
        // maybe we should support path as a condition in a general translator too, not only here
        if (translatedExpression instanceof DbPathExp dbPath) {
            DbPathExp tail = splitPath(entity, dbPath, relationships);
            Expression pathExistExp = pathCondition(tail);
            DbRelationship relationship = relationships.get(tail);
            if(relationship == null) {
                return pathExistExp;
            }
            return subqueryExpression(relationship, pathExistExp);
        }

        // 1. transform all paths
        translatedExpression = translatedExpression.transform(
                o -> o instanceof DbPathExp dbPath ? splitPath(entity, dbPath, relationships) : o
        );
        if (relationships.isEmpty()) {
            // no relationships in the original expression, so use it as is
            return translatedExpression;
        }

        // 2. find the enclosing conditions of the paths, and pair each relationship with the node to spawn
        //    a subquery from
        Ancestry ancestry = Ancestry.of(translatedExpression, relationships);
        List<RelationshipToNode> relationshipToNodes = subqueryNodes(ancestry, relationships);

        // 3. generate subqueries and paste them to the original expression
        return generateSubqueriesAndReplace(translatedExpression, relationshipToNodes, ancestry);
    }

    private Expression generateSubqueriesAndReplace(Expression expressionToTranslate,
                                                    List<RelationshipToNode> relationshipToNodes,
                                                    Ancestry ancestry) {
        Expression finalExpression = null;
        for (RelationshipToNode pair : relationshipToNodes) {
            Expression exp = pair.node() == null ? null : pair.node().deepCopy();
            BaseExp replacement = subqueryExpression(pair.relationship(), exp);

            BaseExp parent = pair.node() == null ? null : ancestry.parents.get(pair.node());
            if (parent == null) {
                if (finalExpression != null) {
                    throw new IllegalStateException("Expected single root expression");
                }
                finalExpression = replacement;
            } else {
                finalExpression = expressionToTranslate;
                for (int i = 0; i < parent.getOperandCount(); i++) {
                    if (parent.getOperand(i) == pair.node()) {
                        parent.setOperand(i, replacement);
                    }
                }
            }
        }
        return finalExpression;
    }

    private BaseExp subqueryExpression(DbRelationship relationship, Expression exp) {
        for (DbJoin join : relationship.getJoins()) {
            Expression joinMatchExp = ExpressionFactory.matchDbExp(join.getTargetName(),
                    ExpressionFactory.enclosingObjectExp(ExpressionFactory.dbPathExp(join.getSourceName())));
            if (exp == null) {
                exp = joinMatchExp;
            } else {
                exp = exp.andExp(joinMatchExp);
            }
        }
        ObjectSelect<Persistent> select = ObjectSelect.query(Persistent.class)
                .dbEntityName(relationship.getTargetEntityName())
                .where(exp);
        return (BaseExp) (not
                ? ExpressionFactory.notExists(select)
                : ExpressionFactory.exists(select));
    }

    /**
     * Returns a condition for a bare path, or null for an empty path, when a plain exists subquery is enough.
     */
    private Expression pathCondition(DbPathExp path) {
        if (path.getPath().isEmpty()) {
            return null;
        }
        return ExpressionFactory.noMatchExp(path, null);
    }

    /**
     * Pairs each relationship with the node whose copy becomes the qualifier of the relationship subquery. When
     * all the children of an aggregate condition are paths of the same relationship, the whole condition is
     * taken; otherwise each path is taken with its own nearest condition. A null node means a subquery with
     * no qualifier beyond the join.
     */
    private List<RelationshipToNode> subqueryNodes(Ancestry ancestry, Map<DbPathExp, DbRelationship> relationships) {
        List<RelationshipToNode> relationshipToNodes = new ArrayList<>(relationships.size());
        Map<BaseExp, Map<DbRelationship, List<DbPathExp>>> parents = new HashMap<>(4);
        for (DbPathExp path : ancestry.paths) {
            DbRelationship relationship = relationships.get(path);
            BaseExp aggregateCondition = ancestry.aggregateConditions.get(path);
            if (aggregateCondition == null) {
                // nothing above the path to take as a whole
                relationshipToNodes.add(new RelationshipToNode(relationship, ancestry.conditions.get(path)));
            } else {
                parents.computeIfAbsent(aggregateCondition, p -> new HashMap<>(4))
                        .computeIfAbsent(relationship, r -> new ArrayList<>(4))
                        .add(path);
            }
        }

        parents.forEach((parent, relToPath) ->
                relToPath.forEach((rel, paths) -> {
                    if (paths.size() != parent.getOperandCount()) {
                        paths.forEach(p -> relationshipToNodes
                                .add(new RelationshipToNode(rel, ancestry.conditions.get(p))));
                    } else {
                        relationshipToNodes.add(new RelationshipToNode(rel, parent));
                    }
                })
        );
        return relationshipToNodes;
    }

    /**
     * Splits a path into the relationship of the root entity it starts with and the rest of the path. Returns a
     * node holding the rest, and records the relationship for it in the map. A path that doesn't start with a
     * relationship is returned whole and is not recorded.
     */
    private DbPathExp splitPath(DbEntity entity, DbPathExp dbPath, Map<DbPathExp, DbRelationship> relationships) {
        CayennePath path = dbPath.getPath();
        DbRelationship relationship = entity.getRelationship(path.first().value());
        if (relationship == null) {
            return new DbPathExp(path);
        }
        DbPathExp tail = new DbPathExp(path.length() > 1 ? path.tail(1) : CayennePath.EMPTY_PATH);
        relationships.put(tail, relationship);
        return tail;
    }

    /**
     * A relationship and the node whose copy qualifies the relationship subquery, null when there is none.
     */
    private record RelationshipToNode(DbRelationship relationship, BaseExp node) {
    }

    /**
     * The tree structure that the translation needs but the nodes themselves don't keep: the parent of each node,
     * and for each relationship path its nearest enclosing condition and aggregate condition, null when there is
     * none. Collected in a single traversal.
     */
    private static class Ancestry {

        final Map<BaseExp, BaseExp> parents = new IdentityHashMap<>();
        final List<DbPathExp> paths = new ArrayList<>();
        final Map<DbPathExp, BaseExp> conditions = new IdentityHashMap<>();
        final Map<DbPathExp, BaseExp> aggregateConditions = new IdentityHashMap<>();

        static Ancestry of(Expression expression, Map<DbPathExp, DbRelationship> relationships) {
            Ancestry ancestry = new Ancestry();
            Deque<BaseExp> stack = new ArrayDeque<>();
            expression.traverse(new TraversalHandler() {
                @Override
                public void startNode(Expression node, Expression parentNode) {
                    BaseExp exp = (BaseExp) node;
                    if (parentNode != null) {
                        ancestry.parents.put(exp, (BaseExp) parentNode);
                    }
                    if (exp instanceof DbPathExp path && relationships.containsKey(path)) {
                        ancestry.paths.add(path);
                        ancestry.conditions.put(path, nearest(stack, ConditionExp.class));
                        ancestry.aggregateConditions.put(path, nearest(stack, AggregateConditionExp.class));
                    }
                    stack.push(exp);
                }

                @Override
                public void endNode(Expression node, Expression parentNode) {
                    stack.pop();
                }
            });
            return ancestry;
        }

        /**
         * Returns the innermost node of the given type among the ancestors on the stack, or null if there is none.
         */
        private static BaseExp nearest(Deque<BaseExp> ancestors, Class<? extends BaseExp> type) {
            for (BaseExp ancestor : ancestors) {
                if (type.isInstance(ancestor)) {
                    return ancestor;
                }
            }
            return null;
        }
    }
}
