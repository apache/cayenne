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
import org.apache.cayenne.access.sqlbuilder.OrderingNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.Ordering;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.2
 */
class OrderingStage implements TranslationStage {

    @Override
    public void perform(TranslatorContext context) {
        if(context.getQuery().getOrderings() == null) {
            return;
        }

        QualifierTranslator qualifierTranslator = context.getQualifierTranslator();
        for(Ordering ordering : context.getQuery().getOrderings()) {
            processOrdering(qualifierTranslator, context, ordering);
        }
    }

    private void processOrdering(QualifierTranslator qualifierTranslator, TranslatorContext context, Ordering ordering) {
        Expression exp = ordering.getSortSpec();
        Node translatedNode = qualifierTranslator.translate(exp);

        NodeBuilder nodeBuilder = node(translatedNode);
        if(ordering.isCaseInsensitive()) {
            nodeBuilder = function("UPPER", nodeBuilder);
        }

        OrderingNodeBuilder orderingNodeBuilder = order(nodeBuilder);
        if(ordering.isDescending()) {
            orderingNodeBuilder.desc();
        }
        context.getSelectBuilder().orderBy(orderingNodeBuilder);
    }

}
