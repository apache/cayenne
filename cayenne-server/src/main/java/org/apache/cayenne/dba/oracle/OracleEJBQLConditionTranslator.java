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
package org.apache.cayenne.dba.oracle;

import java.sql.Types;

import org.apache.cayenne.access.translator.ejbql.EJBQLConditionTranslator;
import org.apache.cayenne.access.translator.ejbql.EJBQLMultiColumnOperand;
import org.apache.cayenne.access.translator.ejbql.EJBQLPathTranslator;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslationContext;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.Node;
import org.apache.cayenne.map.ObjAttribute;

/**
 * @since 3.0
 */
class OracleEJBQLConditionTranslator extends EJBQLConditionTranslator {

    OracleEJBQLConditionTranslator(EJBQLTranslationContext context) {
        super(context);
    }

    @Override
    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {

        expression.visit(new EJBQLPathTranslator(context) {

            @Override
            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                OracleEJBQLConditionTranslator.this.addMultiColumnOperand(operand);
            }

            @Override
            protected void processTerminatingAttribute(ObjAttribute attribute) {
                if (attribute.getDbAttribute().getType() == Types.CHAR) {
                    context.append(' ').append(OracleAdapter.TRIM_FUNCTION).append("(");
                    super.processTerminatingAttribute(attribute);
                    context.append(')');
                }
                else {
                    super.processTerminatingAttribute(attribute);
                }
            }
        });

        return false;
    }

    /**
     * The order of arguments is inverted in Oracle.
     * LOCATE(substr, str) -> INSTR(str, substr)
     * @since 4.0
     */
    @Override
    public boolean visitLocate(EJBQLExpression expression, int finishedChildIndex) {
        if(finishedChildIndex < 0) {
            swapNodeChildren(expression, 0, 1);
            context.append(" INSTR(");
        } else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")");
            swapNodeChildren(expression, 0, 1);
        } else {
            context.append(',');
        }
        return true;
    }

    /**
     * @since 4.0
     */
    private void swapNodeChildren(EJBQLExpression expression, int i, int j) {
        if(!(expression instanceof Node)) {
            return;
        }
        Node node = (Node)expression;
        Node tmp = node.jjtGetChild(i);
        node.jjtAddChild(node.jjtGetChild(j), i);
        node.jjtAddChild(tmp, j);
    }
}
