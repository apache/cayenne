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

import org.apache.cayenne.ejbql.EJBQLDelegatingVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLPath;
import org.apache.cayenne.ejbql.parser.EJBQLPositionalInputParameter;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLConditionTranslator extends EJBQLDelegatingVisitor {

    private EJBQLTranslationContext context;

    EJBQLConditionTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    public boolean visitAnd(EJBQLExpression expression, int finishedChildIndex) {
        afterChild(expression, " AND", finishedChildIndex);
        return true;
    }

    public boolean visitBetween(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                if (expression.isNegated()) {
                    context.append(" NOT");
                }
                context.append(" BETWEEN #bind(");
                break;
            case 1:
                context.append(") AND #bind(");
                break;
            case 2:
                context.append(")");
                break;
        }

        return true;
    }

    public boolean visitOr(EJBQLExpression expression, int finishedChildIndex) {
        afterChild(expression, " OR", finishedChildIndex);
        return true;
    }

    public boolean visitEquals(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                context.append(" #bindEqual(");
                break;
            case 1:
                context.append(")");
                break;
        }

        return true;
    }

    public boolean visitNot(EJBQLExpression expression) {
        context.append(" NOT");
        return true;
    }

    public boolean visitNotEquals(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                context.append(" #bindNotEqual(");
                break;
            case 1:
                context.append(")");
                break;
        }

        return true;
    }

    public boolean visitGreaterThan(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                context.append(" > #bind(");
                break;
            case 1:
                context.append(")");
                break;
        }

        return true;
    }

    public boolean visitGreaterOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                context.append(" >= #bind(");
                break;
            case 1:
                context.append(")");
                break;
        }

        return true;
    }

    public boolean visitLessOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                context.append(" <= #bind(");
                break;
            case 1:
                context.append(")");
                break;
        }

        return true;
    }

    public boolean visitLessThan(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                context.append(" < #bind(");
                break;
            case 1:
                context.append(")");
                break;
        }

        return true;
    }

    public boolean visitLike(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                if (expression.isNegated()) {
                    context.append(" NOT");
                }
                context.append(" LIKE #bind(");
                break;
            case 1:
                context.append(")");
                break;
        }

        return true;
    }

    protected void afterChild(EJBQLExpression e, String text, int childIndex) {
        if (childIndex >= 0) {
            if (childIndex + 1 < e.getChildrenCount()) {
                context.append(text);
            }

            // reset child-specific delegate
            setDelegate(null);
        }
    }

    public boolean visitPath(EJBQLPath expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            setDelegate(new EJBQLPathTranslator(context));
            return true;
        }
        else {
            return super.visitPath(expression, finishedChildIndex);
        }
    }

    public boolean visitStringLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            context.append("null");
        }
        else {
            // note that String Literal text is already wrapped in single quotes, with
            // quotes that are part of the string escaped.
            context.append(expression.getText()).append(" 'VARCHAR'");
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
            context.append('$').append(var).append(" 'INTEGER'");
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
            context.append('$').append(var).append(" 'DECIMAL'");
        }
        return true;
    }

    public boolean visitPatternValue(EJBQLExpression expression) {
        // TODO: andrus 3/25/2007 - implement me
        return true;
    }

    public boolean visitPositionalInputParameter(EJBQLPositionalInputParameter expression) {

        String parameter = context.bindPositionalParameter(expression.getPosition());
        context.append('$').append(parameter);
        return true;
    }
}
