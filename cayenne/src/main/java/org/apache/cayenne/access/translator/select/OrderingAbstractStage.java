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

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

import java.util.function.Predicate;

import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationVisitor;
import org.apache.cayenne.access.sqlbuilder.StringBuilderAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.SelectResultNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SimpleNodeTreeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTAggregateFunctionCall;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.Ordering;

abstract class OrderingAbstractStage implements TranslationStage {

    protected void processOrdering(QualifierTranslator qualifierTranslator, TranslatorContext context, Ordering ordering) {
        Expression orderExp = ordering.getSortSpec();
        NodeBuilder nodeBuilder = node(qualifierTranslator.translate(orderExp));

        if(ordering.isCaseInsensitive()) {
            nodeBuilder = function("UPPER", nodeBuilder);
        }

        // If query is DISTINCT or GROUPING then we need to add the order column as a result column
        if (orderColumnAbsent(context, nodeBuilder)) {
            // deepCopy as some DB expect exactly the same expression in select and in ordering
            ResultNodeDescriptor descriptor = context.addResultNode(nodeBuilder.build().deepCopy());
            if(orderExp instanceof ASTAggregateFunctionCall) {
                descriptor.setAggregate(true);
            }
        }
    }

    private DbAttribute getOrderDbAttribute(Node translatedOrderNode)
    {
        DbAttribute[] orderDbAttribute = {null};
        translatedOrderNode.visit(new SimpleNodeTreeVisitor() {
            @Override
            public boolean onNodeStart(Node node) {
                if (node.getType() == NodeType.COLUMN) {
                    orderDbAttribute[0] = ((ColumnNode) node).getAttribute();
                    return false;
                }
                return true;
            }
        });
        return orderDbAttribute[0];
    }

    private boolean orderColumnAbsent(TranslatorContext context, NodeBuilder nodeBuilder)
    {
        var orderDbAttribute = getOrderDbAttribute(nodeBuilder.build());
        if (orderDbAttribute == null) return false; // Alias ?

        var orderEntity = orderDbAttribute.getEntity().getName();
        var orderColumn = orderDbAttribute.getName();

        Predicate<DbAttribute> columnAndEntity = dba -> dba != null
                                    && orderColumn.equals(dba.getName())
                                    && orderEntity.equals(dba.getEntity().getName());

        var orderStr = getSqlString(order(nodeBuilder));

        return context.getResultNodeList().stream()
            .filter( result -> columnAndEntity.test(result.getDbAttribute()) )
            .noneMatch( result -> getSqlString(node(result.getNode())).equals(orderStr) );
    }

    private String getSqlString(NodeBuilder nb) {
        var node = nb.build();
        if (node instanceof FunctionNode && ((FunctionNode) node).getAlias() != null)
        {
            // Wrap in result node otherwise content isn't generated, only alias
            node = new SelectResultNode().addChild(node.deepCopy());
        }
        var strBuilder = new StringBuilderAppendable();
        var sqlVisitor = new SQLGenerationVisitor(strBuilder);
        node.visit(sqlVisitor);
        return strBuilder.append(' ').toString();
    }
}
