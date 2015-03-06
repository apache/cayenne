package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;
import de.jexp.jequel.literals.Operator;

public abstract class AbstractExpression implements Expression {
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
        BooleanExpression andExpression = Expressions.createBinaryBooleanExpression(e(start), Operator.AND, end);
        return createBinaryBooleanExpression(Operator.BETWEEN, andExpression);
    }

    public BooleanExpression like(Object expression) {
        return createBinaryBooleanExpression(Operator.LIKE, expression);
    }

    public BooleanExpression is(Object expression) {
        return createBinaryBooleanExpression(Operator.IS, expression);
    }

    public BooleanExpression isNot(Object expression) {
        return createBinaryBooleanExpression(Operator.IS_NOT, expression);
    }

    public BooleanExpression in(Object... expressions) {
        return createBinaryBooleanExpression(Operator.IN, e(expressions));
    }

    public NumericBinaryExpression plus(Object expression) {
        return createBinaryNumericExpression(Operator.PLUS, expression);
    }

    public NumericBinaryExpression minus(Object expression) {
        return createBinaryNumericExpression(Operator.MINUS, expression);
    }

    public NumericBinaryExpression times(Object expression) {
        return createBinaryNumericExpression(Operator.TIMES, expression);
    }

    public NumericBinaryExpression by(Object expression) {
        return createBinaryNumericExpression(Operator.BY, expression);
    }

    protected BooleanBinaryExpression createBinaryBooleanExpression(Operator operator, Object expression) {
        return Expressions.createBinaryBooleanExpression(this, operator, expression);
    }

    protected NumericBinaryExpression createBinaryNumericExpression(Operator operator, Object expression) {
        return new NumericBinaryExpression(this, operator, e(expression));
    }

    public <K> void process(ExpressionProcessor<K> expressionProcessor) {
    }

    public boolean isParenthesed() {
        return false;
    }

}
