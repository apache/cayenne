package de.jexp.jequel.sql;

import de.jexp.jequel.expression.*;

import java.util.Collection;

public abstract class Expressions {

    public static final de.jexp.jequel.Expressions EXPRESSIONS
            = new de.jexp.jequel.Expressions(new DefaultExpressionsFactory());

    public static final LiteralExpression<String> STAR = sql("*");

    public static final BooleanExpression TRUE = EXPRESSIONS.boolTrue();
    public static final BooleanExpression FALSE = EXPRESSIONS.boolFalse();
    public static final BooleanExpression NULL = EXPRESSIONS.boolNull();

    public static BooleanExpression not(BooleanExpression expression) {
        return EXPRESSIONS.not(expression);
    }

    public static BooleanExpression exits(Object expression) {
        return EXPRESSIONS.exits(expression);
    }

    public static BooleanExpression notExits(Object expression) {
        return EXPRESSIONS.notExits(expression);
    }

    public static NumericExpression sum(NumericExpression expression) {
        return EXPRESSIONS.sum(expression);
    }

    public static NumericExpression sum(Number number) {
        return EXPRESSIONS.sum(number);
    }

    public static NumericExpression avg(NumericExpression expression) {
        return EXPRESSIONS.avg(expression);
    }

    public static NumericExpression avg(Number number) {
        return EXPRESSIONS.avg(number);
    }

    public static NumericExpression round(NumericExpression expression) {
        return EXPRESSIONS.round(expression);
    }

    public static NumericExpression toNumber(NumericExpression expression) {
        return EXPRESSIONS.toNumber(expression);
    }

    public static UnaryExpression min(Expression expression) {
        return EXPRESSIONS.min(expression);
    }

    public static UnaryExpression min(Object expression) {
        return EXPRESSIONS.min(expression);
    }

    public static UnaryExpression max(Expression expression) {
        return EXPRESSIONS.max(expression);
    }

    public static UnaryExpression max(Object expression) {
        return EXPRESSIONS.max(expression);
    }

    public static <E extends Expression> UnaryExpression<E> count(E expression) {
        return EXPRESSIONS.count(expression);
    }

    public static UnaryExpression<NumericLiteral> count(Number number) {
        return EXPRESSIONS.count(number);
    }

    public static Expression count() {
        return EXPRESSIONS.count();
    }

    public static Expression e(Object... expression) {
        return EXPRESSIONS.e(expression);
    }

    public static <V> LiteralExpression<V> e(V value) {
        return EXPRESSIONS.e(value);
    }

    public static LiteralExpression<String> sql(String sqlString) {
        return EXPRESSIONS.sql(sqlString);
    }

    public static ParamExpression named(String paramName) {
        return EXPRESSIONS.named(paramName);
    }

    public static <T> ParamExpression<T> named(String paramName, T paramValue) {
        return EXPRESSIONS.named(paramName, paramValue);
    }

    public static <T> ParamExpression<Collection<T>> named(String paramName, T... paramValues) {
        return EXPRESSIONS.named(paramName, paramValues);
    }

    public static <T> ParamExpression<T> param(T paramValue) {
        return EXPRESSIONS.param(paramValue);
    }

    public static <T> ParamExpression<Collection<T>> param(T... paramValues) {
        return EXPRESSIONS.param(paramValues);
    }

    public static NumericPathExpression pathNumeric(String path) {
        return EXPRESSIONS.pathNumeric(path);
    }

    public static PathExpression path(String path) {
        return EXPRESSIONS.path(path);
    }

    public static BooleanLiteral boolTrue() {
        return EXPRESSIONS.boolTrue();
    }

    public static BooleanLiteral boolFalse() {
        return EXPRESSIONS.boolFalse();
    }

    public static BooleanLiteral boolNull() {
        return EXPRESSIONS.boolNull();
    }
}
