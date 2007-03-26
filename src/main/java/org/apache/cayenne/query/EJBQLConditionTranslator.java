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
package org.apache.cayenne.query;

import java.math.BigDecimal;

import org.apache.cayenne.ejbql.EJBQLDelegatingVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLConditionTranslator extends EJBQLDelegatingVisitor {

    private EJBQLSelectTranslator parent;

    EJBQLConditionTranslator(EJBQLSelectTranslator parent) {
        this.parent = parent;
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
                    parent.getParent().getBuffer().append(" NOT");
                }
                parent.getParent().getBuffer().append(" BETWEEN #bind(");
                break;
            case 1:
                parent.getParent().getBuffer().append(") AND #bind(");
                break;
            case 2:
                parent.getParent().getBuffer().append(")");
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
                parent.getParent().getBuffer().append(" #bindEqual(");
                break;
            case 1:
                parent.getParent().getBuffer().append(")");
                break;
        }

        return true;
    }

    public boolean visitNot(EJBQLExpression expression) {
        parent.getParent().getBuffer().append(" NOT");
        return true;
    }

    public boolean visitNotEquals(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                parent.getParent().getBuffer().append(" #bindNotEqual(");
                break;
            case 1:
                parent.getParent().getBuffer().append(")");
                break;
        }

        return true;
    }

    public boolean visitGreaterThan(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                parent.getParent().getBuffer().append(" > #bind(");
                break;
            case 1:
                parent.getParent().getBuffer().append(")");
                break;
        }

        return true;
    }

    public boolean visitGreaterOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                parent.getParent().getBuffer().append(" >= #bind(");
                break;
            case 1:
                parent.getParent().getBuffer().append(")");
                break;
        }

        return true;
    }

    public boolean visitLessOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                parent.getParent().getBuffer().append(" <= #bind(");
                break;
            case 1:
                parent.getParent().getBuffer().append(")");
                break;
        }

        return true;
    }

    public boolean visitLessThan(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                parent.getParent().getBuffer().append(" < #bind(");
                break;
            case 1:
                parent.getParent().getBuffer().append(")");
                break;
        }

        return true;
    }

    public boolean visitLike(EJBQLExpression expression, int finishedChildIndex) {
        setDelegate(null);
        switch (finishedChildIndex) {
            case 0:
                if (expression.isNegated()) {
                    parent.getParent().getBuffer().append(" NOT");
                }
                parent.getParent().getBuffer().append(" LIKE #bind(");
                break;
            case 1:
                parent.getParent().getBuffer().append(")");
                break;
        }

        return true;
    }

    protected void afterChild(EJBQLExpression e, String text, int childIndex) {
        if (childIndex >= 0) {
            if (childIndex + 1 < e.getChildrenCount()) {
                parent.getParent().getBuffer().append(text);
            }

            // reset child-specific delegate
            setDelegate(null);
        }
    }

    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            setDelegate(new EJBQLPathTranslator(parent));
            return true;
        }
        else {
            return super.visitPath(expression, finishedChildIndex);
        }
    }

    public boolean visitStringLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            parent.getParent().getBuffer().append("null");
        }
        else {
            // note that String Literal text is already wrapped in single quotes, with
            // quotes that are part of the string escaped.
            parent.getParent().getBuffer().append(expression.getText()).append(
                    " 'VARCHAR'");
        }
        return true;
    }

    public boolean visitIntegerLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            parent.getParent().getBuffer().append("null");
        }
        else {
            Object value;

            try {
                value = new Integer(expression.getText());
            }
            catch (NumberFormatException nfex) {
                throw new EJBQLException("Invalid integer: " + expression.getText());
            }

            String var = parent.getParent().bindParameter(value);
            parent.getParent().getBuffer().append('$').append(var).append(" 'INTEGER'");
        }
        return true;
    }

    public boolean visitDecimalLiteral(EJBQLExpression expression) {
        if (expression.getText() == null) {
            parent.getParent().getBuffer().append("null");
        }
        else {
            Object value;

            try {
                value = new BigDecimal(expression.getText());
            }
            catch (NumberFormatException nfex) {
                throw new EJBQLException("Invalid decimal: " + expression.getText());
            }

            String var = parent.getParent().bindParameter(value);
            parent.getParent().getBuffer().append('$').append(var).append(" 'DECIMAL'");
        }
        return true;
    }
    
    public boolean visitPatternValue(EJBQLExpression expression) {
        // TODO: andrus 3/25/2007 - implement me
        return true;
    }
}
