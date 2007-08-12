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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLPositionalInputParameter;
import org.apache.cayenne.ejbql.parser.EJBQLSubselect;
import org.apache.cayenne.ejbql.parser.EJBQLTrimBoth;
import org.apache.cayenne.ejbql.parser.EJBQLTrimSpecification;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLConditionTranslator extends EJBQLBaseVisitor {

    protected EJBQLTranslationContext context;
    protected List multiColumnOperands;

    EJBQLConditionTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    void addMultiColumnOperand(EJBQLMultiColumnOperand operand) {
        if (multiColumnOperands == null) {
            multiColumnOperands = new ArrayList(2);
        }

        multiColumnOperands.add(operand);
    }

    public boolean visitAggregate(EJBQLExpression expression) {
        expression.visit(new EJBQLAggregateColumnTranslator(context));
        return false;
    }

    public boolean visitAnd(EJBQLExpression expression, int finishedChildIndex) {
        afterChild(expression, " AND", finishedChildIndex);
        return true;
    }

    public boolean visitBetween(EJBQLExpression expression, int finishedChildIndex) {
        switch (finishedChildIndex) {
            case 0:
                if (expression.isNegated()) {
                    context.append(" NOT");
                }
                context.append(" BETWEEN");
                break;
            case 1:
                context.append(" AND");
                break;
        }

        return true;
    }

    public boolean visitExists(EJBQLExpression expression) {
        context.append(" EXISTS");
        return true;
    }

    public boolean visitIsEmpty(EJBQLExpression expression) {

        // handle as "path is [not] null" (an alt. way would've been a correlated subquery
        // on the target entity)...

        if (expression.isNegated()) {
            context.switchToMarker(EJBQLSelectTranslator.makeDistinctMarker(), true);
            context.append(" DISTINCT");
            context.switchToMainBuffer();
        }

        visitIsNull(expression, -1);
        for (int i = 0; i < expression.getChildrenCount(); i++) {
            expression.getChild(i).visit(this);
            visitIsNull(expression, i);
        }

        return false;
    }

    public boolean visitMemberOf(EJBQLExpression expression) {
        // handle as "? =|<> path" (an alt. way would've been a correlated subquery
        // on the target entity)...

        if (expression.isNegated()) {
            context.switchToMarker(EJBQLSelectTranslator.makeDistinctMarker(), true);
            context.append(" DISTINCT");
            context.switchToMainBuffer();

            visitNotEquals(expression, -1);
            for (int i = 0; i < expression.getChildrenCount(); i++) {
                expression.getChild(i).visit(this);
                visitNotEquals(expression, i);
            }
        }
        else {
            visitEquals(expression, -1);
            for (int i = 0; i < expression.getChildrenCount(); i++) {
                expression.getChild(i).visit(this);
                visitEquals(expression, i);
            }
        }

        return false;
    }

    public boolean visitAll(EJBQLExpression expression) {
        context.append(" ALL");
        return true;
    }

    public boolean visitAny(EJBQLExpression expression) {
        context.append(" ANY");
        return true;
    }

    public boolean visitOr(EJBQLExpression expression, int finishedChildIndex) {
        afterChild(expression, " OR", finishedChildIndex);
        return true;
    }

    public boolean visitEquals(EJBQLExpression expression, int finishedChildIndex) {
        switch (finishedChildIndex) {
            case 0:
                context.append(" =");
                break;
            case 1:
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
                            context.append(" AND");
                        }
                    }

                    multiColumnOperands = null;
                }

                break;
        }

        return true;
    }

    public boolean visitNamedInputParameter(EJBQLExpression expression) {
        String parameter = context.bindNamedParameter(expression.getText());
        processParameter(parameter);
        return true;
    }

    public boolean visitNot(EJBQLExpression expression) {
        context.append(" NOT");
        return true;
    }

    public boolean visitNotEquals(EJBQLExpression expression, int finishedChildIndex) {
        switch (finishedChildIndex) {
            case 0:
                context.append(" <>");
                break;
            case 1:
                // check multicolumn match condition and undo op insertion and append it
                // from scratch if needed
                if (multiColumnOperands != null) {

                    if (multiColumnOperands.size() != 2) {
                        throw new EJBQLException(
                                "Invalid multi-column equals expression. Expected 2 multi-column operands, got "
                                        + multiColumnOperands.size());
                    }

                    context.trim(3);

                    EJBQLMultiColumnOperand lhs = (EJBQLMultiColumnOperand) multiColumnOperands
                            .get(0);
                    EJBQLMultiColumnOperand rhs = (EJBQLMultiColumnOperand) multiColumnOperands
                            .get(1);

                    Iterator it = lhs.getKeys().iterator();
                    while (it.hasNext()) {
                        Object key = it.next();

                        lhs.appendValue(key);
                        context.append(" <>");
                        rhs.appendValue(key);

                        if (it.hasNext()) {
                            context.append(" OR");
                        }
                    }

                    multiColumnOperands = null;
                }

                break;
        }
        return true;
    }

    public boolean visitGreaterThan(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(" >");
        }

        return true;
    }

    public boolean visitGreaterOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(" >=");
        }

        return true;
    }

    public boolean visitLessOrEqual(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(" <=");
        }

        return true;
    }

    public boolean visitLessThan(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(" <");
        }

        return true;
    }

    public boolean visitLike(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            if (expression.isNegated()) {
                context.append(" NOT");
            }
            context.append(" LIKE");
        }

        return true;
    }

    public boolean visitIn(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            if (expression.isNegated()) {
                context.append(" NOT");
            }
            context.append(" IN");

            // a cosmetic hack for preventing extra pair of parenthesis from being
            // appended in 'visitSubselect'
            if (expression.getChildrenCount() == 2
                    && expression.getChild(1) instanceof EJBQLSubselect) {
                visitSubselect(expression.getChild(1));
                return false;
            }

            context.append(" (");
        }
        else if (finishedChildIndex == expression.getChildrenCount() - 1) {
            context.append(")");
        }
        else if (finishedChildIndex > 0) {
            context.append(',');
        }

        return true;
    }

    protected void afterChild(EJBQLExpression e, String text, int childIndex) {
        if (childIndex >= 0) {
            if (childIndex + 1 < e.getChildrenCount()) {
                context.append(text);
            }
        }
    }

    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {

        expression.visit(new EJBQLPathTranslator(context) {

            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                EJBQLConditionTranslator.this.addMultiColumnOperand(operand);
            }
        });
        return false;
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

    public boolean visitEscapeCharacter(EJBQLExpression expression) {
        // note that EscapeChar text is already wrapped in single quotes
        context.append(" ESCAPE ").append(expression.getText());
        return false;
    }

    public boolean visitIsNull(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex == 0) {
            context.append(expression.isNegated() ? " IS NOT NULL" : " IS NULL");
        }

        return true;
    }

    public boolean visitPositionalInputParameter(EJBQLPositionalInputParameter expression) {

        String parameter = context.bindPositionalParameter(expression.getPosition());
        processParameter(parameter);
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

    public boolean visitSubselect(EJBQLExpression expression) {
        context.append(" (");
        expression.visit(new EJBQLSelectTranslator(context));
        context.append(')');
        return false;
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
                addMultiColumnOperand(EJBQLMultiColumnOperand.getObjectOperand(
                        context,
                        map));
                return;
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

    public boolean visitCurrentDate(EJBQLExpression expression) {
        context.append(" {fn CURDATE()}");
        return false;
    }

    public boolean visitCurrentTime(EJBQLExpression expression) {
        context.append(" {fn CURTIME()}");
        return false;
    }

    public boolean visitCurrentTimestamp(EJBQLExpression expression) {
        context.append(" {fn NOW()}");
        return false;
    }

    public boolean visitAbs(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn ABS(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitSqrt(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn SQRT(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitMod(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn MOD(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }
        else {
            context.append(',');
        }

        return true;
    }

    public boolean visitConcat(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn CONCAT(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }
        else {
            context.append(',');
        }

        return true;
    }

    public boolean visitSubstring(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn SUBSTRING(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }
        else {
            context.append(',');
        }

        return true;
    }

    public boolean visitLower(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn LCASE(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitUpper(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn UCASE(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitLength(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn LENGTH(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    public boolean visitLocate(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn LOCATE(");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }
        else {
            context.append(',');
        }

        return true;
    }

    public boolean visitTrim(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {

            if (!(expression.getChild(0) instanceof EJBQLTrimSpecification)) {
                context.append(" {fn LTRIM({fn RTRIM(");
            }
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            if (!(expression.getChild(0) instanceof EJBQLTrimSpecification)
                    || expression.getChild(0) instanceof EJBQLTrimBoth) {
                context.append(")})}");
            }
            else {
                context.append(")}");
            }
        }

        return true;
    }

    public boolean visitTrimCharacter(EJBQLExpression expression) {
        // this is expected to be overwritten in adapter-specific translators
        throw new UnsupportedOperationException("Not implemented in a generic translator");
    }

    public boolean visitTrimLeading(EJBQLExpression expression) {
        context.append(" {fn LTRIM(");
        return false;
    }

    public boolean visitTrimTrailing(EJBQLExpression expression) {
        context.append(" {fn RTRIM(");
        return false;
    }

    public boolean visitTrimBoth(EJBQLExpression expression) {
        context.append(" {fn LTRIM({fn RTRIM(");
        return false;
    }
}
