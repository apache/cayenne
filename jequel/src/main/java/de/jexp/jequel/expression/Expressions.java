package de.jexp.jequel.expression;

import de.jexp.jequel.Delimeter;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.UnaryOperator;
import static de.jexp.jequel.literals.UnaryOperator.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public abstract class Expressions {
    public static final ConstantExpression<Void> NULL = new ConstantExpression<Void>("NULL");
    public static final ConstantExpression<Void> STAR = new ConstantExpression<Void>("*"); // TODO other type

    public static final BooleanExpression TRUE = new BooleanConstantExpression("TRUE", true);
    public static final BooleanExpression FALSE = new BooleanConstantExpression("FALSE", false);

    public static BooleanUnaryExpression not(Object expression) {
        return new BooleanUnaryExpression(NOT, e(expression));
    }

    // TODO numericExpr als Param
    public static Expression sum(Object expression) {
        return new NumericUnaryExpression(SUM, e(expression));
    }

    public static Expression avg(Object expression) {
        return new NumericUnaryExpression(AVG, e(expression));
    }

    public static Expression count(Object expression) {
        return new NumericUnaryExpression(COUNT, e(expression));
    }

    public static Expression round(Object expression) {
        return new NumericUnaryExpression(ROUND, e(expression));
    }

    public static Expression toNumber(Object expression) {
        return new NumericUnaryExpression(TO_NUMBER, e(expression));
    }

    public static BooleanUnaryExpression exits(Object expression) {
        return new BooleanUnaryExpression(EXISTS, e(expression));
    }

    public static BooleanUnaryExpression notExits(Object expression) {
        return new BooleanUnaryExpression(NOT_EXISTS, e(expression));
    }

    public static UnaryExpression min(Object expression) {
        return new UnaryExpression(MIN, e(expression));
    }

    public static UnaryExpression max(Object expression) {
        return new UnaryExpression(MAX, e(expression));
    }

    public static Expression count() {
        return count(STAR);
    }

    /*
    public static  <T> Expression<T> e(final Expression<T> expression) {
        return expression;
    }

    public static  Expression<Boolean> e(final Boolean value) {
        return value ? TRUE : FALSE;
    }

    public static  Expression<Number> e(final Number value) {
        return new NumericExpression(value);
    }

    public static <T> ConstantExpression<T> e(final T value) {
        return new ConstantExpression<T>(value);
    }
    */
    public static Expression e(Object... expression) {
        return e(createExpressionCollection(expression));
    }


    public static Expression e(Object expression) {
        if (expression == null) return NULL;
        if (expression instanceof AbstractExpression) return (AbstractExpression) expression;
        if (expression instanceof Boolean) return (Boolean) expression ? TRUE : FALSE;
        if (expression instanceof String) return new StringExpression((String) expression);
        if (expression instanceof Number) return new NumericExpression((Number) expression);
        if (expression instanceof Iterable) {
            return new RowListExpression(Delimeter.COMMA, createExpressionCollection((Iterable<?>) expression)) {
            };
        }
        if (expression.getClass().isArray()) {
            return new RowListExpression(Delimeter.COMMA, createExpressionCollection((Object[]) expression)) {
            };
        }
        return new ConstantExpression(null, expression);
    }

    public static ConstantExpression<String> sql(String sqlString) {
        return new ConstantExpression<String>(sqlString);
    }

    private static Collection<Expression> createExpressionCollection(Object... expression) {
        if (expression == null || expression.length == 0) {
            return Collections.emptyList();
        }
        Collection<Expression> expressions = new LinkedList<Expression>();
        for (Object element : expression) {
            expressions.add(e(element));
        }
        return expressions;
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

    static SimpleListExpression createColumnTuple(Delimeter delim, AbstractExpression... expressions) {
        return new SimpleListExpression(delim, expressions);
    }

    public static ParamExpression named(String paramName) {
        return new ParamExpression(paramName);
    }

    public static <T> ParamExpression<T> named(String paramName, T paramValue) {
        return new ParamExpression<T>(paramName, paramValue);
    }

    public static <T> ParamExpression<Collection<T>> named(String paramName, T... paramValues) {
        return new ParamExpression<Collection<T>>(paramName, Arrays.asList(paramValues));
    }

    public static <T> ParamExpression<T> param(T paramValue) {
        return new ParamExpression<T>(paramValue);
    }

    public static <T> ParamExpression<Collection<T>> param(T... paramValues) {
        return new ParamExpression<Collection<T>>(Arrays.asList(paramValues));
    }

    public static Collection<Expression> toCollection(Expression... expressions) {
        return createExpressionCollection((Expression[]) expressions);
    }

    protected static BooleanBinaryExpression createBinaryBooleanExpression(Expression first, Operator operator, Object expression) {
        return new BooleanBinaryExpression(first, operator, e(expression));
    }

    public static NumericUnaryExpression nvl(Object... expressions) {
        return new NumericUnaryExpression(UnaryOperator.NVL, e(expressions));
    }

}
