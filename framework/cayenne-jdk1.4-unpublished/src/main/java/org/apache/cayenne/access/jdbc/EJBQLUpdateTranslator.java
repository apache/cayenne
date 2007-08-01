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

import java.math.BigDecimal;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLPositionalInputParameter;

/**
 * A translator of EJBQL UPDATE statements into SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLUpdateTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private int itemCount;

    EJBQLUpdateTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    EJBQLTranslationContext getContext() {
        return context;
    }

    public boolean visitUpdate(EJBQLExpression expression) {
        context.append("UPDATE");
        return true;
    }

    public boolean visitWhere(EJBQLExpression expression) {
        context.append(" WHERE");
        expression.visit(new EJBQLConditionTranslator(context));
        return false;
    }

    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        expression.visit(new EJBQLFromTranslator(context));

        return false;
    }

    public boolean visitUpdateItem(EJBQLExpression expression) {
        if (itemCount++ > 0) {
            context.append(',');
        }
        else {
            context.append(" SET");
        }

        return true;
    }

    public boolean visitUpdateField(EJBQLExpression expression, int finishedChildIndex) {

        EJBQLPathTranslator pathTranslator = new EJBQLPathTranslator(context) {

            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                throw new EJBQLException(
                        "Multi-column paths are not yet supported in UPDATEs");
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

    public boolean visitUpdateValue(EJBQLExpression expression) {
        context.append(" =");
        return true;
    }

    // TODO: andrus, 7/31/2007 - all literal processing (visitStringLiteral,
    // visitIntegerLiteral, visitDecimalLiteral, visitBooleanLiteral,
    // visitPositionalInputParameter, visitnamedInputParameter) is duplicated in
    // EJBQLConditionalTranslator - may need to refactor
    public boolean visitStringLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            // note that String Literal text is already wrapped in single quotes, with
            // quotes that are part of the string escaped.
            context.append(" #bind(").append(expression.getText()).append(" 'VARCHAR')");
        }

        return true;
    }

    public boolean visitBooleanLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            Object value = new Boolean(expression.getText());
            String var = context.bindParameter(value);
            context.append(" #bind($").append(var).append(" 'BOOLEAN')");
        }

        return true;
    }

    public boolean visitIntegerLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            Object value;

            try {
                value = new Integer(expression.getText());
            }
            catch (NumberFormatException nfex) {
                throw new EJBQLException("Invalid integer: " + expression.getText());
            }

            String var = context.bindParameter(value);
            context.append(" #bind($").append(var).append(" 'INTEGER')");
        }
        return true;
    }

    public boolean visitDecimalLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            Object value;

            try {
                value = new BigDecimal(expression.getText());
            }
            catch (NumberFormatException nfex) {
                throw new EJBQLException("Invalid decimal: " + expression.getText());
            }

            String var = context.bindParameter(value);
            context.append(" #bind($").append(var).append(" 'DECIMAL')");
        }
        return true;
    }

    public boolean visitPositionalInputParameter(EJBQLPositionalInputParameter expression) {

        String parameter = context.bindPositionalParameter(expression.getPosition());
        processParameter(parameter);
        return true;
    }

    public boolean visitNamedInputParameter(EJBQLExpression expression) {
        String parameter = context.bindNamedParameter(expression.getText());
        processParameter(parameter);
        return true;
    }

    private void processParameter(String boundName) {
        Object object = context.getBoundParameter(boundName);

        Map map = null;
        if (object instanceof Persistent) {
            map = ((Persistent) object).getObjectId().getIdSnapshot();
        }
        else if (object instanceof ObjectId) {
            map = ((ObjectId) object).getIdSnapshot();
        }
        else if (object instanceof Map) {
            map = (Map) object;
        }

        if (map != null) {
            if (map.size() == 1) {
                context.rebindParameter(boundName, map.values().iterator().next());
            }
            else {
                throw new EJBQLException(
                        "Multi-column paths are not yet supported in UPDATEs");
            }
        }

        if (object != null) {
            context.append(" #bind($").append(boundName).append(")");
        }
        else {
            // this is a hack to prevent execptions on DB's like Derby for expressions
            // "X = NULL". The 'VARCHAR' parameter is totally bogus, but seems to work on
            // all tested DB's... Also note what JPA spec, chapter 4.11 says: "Comparison
            // or arithmetic operations with a NULL value always yield an unknown value."

            // TODO: andrus 6/28/2007 Ideally we should track the type of the current
            // expression to provide a meaningful type.
            context.append(" #bind($").append(boundName).append(" 'VARCHAR')");
        }
    }
}
