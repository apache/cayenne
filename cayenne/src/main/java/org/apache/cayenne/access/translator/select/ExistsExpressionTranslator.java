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
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTNotExists;
import org.apache.cayenne.exp.parser.ASTSubquery;
import org.apache.cayenne.exp.parser.AggregateConditionNode;
import org.apache.cayenne.exp.parser.ConditionNode;
import org.apache.cayenne.exp.parser.Node;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 5.0
 */
class ExistsExpressionTranslator {

    private final TranslatorContext context;
    private final Expression expressionToTranslate;
    private final boolean not;

    ExistsExpressionTranslator(TranslatorContext context, SimpleNode exists) {
        this.context = context;
        this.expressionToTranslate = exists;
        this.not = exists instanceof ASTNotExists;
    }

    Expression translate() {
        Object child = expressionToTranslate.getOperand(0);
        if(child instanceof ASTSubquery) {
            return expressionToTranslate;
        }

        Expression translatedExpression;
        if(child instanceof Expression) {
            translatedExpression = (Expression) child;
        } else {
            throw new IllegalArgumentException("Expected expression as a child, got " + child);
        }

        DbEntity entity = context.getRootDbEntity();
        ObjEntity objEntity = context.getMetadata().getObjEntity();
        if (objEntity != null) {
            // unwrap all paths to DB
            translatedExpression = objEntity.translateToDbPath(translatedExpression);
        }

        // 0. quick path for a simple case - exists query for a single path expression
        // maybe we should support path as a condition in a general translator too, not only here
        if (translatedExpression instanceof ASTDbPath) {
            DbPathMarker marker = createPathMarker(entity, (ASTDbPath) translatedExpression);
            Expression pathExistExp = markerToExpression(marker);
            if(marker.relationship == null) {
                return pathExistExp;
            }
            return subqueryExpression(marker.relationship, pathExistExp);
        }

        // 1. transform all paths
        translatedExpression = translatedExpression.transform(
                o -> o instanceof ASTDbPath ? createPathMarker(entity, (ASTDbPath) o) : o
        );

        // 2. group paths with db relationship by their parent conditions and relationships
        Map<SimpleNode, Map<DbRelationship, List<DbPathMarker>>> parents
                = groupPathsByParentAndRelationship(translatedExpression);
        if (parents.isEmpty()) {
            // no relationships in the original expression, so use it as is
            return translatedExpression;
        }

        // 3. make pairs relationship <-> node that should spawn a subquery
        List<RelationshipToNode> relationshipToNodes = uniqueNodes(parents);

        // 4. generate subqueries and paste them to the original expression
        return generateSubqueriesAndReplace(translatedExpression, relationshipToNodes);
    }

    private Expression generateSubqueriesAndReplace(Expression expressionToTranslate, List<RelationshipToNode> relationshipToNodes) {
        Expression finalExpression = null;
        for (RelationshipToNode pair : relationshipToNodes) {
            Expression exp = nodeToExpression(pair.node);
            SimpleNode replacement = subqueryExpression(pair.relationship, exp);

            Node parent = pair.node.jjtGetParent();
            if (parent == null) {
                if (finalExpression != null) {
                    throw new IllegalStateException("Expected single root expression");
                }
                finalExpression = replacement;
            } else {
                finalExpression = expressionToTranslate;
                for (int i = 0; i < parent.jjtGetNumChildren(); i++) {
                    if (parent.jjtGetChild(i) == pair.node) {
                        parent.jjtAddChild(replacement, i);
                        replacement.jjtSetParent(parent);
                    }
                }
            }
        }
        return finalExpression;
    }

    private SimpleNode subqueryExpression(DbRelationship relationship, Expression exp) {
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
        return (SimpleNode) (not
                ? ExpressionFactory.notExists(select)
                : ExpressionFactory.exists(select));
    }

    private Expression nodeToExpression(SimpleNode node) {
        if (node instanceof ParentMarker) {
            return null;
        }
        if (node instanceof DbPathMarker) {
            return markerToExpression((DbPathMarker) node);
        }
        return node.deepCopy();
    }

