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
package org.apache.cayenne.access.jdbc;

import java.util.Iterator;

import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * @since 3.0
 */
class EJBQLUpdateItemTranslator extends EJBQLConditionTranslator {

    EJBQLUpdateItemTranslator(EJBQLTranslationContext context) {
        super(context);
    }

    @Override
    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
        // unexpected, but make sure super is not called....
        return false;
    }

    @Override
    public boolean visitUpdateItem(EJBQLExpression expression, int finishedChildIndex) {

        if (finishedChildIndex == expression.getChildrenCount() - 1) {

            // check multicolumn match condition and undo op insertion and append it
            // from scratch if needed
            if (multiColumnOperands != null) {

                if (multiColumnOperands.size() != 2) {
                    throw new EJBQLException(
                            "Invalid multi-column equals expression. Expected 2 multi-column operands, got "
                                    + multiColumnOperands.size());
                }

                context.trim(2);

                EJBQLMultiColumnOperand lhs = multiColumnOperands.get(0);
                EJBQLMultiColumnOperand rhs = multiColumnOperands.get(1);

                Iterator<?> it = lhs.getKeys().iterator();
                while (it.hasNext()) {
                    Object key = it.next();

                    lhs.appendValue(key);
                    context.append(" =");
                    rhs.appendValue(key);

                    if (it.hasNext()) {
                        context.append(',');
                    }
                }

                multiColumnOperands = null;
            }
        }

        return true;
    }

    @Override
    public boolean visitUpdateField(EJBQLExpression expression, int finishedChildIndex) {

        EJBQLPathTranslator pathTranslator = new EJBQLPathTranslator(context) {

            @Override
            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                EJBQLUpdateItemTranslator.this.addMultiColumnOperand(operand);
            }

            @Override
            public boolean visitUpdateField(
                    EJBQLExpression expression,
                    int finishedChildIndex) {
                return visitPath(expression, finishedChildIndex);
            }
        };

        // some DB's do not support aliases in SET (Postgresql)
        pathTranslator.setUsingAliases(false);

        expression.visit(pathTranslator);
        return false;
    }

    @Override
    public boolean visitEquals(EJBQLExpression expression, int finishedChildIndex) {
        // unlike super, Equals here has no children and is itself a child of UpdateItem
        context.append(" =");
        return false;
    }

    @Override
    public boolean visitUpdateValue(EJBQLExpression expression) {

        // a criteria for NULL is UpdateValue with no children
        if (expression.getChildrenCount() == 0) {
            context.append(" NULL");
            return false;
        }

        return true;
    }
}
