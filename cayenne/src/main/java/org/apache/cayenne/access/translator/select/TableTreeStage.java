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

import java.util.List;

import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.JoinNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.2
 */
class TableTreeStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        context.getTableTree().visit(node -> {
            NodeBuilder tableNode = table(node.getEntity()).as(node.getTableAlias());
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
        String sourceAlias = context.getTableTree().aliasForPath(node.getAttributePath().parent());
        for (DbJoin dbJoin : joins) {
            DbAttribute src = dbJoin.getSource();
            DbAttribute dst = dbJoin.getTarget();
            ExpressionNodeBuilder joinExp = table(sourceAlias).column(src)
                    .eq(table(node.getTableAlias()).column(dst));

            if (expressionNodeBuilder != null) {
                expressionNodeBuilder = expressionNodeBuilder.and(joinExp);
            } else {
                expressionNodeBuilder = joinExp;
            }
        }

        return expressionNodeBuilder;
    }
}