    private Expression markerToExpression(DbPathMarker marker) {
        // special case for an empty path
        // we don't need additional qualifier, just plain exists subquery
        if (marker.getPath().isEmpty()) {
            return null;
        }
        return ExpressionFactory.noMatchExp(marker, null);
    }

    private List<RelationshipToNode> uniqueNodes(Map<SimpleNode, Map<DbRelationship, List<DbPathMarker>>> parents) {
        List<RelationshipToNode> relationshipToNodes = new ArrayList<>(parents.size());
        parents.forEach((parent, relToPath) ->
                relToPath.forEach((rel, paths) -> {
                    if (paths.size() != parent.jjtGetNumChildren()) {
                        paths.forEach(p -> {
                            SimpleNode nearestCondition = getParentCondition(p);
                            relationshipToNodes.add(new RelationshipToNode(rel, nearestCondition));
                        });
                    } else {
                        relationshipToNodes.add(new RelationshipToNode(rel, parent));
                    }
                })
        );
        return relationshipToNodes;
    }

    private Map<SimpleNode, Map<DbRelationship, List<DbPathMarker>>> groupPathsByParentAndRelationship(
            Expression expressionToTranslate) {
        Map<SimpleNode, Map<DbRelationship, List<DbPathMarker>>> parents = new HashMap<>(4);
        expressionToTranslate.traverse((SimpleTraversalHandler) (node, parentNode) -> {
            if (node instanceof DbPathMarker) {
                DbPathMarker marker = (DbPathMarker) node;
                if (marker.root()) {
                    return;
                }
                SimpleNode parent = getParentAggregateCondition(parentNode);
                parents.computeIfAbsent(parent, p -> new HashMap<>(4))
                        .computeIfAbsent(marker.relationship, r -> new ArrayList<>(4))
                        .add(marker);
            }
        });
        return parents;
    }

    private SimpleNode getParentAggregateCondition(Expression parentNode) {
        Node parent = (Node) parentNode;
        while (parent != null && !(parent instanceof AggregateConditionNode)) {
            parent = parent.jjtGetParent();
        }
        if (parent == null) {
            parent = new ParentMarker();
        }
        return (SimpleNode) parent;
    }

    private SimpleNode getParentCondition(Expression parentNode) {
        Node parent = (Node) parentNode;
        while (parent != null && !(parent instanceof ConditionNode)) {
            parent = parent.jjtGetParent();
        }
        if (parent == null) {
            parent = new ParentMarker();
        }
        return (SimpleNode) parent;
    }

    private DbPathMarker createPathMarker(DbEntity entity, ASTDbPath o) {
        CayennePath path = o.getPath();
        CayennePath newPath = CayennePath.EMPTY_PATH;
        if(path.length() > 1) {
            newPath = path.tail(1);
        }
        // mark relationship that this path relates to and transform path
        DbRelationship relationship = entity.getRelationship(path.first().value());
        if (relationship == null) {
            newPath = path;
        }
        return new DbPathMarker(newPath, relationship);
    }

    static class RelationshipToNode {
        final DbRelationship relationship;
        final SimpleNode node;

        RelationshipToNode(DbRelationship relationship, SimpleNode node) {
            this.relationship = relationship;
            this.node = node;
        }
    }

    static class DbPathMarker extends ASTDbPath {

        final DbRelationship relationship;

        DbPathMarker(CayennePath path, DbRelationship relationship) {
            super(path);
            this.relationship = relationship;
        }

        @Override
        public Expression shallowCopy() {
            return new DbPathMarker(getPath(), relationship);
        }

        @Override
        public boolean equals(Object object) {
            return this == object;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        boolean root() {
            return relationship == null;
        }
    }

    static class ParentMarker extends ConditionNode {

        public ParentMarker() {
            super(0);
        }

        @Override
        public Expression shallowCopy() {
            return this;
        }

        @Override
        protected int getRequiredChildrenCount() {
            return 0;
        }

        @Override
        protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
            return null;
        }

        @Override
        protected String getExpressionOperator(int index) {
            return null;
        }

        @Override
        public boolean equals(Object object) {
            return this == object;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    interface SimpleTraversalHandler extends TraversalHandler {
        @Override
        void endNode(Expression node, Expression parentNode);
    }
}
