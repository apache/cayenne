/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.jexp.jequel;

import de.jexp.jequel.expression.*;
import de.jexp.jequel.expression.visitor.ExpressionFormat;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.SqlKeyword;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import static org.apache.commons.lang3.StringUtils.join;

/**
 * @since 4.0
 */
public class Sql92ExpressionFormatter implements ExpressionFormat {

    @Override
    public <V> String visit(LiteralExpression<V> constantExpression) {
        return constantExpression.getValue().toString();
    }

    @Override
    public String visit(NumericLiteral numericLiteral) {
        return numericLiteral.getValue().toString();
    }

    @Override
    public String visit(NumericUnaryExpression numericUnaryExpression) {
        return visit(numericUnaryExpression.getUnaryExpression());
    }

    @Override
    public String visit(NumericBinaryExpression binaryExpression) {
        return visit(binaryExpression.getBinaryExpression());
    }

    @Override
    public String visit(BooleanLiteral bool) {
        if (bool.getValue() == null) {
            return "NULL";
        }
        return bool.getValue() ? "TRUE" : "FALSE";
    }

    @Override
    public String visit(BooleanUnaryExpression booleanUnaryExpression) {
        return visit(booleanUnaryExpression.getUnaryExpression());
    }

    @Override
    public String visit(BooleanBinaryExpression binaryExpression) {
        return visit(binaryExpression.getBinaryExpression());
    }

    @Override
    public String visit(BooleanListExpression list) {
        LinkedList<String> strings = new LinkedList<String>();
        for (BooleanExpression expression : list.getExpressions()) {
            if (expression == null) {
                continue;
            }

            String string = expression.accept(this);
            if (string.isEmpty()) {
                continue;
            }

            if (expression instanceof BooleanListExpression) {
                string = "(" + string + ")";
            }
            strings.add(string);
        }

        return join(strings, " " + list.getOperator().getSqlKeyword() + " ");

    }

    @Override
    public String visit(StringLiteral stringLiteral) {
        return "'" + visit((LiteralExpression<String>) stringLiteral) + "'";
    }

    @Override
    public String visit(UnaryExpression unaryExpression) {
        return visit(unaryExpression.getOperator()) + parenthese(unaryExpression.getExpression().accept(this));
    }

    @Override
    public String visit(BinaryExpression binaryExpression) {
        Expression first = binaryExpression.getFirst();
        Expression second = binaryExpression.getSecond();
        Operator operator = binaryExpression.getOperator();

        if (operator == Operator.IN) {
            return visitIn(first, second);
        }

        if (!binaryExpression.oneIsNull()) {
            return formatBinaryExpression(first, operator, second);
        }
        if (operator == Operator.EQ) {
            return formatBinaryExpression(first, Operator.IS, second);
        }
        if (operator == Operator.NE) {
            return formatBinaryExpression(first, Operator.IS_NOT, second);
        }

        return formatBinaryExpression(first, operator, second); // TODO not all Operators usable
    }

    protected String visitIn(Expression first, Expression second) {
        return first.accept(this) + " in (" + second.accept(this) + ")";
    }

    protected String formatBinaryExpression(Expression first, Operator operator, Expression second) {
        return first.accept(this) + " " + visit(operator) + " " + second.accept(this);
    }

    @Override
    public String visit(CompoundExpression listExpression) {
        return implode(listExpression.getDelimeter(), listExpression.getExpressions());
    }

    protected <E extends Expression> String implode(SqlKeyword delim, Iterable<E> expressions) {
        // TODO move code into compound expressions
        Collection<String> strings = new ArrayList<String>(10);
        for (E expression : expressions) {
            if (expression != null) {
                String string = expression.accept(this);
                if (!string.isEmpty()) {
                    strings.add(string);
                }
            }
        }

        return join(strings, delim.getSqlKeyword());
    }

    @Override
    public String visit(RowListExpression rowTupleExpression) {
        return visit((CompoundExpression) rowTupleExpression);
    }

    @Override
    public <T> String visit(ParamExpression<T> paramExpression) {
        if (paramExpression.isNamedExpression()) {
            return ":" + paramExpression.getLiteral();
        }
        return formatPreparedStatementParameter(paramExpression);
    }

    private <T> String formatPreparedStatementParameter(ParamExpression<T> paramExpression) {
        T value = paramExpression.getValue();
        if (value instanceof Iterable) {
            StringBuilder sb = new StringBuilder();
            for (Iterator it = ((Iterable) value).iterator(); it.hasNext();) {
                it.next();
                sb.append("?");
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        } else {
            return "?";
        }
    }

    @Override
    public <E extends Expression> String visit(ExpressionAlias<E> expression) {
        return expression.getAliased().accept(this) + " as " + expression.getAlias();
    }

    @Override
    public <T> String visit(VariableExpression field) {
        return field.getValue();
    }

    protected String parenthese(String expressionString) {
        return "(" + expressionString + ")";
    }

    @Override
    public String formatAround(String expressionString, Expression expression) {
        return expressionString;
    }

    /** Helpers  **/

    public String visit(SqlKeyword operator) {
        String sqlKeyword = operator.getSqlKeyword();
        if (sqlKeyword != null) {
            return sqlKeyword;
        } else {
            return operator.name().toLowerCase().replaceAll("_", " ");
        }
    }
}
