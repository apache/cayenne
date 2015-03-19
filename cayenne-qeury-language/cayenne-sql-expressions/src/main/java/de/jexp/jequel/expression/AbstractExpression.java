package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;

import de.jexp.jequel.expression.logical.BooleanBinaryExpression;
import de.jexp.jequel.expression.logical.BooleanExpression;
import de.jexp.jequel.expression.logical.BooleanLiteral;
import de.jexp.jequel.literals.Operator;

public abstract class AbstractExpression implements Expression {
    private ExpressionsFactory factory;

    public BooleanExpression eq(Object expression) {
        return createBinaryBooleanExpression(Operator.EQ, expression);
    }

    public BooleanExpression ge(Object expression) {
        return createBinaryBooleanExpression(Operator.GE, expression);
    }

    public BooleanExpression gt(Object expression) {
        return createBinaryBooleanExpression(Operator.GT, expression);
    }

    public BooleanExpression lt(Object expression) {
        return createBinaryBooleanExpression(Operator.LT, expression);
    }

    public BooleanExpression le(Object expression) {
        return createBinaryBooleanExpression(Operator.LE, expression);
    }

    public BooleanExpression ne(Object expression) {
        return createBinaryBooleanExpression(Operator.NE, expression);
    }

    public BooleanExpression between(Object start, Object end) {
        BooleanExpression andExpression = createBinaryBooleanExpression(e(start), Operator.AND, end);
        return createBinaryBooleanExpression(Operator.BETWEEN, andExpression);
    }

    public BooleanExpression like(Object expression) {
        return createBinaryBooleanExpression(Operator.LIKE, expression);
    }

    public BooleanExpression in(Expression subQuery) {
        return createBinaryBooleanExpression(Operator.IN, subQuery);
    }

    public BooleanExpression in(Object... expressions) {
        return createBinaryBooleanExpression(Operator.IN, e(expressions));
    }

    @Override
    public BooleanExpression isNull() {
        return factory().createBoolean(this, Operator.IS, BooleanLiteral.NULL);
    }

    @Override
    public BooleanExpression isNotNull() {
        return factory().createBoolean(this, Operator.IS_NOT, BooleanLiteral.NULL);
    }

    protected BooleanBinaryExpression createBinaryBooleanExpression(Operator operator, Object expression) {
        return createBinaryBooleanExpression(this, operator, expression);
    }

    protected static BooleanBinaryExpression createBinaryBooleanExpression(Expression first, Operator operator, Object expression) {
        return new BooleanBinaryExpression(first, operator, e(expression));
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
    }

    public void setFactory(ExpressionsFactory factory) {
        this.factory = factory;
    }

    @Override
    public ExpressionsFactory factory() {
        return factory;
    }
}
