package de.jexp.jequel.expression;

import de.jexp.jequel.literals.UnaryOperator;

public class BooleanUnaryExpression extends BooleanConstantExpression {
    private final UnaryExpression unaryExpression;

    public BooleanUnaryExpression(UnaryOperator operator, Expression first) {
        this.unaryExpression = new UnaryExpression(operator, first);
    }

    public String toString() {
        return EXPRESSION_FORMAT.visit(this);
    }

    public UnaryExpression getUnaryExpression() {
        return unaryExpression;
    }

    public boolean isAtomic() {
        return false;
    }
}