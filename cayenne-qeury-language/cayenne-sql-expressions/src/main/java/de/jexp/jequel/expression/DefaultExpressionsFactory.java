package de.jexp.jequel.expression;

import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.Operator;
import de.jexp.jequel.literals.UnaryOperator;

public class DefaultExpressionsFactory implements ExpressionsFactory {

    @Override
    public NumericLiteral createNumeric(Number value) {
        return new NumericLiteral(value).factory(this);
    }

    @Override
    public NumericUnaryExpression createNumeric(UnaryOperator operator, NumericExpression first) {
        return new NumericUnaryExpression(operator, first).factory(this);
    }

    @Override
    public NumericBinaryExpression createNumeric(Operator operator, NumericExpression first, NumericExpression second) {
        return new NumericBinaryExpression(createBinary(first, operator, second));
    }

    @Override
    public BooleanLiteral boolTrue() {
        return BooleanLiteral.TRUE.factory(this);
    }

    @Override
    public BooleanLiteral boolFalse() {
        return BooleanLiteral.FALSE.factory(this);
    }

    @Override
    public BooleanLiteral boolNull() {
        return BooleanLiteral.NULL.factory(this);
    }

    @Override
    public BooleanUnaryExpression createBoolean(UnaryOperator operator, Expression first) {
        return new BooleanUnaryExpression(operator, first).factory(this);
    }

    @Override
    public BooleanBinaryExpression createBoolean(Operator operator, Expression first, Expression second) {
        return new BooleanBinaryExpression(createBinary(first, operator, second));
    }

    @Override
    public BooleanListExpression createBooleanList(Operator operator, BooleanExpression... expressions) {
        return new BooleanListExpression(operator, expressions).factory(this);
    }

    @Override
    public StringLiteral create(String value) {
        return new StringLiteral(value).factory(this);
    }

    @Override
    public <E extends Expression> UnaryExpression<E> createUnary(UnaryOperator operator, E exp) {
        return new UnaryExpression<E>(operator, exp).factory(this);
    }

    @Override
    public <E extends Expression> BinaryExpression<E> createBinary(E first, Operator operator, E second) {
        return new BinaryExpression<E>(first, operator, second).factory(this);
    }

    @Override
    public SimpleListExpression create(Delimeter delim, Expression... expressions) {
        return new SimpleListExpression(delim, expressions).factory(this);
    }

    @Override
    public <V> LiteralExpression<V> create(V value) {
        if (value == null) {
            return (LiteralExpression<V>) boolNull();
        }

        if (value instanceof Boolean) {
            return (LiteralExpression<V>) (((Boolean) value) ? boolTrue() : boolFalse());
        }

        if (value instanceof Number) {
            return (LiteralExpression<V>) createNumeric((Number) value);
        }

        return (LiteralExpression<V>) create(value.toString());
    }

    @Override
    public ParamExpression createParam(String paramName) {
        return new ParamExpression(paramName).factory(this);
    }

    @Override
    public <T> ParamExpression<T> createParam(String paramName, T paramValue) {
        return new ParamExpression<T>(paramName, paramValue).factory(this);
    }

    @Override
    public <T> ParamExpression<T> createParam(T paramValue) {
        return new ParamExpression<T>(paramValue).factory(this);
    }

    @Override
    public NumericPathExpression createNumericPath(String path) {
        return new NumericPathExpression(path).factory(this);
    }

    @Override
    public PathExpression path(String path) {
        return new StringPathExpression(path).factory(this);
    }

    @Override
    public SqlLiteral sql(String sql) {
        return new SqlLiteral(sql).factory(this);
    }
}
