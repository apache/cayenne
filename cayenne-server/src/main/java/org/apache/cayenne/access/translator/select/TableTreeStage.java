/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.select;

import java.util.List;
import java.util.function.Function;

import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.JoinNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTJoinPath;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.ASTPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ExpDbJoin;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjEntity;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.2
 */
class TableTreeStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        context.getTableTree().visit(node -> {
            NodeBuilder tableNode = table(node.getEntity().getFullyQualifiedName()).as(node.getTableAlias());
            if(node.getRelationship() != null) {
                tableNode = getJoin(node, tableNode).on(getJoinExpression(context, node));
            }

            context.getSelectBuilder().from(tableNode);
        });
    }

    private JoinNodeBuilder getJoin(TableTreeNode node, NodeBuilder table) {
        switch (node.getJoinType()) {
            case INNER:
                return join(table);
            case LEFT_OUTER:
                return leftJoin(table);
            default:
                throw new IllegalArgumentException("Unsupported join type: " + node.getJoinType());
        }
    }

    private NodeBuilder getJoinExpression(TranslatorContext context, TableTreeNode node) {
        List<DbJoin> joins = node.getRelationship().getJoins();

        ExpressionNodeBuilder expressionNodeBuilder = null;
        String sourceAlias = context.getTableTree().aliasForPath(node.getAttributePath().getParent());

        for(DbJoin dbJoin : joins) {
            ExpressionNodeBuilder joinExp;
            if(node.getRelationship().isUseJoinExp()) {
                Expression modifiedExpression = ((ExpDbJoin)dbJoin).getJoinExpression()
                        .transform(new ExpressionTransformer(node));
                joinExp = exp(() -> context.getQualifierTranslator().translate(modifiedExpression));
            } else {
                DbAttribute src = dbJoin.getSource();
                DbAttribute dst = dbJoin.getTarget();
                joinExp = table(sourceAlias).column(src)
                        .eq(table(node.getTableAlias()).column(dst));
            }

            if (expressionNodeBuilder != null) {
                expressionNodeBuilder = expressionNodeBuilder.and(joinExp);
            } else {
                expressionNodeBuilder = joinExp;
            }
        }

        expressionNodeBuilder = attachTargetQualifier(context, node, expressionNodeBuilder);

        return expressionNodeBuilder;
    }

    private ExpressionNodeBuilder attachTargetQualifier(TranslatorContext context, TableTreeNode node, ExpressionNodeBuilder expressionNodeBuilder) {
        Expression dbQualifier = node.getRelationship().getTargetEntity().getQualifier();
        if (dbQualifier != null) {
            String pathToRoot = node.getAttributePath().getPath();
            dbQualifier = dbQualifier.transform(input -> input instanceof ASTPath
                    ? new ASTDbPath(pathToRoot + '.' + ((ASTPath) input).getPath())
                    : input
            );
            Node translatedQualifier = context.getQualifierTranslator().translate(dbQualifier);
            if (expressionNodeBuilder != null) {
                expressionNodeBuilder = expressionNodeBuilder.and(node(translatedQualifier));
            } else {
                expressionNodeBuilder = exp(node(translatedQualifier));
            }
        }
        return expressionNodeBuilder;
    }

    final class ExpressionTransformer implements Function<Object, Object> {

        private PathComponents attributePath;
        private DbRelationship dbRelationship;

        ExpressionTransformer(TableTreeNode tableTreeNode) {
            this.attributePath = tableTreeNode.getAttributePath();
            this.dbRelationship = tableTreeNode.getRelationship();
        }

        @Override
        public Object apply(Object input) {
            if(input instanceof ASTPath) {
                ASTPath pathExpression = buildExpression((ASTPath) input);
                String prefix = buildPrefix(attributePath);
                String expressionPath = buildPath(attributePath, pathExpression, prefix);
                Expression resultExpression = ExpressionFactory.dbPathExp(expressionPath);

                if(prefix.isEmpty() ||
                        (!expressionPath.contains(dbRelationship.getName()) &&
                                attributePath.getParent().isEmpty())) {
                    return new ASTJoinPath((ASTDbPath) resultExpression, null);
                } else {
                    return new ASTJoinPath((ASTDbPath) resultExpression, prefix);
                }
            }

            return input;
        }

        private ASTPath buildExpression(ASTPath astPath) {
            if(astPath instanceof ASTObjPath) {
                DbEntity dbEntity = dbRelationship.getSourceEntity();
                MappingNamespace mns = dbEntity.getDataMap().getNamespace();
                for (ObjEntity objEntity : mns.getObjEntities()) {
                    if (dbEntity.equals(objEntity.getDbEntity())) {
                        return (ASTPath) objEntity.translateToDbPath(astPath);
                    }
                }
            }

            return astPath;
        }

        private String buildPath(PathComponents pathComponents, ASTPath astPath, String prefix) {
            String expressionPath = astPath.getPath();
            String parentPath = pathComponents.getParent();
            String path = parentPath != null && !parentPath.isEmpty() ?
                    parentPath + "." + expressionPath :
                    expressionPath;
            return path.replace(prefix, "");
        }

        private String buildPrefix(PathComponents pathComponents) {
            String attrPath = pathComponents.getPath();
            int index = attrPath.indexOf(":");
            return index > 0 ? attrPath.substring(0, index + 1) : "";
        }
    }
}
