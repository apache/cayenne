package de.jexp.jequel.expression;

import de.jexp.jequel.expression.logical.BooleanLiteral;
import de.jexp.jequel.expression.logical.BooleanExpression;
import de.jexp.jequel.expression.logical.BooleanUnaryExpression;
import de.jexp.jequel.expression.numeric.NumericExpression;
import de.jexp.jequel.expression.numeric.NumericLiteral;
import de.jexp.jequel.expression.numeric.NumericUnaryExpression;
import de.jexp.jequel.literals.Delimeter;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import static de.jexp.jequel.literals.UnaryOperator.*;
import static java.util.Arrays.asList;

public abstract class Expressions {
    public static final ConstantExpression<Void> NULL = new ConstantExpression<Void>("NULL");
    public static final ConstantExpression<Void> STAR = new ConstantExpression<Void>("*"); // TODO other type

    public static final BooleanExpression TRUE = BooleanLiteral.TRUE;
    public static final BooleanExpression FALSE = BooleanLiteral.FALSE;

    public static BooleanExpression not(BooleanExpression expression) {
        return new BooleanUnaryExpression(NOT, expression);
    }

    public static BooleanExpression exits(Object expression) {
        return new BooleanUnaryExpression(EXISTS, e(expression));
    }

    public static BooleanExpression notExits(Object expression) {
        return new BooleanUnaryExpression(NOT_EXISTS, e(expression));
    }

    // TODO numericExpr als Param
    public static NumericExpression sum(NumericExpression expression) {
        return new NumericUnaryExpression(SUM, expression);
    }

    public static NumericExpression sum(Number number) {
        return new NumericUnaryExpression(SUM, new NumericLiteral(number));
    }

    public static NumericExpression avg(NumericExpression expression) {
        return new NumericUnaryExpression(AVG, expression);
    }

    public static NumericExpression avg(Number number) {
        return new NumericUnaryExpression(AVG, new NumericLiteral(number));
    }

    public static NumericExpression round(NumericExpression expression) {
        return new NumericUnaryExpression(ROUND, expression);
    }

    public static NumericExpression toNumber(NumericExpression expression) {
        return new NumericUnaryExpression(TO_NUMBER, expression);
    }

    public static UnaryExpression min(Object expression) {
        return new UnaryExpression(MIN, e(expression));
    }

    public static UnaryExpression max(Object expression) {
        return new UnaryExpression(MAX, e(expression));
    }

    public static <E extends Expression> UnaryExpression<E> count(E expression) {
        return new UnaryExpression<E>(COUNT, expression);
    }

    public static UnaryExpression<NumericLiteral> count(Number number) {
        return count(new NumericLiteral(number));
    }

    public static Expression count() {
        return count(STAR);
    }

    public static Expression e(Object... expression) {
        return e(asList(expression));
    }

    public static Expression e(Object expression) {
        if (expression == null) return NULL;
        if (expression instanceof AbstractExpression) return (AbstractExpression) expression;
        if (expression instanceof Boolean) return (Boolean) expression ? TRUE : FALSE;
        if (expression instanceof String) return new StringExpression((String) expression);
        if (expression instanceof Number) return new NumericLiteral((Number) expression);
        if (expression instanceof Iterable) {
            return new RowListExpression(Delimeter.COMMA, createExpressionCollection((Iterable<?>) expression)) {
            };
        }
        if (expression.getClass().isArray()) {
            return new RowListExpression(Delimeter.COMMA, createExpressionCollection(asList(expression))) {
            };
        }
        return new ConstantExpression<Object>(null, expression);
    }

    public static ConstantExpression<String> sql(String sqlString) {
        return new ConstantExpression<String>(sqlString);
    }

    private static Collection<Expression> createExpressionCollection(Iterable<?> expression) {
        if (expression == null) {
            return Collections.emptyList();
        }
        Collection<Expression> expressions = new LinkedList<Expression>();
        for (Object element : expression) {
            expressions.add(e(element));
        }
        return expressions;
    }

    public static ParamExpression named(String paramName) {
        return new ParamExpression(paramName);
    }

    public static <T> ParamExpression<T> named(String paramName, T paramValue) {
        return new ParamExpression<T>(paramName, paramValue);
    }

    public static <T> ParamExpression<Collection<T>> named(String paramName, T... paramValues) {
        return new ParamExpression<Collection<T>>(paramName, asList(paramValues));
    }

    public static <T> ParamExpression<T> param(T paramValue) {
        return new ParamExpression<T>(paramValue);
    }

    public static <T> ParamExpression<Collection<T>> param(T... paramValues) {
        return new ParamExpression<Collection<T>>(asList(paramValues));
    }
}
