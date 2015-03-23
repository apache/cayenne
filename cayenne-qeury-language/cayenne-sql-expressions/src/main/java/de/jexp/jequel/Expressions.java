package de.jexp.jequel;

import de.jexp.jequel.expression.*;
import de.jexp.jequel.literals.Delimeter;

import java.util.*;

import static de.jexp.jequel.literals.UnaryOperator.*;
import static java.util.Arrays.asList;

public class Expressions {

    private final ExpressionsFactory factory;

//    public static final ConstantExpression<Void> NULL = new ConstantExpression<Void>("NULL");
//    public static final ConstantExpression<Void> STAR = new ConstantExpression<Void>("*"); // TODO other type

//    public static final BooleanExpression TRUE = BooleanLiteral.TRUE;
//    public static final BooleanExpression FALSE = BooleanLiteral.FALSE;

    public Expressions(ExpressionsFactory factory) {
        this.factory = factory;
    }

    public BooleanExpression not(BooleanExpression expression) {
        return factory.createBoolean(NOT, expression);
    }

    public BooleanExpression exits(Object expression) {
        return factory.createBoolean(EXISTS, e(expression));
    }

    public BooleanExpression notExits(Object expression) {
        return factory.createBoolean(NOT_EXISTS, e(expression));
    }

    // TODO numericExpr als Param
    public NumericExpression sum(NumericExpression expression) {
        return factory.createNumeric(SUM, expression);
    }

    public NumericExpression sum(Number number) {
        return sum(factory.createNumeric(number));
    }

    public NumericExpression avg(NumericExpression expression) {
        return factory.createNumeric(AVG, expression);
    }

    public NumericExpression avg(Number number) {
        return avg(factory.createNumeric(number));
    }

    public NumericExpression round(NumericExpression expression) {
        return factory.createNumeric(ROUND, expression);
    }

    public NumericExpression toNumber(NumericExpression expression) {
        return factory.createNumeric(TO_NUMBER, expression);
    }

    public UnaryExpression min(Expression expression) {
        return factory.createUnary(MIN, expression);
    }

    public UnaryExpression min(Object expression) {
        return min(e(expression));
    }

    public UnaryExpression max(Expression expression) {
        return factory.createUnary(MAX, expression);
    }

    public UnaryExpression max(Object expression) {
        return max(e(expression));
    }

    public <E extends Expression> UnaryExpression<E> count(E expression) {
        return factory.createUnary(COUNT, expression);
    }

    public UnaryExpression<NumericLiteral> count(Number number) {
        return count(factory.createNumeric(number));
    }

    public Expression count() {
        return count(factory.sql("*"));
    }

    public Expression e(Object... expression) {
        Collection<Expression> list = new ArrayList<Expression>(expression.length);
        for (Object o : expression) {
            list.add(e(o));
        }
        return new SimpleListExpression(Delimeter.COMMA, list);
    }

    public <V> LiteralExpression<V> e(V value) {
        return factory.create(value);
    }

    public LiteralExpression<String> sql(String sqlString) {
        return factory.sql(sqlString);
    }

    private Collection<Expression> createExpressionCollection(Iterable<?> expression) {
        if (expression == null) {
            return Collections.emptyList();
        }
        Collection<Expression> expressions = new LinkedList<Expression>();
        for (Object element : expression) {
            expressions.add(e(element));
        }
        return expressions;
    }

    public ParamExpression named(String paramName) {
        return factory.createParam(paramName);
    }

    public <T> ParamExpression<T> named(String paramName, T paramValue) {
        return factory.createParam(paramName, paramValue);
    }

    public <T> ParamExpression<Collection<T>> named(String paramName, T... paramValues) {
        return factory.<Collection<T>>createParam(paramName, asList(paramValues));
    }

    public <T> ParamExpression<T> param(T paramValue) {
        return factory.createParam(paramValue);
    }

    public <T> ParamExpression<Collection<T>> param(T... paramValues) {
        return factory.<Collection<T>>createParam(asList(paramValues));
    }

    public NumericPathExpression pathNumeric(String path) {
        return factory.createNumericPath(path);
    }

    public PathExpression path(String path) {
        return factory.path(path);
    }

    public BooleanLiteral boolTrue() {
        return factory.boolTrue();
    }

    public BooleanLiteral boolFalse() {
        return factory.boolFalse();
    }

    public BooleanLiteral boolNull() {
        return factory.boolNull();
    }
}
