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
 * @author Andrus Adamchik
 */
class EJBQLUpdateItemTranslator extends EJBQLConditionTranslator {

    public EJBQLUpdateItemTranslator(EJBQLTranslationContext context) {
        super(context);
    }

    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
        // unexpected, but make sure super is not called....
        return false;
    }

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

                EJBQLMultiColumnOperand lhs = (EJBQLMultiColumnOperand) multiColumnOperands
                        .get(0);
                EJBQLMultiColumnOperand rhs = (EJBQLMultiColumnOperand) multiColumnOperands
                        .get(1);

                Iterator it = lhs.getKeys().iterator();
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

    public boolean visitUpdateField(EJBQLExpression expression, int finishedChildIndex) {

        EJBQLPathTranslator pathTranslator = new EJBQLPathTranslator(context) {

            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                EJBQLUpdateItemTranslator.this.addMultiColumnOperand(operand);
            }

            public boolean visitUpdateField(
                    EJBQLExpression expression,
                    int finishedChildIndex) {
                return visitPath(expression, finishedChildIndex);
            }
        };

        expression.visit(pathTranslator);
        return false;
    }

    public boolean visitEquals(EJBQLExpression expression, int finishedChildIndex) {
        // unlike super, Equals here has no children and is itself a child of UpdateItem
        context.append(" =");
        return false;
    }

    public boolean visitUpdateValue(EJBQLExpression expression) {

        // a criteria for NULL is UpdateValue with no children
        if (expression.getChildrenCount() == 0) {
            context.append(" NULL");
            return false;
        }
        
        return true;
    }
}
