package de.jexp.jequel.expression;

import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.Operator;

public abstract class AbstractExpression implements Expression {
    private ExpressionsFactory factory;

    @Override
    public BooleanExpression eq(Object expression) {
        return createBinaryBooleanExpression(Operator.EQ, expression);
    }

    @Override
    public BooleanExpression ge(Object expression) {
        return createBinaryBooleanExpression(Operator.GE, expression);
    }

    @Override
    public BooleanExpression gt(Object expression) {
        return createBinaryBooleanExpression(Operator.GT, expression);
    }

    @Override
    public BooleanExpression lt(Object expression) {
        return createBinaryBooleanExpression(Operator.LT, expression);
    }

    @Override
    public BooleanExpression le(Object expression) {
        return createBinaryBooleanExpression(Operator.LE, expression);
    }

    @Override
    public BooleanExpression ne(Object expression) {
        return createBinaryBooleanExpression(Operator.NE, expression);
    }

    @Override
    public <E> BooleanExpression between(E start, E end) {
        LiteralExpression<E> e1 = factory().create(start);
        LiteralExpression<E> e2 = factory().create(end);
        return createBinaryBooleanExpression(Operator.BETWEEN,
                factory().createBinary(e1, Operator.AND, e2));
    }

    @Override
    public BooleanExpression in(Expression subQuery) {
        return createBinaryBooleanExpression(Operator.IN, subQuery);
    }

    @Override
    public BooleanExpression in(Object... expressions) {
        Expression[] e = new Expression[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            Object expression = expressions[i];
            if (expression instanceof Expression) {
                e[i] = (Expression) expression;
            } else {
                e[i] = factory().create(expression);
            }
        }

        return createBinaryBooleanExpression(Operator.IN, factory().create(Delimeter.COMMA, e));
    }

    @Override
    public BooleanExpression isNull() {
        return factory().createBoolean(Operator.IS, this, BooleanLiteral.NULL);
    }

    @Override
    public BooleanExpression isNotNull() {
        return factory().createBoolean(Operator.IS_NOT, this, BooleanLiteral.NULL);
    }

    protected BooleanBinaryExpression createBinaryBooleanExpression(Operator operator, Expression expression) {
        return factory().createBoolean(operator, this, expression);
    }

    protected BooleanBinaryExpression createBinaryBooleanExpression(Operator operator, Object expression) {
        if (expression instanceof Expression) {
            return createBinaryBooleanExpression(operator, (Expression) expression);
        }
        return createBinaryBooleanExpression(operator, factory().create(expression));
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
    }

    protected <T extends Expression> T factory(ExpressionsFactory factory) {
        this.factory = factory;

        return (T) this;
    }

    @Override
    public ExpressionsFactory factory() {
        return factory;
    }
}
