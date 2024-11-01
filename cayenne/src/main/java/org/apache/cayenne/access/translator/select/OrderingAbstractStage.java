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

import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTAggregateFunctionCall;
import org.apache.cayenne.query.Ordering;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

abstract class OrderingAbstractStage implements TranslationStage {

    protected void processOrdering(QualifierTranslator qualifierTranslator, TranslatorContext context, Ordering ordering) {
        Expression orderExp = ordering.getSortSpec();
        NodeBuilder nodeBuilder = node(qualifierTranslator.translate(orderExp));

        if (ordering.isCaseInsensitive()) {
            nodeBuilder = function("UPPER", nodeBuilder);
        }

        // If query is DISTINCT or GROUPING then we need to add the order column as a result column
        Node orderingNode = nodeBuilder.build();
        if (orderColumnAbsent(context, orderingNode)) {
            // deepCopy as some DB expect exactly the same expression in select and in ordering
            ResultNodeDescriptor descriptor = context.addResultNode(orderingNode.deepCopy());
            if (orderExp instanceof ASTAggregateFunctionCall) {
                descriptor.setAggregate(true);
            }
        }
    }

    private boolean orderColumnAbsent(TranslatorContext context, Node orderingNode) {
        for (ResultNodeDescriptor result : context.getResultNodeList()) {
            if (result.getNode().deepEquals(orderingNode)) {
                return false;
            }
        }
        return true;
    }
}
